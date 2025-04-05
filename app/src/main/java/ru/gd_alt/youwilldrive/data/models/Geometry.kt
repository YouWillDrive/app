package ru.gd_alt.youwilldrive.data.models

abstract class Geometry {
}

class Point(val x: Double, val y: Double) : Geometry() {
    override fun toString(): String {
        return "Point(x=$x, y=$y)"
    }
}

class Line(val start: Point, val end: Point) : Geometry() {
    override fun toString(): String {
        return "Line(start=$start, end=$end)"
    }
}

class Polygon(val points: List<Point>) : Geometry() {
    override fun toString(): String {
        return "Polygon(points=$points)"
    }
}

class MultiPoint(val points: List<Point>) : Geometry() {
    override fun toString(): String {
        return "MultiPoint(points=$points)"
    }
}

class MultiLine(val lines: List<Line>) : Geometry() {
    override fun toString(): String {
        return "MultiLine(lines=$lines)"
    }
}

class MultiPolygon(val polygons: List<Polygon>) : Geometry() {
    override fun toString(): String {
        return "MultiPolygon(polygons=$polygons)"
    }
}

class GeometryCollection(val geometries: List<Geometry?>) : Geometry() {
    override fun toString(): String {
        return "GeometryCollection(geometries=$geometries)"
    }
}