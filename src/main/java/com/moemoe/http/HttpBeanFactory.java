package com.moemoe.http;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;

@Component
public class HttpBeanFactory implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Set<BeanDefinition> beanDefinitions = new HttpInterfaceClassFinder()
                .findBeanDefinitions(beanFactory.getBean(Environment.class));
        beanDefinitions.stream()
                .filter(beanDefinition -> StringUtils.hasText(beanDefinition.getBeanClassName()))
                .forEach(beanDefinition -> findClassAndRegisterAsSingletonBean(beanFactory, beanDefinition));
    }

    private void findClassAndRegisterAsSingletonBean(
            ConfigurableListableBeanFactory beanFactory,
            BeanDefinition beanDefinition) {
        SimpleHttpInterfaceFactory httpInterfaceFactory = new SimpleHttpInterfaceFactory();

        beanFactory.registerSingleton(
                Objects.requireNonNull(beanDefinition.getBeanClassName()),
                httpInterfaceFactory.create(findHttpInterfaceClass(beanDefinition))
        );
    }

    private Class<?> findHttpInterfaceClass(BeanDefinition beanDefinition) {
        try {
            return ClassUtils.forName(Objects.requireNonNull(beanDefinition.getBeanClassName()), this.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
