/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.csi;

import java.lang.reflect.InvocationHandler;

/**
 * This interface is implemented by the InvocationHandler object that is used by
 * the generated JCA 1.5 MessageEndpoint proxy object. This interface allows the
 * websphere test resource adapter to call
 * java.lang.reflect.Proxy.getInvocationHandler() method and cast the object
 * returned to this interface. The purpose of this interface is to provide a
 * hook from the test resource adapter to the invocation handler used by the
 * message endpoint so that a "test results" object create by the test resource
 * adapter can be made know to the invocation handler. This allows both the test
 * RA and the invocation handler to both store results in the object and it
 * allows the test RA to return this object to the FVT testcase for verification
 * of the test results.
 */
public interface MessageEndpointInvocationHandler extends InvocationHandler {
    /**
     * This method is used by test resource adapter to make known the
     * MessageEndpointTestResults object to the InvocationHandler.
     *
     *
     * @param results
     *            is the object to use for storing test results. The
     *            InvocationHandler must keep a reference to this object until
     *            the unsetTestResults is called by test RA.
     */
    public void setTestResults(MessageEndpointTestResults results);

    /**
     * This method is used by test resource adapter to cause
     * MessageEndpointInvocationHandler to destroy its reference to the test
     * results object made know to it by the setTestResults method.
     */
    public void unsetTestResults();
}
