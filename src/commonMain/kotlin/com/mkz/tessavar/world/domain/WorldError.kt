package com.mkz.tessavar.world.domain

sealed class WorldError(
    message: String,
) : Throwable(message = message) {
    class InvalidWorldId(
        id: String,
    ) : WorldError("The world id '$id' is not lowercase words joined by hyphens")

    class OtherWorldVersion(
        id: String,
        version: Int,
    ) : WorldError("The world '$id' is of world version $version, not ${World.CURRENT_VERSION}")
}
