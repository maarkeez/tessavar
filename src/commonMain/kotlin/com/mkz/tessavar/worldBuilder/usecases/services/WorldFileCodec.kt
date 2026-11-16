package com.mkz.tessavar.worldBuilder.usecases.services

import com.mkz.tessavar.worldBuilder.domain.WorldBuilder

interface WorldFileCodec {
    fun decode(text: String): WorldBuilder.Dto.WorldFileDto?

    fun encode(file: WorldBuilder.Dto.WorldFileDto): String
}
