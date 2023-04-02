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
package io.openliberty.cdi40.internal.fat.startupEvents.sharedLib;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Shutdown;
import jakarta.enterprise.event.Startup;

/**
 * The expected order of events is...
 *
 * observeInit
 * c_observeStartup1
 * a_observeStartup2
 * b_observeStartup3
 * test
 * observeShutdown
 * observeDestroy
 */
public abstract class AbstractObserver {

    protected volatile boolean initObserved = false;
    protected volatile boolean startupObserved1 = false;
    protected volatile boolean startupObserved2 = false;
    protected volatile boolean startupObserved3 = false;
    protected volatile boolean testRequested = false;
    protected volatile boolean shutdownObserved = false;
    protected volatile boolean destroyObserved = false;

    protected String logMsg(String msg) {
        return this.getClass().getSimpleName() + "#" + this.hashCode() + ": " + msg;
    }

    protected void trace(String method) {
        System.out.println(logMsg(method));
    }

    public void observeInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
        trace("observeInit");
        initObserved = true;
        //@Initialized(ApplicationScoped.class) should be seen before Startup
        assertFalse(logMsg("@Initialized(ApplicationScoped.class) was observed after Startup"), startupObserved1);
        //@Initialized(ApplicationScoped.class) should be seen before any request
        assertFalse(logMsg("@Initialized(ApplicationScoped.class) was observed after test"), testRequested);
    }

    //note the prefixes on these method names and the order they appear in the class is
    //designed to try and make sure that it is the Priority that decides the order, not some other factor
    public void a_observeStartup2(@Observes @Priority(2) Startup startupEvent) {
        trace("a_observeStartup2");
        startupObserved2 = true;
        //@Priority(1) Startup should be seen before @Priority(2) Startup
        assertTrue(logMsg("Startup 2 was observed before Startup 1"), startupObserved1);
        //Startup 2 should be seen before any request
        assertFalse(logMsg("Startup 2 was observed after test"), testRequested);
    }

    public void c_observeStartup1(@Observes @Priority(1) Startup startupEvent) {
        trace("c_observeStartup1");
        startupObserved1 = true;
        //@Initialized(ApplicationScoped.class) should be seen before Startup
        assertTrue(logMsg("Startup 1 was observed before @Initialized(ApplicationScoped.class)"), initObserved);
        //Startup 1 should be seen before any request
        assertFalse(logMsg("Startup 1 was observed after test"), testRequested);
    }

    public void b_observeStartup3(@Observes @Priority(3) Startup startupEvent) {
        trace("b_observeStartup3");
        startupObserved3 = true;
        //@Priority(2) Startup should be seen before @Priority(3) Startup
        assertTrue(logMsg("Startup 3 was observed before Startup 2"), startupObserved2);
        //Startup 3 should be seen before any request
        assertFalse(logMsg("Startup 3 was observed after test"), testRequested);
    }

    public void test() {
        trace("test");
        testRequested = true;
        assertTrue(logMsg("@Initialized(ApplicationScoped.class) was not observed"), initObserved);
        assertTrue(logMsg("test requested before Startup 1 was observed"), startupObserved1);
        assertTrue(logMsg("test requested before Startup 2 was observed"), startupObserved2);
        assertTrue(logMsg("test requested before Startup 3 was observed"), startupObserved3);
        assertFalse(logMsg("Shutdown observed before test requested"), shutdownObserved);
    }

    public void observeShutdown(@Observes Shutdown shutdownEvent) {
        trace("observeShutdown");
        shutdownObserved = true;
        //Shutdown should be seen after any request
        assertTrue(logMsg("Shutdown was observed before test"), testRequested);
        //Shutdown should be seen before @BeforeDestroyed(ApplicationScoped.class)
        assertFalse(logMsg("Shutdown was observed after @BeforeDestroyed(ApplicationScoped.class)"), destroyObserved);
    }

    public void observeDestroy(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
        trace("observeDestroy");
        destroyObserved = true;
        //@BeforeDestroyed(ApplicationScoped.class) should be seen after Shutdown
        assertTrue(logMsg("@BeforeDestroyed(ApplicationScoped.class) was observed before Shutdown"), shutdownObserved);
        //@BeforeDestroyed(ApplicationScoped.class) should be seen after any request
        assertTrue(logMsg("Shutdown was observed before test"), testRequested);
    }

}
