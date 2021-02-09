/*******************************************************************************
 * Copyright (c) 2013,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.threadfactory;

import java.util.concurrent.Executor;

/**
 * Executor that runs tasks on the current thread.
 * This is useless on its own, but if we use a ContextService to contextualize it,
 * then it can be used to store thread context and apply it to any Runnable that
 * a test bucket wants to execute.
 */
public class CurrentThreadExecutor implements Executor {
    /**
     * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
     */
    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
