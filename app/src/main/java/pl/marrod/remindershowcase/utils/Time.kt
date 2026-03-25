import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

     fun Long.toDisplayDateTime(): String =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
            .format(dateTimeFormatter)
     fun Long.toDisplayTime(): String =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalTime()
            .format(timeFormatter)
