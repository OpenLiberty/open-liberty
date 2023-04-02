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
package com.ibm.ws.cdi20.fat.apps.events.ejb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

/**
 * The expected order of events is...
 *
 * observeInit
 * observeEjbPostConstruct
 * test
 * observeEjbPreDestroy
 * observeDestroy
 */
@ApplicationScoped
public class EJBApplicationScopedBean {
    private volatile boolean initObserved = false;
    private volatile boolean testRequested = false;
    private volatile boolean postConstructObserved = false;
    private volatile boolean preDestroyObserved = false;
    private volatile boolean destroyObserved = false;

    protected String logMsg(String msg) {
        return this.getClass().getSimpleName() + "#" + this.hashCode() + ": " + msg;
    }

    protected void trace(String method) {
        System.out.println(logMsg(method));
    }

    public void observeInit(@Observes
    @Initialized(ApplicationScoped.class) Object event) {
        trace("observeInit");
        initObserved = true;
        //@Initialized(ApplicationScoped.class) should be seen before any request
        assertFalse(logMsg("@Initialized(ApplicationScoped.class) was observed after test"), testRequested);
        //@Initialized(ApplicationScoped.class) should be seen before EJB PostConstruct
        assertFalse(logMsg("@Initialized(ApplicationScoped.class) was observed after EJB PostConstruct"), postConstructObserved);
    }

    public void observeEjbPostConstruct() {
        trace("observeEjbPostConstruct");
        this.postConstructObserved = true;

        //EJB PostConstruct should be seen after @Initialized(ApplicationScoped.class)
        assertTrue(logMsg("EJB PostConstruct observed before @Initialized(ApplicationScoped.class)"), initObserved);
        //EJB PostConstruct should be seen before any request
        assertFalse(logMsg("EJB PostConstruct was observed after test"), testRequested);
    }

    public void test() {
        trace("test");
        testRequested = true;
        assertTrue(logMsg("@Initialized(ApplicationScoped.class) was not observed"), initObserved);

        //test should be seen before EJB PreDestroy
        assertFalse(logMsg("EJB PreDestroy was observed after Startup"), preDestroyObserved);
    }

    public void observeEjbPreDestroy() {
        trace("observeEjbPreDestroy");
        this.preDestroyObserved = true;

        //EJB PreDestroy should be seen after any test request
        assertTrue(logMsg("EJB PreDestroy was observed before test"), testRequested);

        //test should be seen before EJB PreDestroy
        assertFalse(logMsg("EJB PreDestroy was observed before Destroy"), destroyObserved);
    }

    public void observeDestroy(@Observes
    @BeforeDestroyed(ApplicationScoped.class) Object event) {
        trace("observeDestroy");
        this.destroyObserved = true;

        //@BeforeDestroyed(ApplicationScoped.class) should be seen after any request
        assertTrue(logMsg("Destroy was observed before test"), testRequested);
    }
}
