package viewmodel.panes

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpPost
import javafx.event.Event
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import main.Helper
import viewmodel.Config
import viewmodel.SceneManager
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.Paths
import java.security.KeyStore
import java.security.cert.CertificateFactory
import kotlin.text.Charsets.UTF_8


object LogInPane : BorderPane() {
    private val container: VBox = VBox(25.0)
    private val title = Label("SIGN IN")
    private val ipLabel = Label("IP Address")
    private val ssl = CheckBox("TLS/SSL")
    private val ip = TextField()
    private val certInvisibleLabel = Label("")
    private val certificateLabel = Label("Certificate")
    private val certificateBtn = Button("Import Certificate")
    private var certFile = Paths.get(Config.CERT_URL.toURI()).toFile()
    private val certFileLabel = Label("")
    private val usernameLabel = Label("Username")
    private val username = TextField()
    private val passwordLabel = Label("Password")
    private val invalidWarning = Label("")
    private val password = PasswordField()
    private val submitBtn = Button("Sign In")

    init {
        container.alignment = Pos.CENTER
        ip.promptText = "API IP Address"
        username.promptText = "Username"
        password.promptText = "Password"
        ip.text = "25.30.118.78"
        ssl.isSelected = true
        certFileLabel.text = certFile.name
        certInvisibleLabel.text = certFile.name
        invalidWarning.visibleProperty().value = false
        connectComponents()
        styleComponents()
        setCallbacks()
        username.text = "root"
        password.text = "1234"
    }

    private fun connectComponents() {
        val invisibleLabel = Label("TLS/SSL")
        invisibleLabel.isVisible = false
        val hBox0 = HBox(20.0, invisibleLabel, ipLabel, ip, ssl)
        hBox0.alignment = Pos.CENTER
        certInvisibleLabel.isVisible = false
        val hBox1 = HBox(20.0, certInvisibleLabel, certificateLabel, certificateBtn, certFileLabel)
        hBox1.alignment = Pos.CENTER
        val hBox2 = HBox(20.0, usernameLabel, username)
        hBox2.alignment = Pos.CENTER
        val hBox3 = HBox(20.0, passwordLabel, password)
        hBox3.alignment = Pos.CENTER
        container.children.addAll(
            title,
            hBox1, hBox0, hBox2, hBox3,
            invalidWarning,
            submitBtn
        )
        container.alignment = Pos.CENTER
        center = container
    }

    private fun styleComponents() {
        invalidWarning.styleClass.add("warning-text")
        certificateBtn.styleClass.add("small-button")
    }

    private fun setCallbacks() {
        setOnKeyPressed {
            if(it.code == KeyCode.ENTER)
                Event.fireEvent(
                    submitBtn, MouseEvent(MouseEvent.MOUSE_CLICKED, 0.0, 0.0, 0.0, 0.0,
                        MouseButton.PRIMARY, 1, true, true, true,
                        true, true, true, true,
                        true, true, true, null)
                )
        }

        certificateBtn.setOnAction {
            val fc = FileChooser()
            fc.title = "Import Certificate"
            fc.initialDirectory = File(System.getProperty("user.home"))
            fc.extensionFilters.clear()
            fc.extensionFilters.addAll(FileChooser.ExtensionFilter("cer files", "*.cer"))
            val file = fc.showOpenDialog(SceneManager.stage)
            if(file != null) {
                certFile = file
                certFileLabel.text = file.name
                certInvisibleLabel.text = file.name
            }
        }

        submitBtn.setOnMouseClicked {
            val ip = this.ip.text
            val username = this.username.text
            val password = this.password.text
            when {
                ip == "" || !ip.contains(Regex("[0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}")) -> {
                    this.ip.requestFocus()
                    invalidWarning.visibleProperty().value = true
                    invalidWarning.text = Config.IP_WARNING
                }
                username == "" -> {
                    this.username.requestFocus()
                    invalidWarning.visibleProperty().value = true
                    invalidWarning.text = Config.USERNAME_WARNING
                }
                password == "" -> {
                    this.password.requestFocus()
                    invalidWarning.visibleProperty().value = true
                    invalidWarning.text = Config.PASSWORD_WARNING
                }
                else -> {
                    FuelManager.instance.keystore = KeyStore.getInstance("pkcs12", Helper.getProvider())
                    val keyStore = FuelManager.instance.keystore!!
                    keyStore.load(null)
                    if(certFile != null) {
                        val fis = certFile.inputStream()
                        val bis = BufferedInputStream(fis)
                        val cf = CertificateFactory.getInstance("X.509")
                        while (bis.available() > 0) {
                            val cert = cf.generateCertificate(bis)
                            keyStore.setCertificateEntry("MediRec" + bis.available(), cert)
                        }
                        bis.close()
                    }

                    Config.BASE_URL = "http${if(ssl.isSelected) "s" else ""}://$ip/api"
                    val currentTime = System.currentTimeMillis()
                    val response = "${Config.BASE_URL}/user/login"
                        .httpPost()
                        .header(mapOf("Content-Type" to "application/json; charset=utf-8"))
                        .body("""{"username":"$username","password":"$password"}""", UTF_8)
                        .timeout(500)
                        .responseString()

                    if(response.third.component1() != null && response.third.component1() != "") {
                        invalidWarning.visibleProperty().value = false
                        Helper.token = response.third.component1()!!
                        SceneManager.showMainMenuScene()
                    } else {
                        invalidWarning.visibleProperty().value = true
                        invalidWarning.text =
                                if(System.currentTimeMillis() - currentTime > 500) Config.TIMEOUT else Config.WRONG_WARNING
                    }
                }
            }
        }
    }
}
