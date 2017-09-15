/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import com.ibm.ws.injectionengine.InternalInjectionEngine;

/**
 * Accessor for InternalInjectionEngine. <p>
 *
 * Used to expose package protected methods of InjectionEngineAccessor
 * within the injection.impl build component. <p>
 */
public final class InternalInjectionEngineAccessor
{
    /**
     * Do not allow instances to be created.
     */
    private InternalInjectionEngineAccessor()
    {
        //Private constructor to follow the singleton pattern
    }

    /**
     * Returns the single instance of the InternalInjectionEngine for the
     * current process.
     */
    public final static InternalInjectionEngine getInstance()
    {
        return InjectionEngineAccessor.getInternalInstance();
    }

    /**
     * Internal mechanism to support providing a server type specific
     * implementation of the InjectionEngine. <p>
     */
    public static void setInjectionEngine(InternalInjectionEngine ie)
    {
        InjectionEngineAccessor.setInjectionEngine(ie);
    }
}
