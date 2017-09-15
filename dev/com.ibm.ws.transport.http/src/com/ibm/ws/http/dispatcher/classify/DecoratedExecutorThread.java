/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.classify;

import java.util.concurrent.Executor;

/**
 * Class supports providing a reference to the Classified Executor for the current piece of work.
 */
public class DecoratedExecutorThread {

    private static ThreadLocal<Executor> currentExecutor = new ThreadLocal<Executor>();

    /**
     * Set the Classified Executor on the thread.
     * 
     * @param ex Classified Executor
     */
    public static void setExecutor(Executor ex) {
        currentExecutor.set(ex);
    }

    /**
     * @return Value for Classified Executor
     */
    public static Executor getExecutor() {
        return currentExecutor.get();
    }

}
