/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.v11.cdi.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.validation.ValidationException;

import org.apache.bval.cdi.AnyLiteral;
import org.apache.bval.cdi.BValExtension;
import org.apache.bval.cdi.DefaultLiteral;

import com.ibm.ejs.util.dopriv.SetContextClassLoaderPrivileged;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.service.BeanValidationExtensionHelper;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * This is a Bean class created for org.apache.bval.cdi.BValExtension.
 * org.apache.bval.cdi.BValInterceptor injects BValExtension, and since BValExtension is not
 * registered as an extension service it cannot be injected unless defined as a Bean.
 */
public class BValExtensionBean implements Bean<BValExtension>, PassivationCapable {
    private static final TraceComponent TC = Tr.register(BValExtensionBean.class);
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;

    private static final PrivilegedAction<BValExtension> getBValExtensionAction = 
                    new PrivilegedAction<BValExtension>() {
                        @Override
                        public BValExtension run() {
                            return new BValExtension();
                        }
                    };
    
    private static final PrivilegedAction<ThreadContextAccessor> getThreadContextAccessorAction =
                    new PrivilegedAction<ThreadContextAccessor>() {
                        @Override
                        public ThreadContextAccessor run() {
                            return ThreadContextAccessor.getThreadContextAccessor();
                        }
                    };

    public BValExtensionBean() {
        types = new HashSet<Type>();
        types.add(BValExtension.class);
        types.add(Object.class);

        qualifiers = new HashSet<Annotation>();
        qualifiers.add(DefaultLiteral.INSTANCE);
        qualifiers.add(AnyLiteral.INSTANCE);
    }

    @Override
    public BValExtension create(CreationalContext<BValExtension> creationalContext) {
        SetContextClassLoaderPrivileged setClassLoader = null;
        ClassLoader oldClassLoader = null;
        ClassLoader classLoader = null;
        BValExtension bValExtension;

        try {
            ThreadContextAccessor tca = System.getSecurityManager() == null ?
                            ThreadContextAccessor.getThreadContextAccessor() :
                            AccessController.doPrivileged(getThreadContextAccessorAction);
            classLoader = tca.getContextClassLoader(Thread.currentThread());

            //Use customer classloader to handle multiple validation.xml being in the same ear.
            classLoader = BeanValidationExtensionHelper.newValidationClassLoader(classLoader);

            // set the thread context class loader to be used, must be reset in finally block
            setClassLoader = new SetContextClassLoaderPrivileged(tca);
            oldClassLoader = setClassLoader.execute(classLoader);
            if (TraceComponent.isAnyTracingEnabled() && TC.isDebugEnabled()) {
                Tr.debug(TC, "Called setClassLoader with oldClassLoader of " + oldClassLoader + " and newClassLoader of " + classLoader);
            }

            //create a BValExtension with a ValidationClassLoader set.
            bValExtension = null;
            if (System.getSecurityManager() == null)
            	bValExtension = new BValExtension();
            else
            	bValExtension = AccessController.doPrivileged(getBValExtensionAction);
        } catch (ValidationException e) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Returning a null Configuration: " + e.getMessage());
            }
            bValExtension = null;
        } finally {
            if (setClassLoader != null) {
                setClassLoader.execute(oldClassLoader);
                if (TraceComponent.isAnyTracingEnabled() && TC.isDebugEnabled()) {
                    Tr.debug(TC, "Set Class loader back to " + oldClassLoader);
                }
            }

        }
        return bValExtension;
    }

    @Override
    public void destroy(BValExtension instance, CreationalContext<BValExtension> creationalContext) {

    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getId() {
        return "BValExtension: " + hashCode();
    }

    @Override
    public Class<?> getBeanClass() {
        return BValExtension.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

}
