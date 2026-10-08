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
package io.openliberty.jsonp21.fat.web;

import static org.junit.Assert.assertEquals;

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

    @Test
    public void testBundledProvider() {
        String providerName = JsonProvider.provider().getClass().getName();
        assertEquals("org.apache.johnzon.core.JsonProviderImpl", providerName);
    }
}
