package xyz.medirec.medirec.pojo

import java.io.Serializable
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint

class KeyProperties(val encoded: ByteArray, val format: String, val algorithm: String, val w : ECPoint, val ecSpec: ECParameterSpec): Serializable{
    companion object { const val serialVersionUID = 1234123412341L }
}