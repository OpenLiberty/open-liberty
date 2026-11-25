/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Object returned by the JDNI cache which represents a target EJB.
 */
public final class WOLAInboundTarget {

    /**
     * The EJB instance that will be driven.
     */
    private final Object ejbInstance;

    /**
     * The method on the EJB that will be driven.
     */
    private final Method executeMethod;

    /**
     * Initialize the target.
     */
    WOLAInboundTarget(Object bean, Method executeMethod) {
        ejbInstance = bean;
        this.executeMethod = executeMethod;
    }

    /**
     * Invoke the target WOLA EJB.
     *
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public byte[] execute(byte[] inputBytes) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return (byte[]) executeMethod.invoke(ejbInstance, inputBytes);
    }
}
