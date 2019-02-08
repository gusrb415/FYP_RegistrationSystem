package viewmodel.panes

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpPut
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import main.Helper
import main.SecurityHelper
import viewmodel.Config
import viewmodel.SceneManager
import java.io.File
import java.lang.Exception
import java.lang.StringBuilder
import java.security.PublicKey
import java.text.SimpleDateFormat
import java.util.*

object AddHospitalPane : BorderPane() {
    private val hospitalName = TextField()
    private val hospitalNameLabel = Label("Medical Organization Name")
    private val publicKeyImport = Button("Import public key")
    private val publicKeyImportLabel = Label("Medical Organization Public Key")
    private val dateNoAfter = TextField()
    private val dateNoAfterLabel = Label("Validate until")
    private val warningText = Label()
    private val addButton = Button("Add")
    private val backButton = Button("Back To Main")
    private var publicKey : PublicKey? = null
    private var publicKeyFile : File? = null

    init {
        hospitalName.promptText = "Medical Organization Name"
        dateNoAfter.promptText = "YYYY-MM-DD"
        connectComponents()
        styleComponents()
        setCallbacks()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getDefault()
        dateNoAfter.text = dateFormat.format(Date(System.currentTimeMillis() + (86_400_000 * (365.25 * 2 - 1)).toLong()))
    }

    private fun connectComponents() {
        val container = VBox(30.0)
        container.alignment = Pos.CENTER
        container.children.addAll(
            HBox(30.0, hospitalNameLabel, hospitalName),
            HBox(30.0, publicKeyImportLabel, publicKeyImport),
            HBox(30.0, dateNoAfterLabel, dateNoAfter),
            HBox(30.0, warningText)
        )
        container.children.forEach {
            (it as HBox).alignment = Pos.CENTER
        }
        this.center = container
        val bottomBox = HBox(30.0, backButton, addButton)
        bottomBox.alignment = Pos.CENTER
        bottomBox.padding = Insets(20.0, 20.0, 20.0, 20.0)
        this.bottom = bottomBox
    }

    private fun styleComponents() {
        warningText.styleClass.add("warning-text")
    }

    private fun setCallbacks() {
        publicKeyImport.setOnAction {
            val fc = FileChooser()
            fc.title = "Public Key Import"
            fc.initialDirectory = File(System.getProperty("user.home"))
            fc.extensionFilters.clear()
            fc.extensionFilters.addAll(FileChooser.ExtensionFilter("pem files", "*.pem"))

            val file = fc.showOpenDialog(SceneManager.stage)
            if(file != null) {
                publicKeyFile = file
                publicKey = SecurityHelper.getPublicKeyFromPEM(file, "EC")
            }
        }

        addButton.setOnAction {
            warningText.visibleProperty().set(true)
            when {
                hospitalName.text == "" -> {
                    hospitalName.requestFocus()
                    warningText.text = "Please input name of the medical organization"
                }
                publicKey == null -> {
                    warningText.text = "Please import the medical organization's public key"
                }
                dateNoAfter.text == "" -> {
                    dateNoAfter.requestFocus()
                    warningText.text = "Please input correct date"
                }
                else -> {
                    warningText.visibleProperty().set(true)
                    val response =
                        "${Config.BASE_URL}/medical-org/authorize"
                            .httpPut()
                            .header(
                                mapOf(
                                    "Content-Type" to "application/json; charset=utf-8",
                                    "Authorization" to Helper.token
                                )
                            )
                            .body("""{
                                "medicalOrgInfo": {
                                    "name": "${hospitalName.text}",
                                    "ecPublicKey": "${Helper.encodeToString(publicKey!!.encoded)}",
                                    "keyDEREncoded": true
                                },
                                "noAfter": "${dateNoAfter.text}"
                            }""".replace("\\s".toRegex(), ""), Charsets.UTF_8)
                            .responseString()
                    if(response.second.statusCode == 200) {
                        try {
                            val jsonData = Parser().parse(StringBuilder(response.third.component1()!!)) as JsonObject
                            val data = Helper.decodeFromString(jsonData["content"] as String)

                            if (data[0].toInt() != 0)
                                throw IllegalStateException("FAILED TO REGISTER: ${data[0].toInt()}")

                            val certBytes = Arrays.copyOfRange(data, 1, data.size)
                            val cert = SecurityHelper.getX509FromBytes(certBytes)
                            SecurityHelper.writeX509ToDER(
                                cert,
                                File("${publicKeyFile!!.parentFile.absolutePath}${hospitalName.text}.cer")
                            )

                            val alert = Alert(Alert.AlertType.CONFIRMATION)
                            alert.title = "Certificate Saved"
                            alert.contentText = "Certificate saved in the public key directory"
                            alert.dialogPane.setPrefSize(300.0, 100.0)
                            alert.showAndWait()
                        } catch (e: Exception) {
                            println(e.message)
                        }
                    } else {
                        println(response)
                    }
                    SceneManager.showMainMenuScene()
                }
            }
        }

        backButton.setOnAction {
            SceneManager.showMainMenuScene()
        }
    }
}