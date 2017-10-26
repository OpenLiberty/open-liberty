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
package com.ibm.ws.zos.core.internal;

/**
 * The real NativeServiceTracker class is located in the com.ibm.ws.zos.core component. This
 * version is a stub that is required by the z/OS native unit test code so that the compiler
 * has access to the inner class NativeServiceTracker$ServiceResults, which is a return value
 * from some of the native functions used to register native methods.
 */
public class NativeServiceTracker {
    /**
     * Helper class to hold return code data from native services.
     */
    public final static class ServiceResults {
        public final int returnValue;
        public final int returnCode;
        public final int reasonCode;

        ServiceResults(int returnValue, int returnCode, int reasonCode) {
            this.returnValue = returnValue;
            this.returnCode = returnCode;
            this.reasonCode = reasonCode;
        }
    }
}
