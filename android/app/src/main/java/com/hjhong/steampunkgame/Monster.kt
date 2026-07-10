package com.hjhong.steampunkgame

data class Monster(
    val id: String = "",
    val name: String = "",
    val level: Int = 0,
    val hp: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val element: String = "",
    val lore: String = "",
    val imagePath: String = ""
)