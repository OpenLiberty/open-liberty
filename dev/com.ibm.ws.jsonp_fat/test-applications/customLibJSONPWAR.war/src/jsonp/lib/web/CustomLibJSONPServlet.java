/*******************************************************************************
 * Copyright (c) 2014,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jsonp.lib.web;

import java.io.InputStream;

import javax.json.spi.JsonProvider;
import javax.json.stream.JsonParser;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import junit.framework.Assert;

@WebServlet("/CustomLibJSONPServlet")
@SuppressWarnings("serial")
public class CustomLibJSONPServlet extends FATServlet {

    /**
     * Test plugging in a custom implementation for JSON processing,
     * where the custom implementation is packaged in a shared library.
     */
    @Test
    public void testCustomLibJsonProvider() {
        // Verify the custom JSON Provider class is being used.
        JsonProvider dummyProvider = JsonProvider.provider();
        String providerName = dummyProvider.getClass().getName();
        String expectedString1 = "jsonp.lib.provider.JsonProviderImpl";
        Assert.assertEquals(expectedString1, providerName);

        // Verify the custom implemented JsonParser class gets loaded and used.
        InputStream dummyInputStream = null;
        JsonParser dummyParser = dummyProvider.createParser(dummyInputStream);
        String parserString = dummyParser.getString();
        String expectedString2 = "Custom JSONP implementation loaded from a shared library";
        Assert.assertEquals(expectedString2, parserString);

    }
}