/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.filemonitor.internal;

import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

/**
 * This class is useful for allowing JMock objects to just parrot back arguments
 * when invoked.
 */
class ArgumentEchoer<T> implements Action {

    public void describeTo(Description description) {
        description.appendText("mirroring argument");
    }

    public Object invoke(Invocation invocation) throws Throwable {
        return invocation.getParameter(0);
    }

    public static <T> Action echoArgument() {
        return new ArgumentEchoer<T>();
    }
}