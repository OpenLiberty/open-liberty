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
package jsonp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.JsonPointer;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JSONPServlet")
public class JSONPServlet extends FATServlet {

    @Test
    public void testJSONP(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                        .add("firstName", "Bob")
                        .add("lastName", "Ray")
                        .build();

        //Test whole document pointer
        JsonPointer p = Json.createPointer("");
        JsonObject json2 = (JsonObject) p.getValue(json);
        assertEquals("Json Objects were not equal. json: " + json + "  json2: " + json2, json, json2);
        
        //Test Adding an Object Member
        json2 = Json.createObjectBuilder()
                        .add("firstName", "Bob")
                        .add("lastName", "Ray")
                        .add("age", "45")
                        .build();
        
        JsonPatch patch = Json.createPatchBuilder().add("/age", "45").build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);
        
        //Testing a Value: Success
        json2 = Json.createObjectBuilder()
                        .add("lastName", "Ray")
                        .add("firstName", "Bob")
                        .add("age", "45")
                        .build();

        patch = Json.createPatchBuilder().test("/lastName", "Ray").build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);
       
        //Testing a Value: Failure
        json2 = Json.createObjectBuilder()
                        .add("lastName", "Ray")
                        .add("firstName", "Bob")
                        .add("age", "45")
                        .build();

        patch = Json.createPatchBuilder().test("/lastName", "May").build();
        try {
            json = patch.apply(json);
            assertTrue("Should have receieved exception", false);
        } catch (JsonException je) {
            assertEquals("Json Objects did not match", json2, json);
        }       

    }
}
