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
package com.ibm.ws.ejbcontainer.jitdeploy;

public class TestMethodCalls
{
    final String ivOperation;
    final boolean ivApplicationException;
    private final TestMethodCall[] ivCalls;
    private int ivCallIndex;

    TestMethodCalls(String operation, boolean appEx, TestMethodCall[] calls)
    {
        ivOperation = operation;
        ivApplicationException = appEx;
        ivCalls = calls;
    }

    public Object invoke(String methodName, Object... args)
    {
        return ivCalls[ivCallIndex++].invoke(methodName, args);
    }
}
