/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi;

import javax.enterprise.inject.spi.BeanManager;

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

}
