/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.service;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ThreadContext;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.ManagedExecutorBuilderImpl;
import com.ibm.ws.concurrent.mp.ThreadContextBuilderImpl;

/**
 * Utility class that allows the ConcurrencyCDIExtension to set names for built instances.
 */
@Trivial
public class ConcurrencyCDIHelper {
    public static void setName(ManagedExecutor.Builder builder, String injectionPointName) {
        ((ManagedExecutorBuilderImpl) builder).name(injectionPointName);
    }

    public static void setName(ThreadContext.Builder builder, String injectionPointName) {
        ((ThreadContextBuilderImpl) builder).name(injectionPointName);
    }
}