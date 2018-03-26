/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Test;

import com.ibm.ws.install.internal.Product;

/**
 *
 */
public class InvalidProductTest {

    @Test
    public void testProductNullInstallPath() {
        Product p = new Product(null);
        assertNotNull(p.getInstallDir());
        File wasPropFile = new File(p.getInstallDir(), "lib/versions/WebSphereApplicationServer.properties");
        if (wasPropFile.exists()) {
            assertNotNull(p.getProductId());
            assertNotNull(p.getProductVersion());
            assertNotNull(p.getProductEdition());
            assertEquals(2, p.getExtensionNames().size());
        } else {
            assertNull(p.getProductId());
            assertNull(p.getProductVersion());
            assertNull(p.getProductEdition());
            assertEquals(0, p.getExtensionNames().size());
        }
    }

    @Test
    public void testProductInvalidInstallPath() {
        Product p = new Product(new File("unknown"));
        assertNotNull(p.getInstallDir());
        assertNull(p.getProductId());
        assertNull(p.getProductVersion());
        assertNull(p.getProductEdition());
        assertEquals(0, p.getExtensionNames().size());
    }
}
