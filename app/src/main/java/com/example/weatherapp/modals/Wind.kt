package com.example.weatherapp.modals

import java.io.Serializable

data class Wind(
    val speed:Double,
    val deg :Int,
    val gust :Double
): Serializable
