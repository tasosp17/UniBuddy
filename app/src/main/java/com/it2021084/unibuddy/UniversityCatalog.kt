package com.it2021084.unibuddy

import com.it2021084.unibuddy.UniversityCatalog.allUniversities

data class University(
    val id: String,
    val displayName: String,
    val ssids: List<String>
)

object UniversityCatalog{
    val allUniversities = listOf(
        University(
            id = "HUA",
            displayName = "Harokopio University of Athens",
            ssids = listOf("HAROKOPIO-FREE")
        ),
        University(
            id = "TESTING",
            displayName = "Testing University",
            ssids = listOf("Turbo-X-F184A8")
        )
    )
}

fun getSsidsForUniversityId(id: String): List<String> =
    allUniversities.find {it.id == id}?.ssids ?: emptyList()

fun getUniversityIdByDisplayName(name: String): String =
    allUniversities.find {it.displayName == name}?.id ?: ""

fun getDisplayNameByUniversityId(id: String): String =
    allUniversities.find { it.id == id }?.displayName ?: ""