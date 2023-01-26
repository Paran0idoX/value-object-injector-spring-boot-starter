package com.para.valueobjectinjector;

import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;

import java.util.*;

//@Component
public class FieldConnectionRegistry {
    private final Map<Class<?>, List<FieldConnection>> fieldConnectionMap;

    private final Map<Class<?>, Class<?>> modelToRootMap;

    private boolean registerCompleted = false;

    public FieldConnectionRegistry(){
        fieldConnectionMap = new HashMap<>();
        modelToRootMap = new HashMap<>();
    }

    public void register(Class<?> modelClass, Class<?> rootClass){
        if (!registerCompleted){
            fieldConnectionMap.put(modelClass, ValueObjectInjectorUtil.getFieldConnectionList(modelClass, rootClass));
            modelToRootMap.put(modelClass, rootClass);
        }
    }

    public <M, R> M injectToModel(M model, R root){
        if (model == null || root == null || !fieldConnectionMap.containsKey(model.getClass())){
            return null;
        }
        Class<?> rootClass = modelToRootMap.get(model.getClass());
        if (rootClass != null && rootClass.equals(root.getClass())){
            fieldConnectionMap.get(model.getClass()).forEach(fieldConnection -> fieldConnection.inject(model, root));
        }
        return model;
    }

    public <M, R> R buildRootFromModel(M model, R root){
        if (model == null || root == null || !fieldConnectionMap.containsKey(model.getClass())){
            return null;
        }
        Class<?> rootClass = modelToRootMap.get(model.getClass());
        if (rootClass != null && rootClass.equals(root.getClass())){
            fieldConnectionMap.get(model.getClass()).forEach(fieldConnection -> fieldConnection.reverseInject(model, root));
        }
        return root;
    }

    public void complete(){
        registerCompleted = true;
    }
}
