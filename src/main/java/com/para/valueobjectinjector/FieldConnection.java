package com.para.valueobjectinjector;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.para.valueobjectinjector.annotation.InjectInfo;
import com.para.valueobjectinjector.annotation.InjectValue;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class FieldConnection{
    private final List<Field> injectFieldList;
    private final Field rootField;
    private final boolean directInject;
    private final Class<?> rootClass;
    private final Class<?> modelClass;
    private final BiMap<Field, Field> rootToModelFieldMap;

    private final BiMap<Field, Field> inverseMap;

    public void inject(Object model, Object root){
        if (model == null || root == null){
            return;
        }
        injectFieldList.forEach(field -> field.setAccessible(true));
        if (directInject){
            ValueObjectInjectorUtil.valueInject(injectFieldList.get(0), rootField, model, root);
        } else {
            try {
                Object rootFieldValue = rootField.get(root);
                if (rootFieldValue == null){
                    return;
                }
                for (Map.Entry<Field, Field> entry : rootToModelFieldMap.entrySet()) {
                    Field valueObjectField = entry.getKey();
                    Field modelField = entry.getValue();
                    Object value = valueObjectField.get(rootFieldValue);
                    if (value != null && Enum.class.isAssignableFrom(valueObjectField.getType())){
                        value = ValueObjectInjectorUtil.getInjectValueProviderById(valueObjectField.getType(), InjectValue.DEFAULT_VALUE_ID)
                                .get(value);
                    }
                    modelField.set(model, value);
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                log.error(e.getMessage());
            }
        }
    }

    public void reverseInject(Object model, Object root){
        if (model == null || root == null){
            return;
        }
        if (directInject){
            ValueObjectInjectorUtil.valueInject(rootField, injectFieldList.get(0), root, model);
        } else {
            try {
                if (Enum.class.isAssignableFrom(rootField.getType())){
                    try {
                        Enum enumValue = ValueObjectInjectorUtil.getEnumObjectByValue(
                                (Class<? extends Enum>) rootField.getType(), injectFieldList.get(0).get(model));
                        rootField.set(root, enumValue);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    Constructor<?> constructor = rootField.getType().getConstructor();
                    constructor.setAccessible(true);
                    try {
                        Object valueObject = constructor.newInstance();
                        for (Map.Entry<Field, Field> entry : inverseMap.entrySet()) {
                            Field valueObjectField = entry.getValue();
                            Field modelField = entry.getKey();
                            if (Enum.class.isAssignableFrom(valueObjectField.getType())){
                                try {
                                    Enum enumValue = ValueObjectInjectorUtil.getEnumObjectByValue(
                                            (Class<? extends Enum>) valueObjectField.getType(), modelField.get(model));
                                    valueObjectField.set(valueObject, enumValue);
                                } catch (NoSuchFieldException | IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            }else {
                                valueObjectField.set(valueObject, modelField.get(model));
                            }
                        }
                        rootField.set(root, valueObject);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
            } catch (NoSuchMethodException e) {
                log.error("Class {} has no NoArgsConstructor", rootField.getType(), e);
            }
        }
    }

    public FieldConnection(List<Field> injectFieldList, Field rootField, boolean directInject, Class<?> rootClass, Class<?> modelClass) throws NoSuchFieldException {
        this.injectFieldList = injectFieldList;
        this.rootField = rootField;
        this.directInject = directInject;
        this.rootClass = rootClass;
        this.modelClass = modelClass;
        this.rootField.setAccessible(true);
        if (!directInject){
            rootToModelFieldMap = HashBiMap.create(injectFieldList.size());
            for (Field field : injectFieldList) {
                Field valueObjectField = ValueObjectInjectorUtil.getInjectValueProviderById(rootField.getType(),
                        AnnotatedElementUtils.isAnnotated(field, InjectInfo.class) ? field.getAnnotation(InjectInfo.class).injectValueId() : InjectValue.DEFAULT_VALUE_ID);
                field.setAccessible(true);
                valueObjectField.setAccessible(true);
                rootToModelFieldMap.put(valueObjectField, field);
            }
            inverseMap = rootToModelFieldMap.inverse();
        }else {
            injectFieldList.get(0).setAccessible(true);;
            rootToModelFieldMap = null;
            inverseMap = null;
        }
    }
}
