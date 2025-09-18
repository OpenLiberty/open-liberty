/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package concurrent.cdi4.webapp;

import static org.junit.Assert.assertNotNull;

import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * An application scoped bean that injects a default managed thread factory
 * and runs a provided task using a new thread
 */
@RequestScoped
public class TestBean {

    @Inject
    private ManagedThreadFactory defaultManagedThreadFactory;

    public void runTaskUsingDefaultManagedThreadFactory(Runnable task) {
        assertNotNull(defaultManagedThreadFactory);
        Thread thread = defaultManagedThreadFactory.newThread(task);
        thread.start();
    }
}
