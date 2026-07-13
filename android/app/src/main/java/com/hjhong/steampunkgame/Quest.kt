package com.hjhong.steampunkgame

data class Quest(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val difficulty: Int = 0,
    val rewardExp: Int = 0,
    val scripture: String = "",
    val scriptureRef: String = ""
)