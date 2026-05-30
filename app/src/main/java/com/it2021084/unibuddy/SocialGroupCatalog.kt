package com.it2021084.unibuddy

data class SocialGroup(
    val id: String,
    val name: String
)

object SocialGroupCatalog{
    val allSocialGroups = listOf(
        SocialGroup(
            id = "td01",
            name = "Traditional Dancing"
        ),
        SocialGroup(
            id = "ctf01",
            name = "CTF Group"
        ),
        SocialGroup(
            id = "bb01",
            name = "Basketball"
        ),
        SocialGroup(
            id = "fb01",
            name = "Football"
        ),
        SocialGroup(
            id = "sw01",
            name = "Swimming"
        )
    )
}