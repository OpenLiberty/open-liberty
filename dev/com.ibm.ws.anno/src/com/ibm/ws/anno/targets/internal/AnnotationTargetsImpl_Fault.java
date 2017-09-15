/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.targets.internal;

import java.text.MessageFormat;

import com.ibm.wsspi.anno.targets.AnnotationTargets_Fault;

public class AnnotationTargetsImpl_Fault implements AnnotationTargets_Fault {

    protected static Object[] copyArray(Object[] sourceArray) {
        if (sourceArray == null) {
            return null;
        }

        Object[] targetArray = new Object[sourceArray.length];

        for (int elementNo = 0; elementNo < sourceArray.length; elementNo++) {
            targetArray[elementNo] = sourceArray[elementNo];
        }

        return targetArray;
    }

    //

    public AnnotationTargetsImpl_Fault(String unresolvedText) {
        this(unresolvedText, null);
    }

    public AnnotationTargetsImpl_Fault(String unresolvedText, Object[] parameters) {
        super();

        this.unresolvedText = unresolvedText;
        this.parameters = copyArray(parameters);

        this.resolvedText = null;

    }

    //

    protected final String unresolvedText;

    @Override
    public String getUnresolvedText() {
        return unresolvedText;
    }

    protected final Object[] parameters;

    @Override
    public int getParameterCount() {
        return ((parameters == null) ? 0 : parameters.length);
    }

    @Override
    public Object getParamater(int paramNo) {
        if (parameters == null) {
            throw new ArrayIndexOutOfBoundsException(paramNo);

        } else {
            return parameters[paramNo];
        }
    }

    @Override
    public Object[] getParameters() {
        // Copy the parameters
        return (parameters == null ? null : copyArray(parameters));
    }

    protected Object[] getRawParameters() {
        return parameters;
    }

    //

    protected String resolvedText;

    @Override
    public String getResolvedText() {
        if (resolvedText == null) {
            resolvedText = new MessageFormat(getUnresolvedText()).format(getRawParameters());
        }

        return resolvedText;
    }
}
