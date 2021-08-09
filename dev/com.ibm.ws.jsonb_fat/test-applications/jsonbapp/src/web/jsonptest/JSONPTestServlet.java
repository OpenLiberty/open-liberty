/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.jsonptest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JSONPTestServlet")
public class JSONPTestServlet extends FATServlet {
    // Basic usage of JSON-P to generate JSON and parse back into JsonObject.
    @Test
    public void testJsonpGeneratorAndReader() throws Exception {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = Json.createGenerator(sw)) {
            g.writeStartObject()
                            .write("number", 3605)
                            .write("street", "Highway 52 N")
                            .write("city", "Rochester")
                            .write("state", "Minnesota")
                            .write("zip", 55901)
                            .writeEnd();
        }
        String json = sw.toString();

        assertTrue(json, json.contains("{") && json.contains("city") && json.contains(":") && json.contains("Rochester") && json.contains("}"));

        JsonObject o;
        try (JsonReader r = Json.createReader(new StringReader(json))) {
            o = r.readObject();
        }

        assertEquals(3605, o.getInt("number"));
        assertEquals("Highway 52 N", o.getString("street"));
        assertEquals("Rochester", o.getString("city"));
        assertEquals("Minnesota", o.getString("state"));
        assertEquals(55901, o.getInt("zip"));
    }

    // Verify that the specified JSON-P provider is available and is loaded as the default provider.
    public void testJsonpProviderAvailable(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JsonProvider provider = JsonProvider.provider();
        assertEquals(request.getParameter("JsonpProvider"), provider.getClass().getName());
    }
}
