package viewmodel.panes

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import javafx.collections.FXCollections
import main.SecurityHelper
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import main.Helper
import viewmodel.Config
import viewmodel.SceneManager
import xyz.medirec.medirec.pojo.KeyTime
import xyz.medirec.medirec.pojo.PatientIdentity
import java.lang.Exception
import java.security.interfaces.ECPublicKey
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.spec.SecretKeySpec


class PatientInfoPane(private val keyTime: KeyTime, private val isViewOnly: Boolean) : BorderPane() {
    private val container = VBox(30.0)
    private val nameLabel = Label("Name")
    private val name = TextField()
    private val genderLabel = Label("Gender")
    private val gender = TextField()
    private val birthDateLabel = Label("Date of Birth")
    private val birthDate = TextField()
    private val identificationLabel = Label("ID Number")
    private val identification = TextField()
    private val bloodTypeLabel = Label("Blood Type")
    private val bloodType = TextField()
    private val weightLabel = Label("Weight")
    private val weight = TextField()
    private val heightLabel = Label("Height")
    private val height = TextField()
    private val cancelButton = Button("Cancel")
    private val createRecordButton = Button("Create Patient Record")
    private val errorLabel = Label()
    private val timeList = FXCollections.observableArrayList<String>()
    private val listView = ListView<String>(timeList)
    private val timeToRecord = mutableMapOf<String, PatientIdentity>()
    var allRecordsRaw: Array<String>? = null
    var allRecordsTimestamp: LongArray? = null
    var info : String? = null

    init {
        allRecordsRaw = findRecord(Helper.generatePublicKey(keyTime.pubKeyEncoded))
        if(allRecordsRaw != null) fillUpData()
        decideEditable()
        if(isViewOnly) cancelButton.text = "Back To Menu"
        name.prefColumnCount = 50
        identification.prefColumnCount = 50
        errorLabel.visibleProperty().set(false)
        connectComponents()
        styleComponents()
        setCallbacks()
    }

    private fun decideEditable() {
        name.editableProperty().set(!isViewOnly)
        gender.editableProperty().set(!isViewOnly)
        birthDate.editableProperty().set(!isViewOnly)
        identification.editableProperty().set(!isViewOnly)
        bloodType.editableProperty().set(!isViewOnly)
        weight.editableProperty().set(!isViewOnly)
        height.editableProperty().set(!isViewOnly)
    }

