/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.ejbdiscovery.extension;

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
