/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.context.serialization.app;

import java.io.Serializable;
import java.util.concurrent.Executor;

/**
 * Executor that runs a task on the current thread.
 * This isn't very useful on its own. However, in combination with ContextService, it can be
 * contextualized and used to store thread context for future use with any generic Runnable.
 * 
 * Executor contextualExecutor = contextService.createContextualProxy(new CurrentThreadExecutor(), Executor.class);
 * ...
 * contextualExecutor.execute(task);
 */
public class CurrentThreadExecutor implements Executor, Serializable {
    private static final long serialVersionUID = -1062424697272890090L;

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
