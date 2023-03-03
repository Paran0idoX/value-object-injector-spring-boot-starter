package com.para.valueobjectinjector;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.para.valueobjectinjector.annotation.InjectIgnore;
import com.para.valueobjectinjector.annotation.InjectInfo;
import com.para.valueobjectinjector.annotation.InjectValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ValueObjectInjectorUtil {

    public static Field getInjectValueProviderById(Class valueObjectClass, int injectValueId) throws NoSuchFieldException {
        Field[] declaredFields = FieldUtils.getFieldsWithAnnotation(valueObjectClass, InjectValue.class);
        for (Field declaredField : declaredFields) {
            int modelInjectValueId = declaredField.getAnnotation(InjectValue.class).value();
            if (modelInjectValueId == injectValueId){
                declaredField.setAccessible(true);
                return declaredField;
            }
        }
        NoSuchFieldException noSuchFieldException =
                new NoSuchFieldException("modelFieldValueObject doesn't have such injectValue");
        log.error("[ValueObjectInjector] modelFieldValueObject doesn't have such injectValue. ValueObjectClass: {}, injectValueId: {}",
                valueObjectClass.getCanonicalName(), injectValueId);
        throw noSuchFieldException;
    }

    public static void valueInject(Field injectedField, Field valueField, Object injectedObject, Object valueObject){
        try {
            Object value = valueField.get(valueObject);
            injectedField.set(injectedObject, value);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage());
        }
    }

    private static boolean directInject(Field rootField, List<Field> fieldList){
        return fieldList.size() == 1 && rootField.getType().equals(fieldList.get(0).getType());
    }

    public static List<FieldConnection> getFieldConnectionList(Class<?> modelClass, Class<?> rootClass){
        List<Field> valueObjectInjectFieldList = FieldUtils.getAllFieldsList(modelClass).stream()
                .filter(field -> !Modifier.isFinal(field.getModifiers()) && !AnnotatedElementUtils.isAnnotated(field, InjectIgnore.class))
                .collect(Collectors.toList());
        Map<Field, List<Field>> fieldMap = Maps.newHashMapWithExpectedSize(valueObjectInjectFieldList.size());
        for (Field field : valueObjectInjectFieldList) {
            //Root中需注入的Field名称
            String rootFieldName = AnnotatedElementUtils.isAnnotated(field, InjectInfo.class) ?
                    field.getAnnotation(InjectInfo.class).modelFieldName() : field.getName();
            if (StringUtils.isBlank(rootFieldName)){
                rootFieldName = field.getName();
            }
            Field rootField = null;
            try {
                rootField = rootClass.getDeclaredField(rootFieldName);
            } catch (NoSuchFieldException e) {
                log.error("{} has no such field as {}", rootClass.getCanonicalName(), rootFieldName);
            }
            if (rootField == null){
                log.error("rootField null, class:{}, fieldName:{}", rootClass.getCanonicalName(), rootFieldName);
            }
            if (fieldMap.containsKey(rootField)){
                fieldMap.get(rootField).add(field);
            } else {
                fieldMap.put(rootField, Lists.newArrayList(field));
            }
        }
        List<FieldConnection> fieldConnectionList = new ArrayList<>();
        for (Map.Entry<Field, List<Field>> entry : fieldMap.entrySet()) {
            Field rootField = entry.getKey();
            List<Field> fieldList = entry.getValue();
            try {
                fieldConnectionList.add(new FieldConnection(fieldList, rootField, directInject(rootField, fieldList), rootClass, modelClass));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return fieldConnectionList;
    }

    public static Enum getEnumObjectByValue(Class<? extends Enum> enumClass, Object value) throws NoSuchFieldException, IllegalAccessException {
        Enum[] enumConstants = enumClass.getEnumConstants();
        for (Enum enumConstant : enumConstants) {
            Field valueField = getInjectValueProviderById(enumClass, InjectValue.DEFAULT_VALUE_ID);
            valueField.setAccessible(true);
            if (valueField.get(enumConstant).equals(value)){
                return enumConstant;
            }
        }
        return null;
    }
}
