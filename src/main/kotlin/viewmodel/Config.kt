package viewmodel

object Config {
    var BASE_URL = "https://25.30.118.78/api"
    const val WIDTH = 1600.0
    const val HEIGHT = 900.0

    const val QRCODE_HEIGHT = 300.0
    const val QRCODE_WIDTH = 300.0

    const val IMAGE_HEIGHT = 400.0
    const val IMAGE_WIDTH = 400.0

    const val IP_WARNING = "Please enter valid ip address"
    const val USERNAME_WARNING = "Please enter your username"
    const val PASSWORD_WARNING = "Please enter your password"
    const val WRONG_WARNING = "You entered wrong Username or Password"
    const val TIMEOUT = "Failed to connect"

    val CERT_URL = Config::class.java.classLoader.getResource("auth0.cer")!!

    val CSS_STYLES = Config::class.java.classLoader.getResource("css/styles.css").toExternalForm()!!

    val IMAGES = mapOf(
        "icon" to Config::class.java.classLoader.getResource("images/icon.png").toExternalForm(),
        "createRecord" to Config::class.java.classLoader.getResource("images/menu/createRecord.png").toExternalForm(),
        "modifyRecord" to Config::class.java.classLoader.getResource("images/menu/modifyRecord.png").toExternalForm(),
        "viewRecord" to Config::class.java.classLoader.getResource("images/menu/viewRecord.png").toExternalForm(),
        "addHospital" to Config::class.java.classLoader.getResource("images/menu/addHospital.png").toExternalForm(),
        "removeHospital" to Config::class.java.classLoader.getResource("images/menu/removeHospital.png").toExternalForm(),
        "viewHospitals" to Config::class.java.classLoader.getResource("images/menu/viewHospitals.png").toExternalForm(),
        "addValidator" to Config::class.java.classLoader.getResource("images/menu/addValidator.png").toExternalForm(),
        "voteValidator" to Config::class.java.classLoader.getResource("images/menu/voteValidator.png").toExternalForm(),
        "viewValidators" to Config::class.java.classLoader.getResource("images/menu/viewValidators.png").toExternalForm()
    )

    /////////////////////////////////////

    val ELIPTIC_CURVE = "secp256k1"
    val ELIPTIC_CURVE_SIGNATURE_ALGORITHM = "SHA256withECDSA"
    val BLOCKCHAIN_HASH_ALGORITHM = "SHA256"


    val MAX_NAME_LENGTH = java.lang.Byte.MAX_VALUE.toInt()
    val HASH_LENGTH = 32
    val SIGNATURE_LENGTH = 64
    val IDENTIFIER_LENGTH = 20
    val RAW_PUBLICKEY_LENGTH = 33
}