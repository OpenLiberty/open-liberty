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
package concurrent.ejb.standalone.jar;

import static org.junit.Assert.assertNotNull;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.inject.Inject;

import concurrent.ejb.shared.SharedInvoker;

@Stateless
@Local(SharedInvoker.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class StandaloneBean implements SharedInvoker {

    @Inject
    private ManagedThreadFactory defaultManagedThreadFactory;

    @Override
    public void runTaskUsingDefaultManagedThreadFactory(Runnable task) {
        assertNotNull(defaultManagedThreadFactory);
        Thread thread = defaultManagedThreadFactory.newThread(task);
        thread.start();
    }

}
