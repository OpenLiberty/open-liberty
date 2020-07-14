/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transactional.web;

import javax.transaction.Synchronization;

/**
 *
 */
public class TestBeanSync implements Synchronization {
    private final TestContext testContext;

    TestBeanSync(TestContext tc) {
        testContext = tc;
    }

    @Override
    public void afterCompletion(int status) {
        testContext.setStatus(status);
    }

    @Override
    public void beforeCompletion() {
    }
}