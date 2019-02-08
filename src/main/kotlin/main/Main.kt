package main

import javafx.application.Application
import javafx.stage.Stage
import viewmodel.SceneManager

class Main : Application() {
    override fun start(primaryStage: Stage) {
        SceneManager.stage = primaryStage
        SceneManager.showLogInScene()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(Main::class.java, *args)
        }
    }
}
