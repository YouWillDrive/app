package ru.gd_alt.youwilldrive.data.cbor

import ru.gd_alt.youwilldrive.data.models.Geometry
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


private val customEnHook: (Any) -> Pair<Long, Any>? = { item ->
    when (item) {
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
        is Future -> CustomTags.TAG_FUTURE to ""
        is TableName -> CustomTags.TAG_TABLE_NAME to item.name
        is RecordID -> CustomTags.TAG_RECORD_ID to listOf(item.tableName, item.recordId),
        is Range<*> -> CustomTags.TAG_RANGE to listOf(item.getMin(), item.getMax())
        else -> null
    }
}

object SurrealCbor {
    val cbor: CborInteractor = CborInteractor()
}