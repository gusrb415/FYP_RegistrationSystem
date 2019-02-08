package viewmodel.panes

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import viewmodel.Config
import viewmodel.SceneManager

object MainMenuPane : BorderPane() {
    private val userContainer = HBox(50.0)
    private val hospitalContainer = HBox(100.0)
    private val validatorContainer = HBox(50.0)
    private val userButtons = arrayOf(Button("Create/Update Record"), Button("Get Patient Key"), Button("View Record"))
    private val userImages = arrayOf(
        Image(Config.IMAGES["createRecord"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
        Image(Config.IMAGES["modifyRecord"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
        Image(Config.IMAGES["viewRecord"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true)
    )
    private val hospitalButtons = arrayOf(Button("Authorize Hospital"), Button("View Hospitals"))
    private val hospitalImages = arrayOf(
        Image(Config.IMAGES["addHospital"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
//        Image(Config.IMAGES["removeHospital"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
        Image(Config.IMAGES["viewHospitals"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true)
    )
    private val validatorButtons = arrayOf(Button("Add Validator"), Button("Vote Validator"), Button("View Validators"))
    private val validatorImages = arrayOf(
        Image(Config.IMAGES["addValidator"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
        Image(Config.IMAGES["voteValidator"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
        Image(Config.IMAGES["viewValidators"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true)
    )

    init {
        connectComponents()
        styleComponents()
        setCallbacks()
    }

    private fun connectComponents() {
        val navBar = VBox(20.0)
        navBar.alignment = Pos.CENTER
        val tabs = arrayOf(
            Tab("User Tool", userContainer),
            Tab("Hospital Tool", hospitalContainer),
            Tab("Validator Tool", validatorContainer)
        )
        tabs.forEach { it.closableProperty().set(false) }
        val tabPane = TabPane(*tabs)
        tabPane.tabMinWidth = Config.WIDTH / 3.0 - 25
        tabPane.tabMaxWidth = Config.WIDTH / 3.0 - 25
        tabPane.tabMinHeight = Config.HEIGHT / 12.0
        tabPane.tabMaxHeight = Config.HEIGHT / 12.0
        userContainer.alignment = Pos.CENTER
        hospitalContainer.alignment = Pos.CENTER
        validatorContainer.alignment = Pos.CENTER
        for(i in 0 until userImages.size) {
            val tempBox = VBox(30.0, ImageView(userImages[i]), userButtons[i])
            tempBox.alignment = Pos.CENTER
            userContainer.children.add(tempBox)
        }
        for(i in 0 until hospitalImages.size) {
            val tempBox = VBox(30.0, ImageView(hospitalImages[i]), hospitalButtons[i])
            tempBox.alignment = Pos.CENTER
            hospitalContainer.children.add(tempBox)
        }
        for(i in 0 until validatorImages.size) {
            val tempBox = VBox(30.0, ImageView(validatorImages[i]), validatorButtons[i])
            tempBox.alignment = Pos.CENTER
            validatorContainer.children.add(tempBox)
        }
        this.center = tabPane
    }

    private fun styleComponents() {

    }

    private fun setCallbacks() {
        userCallbacks()
        hospitalCallbacks()
        validatorCallbacks()
    }

    private fun userCallbacks() {
        userButtons[0].setOnAction {
            //SCAN USER KEY -> INPUT USER IDENTITY -> GET HASH -> SIGN BY USER -> CREATE RECORD
            ScanPane.isViewOnly = false
            SceneManager.showScanScene(ScanPane.TYPE.PUBLIC_KEY)
        }

        userButtons[1].setOnAction {
            SceneManager.showScanScene(ScanPane.TYPE.SECRET_KEY)
        }

        userButtons[2].setOnAction {
            //SCAN USER KEY -> SEARCH BLOCKCHAIN -> GET RECORD
            ScanPane.isViewOnly = true
            SceneManager.showScanScene(ScanPane.TYPE.PUBLIC_KEY)
        }
    }

    private fun hospitalCallbacks() {
        hospitalButtons[0].setOnAction {
            //GET HOSPITAL KEY
            SceneManager.showAddHospitalScene()
        }
        hospitalButtons[1].setOnAction {
            //VIEW HOSPITALS
            ViewHospitalsPane.loadList()
            SceneManager.showHospitalViewScene()
        }
    }

    private fun validatorCallbacks() {
        validatorButtons[0].setOnAction {
            //ADD VALIDATOR -> Get New Validator username, password,
            SceneManager.showAddRemoveValidatorScene()
        }
        validatorButtons[1].setOnAction {
            //VOTE VALIDATOR -> Show
            SceneManager.showVoteValidatorsScene()
        }
        validatorButtons[2].setOnAction {
            //VIEW VALIDATORS
            SceneManager.showViewValidatorsScene()
        }
    }
}
