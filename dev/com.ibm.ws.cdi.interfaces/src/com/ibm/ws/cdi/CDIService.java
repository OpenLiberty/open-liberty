/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.inject.spi.BeanManager;
import javax.interceptor.InvocationContext;

/**
 * Provides access to CDI classes
 */
public interface CDIService {

    /**
     * Gets the bean manager for the calling class (obtained by walking the stack looking for a class which is in a BDA) or
     * for the current module ({@link #getCurrentModuleBeanManager()}) if there are no BDA classes on the stack.
     *
     * In most cases getCurrentBeanManager should be used instead of ({@link #getCurrentModuleBeanManager()}) and all calls
     * to getCurrentBeanManager should be cached.
     *
     * @return the current bean manager
     */
    public BeanManager getCurrentBeanManager();

    /**
     * Gets the bean manager for the current module
     *
     * @return the bean manager for the current module
     */
    public BeanManager getCurrentModuleBeanManager();

    /**
     * Return the context ID used by CDI to identify the current application. The exact format should not be relied on
     * and it is recommended that this method should only be used for debug and trace. At time of writing, the
     * string happens to be the same as the application's J2EEName. The current application is determined
     * using ComponentMetaDataAccessorImpl.
     *
     * @return the current application context id
     */
    public String getCurrentApplicationContextID();

    /**
     * Returns whether CDI is enabled for the current module.
     *
     * @return true if the current module, or any module or libraries it can access, has any CDI Beans
     */
    public boolean isCurrentModuleCDIEnabled();

    /**
     * Returns whether a class is a weld proxy subclass.
     *
     * @return true if clazz is a weld proxy subclass.
     */
    public boolean isWeldProxy(Class clazz);

    /**
     * Returns whether an object is an instance of weld proxy subclass.
     *
     * @return true if obj is an instance of a weld proxy subclass.
     */
    public boolean isWeldProxy(Object obj);

    /**
     * Returns all interceptor bindings which apply to the current invocation or lifecycle event.
     *
     * @return a set of interceptor bindings which apply to the current invocation or lifecycle event. This will include all interceptor bindings that apply, not just those that were used to bind the current interceptor.
     * @throws IllegalArgumentException if InvocationContext is not an instance of org.jboss.weld.interceptor.proxy.AbstractInvocationContext;
     */
    public Set<Annotation> getInterceptorBindingsFromInvocationContext(InvocationContext ic) throws IllegalArgumentException;

}
