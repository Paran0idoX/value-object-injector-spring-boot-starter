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
            rootField.setAccessible(true);
            try {
                Object rootFieldValue = rootField.get(root);
                if (rootFieldValue == null){
                    return;
                }
                for (Map.Entry<Field, Field> entry : rootToModelFieldMap.entrySet()) {
                    entry.getValue().setAccessible(true);
                    entry.getKey().setAccessible(true);
                    entry.getValue().set(model, entry.getKey().get(rootFieldValue));
                }
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
            }
        }
    }

    public void reverseInject(Object model, Object root){
        if (model == null || root == null){
            return;
        }
        injectFieldList.forEach(field -> field.setAccessible(true));
        if (directInject){
            ValueObjectInjectorUtil.valueInject(rootField, injectFieldList.get(0), root, model);
        } else {
            try {
                rootField.setAccessible(true);
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
                            entry.getKey().setAccessible(true);
                            entry.getValue().setAccessible(true);
                            entry.getValue().set(valueObject, entry.getKey().get(model));
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
        if (!directInject){
            rootToModelFieldMap = HashBiMap.create(injectFieldList.size());
            for (Field field : injectFieldList) {
                Field valueObjectField = ValueObjectInjectorUtil.getInjectValueProviderById(rootField.getType(),
                        AnnotatedElementUtils.isAnnotated(field, InjectInfo.class) ? field.getAnnotation(InjectInfo.class).injectValueId() : InjectValue.DEFAULT_VALUE_ID);
                rootToModelFieldMap.put(valueObjectField, field);
            }
            inverseMap = rootToModelFieldMap.inverse();
        }else {
            rootToModelFieldMap = null;
            inverseMap = null;
        }
    }
}
