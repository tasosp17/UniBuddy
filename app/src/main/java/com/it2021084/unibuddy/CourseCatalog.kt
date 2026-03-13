package com.it2021084.unibuddy

import android.icu.util.Calendar

data class CourseSchedule(
    val dayOfWeek: Int, //Sun = 1, Mon= 2, ... Sat = 7
    val hour: Int, //0-23
    val minute: Int
)

data class Course(
    val id: String,
    val name: String,
    val schedule: List<CourseSchedule> = emptyList()
)

object CourseCatalog {
    val allCourses = listOf(
        Course(
            id = "cs01",
            name = "Computational Mathematics",
            schedule = listOf(CourseSchedule(Calendar.MONDAY,9, 0))
        ),
        Course(
            id = "cs02",
            name = "Digital Technology and Telematics Applications",
            schedule = listOf(CourseSchedule(Calendar.TUESDAY,12, 0))
        ),
        Course(
            id = "cs03",
            name = "Programming I",
            schedule = listOf(CourseSchedule(Calendar.WEDNESDAY,9, 0))
        ),
        Course(
            id = "cs04",
            name = "Logical Design",
            schedule = listOf(CourseSchedule(Calendar.WEDNESDAY,12, 0))
        ),
        Course(
            id = "cs05",
            name = "Discrete Mathematics",
            schedule = listOf(CourseSchedule(Calendar.THURSDAY,19, 9))
        )
    )
}