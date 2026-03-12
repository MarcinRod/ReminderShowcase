package pl.marrod.remindershowcase.data

data class Reminder(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long
)
