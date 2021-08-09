/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal.bci;

final class MethodInfo {

    private final ProbeInjectionClassAdapter classAdapter;
    private final int accessFlags;
    private final String methodName;
    private final String descriptor;
    private final String signature;
    private final String[] declaredExceptions;

    MethodInfo(ProbeInjectionClassAdapter classAdapter, int accessFlags, String methodName, String descriptor, String signature, String[] exceptions) {
        this.classAdapter = classAdapter;
        this.accessFlags = accessFlags;
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.signature = signature;
        this.declaredExceptions = exceptions;
    }

    ProbeInjectionClassAdapter getClassAdapter() {
        return classAdapter;
    }

    String getMethodName() {
        return methodName;
    }

    int getAccessFlags() {
        return accessFlags;
    }

    String getDescriptor() {
        return descriptor;
    }

    String getSignature() {
        return signature;
    }

    String[] getDeclaredExceptions() {
        return declaredExceptions;
    }
}
