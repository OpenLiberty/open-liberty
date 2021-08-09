/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.interfaces;

import org.jboss.weld.injection.spi.InjectionServices;

import com.ibm.ws.cdi.CDIException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTarget;

public interface WebSphereInjectionServices extends InjectionServices {

    /**
     * @param targetClass
     * @return
     * @throws CDIException
     */
    public InjectionTarget[] getInjectionTargets(Class<?> targetClass) throws InjectionException;

    /**
     * @param listener
     * @return
     * @throws InjectionException
     */
    public void registerInjectionTargetListener(WebSphereInjectionTargetListener<?> listener);

    /**
     * @param listener
     */
    public void deregisterInjectionTargetListener(WebSphereInjectionTargetListener<?> listener);

}
