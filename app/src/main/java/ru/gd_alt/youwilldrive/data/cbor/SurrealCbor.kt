package ru.gd_alt.youwilldrive.data.cbor

import ru.gd_alt.youwilldrive.data.models.Point
import ru.gd_alt.youwilldrive.data.models.Line
import ru.gd_alt.youwilldrive.data.models.Polygon
import ru.gd_alt.youwilldrive.data.models.MultiPoint
import ru.gd_alt.youwilldrive.data.models.MultiLine
import ru.gd_alt.youwilldrive.data.models.MultiPolygon
import ru.gd_alt.youwilldrive.data.models.Future
import ru.gd_alt.youwilldrive.data.models.TableName
import ru.gd_alt.youwilldrive.data.models.RecordID
import ru.gd_alt.youwilldrive.data.models.Range
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.Instant
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import ru.gd_alt.youwilldrive.data.models.Bound
import ru.gd_alt.youwilldrive.data.models.ExcludedBound
import ru.gd_alt.youwilldrive.data.models.Geometry
import ru.gd_alt.youwilldrive.data.models.GeometryCollection
import ru.gd_alt.youwilldrive.data.models.IncludedBound


@OptIn(ExperimentalUuidApi::class)
val customDeHook: (Long, Any?) -> Any? = { tag, decodedItem ->
    when (tag) {
        CustomTags.TAG_NULL -> null
        CustomTags.TAG_TABLE_NAME -> TableName(decodedItem as String)
        CustomTags.TAG_RECORD_ID -> {
            val list = decodedItem as List<*>
            RecordID(list[0] as String, list[1] as String)
        }
        CustomTags.TAG_UUID_STRING -> Uuid.parse(decodedItem as String)
        CustomTags.TAG_DECIMAL -> {
            BigDecimal(decodedItem as String)
        }
        CustomTags.TAG_DT -> {
            val list = decodedItem as List<Long>
            Instant.fromEpochSeconds(list[0], list[1].toInt())
                .toLocalDateTime(timeZone = TimeZone.of("Europe/Moscow"))
        }
        CustomTags.TAG_DURATION -> {
            val list = decodedItem as String
            val years = list.substringBefore("y").toInt()
            val weeks = list.substringAfter("y").substringBefore("w").toInt()
            val days = list.substringAfter("w").substringBefore("d").toInt()
            val hours = list.substringAfter("d").substringBefore("h").toInt()
            val minutes = list.substringAfter("h").substringBefore("m").toInt()
            val seconds = list.substringAfter("m").substringBefore("s").toInt()
            val milliseconds = list.substringAfter("s").substringBefore("ms").toInt()
            val microseconds = list.substringAfter("ms").substringBefore("us").toInt()
            val nanoseconds = list.substringAfter("us").substringBefore("ns").toInt()
            DateTimePeriod(
                years = years,
                months = 0,
                days = days + weeks * 7,
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                nanoseconds = milliseconds * 1_000_000 + microseconds * 1_000 + nanoseconds.toLong()
            )
        }
        CustomTags.TAG_DUR -> {
            val list = decodedItem as List<*>
            DateTimePeriod(
                seconds = list[0] as Int,
                nanoseconds = list[1] as Long
            )
        }
        CustomTags.TAG_FUTURE -> {
            Future(listOf())
        }
        CustomTags.TAG_BIN_UUID -> {
            decodedItem as String
            Uuid.fromByteArray(decodedItem.encodeToByteArray())
        }
        CustomTags.TAG_RANGE -> {
            val list = decodedItem as List<*>
            Range<Any>(
                min = list[0] as Bound,
                max = list[1] as Bound
            )
        }
        CustomTags.TAG_BOUNDARY_INC -> {
            IncludedBound(decodedItem as Any)
        }
        CustomTags.TAG_BOUNDARY_EXC -> {
            ExcludedBound(decodedItem as Any)
        }
        CustomTags.TAG_POINT -> {
            val list = decodedItem as List<*>
            Point(list[0] as Double, list[1] as Double)
        }
        CustomTags.TAG_LINE -> {
            val list = decodedItem as List<*>
            Line(
                start = Point((list[0] as List<*>)[0] as Double, (list[0] as List<*>)[1] as Double),
                end = Point((list[1] as List<*>)[0] as Double, (list[1] as List<*>)[1] as Double)
            )
        }
        CustomTags.TAG_POLYGON -> {
            val list = decodedItem as List<*>
            Polygon(
                points = list.map { point ->
                    Point((point as List<*>)[0] as Double, point[1] as Double)
                }
            )
        }
        CustomTags.TAG_MULTIPOINT -> {
            val list = decodedItem as List<*>
            MultiPoint(
                points = list.map { point ->
                    Point((point as List<*>)[0] as Double, point[1] as Double)
                }
            )
        }
        CustomTags.TAG_MULTILINE -> {
            val list = decodedItem as List<*>
            MultiLine(
                lines = list.map { line ->
                    Line(
                        start = Point((line as List<*>)[0] as Double, line[1] as Double),
                        end = Point((line[2] as List<*>)[0] as Double, line[3] as Double)
                    )
                }
            )
        }
        CustomTags.TAG_MULTIPOLYGON -> {
            val list = decodedItem as List<*>
            MultiPolygon(
                polygons = list.map { polygon ->
                    Polygon(
                        points = (polygon as List<*>).map { point ->
                            Point((point as List<*>)[0] as Double, point[1] as Double)
                        }
                    )
                }
            )
        }
        CustomTags.TAG_GEOMETRY -> {
            GeometryCollection(
                geometries = decodedItem as List<Geometry?>
            )
        }
        else -> decodedItem
    }
}


