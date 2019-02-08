package viewmodel.panes

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import main.Helper
import viewmodel.Config
import viewmodel.SceneManager

object ViewValidatorsPane : BorderPane() {
    private val validatorList = FXCollections.observableArrayList<HBox>()
    private val listView = ListView(validatorList)
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
        validatorList.clear()
        val response = "${Config.BASE_URL}/authority/get-overall-list"
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
                val ecPubKey = it["ecPublicKey"] as String
                nameToIdentityMap[name] = ecPubKey
                val disqualifyButton = Button("Start voting on disqualification")
                val buttonBox = HBox(20.0, disqualifyButton)
                buttonBox.alignment = Pos.CENTER
                val tempBox = HBox(Config.WIDTH * 9 / 16, Label(it["name"] as String), buttonBox)
                tempBox.alignment = Pos.CENTER_RIGHT
                validatorList.add(tempBox)
                disqualifyButton.setOnAction {
                    "${Config.BASE_URL}/authority/vote/cast".httpPost()
                        .header(
                                mapOf(
                                        "Content-Type" to "application/json; charset=utf-8",
                                        "Authorization" to Helper.token
                                )
                        )
                        .body("""{
                        "beneficiary": {
                            "name": "$name",
                            "ecPublicKey": "$ecPubKey",
                            "keyDEREncoded": true
                        },
                        "add": false,
                        "agree": true
                    }""".replace("\\s".toRegex(), ""), Charsets.UTF_8).responseString()
                }
            }
        } else {
            println("LOADING VALIDATOR LIST FAILED: ${response.third.component2()}")
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