/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.jakarta.concurrency.web;

/**
 * Runs tasks inline on the same thread.
 * Tests can use ContextService contextualize serializable proxies for this class
 * in order to test how context behaves after deserialization.
 */
public class SameThreadExecutor implements Ser2Executor {
    private static final long serialVersionUID = 1L;

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
