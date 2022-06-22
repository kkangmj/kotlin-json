package ru.yole.jkid.deserialization

import ru.yole.jkid.*
import ru.yole.jkid.serialization.getSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

class ClassInfoCache {
    private val cacheData = mutableMapOf<KClass<*>, ClassInfo<*>>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(cls: KClass<T>): ClassInfo<T> =
        cacheData.getOrPut(cls) { ClassInfo(cls) } as ClassInfo<T>
}

class ClassInfo<T : Any>(cls: KClass<T>) {
    private val className = cls.qualifiedName
    private val constructor = cls.primaryConstructor  // primaryConstructor는 cls의 주 생성자 리턴
        ?: throw JKidException("Class ${cls.qualifiedName} doesn't have a primary constructor")

    // JSON 파일의 각 키에 해당하는 파라미터를 저장함.
    // cacheDataForParameter(), getConstructorParameter()에서 사용됨.
    private val jsonNameToParamMap = hashMapOf<String, KParameter>()

    // 각 파라미터에 대한 직렬화기(파라미터 타입에 맞는 ValueSerializer)를 저장함.
    private val paramToSerializerMap = hashMapOf<KParameter, ValueSerializer<out Any?>>()
    private val jsonNameToDeserializeClassMap = hashMapOf<String, Class<out Any>?>()

    init {
        constructor.parameters.forEach { cacheDataForParameter(cls, it) }
    }

    // 클래스 초기화 시 실행됨
    // 각 생성자 파라미터에 해당하는 프로퍼티를 찾아 애노테이션을 가져와 데이터를 세 가지 맵에 저장함.
    private fun cacheDataForParameter(cls: KClass<*>, param: KParameter) {
        val paramName = param.name
            ?: throw JKidException("Class $className has constructor parameter without name")

        // (1) JSON 파일의 각 키에 해당하는 파라미터를 찾아 저장함.
        val property = cls.declaredMemberProperties.find { it.name == paramName } ?: return
        val name = property.findAnnotation<JsonName>()?.name ?: paramName
        jsonNameToParamMap[name] = param

        // (2) @DeserializeInterface 애노테이션 인자로 지정한 클래스 타입 저장
        val deserializeClass =
            property.findAnnotation<DeserializeInterface>()?.targetClass?.java
        jsonNameToDeserializeClassMap[name] = deserializeClass


        // (3) 각 파라미터에 대해 적절한 직렬화기를 찾아 저장함.
        val valueSerializer = property.getSerializer()
            ?: property.findAnnotation<DateFormat>()?.format?.let { serializerForDateType(it) }
            ?: serializerForType(param.type.javaType)  // KParameter.type 프로퍼티를 활용하면 파라미터의 타입을 알 수 있음.
            ?: return
        paramToSerializerMap[param] = valueSerializer
    }

    fun getConstructorParameter(propertyName: String): KParameter =
        jsonNameToParamMap[propertyName]
            ?: throw JKidException("Constructor parameter $propertyName is not found for class $className")

    fun getDeserializeClass(propertyName: String) = jsonNameToDeserializeClassMap[propertyName]

    // setSimpleProperty에서 호출됨. 파라미터를 인자로 받으면 해당 파라미터에 맞는 직렬화기(ValueSerializer)를 찾아 직렬화함.
    fun deserializeConstructorArgument(param: KParameter, value: Any?): Any? {
        val serializer = paramToSerializerMap[param]
        if (serializer != null) return serializer.fromJsonValue(value)

        validateArgumentType(param, value)
        return value
    }

    private fun validateArgumentType(param: KParameter, value: Any?) {
        if (value == null && !param.type.isMarkedNullable) {
            throw JKidException("Received null value for non-null parameter ${param.name}")
        }
        if (value != null && value.javaClass != param.type.javaType) {
            throw JKidException(
                "Type mismatch for parameter ${param.name}: " +
                      "expected ${param.type.javaType}, found ${value.javaClass}"
            )
        }
    }

    // spawn 메서드에서 호출됨.
    fun createInstance(arguments: Map<KParameter, Any?>): T {
        ensureAllParametersPresent(arguments)
        return constructor.callBy(arguments)
    }

    // 필수 파라미터가 있는지 검증함.
    private fun ensureAllParametersPresent(arguments: Map<KParameter, Any?>) {
        for (param in constructor.parameters) {
            if (arguments[param] == null && !param.isOptional && !param.type.isMarkedNullable) {
                throw JKidException("Missing value for parameter ${param.name}")
            }
        }
    }
}

class JKidException(message: String) : Exception(message)
