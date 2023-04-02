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
package com.ibm.ws.cdi20.fat.apps.events.war;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class WarApplicationScopedBean {
    private volatile boolean initObserved = false;
    private volatile boolean testRequested = false;

    protected String logMsg(String msg) {
        return this.getClass().getSimpleName() + "#" + this.hashCode() + ": " + msg;
    }

    protected void trace(String method) {
        System.out.println(logMsg(method));
    }

    public void test() {
        trace("test");
        testRequested = true;
        assertTrue(logMsg("@Initialized(ApplicationScoped.class) was not observed"), initObserved);
    }

    public void observeInit(@Observes
    @Initialized(ApplicationScoped.class) Object event) {
        trace("observeInit");
        initObserved = true;
        //@Initialized(ApplicationScoped.class) should be seen before any request
        assertFalse(logMsg("@Initialized(ApplicationScoped.class) was observed after test"), testRequested);
    }

    public void observeDestroy(@Observes
    @BeforeDestroyed(ApplicationScoped.class) Object event) {
        trace("observeDestroy");
        //@BeforeDestroyed(ApplicationScoped.class) should be seen after any request
        assertTrue(logMsg("Destroy was observed before test"), testRequested);
    }
}
