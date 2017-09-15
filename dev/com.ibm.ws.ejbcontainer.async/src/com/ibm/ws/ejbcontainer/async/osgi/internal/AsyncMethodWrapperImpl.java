/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.osgi.internal;

import com.ibm.ejs.container.AsyncMethodWrapper;
import com.ibm.ejs.container.EJSWrapperBase;

public class AsyncMethodWrapperImpl extends AsyncMethodWrapper {
    private final ServerAsyncResultImpl serverFuture;

    public AsyncMethodWrapperImpl(EJSWrapperBase theCallingWrapper,
                                  int theMethodId, Object[] theMethodArgs,
                                  ServerAsyncResultImpl theServerFuture) {
        super(theCallingWrapper, theMethodId, theMethodArgs, theServerFuture);
        serverFuture = theServerFuture;
    }

    @Override
    public void run() {
        // If another thread has cancelled the task before we started running,
        // then just do nothing.
        if (serverFuture == null || serverFuture.runOrCancel()) {
            super.run();
        }
    }
}