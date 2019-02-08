package viewmodel

import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import viewmodel.panes.*

object SceneManager {
    private val logInScene = Scene(LogInPane, Config.WIDTH / 2, Config.HEIGHT / 2)
    private val mainMenuScene = Scene(MainMenuPane, Config.WIDTH, Config.HEIGHT)
    private val scanScene = Scene(ScanPane, Config.WIDTH, Config.HEIGHT)
    private val qrScene = Scene(QRCodePane, Config.WIDTH, Config.HEIGHT)
    private val hospitalScene = Scene(ViewHospitalsPane, Config.WIDTH, Config.HEIGHT)
    private val addHospitalScene = Scene(AddHospitalPane, Config.WIDTH / 2, Config.HEIGHT / 2)
    private val addRemoveValidatorScene = Scene(AddRemoveValidatorsPane, Config.WIDTH / 2, Config.HEIGHT / 2)
    private val viewValidatorsScene = Scene(ViewValidatorsPane, Config.WIDTH, Config.HEIGHT)
    private val voteValidatorsScene = Scene(VoteValidatorsPane, Config.WIDTH, Config.HEIGHT)
    var stage: Stage? = null
        get() = field!!
        set(value) {
            if(value == null) return
            field = value
            field!!.title = "MediRec"
            field!!.icons.add(Image(Config.IMAGES["icon"]))
            field!!.setOnCloseRequest {
                println("Exiting...")
                (scanScene.root as ScanPane).disposeWebCamCamera()
            }
        }
    private var lastPatientInfoScene : Scene? = null
    var lastPatientPane : PatientInfoPane? = null

    init {
        val scenes = arrayOf(
                logInScene, mainMenuScene, scanScene,
                qrScene, hospitalScene, addHospitalScene,
                addRemoveValidatorScene, viewValidatorsScene, voteValidatorsScene
        )
        addStylesheets(*scenes)
    }

    private fun addStylesheets(vararg scenes: Scene) {
        for (scene in scenes) {
            scene.stylesheets.add(Config.CSS_STYLES)
        }
    }

    private fun showScene(scene: Scene) {
        if (stage == null)
            return

        stage!!.hide()
        stage!!.scene = scene
        stage!!.show()
    }

    fun showLogInScene() {
        showScene(logInScene)
    }

    fun showMainMenuScene() {
        showScene(mainMenuScene)
    }

    fun showScanScene(type: ScanPane.TYPE) {
        showScene(scanScene)
        (scanScene.root as ScanPane).startWebCam()
        (scanScene.root as ScanPane).type = type
    }

    fun showQRScene() {
        showScene(qrScene)
    }

    fun showPatientInfoScene(infoPane: PatientInfoPane) {
        val scene = Scene(infoPane, Config.WIDTH, Config.HEIGHT)
        lastPatientPane = infoPane
        lastPatientInfoScene = scene
        addStylesheets(scene)
        showScene(scene)
    }

    fun showHospitalViewScene() {
        showScene(hospitalScene)
    }

    fun showAddHospitalScene() {
        showScene(addHospitalScene)
    }

    fun showAddRemoveValidatorScene() {
        showScene(addRemoveValidatorScene)
    }

    fun showVoteValidatorsScene() {
        VoteValidatorsPane.loadList()
        showScene(voteValidatorsScene)
    }

    fun showViewValidatorsScene() {
        ViewValidatorsPane.loadList()
        showScene(viewValidatorsScene)
    }

}
