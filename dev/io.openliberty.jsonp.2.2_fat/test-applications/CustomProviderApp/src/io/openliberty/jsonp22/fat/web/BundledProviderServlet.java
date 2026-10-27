/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.jsonp22.fat.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.json.spi.JsonProvider;
import jakarta.servlet.annotation.WebServlet;

/**
 * Deployed to BundledProviderApp (Johnzon bundled in WEB-INF/lib). Asserts that
 * Johnzon overrides the feature provider for this WAR's classloader only.
 */
@WebServlet("/BundledProviderServlet")
@SuppressWarnings("serial")
public class BundledProviderServlet extends FATServlet {

    /**
     * Existing correctness check: the bundled Johnzon provider wins over
     * the feature-provided Parsson for this WAR's classloader.
     * Extended with a classloader-visibility assertion: the discovered provider
     * class must be loadable from the thread context classloader (i.e. it is
     * in WEB-INF/lib, not just on the API bundle's classpath).
     */
    public void testBundledProvider() throws Exception {
        JsonProvider provider = JsonProvider.provider();
        assertEquals("org.apache.johnzon.core.JsonProviderImpl", provider.getClass().getName());

        // The provider's class must be resolvable from the TCCL (it is in WEB-INF/lib).
        // This confirms ServiceLoader used the explicit classloader arg, not the API CL.
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Class<?> cls = Class.forName("org.apache.johnzon.core.JsonProviderImpl", false, tccl);
        assertNotNull("Johnzon class must be visible from the thread context classloader", cls);
    }

    /**
     * Cache stickiness: two calls to JsonProvider.provider() on the same thread
     * must return the same object instance.
     */
    public void testProviderIsSameInstance() {
        JsonProvider p1 = JsonProvider.provider();
        JsonProvider p2 = JsonProvider.provider();
        assertSame("JsonProvider.provider() must return the same cached instance on repeated calls", p1, p2);
    }
}
