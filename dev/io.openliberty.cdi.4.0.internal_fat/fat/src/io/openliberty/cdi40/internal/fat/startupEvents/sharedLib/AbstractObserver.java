/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    private volatile boolean initObserved = false;
    private volatile boolean startupObserved1 = false;
    private volatile boolean startupObserved2 = false;
    private volatile boolean startupObserved3 = false;
    private volatile boolean testRequested = false;
    private volatile boolean shutdownObserved = false;
    private volatile boolean destroyObserved = false;

    public void test() {
        System.out.println(this.getClass().getSimpleName() + ": test");
        testRequested = true;
        assertTrue("@Initialized(ApplicationScoped.class) was not observed", initObserved);
        assertTrue("Startup 1 was not observed", startupObserved1);
        assertTrue("Startup 2 was not observed", startupObserved2);
        assertTrue("Startup 3 was not observed", startupObserved3);
    }

    public void observeInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
        System.out.println(this.getClass().getSimpleName() + ": observeInit");
        initObserved = true;
        //@Initialized(ApplicationScoped.class) should be seen before Startup
        assertFalse("@Initialized(ApplicationScoped.class) was observed after Startup", startupObserved1);
        //@Initialized(ApplicationScoped.class) should be seen before any request
        assertFalse("@Initialized(ApplicationScoped.class) was observed after test", testRequested);
    }

    //note the prefixes on these method names and the order they appear in the class is
    //designed to try and make sure that it is the Priority that decides the order, not some other factor
    public void a_observeStartup2(@Observes @Priority(2) Startup startupEvent) {
        System.out.println(this.getClass().getSimpleName() + ": a_observeStartup2");
        startupObserved2 = true;
        //@Priority(1) Startup should be seen before @Priority(2) Startup
        assertTrue("Startup 2 was observed before Startup 1", startupObserved1);
        //Startup 2 should be seen before any request
        assertFalse("Startup 2 was observed after test", testRequested);
    }

    public void c_observeStartup1(@Observes @Priority(1) Startup startupEvent) {
        System.out.println(this.getClass().getSimpleName() + ": c_observeStartup1");
        startupObserved1 = true;
        //@Initialized(ApplicationScoped.class) should be seen before Startup
        assertTrue("Startup 1 was observed before @Initialized(ApplicationScoped.class)", initObserved);
        //Startup 1 should be seen before any request
        assertFalse("Startup 1 was observed after test", testRequested);
    }

    public void b_observeStartup3(@Observes @Priority(3) Startup startupEvent) {
        System.out.println(this.getClass().getSimpleName() + ": b_observeStartup3");
        startupObserved3 = true;
        //@Priority(2) Startup should be seen before @Priority(3) Startup
        assertTrue("Startup 3 was observed before Startup 2", startupObserved2);
        //Startup 3 should be seen before any request
        assertFalse("Startup 3 was observed after test", testRequested);
    }

    public void observeShutdown(@Observes Shutdown shutdownEvent) {
        System.out.println(this.getClass().getSimpleName() + ": observeShutdown");
        shutdownObserved = true;
        //Shutdown should be seen before @BeforeDestroyed(ApplicationScoped.class)
        assertFalse("Shutdown was observed after @BeforeDestroyed(ApplicationScoped.class)", destroyObserved);
        //Shutdown should be seen after any request
        assertTrue("Shutdown was observed before test", testRequested);
    }

    public void observeDestroy(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
        System.out.println(this.getClass().getSimpleName() + ": observeDestroy");
        destroyObserved = true;
        //@BeforeDestroyed(ApplicationScoped.class) should be seen after Shutdown
        assertTrue("@BeforeDestroyed(ApplicationScoped.class) was observed before Shutdown", shutdownObserved);
        //@BeforeDestroyed(ApplicationScoped.class) should be seen after any request
        assertTrue("Shutdown was observed before test", testRequested);
    }

}
