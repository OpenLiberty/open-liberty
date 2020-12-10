/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.unwrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class UnwrapServlet extends FATServlet {

    /**
     * Test what happens when Config.unwrap() is called on an object that can be unwrapped.
     */
    @Test
    public void testValidUnwrap() throws Exception {
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        Config config = b.build();
        Config unwrappedConfig = config.unwrap(Config.class);

        assertEquals("The Config object from ConfigBuilder.build() and Config.unwrap() should be the same", config, unwrappedConfig);
    }

    /**
     * Test what happens when Config.unwrap() is called on an invalid object.
     *
     * @throws IllegalArgumentException
     */
    @Test
    public void testInvalidUnwrap() throws Exception {
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        Config c = b.build();

        try {
            @SuppressWarnings("unused")
            String myString = c.unwrap(String.class);

            fail("FAILED: IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

}