package com.irv205.pokeapi.data.mock

enum class ApiEnvironment {
    PROD,
    MOCK,
}

interface EnvironmentProvider {
    val current: ApiEnvironment
}

class MutableEnvironmentProvider(
    override var current: ApiEnvironment = ApiEnvironment.PROD,
) : EnvironmentProvider
