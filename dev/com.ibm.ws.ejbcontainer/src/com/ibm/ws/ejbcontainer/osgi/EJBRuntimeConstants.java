/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi;

public final class EJBRuntimeConstants {

    /**
     * This is the value of javax.enterprise.concurrent.ManagedTask.IDENTITY_NAME,
     * but is hard-coded here to avoid a dependency on the concurrency feature.
     */
    private static final String MANAGEDTASK_IDENTITY_NAME = "javax.enterprise.concurrent.IDENTITY_NAME";

    /**
     * Accessing through a method so that the String is not put into the caller's byte code if referenced the
     * static final variable which would end up requiring the caller to do a transform to convert it to jakarta.
     *
     * @return the constants for the ManagedTask identity name
     */
    public static String getManagedTaskIdentityName() {
        return MANAGEDTASK_IDENTITY_NAME;
    }
}