@OptIn(ExperimentalUuidApi::class)
private val customEnHook: (Any) -> Pair<Long, Any>? = { item ->
    when (item) {
        is TableName -> CustomTags.TAG_TABLE_NAME to item.name
        is RecordID -> CustomTags.TAG_RECORD_ID to listOf(item.tableName, item.recordId)
        is Uuid -> CustomTags.TAG_UUID_STRING to item.toString()
        is BigDecimal -> CustomTags.TAG_DECIMAL to item.toString()
        is LocalDateTime -> {
            val epochSeconds = item.toInstant(timeZone = TimeZone.of("Europe/Moscow")).epochSeconds
            val nanoseconds = item.toInstant(timeZone = TimeZone.of("Europe/Moscow")).nanosecondsOfSecond
            CustomTags.TAG_DT to listOf(epochSeconds, nanoseconds)
        }
        is DateTimePeriod -> {
            CustomTags.TAG_DURATION to "${item.years}y${item.days / 7}w${item.days % 7}d${item.hours}h${item.minutes}m${item.seconds}s${item.nanoseconds / 1_000_000}ns"
        }
        is Point -> CustomTags.TAG_POINT to listOf(item.x, item.y)
        is Line -> CustomTags.TAG_LINE to listOf(listOf(item.start.x, item.start.y), listOf(item.end.x, item.end.y))
        is Polygon -> CustomTags.TAG_POLYGON to item.points.map { listOf(it.x, it.y) }
        is MultiPoint -> CustomTags.TAG_MULTIPOINT to item.points.map { listOf(it.x, it.y) }
        is MultiLine -> CustomTags.TAG_MULTILINE to item.lines.map { line ->
            listOf(
                listOf(line.start.x, line.start.y),
                listOf(line.end.x, line.end.y)
            )
        }
        is MultiPolygon -> CustomTags.TAG_MULTIPOLYGON to item.polygons.map { polygon ->
            polygon.points.map { listOf(it.x, it.y) }
        }
        is GeometryCollection -> CustomTags.TAG_GEOMETRY to item.geometries
        is Future -> CustomTags.TAG_FUTURE to ""
        is Range<*> -> CustomTags.TAG_RANGE to listOf(item.getMin<Any>(), item.getMax<Any>())
        is IncludedBound -> CustomTags.TAG_BOUNDARY_INC to item.value
        is ExcludedBound -> CustomTags.TAG_BOUNDARY_EXC to item.value
        else -> null
    }
}

object SurrealCbor {
    val cbor: CborInteractor = CborInteractor(enHook = customEnHook, deHook = customDeHook)
}