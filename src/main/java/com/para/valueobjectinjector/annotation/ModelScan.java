package com.para.valueobjectinjector.annotation;

import com.para.valueobjectinjector.FieldConnectionRegistry;
import com.para.valueobjectinjector.ModelRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(ModelRegistrar.class)
public @interface ModelScan {
    String[] basePackages();

    String BASE_PACKAGES = "basePackages";
}
