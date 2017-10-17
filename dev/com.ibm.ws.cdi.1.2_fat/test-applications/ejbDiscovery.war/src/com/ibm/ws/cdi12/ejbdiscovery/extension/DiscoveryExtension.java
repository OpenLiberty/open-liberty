package com.ibm.ws.cdi12.ejbdiscovery.extension;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;

public class DiscoveryExtension implements Extension {

    private Set<Class<?>> observedTypes = new HashSet<Class<?>>();
    private Set<Class<?>> observedBeans = new HashSet<Class<?>>();
    private Set<Type> observedBeanTypes = new HashSet<Type>();

    void processType(@Observes ProcessAnnotatedType<?> event) {
        observedTypes.add(event.getAnnotatedType().getJavaClass());
    }

    void processBean(@Observes ProcessBean<?> event) {
        observedBeans.add(event.getBean().getBeanClass());
        for (Type type : event.getBean().getTypes()) {
            observedBeanTypes.add(type);
        }
    }

    public Set<Class<?>> getObservedTypes() {
        return observedTypes;
    }

    public Set<Class<?>> getObservedBeans() {
        return observedBeans;
    }

    public Set<Type> getObservedBeanTypes() {
        return observedBeanTypes;
    }
}
