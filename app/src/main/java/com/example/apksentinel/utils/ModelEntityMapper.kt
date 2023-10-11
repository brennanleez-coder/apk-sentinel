package com.example.apksentinel.utils

import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties

object ModelEntityMapper {
    fun <T : Any, E : Any> mapModelToEntity(modelToMap: T, entityClass: Class<E>): E {
        val entity = entityClass.kotlin.createInstance()

        val objectProperties = modelToMap.javaClass.kotlin.memberProperties

        objectProperties.forEach { property ->
            val entityProperty = entityClass.kotlin.memberProperties.firstOrNull { it.name == property.name }

            if (entityProperty is KMutableProperty<*>) {
                val objectPropertyValue = property.getter.call(modelToMap)
                entityProperty.setter.call(entity, objectPropertyValue)
            }
        }

        return entity
    }
}