    private fun fillUpData() {
        try {
            val gson = Gson()
            for (i in 0 until allRecordsTimestamp!!.size) {
                for (j in 0 until keyTime.timeList.size) {
                    if (allRecordsTimestamp!![i] == keyTime.timeList[j]) {
                        val key = SecretKeySpec(keyTime.secretKeysEncoded[j], "AES")
                        val recordJson = String(
                            SecurityHelper.decryptAES(
                                Helper.decodeFromString(allRecordsRaw!![i]), key, ByteArray(16)
                            )
                        )

                        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z", Locale.getDefault())
                        dateFormat.timeZone = TimeZone.getDefault()
                        val timeString = dateFormat.format(Date(keyTime.timeList[j]))
                        timeList.add(timeString)
                        timeToRecord[timeString] = gson.fromJson(recordJson, PatientIdentity::class.java)
                        break
                    }
                }
            }
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
    }

    private fun findRecord(publicKey: ECPublicKey) : Array<String>? {
        val response = "${Config.BASE_URL}/patient/get-patient-short-info-list"
            .httpPost()
            .header(mapOf("Content-Type" to "application/json; charset=utf-8", "Authorization" to Helper.token))
            .body("""{
                "content": "${Helper.encodeToString(getIdentifier(publicKey))}"
                }""".replace("\\s".toRegex(), ""), Charsets.UTF_8)
            .responseString()
//        println(response)
        return if(response.second.statusCode == 200) {
            val resultArr = Parser().parse(StringBuilder(response.third.component1()!!)) as JsonArray<*>
            val blockHashList = mutableListOf<String>()
            val targetIdentifierList = mutableListOf<String>()
            val timestampList = mutableListOf<Long>()
            for(each in resultArr) {
                each as JsonObject
                blockHashList.add((each["location"] as JsonObject)["blockHash"] as String)
                targetIdentifierList.add((each["location"] as JsonObject)["targetIdentifier"] as String)
                timestampList.add(each["timestamp"] as Long)
            }
            if(resultArr.size > 0) {
                allRecordsTimestamp = timestampList.toLongArray()
                findRecordDetails(blockHashList.toTypedArray(), targetIdentifierList.toTypedArray())
            } else null
        } else null
    }

    private fun findRecordDetails(blockHash: Array<String>, targetIdentifier: Array<String>) : Array<String>? {
        val sb = StringBuilder()
        for(i in 0 until blockHash.size)
            sb.append("""{ "blockHash": "${blockHash[i]}", "targetIdentifier": "${targetIdentifier[i]}" }""")
                .append(if(i + 1 != blockHash.size) "," else "")
        val response = "${Config.BASE_URL}/patient/get-patient-info-content-list"
            .httpPost()
            .header(mapOf("Content-Type" to "application/json; charset=utf-8", "Authorization" to Helper.token))
            .body("""[
                $sb
            ]""".replace("\\s".toRegex(), ""), Charsets.UTF_8).responseString()
//        println(response)
        return if(response.second.statusCode == 200) {
            val resultList = mutableListOf<String>()
            val resultArr = Parser().parse(StringBuilder(response.third.component1()!!)) as JsonArray<*>
            resultArr.forEach {
                it as JsonObject
                resultList.add(it["encryptedInfo"] as String)
            }
            resultList.toTypedArray()
        } else null
    }

    private fun getIdentifier(publicKey: ECPublicKey) : ByteArray {
        var hash = ByteArray(0)
        try {
            hash = SecurityHelper.hash(
                SecurityHelper.getCompressedRawECPublicKey(publicKey),
                Config.BLOCKCHAIN_HASH_ALGORITHM
            )
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        return Arrays.copyOfRange(hash, hash.size - Config.IDENTIFIER_LENGTH, hash.size)
    }

    private fun connectComponents() {
        container.alignment = Pos.CENTER
        val buttonBar = HBox(30.0, cancelButton)
        if(!isViewOnly) buttonBar.children.add(createRecordButton)
        container.children.addAll(
            HBox(30.0, nameLabel, name),
            HBox(30.0, genderLabel, gender),
            HBox(30.0, birthDateLabel, birthDate),
            HBox(30.0, identificationLabel, identification),
            HBox(30.0, bloodTypeLabel, bloodType),
            HBox(30.0, weightLabel, weight),
            HBox(30.0, heightLabel, height),
            HBox(30.0, errorLabel),
            buttonBar
        )
        container.children.forEach{
            (it as HBox).alignment = Pos.CENTER
        }
        listView.setPrefSize(Config.WIDTH * 0.25, Config.HEIGHT * 0.85)
        val vBox = VBox(listView)
        vBox.alignment = Pos.CENTER
        this.left = vBox
        this.center = container
    }

    private fun styleComponents() {
        nameLabel.styleClass.add("small-size")
        identificationLabel.styleClass.add("small-size")
        errorLabel.styleClass.add("warning-text")
    }

    private fun setCallbacks() {
        cancelButton.setOnAction {
            SceneManager.showMainMenuScene()
        }
        createRecordButton.setOnAction {
            errorLabel.visibleProperty().set(true)
            when {
                name.text == "" -> {
                    name.requestFocus()
                    errorLabel.text = "Please enter the name"
                }
                gender.text == "" || !(gender.text == "M" || gender.text =="F") -> {
                    gender.requestFocus()
                    errorLabel.text = "Please enter the gender as 'M' or 'F'"
                }
                birthDate.text == "" -> {
                    birthDate.requestFocus()
                    errorLabel.text = "Please enter the date of birth"
                }
                identification.text == "" -> {
                    identification.requestFocus()
                    errorLabel.text = "Please enter the identification number"
                }
                bloodType.text == "" -> {
                    bloodType.requestFocus()
                    errorLabel.text = "Please enter the correct blood type"
                }
                weight.text.contains("[^0-9.]".toRegex()) -> {
                    weight.requestFocus()
                    errorLabel.text = "Please enter correct weight value"
                }
                height.text.contains("[^0-9.]".toRegex()) -> {
                    height.requestFocus()
                    errorLabel.text = "Please enter correct weight value"
                }
                else -> {
                    errorLabel.visibleProperty().set(false)
                    this.info = """{
                        "name": "${name.text}", "gender": "${gender.text}", "birthDate": "${birthDate.text}",
                        "identificationNumber": "${identification.text}", "bloodType": "${bloodType.text}",
                         "weight": ${weight.text}, "height": ${height.text}
                    }""".replace("\\s".toRegex(), "")
                    SceneManager.showMainMenuScene()
                }
            }
        }

        listView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            name.text = timeToRecord[newValue]?.name ?: ""
            gender.text = timeToRecord[newValue]?.gender ?: ""
            birthDate.text = timeToRecord[newValue]?.birthDate ?: ""
            identification.text = timeToRecord[newValue]?.identificationNumber ?: ""
            bloodType.text = timeToRecord[newValue]?.bloodType ?: ""
            weight.text = timeToRecord[newValue]?.weight.toString()
            height.text = timeToRecord[newValue]?.height.toString()
        }

        listView.selectionModel.selectLast()
    }
}