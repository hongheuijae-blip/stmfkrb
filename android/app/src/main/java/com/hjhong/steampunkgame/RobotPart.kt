package com.hjhong.steampunkgame

data class RobotPart(
    val id: String = "",
    val name: String = "",
    val slot: String = "",
    val bonusAttack: Int = 0,
    val bonusDefense: Int = 0,
    val specialEffect: String = "",
    val imagePath: String = "",
    val scripture: String = "",
    val scriptureRef: String = ""
)