package ru.yole.jkid

import ru.yole.jkid.deserialization.JKidException
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

fun serializerForBasicType(type: Type): ValueSerializer<out Any?> {
    assert(type.isPrimitiveOrString()) { "Expected primitive type or String: ${type.typeName}" }
    return serializerForType(type)!!
}

// JSON 값을 적절한 타입으로 변환해줌.
fun serializerForType(type: Type): ValueSerializer<out Any?>? =
    when (type) {
        Byte::class.java, Byte::class.javaObjectType -> ByteSerializer
        Short::class.java, Short::class.javaObjectType -> ShortSerializer
        Int::class.java, Int::class.javaObjectType -> IntSerializer
        Long::class.java, Long::class.javaObjectType -> LongSerializer
        Float::class.java, Float::class.javaObjectType -> FloatSerializer
        Double::class.java, Double::class.javaObjectType -> DoubleSerializer
        Boolean::class.java, Boolean::class.javaObjectType -> BooleanSerializer
        String::class.java -> StringSerializer
        else -> null
    }

fun serializerForDateType(format: String): ValueSerializer<Any?>? {
    @Suppress("UNCHECKED_CAST")
    return DateSerializer(SimpleDateFormat(format)) as ValueSerializer<Any?>?
}

class DateSerializer(simpleDateFormat: SimpleDateFormat) : ValueSerializer<Date> {
    private val dateFormat = simpleDateFormat

    override fun fromJsonValue(jsonValue: Any?): Date = dateFormat.parse(jsonValue as String)
    override fun toJsonValue(value: Date): Any? = dateFormat.format(value)
}

private fun Any?.expectNumber(): Number {
    if (this !is Number) throw JKidException("Expected number, was: $this")
    return this
}

object ByteSerializer : ValueSerializer<Byte> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toByte()
    override fun toJsonValue(value: Byte) = value
}

object ShortSerializer : ValueSerializer<Short> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toShort()
    override fun toJsonValue(value: Short) = value
}

object IntSerializer : ValueSerializer<Int> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toInt()
    override fun toJsonValue(value: Int) = value
}

object LongSerializer : ValueSerializer<Long> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toLong()
    override fun toJsonValue(value: Long) = value
}

object FloatSerializer : ValueSerializer<Float> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toFloat()
    override fun toJsonValue(value: Float) = value
}

object DoubleSerializer : ValueSerializer<Double> {
    override fun fromJsonValue(jsonValue: Any?) = jsonValue.expectNumber().toDouble()
    override fun toJsonValue(value: Double) = value
}

object BooleanSerializer : ValueSerializer<Boolean> {
    override fun fromJsonValue(jsonValue: Any?): Boolean {
        if (jsonValue !is Boolean) throw JKidException("Expected boolean, was: $jsonValue")
        return jsonValue
    }

    override fun toJsonValue(value: Boolean) = value
}

object StringSerializer : ValueSerializer<String?> {
    override fun fromJsonValue(jsonValue: Any?): String? {
        if (jsonValue !is String?) throw JKidException("Expected string, was: $jsonValue")
        return jsonValue
    }

    override fun toJsonValue(value: String?) = value
}