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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jsonp22.fat.provider.NoopJsonProvider;
import jakarta.json.spi.JsonProvider;
import jakarta.servlet.annotation.WebServlet;

/**
 * Deployed to ProviderFeatureApp alongside FeatureProviderServlet. Verifies that
 * the jakarta.json.provider system property override still routes to the named class
 * and that the returned instance is not the cached ServiceLoader-discovered provider.
 */
@WebServlet("/SystemPropertyProviderServlet")
@SuppressWarnings("serial")
public class SystemPropertyProviderServlet extends FATServlet {

    private static final String PROP = JsonProvider.JSONP_PROVIDER_FACTORY; // "jakarta.json.provider"

    /**
     * Verifies both that the system property override routes to the named class and
     * that after clearing it, provider() falls back to the cached ServiceLoader entry —
     * confirming the system-property result was never written into the cache.
     */
    public void testCacheRestoredAfterSystemPropertyCleared() {
        // Warm the cache with the normal provider first
        JsonProvider normal = JsonProvider.provider();
        assertEquals("org.eclipse.parsson.JsonProviderImpl", normal.getClass().getName());

        // Override via system property
        System.setProperty(PROP, NoopJsonProvider.class.getName());
        try {
            JsonProvider overridden = JsonProvider.provider();
            assertEquals(NoopJsonProvider.class.getName(), overridden.getClass().getName());
            // The overridden instance must not be the same object as the cached normal provider
            assertNotSame("System property result must not come from the classloader cache",
                          normal, overridden);
        } finally {
            System.clearProperty(PROP);
        }

        // Cache entry for this classloader must still be the Parsson provider,
        // and it must be the exact same instance that was warmed at the top of this method —
        // proving the cache was used on the restore path, not ServiceLoader re-run.
        JsonProvider restored = JsonProvider.provider();
        assertEquals("org.eclipse.parsson.JsonProviderImpl", restored.getClass().getName());
        assertSame("Cache must return the original warmed instance after property cleared", normal, restored);
    }
}
