/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package reactiveapp.web;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A CountDownLatch that can check if it has access to JNDI Context
 */
public class ContextCDLImpl implements ContextCDL {

    private final CountDownLatch cdl;

    public ContextCDLImpl(int count) {
        cdl = new CountDownLatch(count);
    }

    @Override
    public void countDown() {
        cdl.countDown();
    }

    @Override
    public void checkContext() throws NamingException {
        new InitialContext().lookup("java:comp/env/entry1");
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return cdl.await(timeout, unit);
    }

}
