package com.it2021084.unibuddy

data class FacultyMember(
    val id: String,
    val name: String,
    val email: String,
    val department: String
)

object FacultyCatalog{
    val facultyList = listOf(
        FacultyMember("f01", "A. Poutos", "tasos.poutos@hotmail.com", "Computer Science"),
        FacultyMember("f02", "M. Georgiou", "georgiou@uni.edu", "Mathematics"),
        FacultyMember("f03", "S. Ioannou", "ioannou@uni.edu", "Physics"),
        FacultyMember("sec01", "Secretariat", "info@cs.uni.edu", "CS Department"),
        FacultyMember("sec02", "Student Help Desk", "help@uni.edu", "Administration")
    )
}