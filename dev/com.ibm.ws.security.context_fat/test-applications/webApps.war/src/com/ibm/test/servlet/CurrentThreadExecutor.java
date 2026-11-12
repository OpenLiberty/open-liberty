/*******************************************************************************
 * Copyright (c) 2013, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.test.servlet;

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

	/**
	 * Serial version 
	 */
	private static final long serialVersionUID = -5388601697286898846L;

	@Override
    public void execute(Runnable command) {
        command.run();
    }
}
