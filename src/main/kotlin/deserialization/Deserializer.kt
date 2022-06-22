package ru.yole.jkid.deserialization

import ru.yole.jkid.*
import java.io.Reader
import java.io.StringReader
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType

inline fun <reified T : Any> deserialize(json: String): T {
    return deserialize(StringReader(json))
}

inline fun <reified T : Any> deserialize(json: Reader): T {
    return deserialize(json, T::class)
}

fun <T : Any> deserialize(json: Reader, targetClass: KClass<T>): T {
    // 직렬화할 객체의 프로퍼티를 담을 ObjectSeed 생성
    val seed = ObjectSeed(targetClass, ClassInfoCache())    // ObjectSeed는 Seed 타입 리턴
    // Seed는 JsonObject 인터페이스 확장함.
    // Parser는 Reader, JsonObject를 인자로 받음.
    Parser(json, seed).parse()
    // 입력 데이터의 끝에 도달하면 spawn 함수를 호출해 결과 객체를 생성함.
    return seed.spawn()
}

interface JsonObject {
    fun setSimpleProperty(propertyName: String, value: Any?)

    fun createObject(propertyName: String): JsonObject

    fun createArray(propertyName: String): JsonObject
}

interface Seed : JsonObject {
    val classInfoCache: ClassInfoCache

    fun spawn(): Any?

    fun createCompositeProperty(propertyName: String, isList: Boolean): JsonObject

    override fun createObject(propertyName: String) = createCompositeProperty(propertyName, false)

    override fun createArray(propertyName: String) = createCompositeProperty(propertyName, true)
}

// 파라미터의 타입을 분석해서 적절히 ObjectSeed, ObjectListSeed, ValueListSeed 중 하나를 생성해줌.
fun Seed.createSeedForType(paramType: Type, isList: Boolean): Seed {
    val paramClass = paramType.asJavaClass()

    if (List::class.java.isAssignableFrom(paramClass)) {
        if (!isList) throw JKidException("An array expected, not a composite object")
        val parameterizedType = paramType as? ParameterizedType
            ?: throw UnsupportedOperationException("Unsupported parameter type $this")

        val elementType = parameterizedType.actualTypeArguments.single()
        if (elementType.isPrimitiveOrString()) {
            return ValueListSeed(elementType, classInfoCache)
        }
        return ObjectListSeed(elementType, classInfoCache)
    }
    if (isList) throw JKidException("Object of the type ${paramType.typeName} expected, not an array")
    return ObjectSeed(paramClass.kotlin, classInfoCache)
}


class ObjectSeed<out T : Any>(
    targetClass: KClass<T>,
    override val classInfoCache: ClassInfoCache
) : Seed {

    private val classInfo: ClassInfo<T> = classInfoCache[targetClass]

    private val valueArguments = mutableMapOf<KParameter, Any?>() // 간단한 값 기록
    private val seedArguments = mutableMapOf<KParameter, Seed>()  // 복합 객체나 리스트 기록

    private val arguments: Map<KParameter, Any?>
        get() = valueArguments + seedArguments.mapValues { it.value.spawn() }

    override fun setSimpleProperty(propertyName: String, value: Any?) {
        val param = classInfo.getConstructorParameter(propertyName)  // KParameter 타입
        // 파라미터 타입에 맞춰 value를 deserialize 해주고, valueArguments에 기록함.
        valueArguments[param] = classInfo.deserializeConstructorArgument(param, value)
    }

    override fun createCompositeProperty(propertyName: String, isList: Boolean): Seed {
        val param = classInfo.getConstructorParameter(propertyName)
        val deserializeAs = classInfo.getDeserializeClass(propertyName)
        // 파라미터 타입에 따라 ObjectSeed나 CollectionSeed를 만듦.
        val seed = createSeedForType(
            deserializeAs ?: param.type.javaType, isList
        )
        // 위에서 만든 Seed 객체를 seedArgument 맵에 기록함.
        return seed.apply { seedArguments[param] = this }
    }

    // 인자 맵을 넘겨서 targetClass 타입의 인스턴스를 만듦.
    override fun spawn(): T = classInfo.createInstance(arguments)
}

class ObjectListSeed(
    val elementType: Type,
    override val classInfoCache: ClassInfoCache
) : Seed {
    private val elements = mutableListOf<Seed>()

    override fun setSimpleProperty(propertyName: String, value: Any?) {
        throw JKidException("Found primitive value in collection of object types")
    }

    override fun createCompositeProperty(propertyName: String, isList: Boolean) =
        createSeedForType(elementType, isList).apply { elements.add(this) }

    override fun spawn(): List<*> = elements.map { it.spawn() }
}

class ValueListSeed(
    elementType: Type,
    override val classInfoCache: ClassInfoCache
) : Seed {
    private val elements = mutableListOf<Any?>()
    private val serializerForType = serializerForBasicType(elementType)

    override fun setSimpleProperty(propertyName: String, value: Any?) {
        elements.add(serializerForType.fromJsonValue(value))
    }

    override fun createCompositeProperty(propertyName: String, isList: Boolean): Seed {
        throw JKidException("Found object value in collection of primitive types")
    }

    override fun spawn() = elements
}