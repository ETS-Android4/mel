package de.miniserver.http

import de.mein.core.serialize.SerializableEntity

class BuildRequest() : SerializableEntity {
    var server: Boolean? = null
    var apk: Boolean? = null
    var jar: Boolean? = null
    var pw: String? = null
    val valid: Boolean
        get() {
            return server != null && apk != null && jar != null
        }
}