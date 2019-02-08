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
import javafx.scene.layout.*
import javafx.scene.paint.Color
import main.Helper
import viewmodel.Config
import viewmodel.SceneManager

object VoteValidatorsPane : BorderPane() {
    private val validatorList = FXCollections.observableArrayList<HBox>()
    private val listView = ListView(validatorList)
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
        val response = "${Config.BASE_URL}/authority/vote/get-current-list"
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
                val addOrRemove = it["add"] as Boolean
                val beneficiary = it["beneficiary"] as JsonObject
                val name = beneficiary["name"] as String
                val pubKey = beneficiary["ecPublicKey"] as String
                val tempStr = if(addOrRemove) "addition" else "removal"
                val agreeButton = Button("Agree on $tempStr")
                val disAgreeButton = Button("Disagree on $tempStr")
                val buttonBox = HBox(20.0, agreeButton, disAgreeButton)
                val labelBox = HBox(Label("Agree count: ${it["agree"] as Int}, Disagree count: ${it["disagree"] as Int}"))
                labelBox.alignment = Pos.CENTER
                buttonBox.alignment = Pos.CENTER
                val tempBox = HBox(100.0, Label("Name: $name"), labelBox, buttonBox)
                tempBox.background = Background(BackgroundFill(
                        if(it["add"] as Boolean) Color.LIGHTGREEN else Color.PINK, CornerRadii.EMPTY, Insets.EMPTY))
                tempBox.alignment = Pos.CENTER_RIGHT
                validatorList.add(tempBox)

                agreeButton.setOnAction {
                    vote(name, pubKey, addOrRemove, true)
                    disAgreeButton.isDisable = true
                    agreeButton.isDisable = true
                }

                disAgreeButton.setOnAction {
                    vote(name, pubKey, addOrRemove, false)
                    disAgreeButton.isDisable = true
                    agreeButton.isDisable = true
                }

                if(it["voted"] as Boolean) {
                    disAgreeButton.isDisable = true
                    agreeButton.isDisable = true
                }
            }
        } else {
            println("LOADING VOTING LIST FAILED: ${response.third.component2()}")
        }
    }

    private fun vote(name : String, pubKey: String, addOrRemove: Boolean, agree: Boolean) : Boolean {
        val btnResponse = "${Config.BASE_URL}/authority/vote/cast"
                .httpPost()
                .header(
                        mapOf(
                                "Content-Type" to "application/json; charset=utf-8",
                                "Authorization" to Helper.token
                        )
                )
                .body("""{
                    "beneficiary": {
                        "name": "$name",
                        "ecPublicKey": "$pubKey",
                        "keyDEREncoded": true
                    },
                    "add": $addOrRemove,
                    "agree": $agree
                }""".replace("\\s".toRegex(), ""), Charsets.UTF_8).responseString()
        return btnResponse.second.statusCode == 200 && btnResponse.third.component1()!!.toInt() == 0
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