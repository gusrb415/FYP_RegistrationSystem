package viewmodel.panes

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import main.Helper
import main.SecurityHelper
import viewmodel.Config
import viewmodel.SceneManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ViewHospitalsPane : BorderPane() {
    private val hospitalList = FXCollections.observableArrayList<HBox>()
    private val listView = ListView(hospitalList)
    private val nameToIdentityMap = mutableMapOf<String, String>()
    private val backButton = Button("Back To Main")
    private val warningText = Label()

    init {
        listView.isFocusTraversable = false
        listView.isMouseTransparent = false
        listView.fixedCellSize = 50.0
        connectComponents()
        styleComponents()
        setCallbacks()
    }

    fun loadList() {
        hospitalList.clear()
        val response = "${Config.BASE_URL}/medical-org/get-authorization-list"
            .httpGet()
            .header(
                mapOf(
                    "Content-Type" to "application/json; charset=utf-8",
                    "Authorization" to Helper.token
                )
            )
            .responseString()
        if(response.second.statusCode == 200) {
            val jsonArray = Parser().parse(StringBuilder(response.third.component1())) as JsonArray<*>
            jsonArray.forEach {
                it as JsonObject
                val name = it["name"] as String
                nameToIdentityMap[name] = it["identifier"] as String
                val getButton = Button("Get Certificate")
                val revokeButton = Button("Revoke Certificate")
                val renewButton = Button("Renew Certificate")
                val buttonBox = HBox(20.0, getButton, revokeButton, renewButton)
                buttonBox.alignment = Pos.CENTER
                val tempBox = HBox(150.0, Label(it["name"] as String), buttonBox)
                tempBox.alignment = Pos.CENTER_RIGHT
                hospitalList.add(tempBox)

                getButton.setOnAction {
                    val dirChooser = DirectoryChooser()
                    dirChooser.title = "Choose directory to export"
                    dirChooser.initialDirectory = File(System.getProperty("user.home"))
                    val directory = dirChooser.showDialog(SceneManager.stage)
                    if(directory != null) {
                        val btnResponse = "${Config.BASE_URL}/medical-org/get-certificate"
                            .httpPost()
                            .header(mapOf(
                                "Content-Type" to "application/json; charset=utf-8",
                                "Authorization" to Helper.token))
                            .body("""{"content":"${nameToIdentityMap[name]}"}""", Charsets.UTF_8)
                            .responseString()
                        if(btnResponse.second.statusCode == 200) {
                            val jsonData = Parser().parse(StringBuilder(btnResponse.third.component1()!!)) as JsonObject
                            val data = Helper.decodeFromString(jsonData["content"] as String)
                            val cert = SecurityHelper.getX509FromBytes(data)
                            SecurityHelper.writeX509ToDER(cert, File("${directory.absolutePath}\\$name.cer"))
                        } else {
                            println(btnResponse)
                        }
                    }
                }

                revokeButton.setOnAction {
                    val btnResponse = "${Config.BASE_URL}/medical-org/revoke"
                        .httpPut()
                        .header(
                            mapOf(
                                "Content-Type" to "application/json; charset=utf-8",
                                "Authorization" to Helper.token
                            )
                        )
                        .body("""{
                            "content": "${nameToIdentityMap[name]}"
                        }""".replace("\\s".toRegex(), ""), Charsets.UTF_8)
                        .responseString()
                    if(btnResponse.second.statusCode == 200 && btnResponse.third.component1()!!.toInt() == 0) {
                        for(hBox in hospitalList) {
                            if((hBox.children[0] as Label).text == name) {
                                hospitalList.remove(hBox)
                                break
                            }
                        }
                        nameToIdentityMap.remove(name)
                    } else {
                        println(btnResponse)
                    }
                }

                renewButton.setOnAction {
                    val expiry = Label("Expiry date")
                    val newDate = TextField()
                    newDate.promptText = "YYYY-MM-DD"
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dateFormat.timeZone = TimeZone.getDefault()
                    newDate.text = dateFormat.format(Date(System.currentTimeMillis() + (86_400_000 * (365.25 * 2 - 1)).toLong()))
                    val okBtn = Button("OK")
                    val bottomBox = HBox(okBtn)
                    bottomBox.alignment = Pos.CENTER
                    val centerBox = HBox(20.0, expiry, newDate)
                    centerBox.alignment = Pos.CENTER
                    val warningLabel = Label()
                    warningLabel.styleClass.add("warning-text")
                    warningLabel.visibleProperty().set(false)
                    val wrapperBox = VBox(20.0, centerBox, warningLabel, bottomBox)
                    wrapperBox.alignment = Pos.CENTER
                    val newPane = BorderPane(wrapperBox)
                    val newScene = Scene(newPane, Config.WIDTH / 4, Config.HEIGHT / 4)
                    newScene.stylesheets.add(Config.CSS_STYLES)
                    val newStage = Stage()
                    newStage.title = "Renew Certificate"
                    newStage.scene = newScene
                    newStage.show()
                    okBtn.setOnAction {
                        if(newDate.text != "") {
                            warningLabel.text = ""
                            warningLabel.visibleProperty().set(false)
                            val btnResponse = "${Config.BASE_URL}/medical-org/renew-certificate"
                                .httpPost()
                                .header(
                                    mapOf(
                                        "Content-Type" to "application/json; charset=utf-8",
                                        "Authorization" to Helper.token
                                    )
                                )
                                .body("""{
                                    "identifier": "${nameToIdentityMap[name]}",
                                    "noAfter": "${newDate.text}"
                                }""".replace("\\s".toRegex(), ""), Charsets.UTF_8)
                                .responseString()
                            if(btnResponse.second.statusCode == 200) {
                                newStage.close()
                            } else {
                                warningLabel.text = "Please input the correct date"
                                warningLabel.visibleProperty().set(true)
                            }
                        } else {
                            warningLabel.text = "Please input the date"
                        }
                    }
                }
            }
        } else {
            println("LOADING HOSPITAL LIST FAILED: ${response.third.component2()}")
        }
    }

    private fun connectComponents() {
        listView.minWidth = Config.WIDTH * 0.8
        val centerBox = HBox(30.0, listView)
        centerBox.padding = Insets(20.0, 20.0, 0.0, 20.0)
        centerBox.alignment = Pos.CENTER
        this.center = centerBox
        val bottomBox = HBox(30.0, Label(), backButton, warningText)
        bottomBox.padding = Insets(20.0, 20.0, 20.0, 20.0)
        bottomBox.alignment = Pos.CENTER
        this.bottom = bottomBox
    }

    private fun styleComponents() {
        warningText.styleClass.add("warning-text")
    }

    private fun setCallbacks() {
        backButton.setOnAction {
            SceneManager.showMainMenuScene()
        }
    }
}