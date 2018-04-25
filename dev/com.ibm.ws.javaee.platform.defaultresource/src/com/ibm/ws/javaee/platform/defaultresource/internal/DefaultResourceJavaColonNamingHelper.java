/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.platform.defaultresource.internal;

import java.security.AccessController;
import java.util.Collection;
import java.util.Collections;

import javax.naming.NameClassPair;
import javax.naming.NamingException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.container.service.naming.JavaColonNamespaceBindings;
import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.resource.ResourceFactory;

@Component(service = JavaColonNamingHelper.class,
// Indication to injection that default resources are enabled.
           property = "javaCompDefault:Boolean=true")
public class DefaultResourceJavaColonNamingHelper implements JavaColonNamingHelper, JavaColonNamespaceBindings.ClassNameProvider<DefaultResourceJavaColonNamingHelper.Binding> {
    private static final String JAVA_COMP_DEFAULT_NAME = com.ibm.ws.resource.ResourceFactory.JAVA_COMP_DEFAULT_NAME;
    private final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private static final String REFERENCE_RESOURCE_FACTORIES = "resourceFactories";

    static class Binding {
        private final ServiceReference<ResourceFactory> reference;
        final String className;
        private ResourceFactory factory;

        Binding(ServiceReference<ResourceFactory> reference, String className) {
            this.reference = reference;
            this.className = className;
        }

        synchronized ResourceFactory getResourceFactory(final ComponentContext context) {
            if (factory == null) {
                factory = priv.locateService(context, REFERENCE_RESOURCE_FACTORIES, reference);
            }
            return factory;
        }
    }

    private final JavaColonNamespaceBindings<Binding> bindings = new JavaColonNamespaceBindings<Binding>(NamingConstants.JavaColonNamespace.COMP, this);
    private ComponentContext context;

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
    }

    private static String getPrimaryCreatesObjectClass(Object createsObjectClass) {
        if (createsObjectClass instanceof String[]) {
            String[] createsObjectClasses = (String[]) createsObjectClass;
            if (createsObjectClasses.length > 0) {
                return createsObjectClasses[0];
            }
        } else if (createsObjectClass instanceof String) {
            return (String) createsObjectClass;
        }
        return null;
    }

    @Reference(name = REFERENCE_RESOURCE_FACTORIES,
               service = ResourceFactory.class,
               cardinality = ReferenceCardinality.AT_LEAST_ONE,
               policy = ReferencePolicy.DYNAMIC,
               target = "(&(" + ResourceFactory.CREATES_OBJECT_CLASS + "=*)(" + JAVA_COMP_DEFAULT_NAME + "=*))")
    protected void addResourceFactory(ServiceReference<ResourceFactory> reference) {
        String defaultName = (String) reference.getProperty(JAVA_COMP_DEFAULT_NAME);
        String className = getPrimaryCreatesObjectClass(reference.getProperty(ResourceFactory.CREATES_OBJECT_CLASS));
        synchronized (bindings) {
            bindings.bind(defaultName, new Binding(reference, className));
        }
    }

    protected void removeResourceFactory(ServiceReference<ResourceFactory> reference) {
        String defaultName = (String) reference.getProperty(JAVA_COMP_DEFAULT_NAME);
        synchronized (bindings) {
            bindings.unbind(defaultName);
        }
    }

    @Override
    public Object getObjectInstance(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        if (namespace == NamingConstants.JavaColonNamespace.COMP) {
            Binding binding;
            synchronized (bindings) {
                binding = bindings.lookup(name);
            }
            if (binding != null) {
                ResourceFactory factory = binding.getResourceFactory(context);
                if (factory != null) {
                    try {
                        return factory.createResource(null);
                    } catch (Exception e) {
                        NamingException ne = new NamingException();
                        ne.setRootCause(e);
                        throw ne;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean hasObjectWithPrefix(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        if (namespace == NamingConstants.JavaColonNamespace.COMP) {
            synchronized (bindings) {
                return bindings.hasObjectWithPrefix(name);
            }
        }
        return false;
    }

    @Override
    public Collection<? extends NameClassPair> listInstances(NamingConstants.JavaColonNamespace namespace, String nameInContext) throws NamingException {
        if (namespace == NamingConstants.JavaColonNamespace.COMP) {
            synchronized (bindings) {
                return bindings.listInstances(nameInContext);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String getBindingClassName(Binding binding) {
        return binding.className;
    }
}
