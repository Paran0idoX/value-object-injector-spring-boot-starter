package com.para.valueobjectinjector;

import com.para.valueobjectinjector.annotation.Model;
import com.para.valueobjectinjector.annotation.ModelScan;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ModelRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    private Environment environment;
    private ResourceLoader resourceLoader;

    private FieldConnectionRegistry fieldConnectionRegistry;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        fieldConnectionRegistry = new FieldConnectionRegistry();
        RootBeanDefinition rootBeanDefinition = new RootBeanDefinition(FieldConnectionRegistry.class, () -> fieldConnectionRegistry);
//        definition.setSynthetic(true);
        registry.registerBeanDefinition("fieldConnectionRegistry", rootBeanDefinition);
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Model.class));
        Set<String> basePackages = getBasePackages(importingClassMetadata);
        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidateComponent : candidateComponents) {
                if (candidateComponent instanceof AnnotatedBeanDefinition definition){
                    Map<String, Object> annotationAttributes = definition.getMetadata().getAnnotationAttributes(Model.class.getCanonicalName());
                    Class<?> rootClass = (Class<?>) annotationAttributes.get(Model.TARGET_CLASS);
                    Class<?> modelClass;
                    try {
                        modelClass = Class.forName(definition.getBeanClassName());
                    } catch (ClassNotFoundException e) {
                        log.error("[ValueObjectInjector] Class not found during fieldConnection registration.", e);
                        throw new RuntimeException(e);
                    }
                    fieldConnectionRegistry.register(modelClass, rootClass);
                    log.info("[ValueObjectInjector] FieldConnection built, modelClass : {}, rootClass: {}", modelClass.getCanonicalName(), rootClass.getCanonicalName());
                }
            }
        }
        fieldConnectionRegistry.complete();
        log.info("[ValueObjectInjector] FieldConnections has been completely registered.");
    }

    protected ClassPathScanningCandidateComponentProvider getScanner(){
        return new ClassPathScanningCandidateComponentProvider(false, environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isIndependent();
            }
        };
    }

    protected static Set<String> getBasePackages(AnnotationMetadata importingClassMetadata){
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(ModelScan.class.getCanonicalName());
        assert attributes != null;
        Set<String> basePackages = new HashSet<>();
        for (String pkg : (String[])attributes.get(ModelScan.BASE_PACKAGES)){
            if (StringUtils.hasText(pkg)){
                basePackages.add(pkg);
            }
        }
        if (basePackages.isEmpty()){
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }
        return basePackages;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
