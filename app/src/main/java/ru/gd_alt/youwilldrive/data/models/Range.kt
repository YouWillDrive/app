package ru.gd_alt.youwilldrive.data.models

import kotlin.reflect.typeOf


class IncludedBound(var value: Any) {
    init {
        if (value !is Int && value !is Double && value !is Float && value !is Long && value !is String) {
            throw IllegalArgumentException("Unsupported type")
        }
    }

    /**
     * The IncludedBound class represents a bound that is included in the range.
     * It can be of type Int, Double, Float, Long, or String.
     * The constructor checks if the value is of a supported type.
     *
     * @param value The value of the IncludedBound object.
     * @throws IllegalArgumentException if the value is not of a supported type.
     *
     *
     */
    fun <T> getValue(): T {
        return value as T
    }
}

/**
 * The ExcludedBound class represents a bound that is excluded from the range.
 * It can be of type Int, Double, Float, Long, or String.
 * The constructor checks if the value is of a supported type.
 *
 * @param value The value of the ExcludedBound object.
 * @throws IllegalArgumentException if the value is not of a supported type.
 *
 */
class ExcludedBound(var value: Any) {
    init {
        if (value !is Int && value !is Double && value !is Float && value !is Long && value !is String) {
            throw IllegalArgumentException("Unsupported type")
        }
    }

    fun <T> getValue(): T {
        return value as T
    }
}

/**
 * The Range class represents a range of values with a minimum and maximum bound.
 * The minimum bound is included in the range, while the maximum bound is excluded.
 * The constructor checks if the min and max bounds are of the same type.
 *
 * @param min The minimum bound of the range (included).
 * @param max The maximum bound of the range (excluded).
 * @throws IllegalArgumentException if min and max are not of the same type.
 *
 */
class Range<T>(var min: IncludedBound, var max: ExcludedBound) {
    init {
        if (min::class.simpleName != max::class.simpleName) {
            throw IllegalArgumentException("min and max must be of the same type")
        }
    }

    /**
     * Return the minimum bound.
     */
    fun <T> getMin(): T {
        return min.getValue()
    }

    /**
     * Return the maximum bound.
     */
    fun <T> getMax(): T {
        return max.getValue()
    }

    /**
     * Check if a value is within the range.
     * The minimum bound is included, while the maximum bound is excluded.
     *
     * @param value The value to check.
     * @throws IllegalArgumentException if the value is not of a supported type.
     * @return true if the value is within the range, false otherwise.
     */
    fun inBounds(value: T): Boolean {
        val minValue = min.getValue<T>()
        val maxValue = max.getValue<T>()
        return when (value) {
            is Int -> value in (minValue as Int)..(maxValue as Int)
            is Double -> value in (minValue as Double)..(maxValue as Double)
            is Float -> value in (minValue as Float)..(maxValue as Float)
            is Long -> value in (minValue as Long)..(maxValue as Long)
            is String -> (value as String) >= minValue as String &&
                    (value as String) < (maxValue as String)
            else -> throw IllegalArgumentException("Unsupported type")
        }
    }
}