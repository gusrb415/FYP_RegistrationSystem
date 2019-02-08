package viewmodel.panes

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import main.Helper
import viewmodel.SceneManager

object QRCodePane : BorderPane() {
    private val container = VBox(50.0)
    private val qrView = ImageView()
    private val backButton = Button("Back")
    private val scanButton = Button("Scan Signature")

    init {
        connectComponents()
        styleComponents()
        setCallbacks()
    }

    fun drawQRCode(str: String) {
        Helper.drawQRCode(qrView, str)
    }

    private fun connectComponents() {
        val tempBox = HBox(30.0, backButton, scanButton)
        tempBox.alignment = Pos.CENTER
        container.children.addAll(
            qrView, tempBox
        )
        container.alignment = Pos.CENTER
        center = container
    }

    private fun styleComponents() {

    }

    private fun setCallbacks() {
        backButton.setOnAction {
            SceneManager.showMainMenuScene()
        }

        scanButton.setOnAction {
            SceneManager.showScanScene(ScanPane.TYPE.SIGNATURE)
        }
    }
}
