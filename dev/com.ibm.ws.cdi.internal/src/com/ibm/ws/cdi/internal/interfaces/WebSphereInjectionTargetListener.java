/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.interfaces;

import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;

/**
 *
 */
public interface WebSphereInjectionTargetListener<T> {

    /**
     * @param injectionTarget
     */
    void injectionTargetProcessed(InjectionTarget injectionTarget);

    /**
     * @return
     */
    InjectionTargetContext getCurrentInjectionTargetContext();

    T getObject();

}
