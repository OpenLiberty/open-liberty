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
package io.openliberty.org.jboss.resteasy.common.core.se;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.junit.Test;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.core.Application;

/**
 * Unit tests to ensure an attempt to use the optional Java Se Bootsrap APIs in Liberty will result in
 * an exception.
 */
public class LibertyResteasyJavaSeBootstrapTest {

    @Test
    public void testAttemptToUseJavaSEBootstrapAPIThrowsException() {
        boolean exceptionCaught = false;
        try {
            final SeBootstrap.Configuration.Builder bootstrapConfigurationBuilder = SeBootstrap.Configuration.builder();
        } catch (Throwable t) {
            exceptionCaught = true;
            assertTrue("Received throwable message is: " + t.getMessage() + " and the expected message is: Liberty does not support the optional Jakarta Rest SE Bootstrap API.",
                       t.getMessage().contains("Liberty does not support the optional Jakarta Rest Java SE Bootstrap API."));
        }
        if (!exceptionCaught) {
            fail("Expected UnsupportedOperationException not caught");
        }
    }
    @Test
    public void testAttemptToStartJavaSeBootstrapAPIThrowsException() {
        boolean exceptionCaught = false;
        try {
            final Application application = new TestApplication();
            final CompletionStage<SeBootstrap.Instance> completionStage = SeBootstrap.start(application,
                null);
        } catch (Throwable t) {
            exceptionCaught = true;
            assertTrue("Received throwable message is: " + t.getMessage() + " and the expected message is: Liberty does not support the optional Jakarta Rest SE Bootstrap API.",
                       t.getMessage().contains("Liberty does not support the optional Jakarta Rest Java SE Bootstrap API."));
        }
        if (!exceptionCaught) {
            fail("Expected UnsupportedOperationException not caught");
        }
    }
    @Test
    public void testAttemptToStartJavaSeBootstrapAPIWithClassThrowsException() {
        boolean exceptionCaught = false;
        try {
            final Application application = new TestApplication();
            final CompletionStage<SeBootstrap.Instance> completionStage = SeBootstrap.start(application.getClass(),
                null);
        } catch (Throwable t) {
            exceptionCaught = true;
            assertTrue("Received throwable message is: " + t.getMessage() + " and the expected message is: Liberty does not support the optional Jakarta Rest SE Bootstrap API.",
                       t.getMessage().contains("Liberty does not support the optional Jakarta Rest Java SE Bootstrap API."));
        }
        if (!exceptionCaught) {
            fail("Expected UnsupportedOperationException not caught");
        }
    }
 
    @ApplicationPath("test")
    public static class TestApplication extends Application {

        private final TestResource testResource;

        private TestApplication() {
            this.testResource = new TestResource();
        }

        @Override
        public Set<Class<?>> getClasses() {
            Set<Class<?>> set = new HashSet<>();
            set.add(TestResource.class);
            return set;
        }
        
        @Path("testResource")
        public static class TestResource {


            @GET
            public final int testResponse() {
                return 1;
            }
        }
    };


}
