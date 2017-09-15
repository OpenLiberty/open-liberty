/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.objectweb.asm.Type;

public class MethodInfo {

    private final String methodName;
    private final String methodDescriptor;
    private boolean trivial;
    private boolean resultSensitive;
    private boolean[] argIsSensitive;
    private Set<Type> ffdcIgnoreExceptions = new LinkedHashSet<Type>();

    public MethodInfo(String name, String descriptor) {
        this.methodName = name;
        this.methodDescriptor = descriptor;
        this.argIsSensitive = new boolean[Type.getArgumentTypes(descriptor).length];
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }

    public String getMethodName() {
        return methodName;
    }

    public void addFFDCIgnoreException(Type exceptionType) {
        ffdcIgnoreExceptions.add(exceptionType);
    }

    public Set<Type> getFFDCIgnoreExceptions() {
        return ffdcIgnoreExceptions;
    }

    public boolean isResultSensitive() {
        return resultSensitive;
    }

    public void setResultSensitive(boolean sensitive) {
        this.resultSensitive = sensitive;
    }

    public boolean isTrivial() {
        return trivial;
    }

    public void setTrivial(boolean trivial) {
        this.trivial = trivial;
    }

    public boolean isArgSensitive(int index) {
        if (index < argIsSensitive.length) {
            return argIsSensitive[index];
        }
        return false;
    }

    public void setArgIsSensitive(int index, boolean isSensitive) {
        this.argIsSensitive[index] = isSensitive;
    }

    public void updateDefaultValuesFromClassInfo(ClassInfo classInfo) {
        if (!trivial) {
            trivial = classInfo.isTrivial();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";methodName=").append(methodName);
        sb.append(",methodDescriptor=").append(methodDescriptor);
        sb.append(",trivial=").append(trivial);
        sb.append(",resultSensitive=").append(resultSensitive);
        sb.append(",argIsSensitive=").append(Arrays.toString(argIsSensitive));
        sb.append(",ffdcIgnoreExceptions=").append(ffdcIgnoreExceptions);
        return sb.toString();
    }
}
