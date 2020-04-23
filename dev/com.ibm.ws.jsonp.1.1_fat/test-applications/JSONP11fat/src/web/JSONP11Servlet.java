/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.JsonPointer;
import javax.json.JsonString;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JSONP11Servlet")
public class JSONP11Servlet extends FATServlet {

    @Test
    public void testJSONPointer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                        .add("firstName", "Steve")
                        .add("lastName", "Watson")
                        .add("age", 45)
                        .add("phoneNumber", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                        .add("type", "office")
                                        .add("number", "507-253-1234")))
                        .add("~/", "specialCharacters")
                        .build();

        //Test whole document pointer
        JsonPointer p = Json.createPointer("");
        JsonObject json2 = (JsonObject) p.getValue(json);
        assertEquals("Json Objects were not equal. json: " + json + "  json2: " + json2, json, json2);

        //Test Pointer to document element
        JsonArray json3 = Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("type", "office")
                        .add("number", "507-253-1234")).build();

        p = Json.createPointer("/phoneNumber");
        JsonArray json4 = (JsonArray) p.getValue(json);

        assertEquals("JsonArray was not [\"type\":\"office\",\"number\":\"507-253-1234\"]", json3, json4);

        //Test pointer to array element
        p = Json.createPointer("/phoneNumber/0/number");
        JsonString json5 = Json.createValue("507-253-1234");
        JsonString json6 = (JsonString) p.getValue(json);
        assertEquals("JsonString was not \"507-253-1234\"", json5, json6);

        //Test pointer encoding
        String ep = Json.encodePointer("~1");
        assertEquals("Encoded pointers were not equal", "~01", ep);

        String dp = Json.decodePointer("~01");
        assertEquals("Decoded pointers were not equal", "~1", dp);

        ep = Json.encodePointer("~/");
        assertEquals("Encoded pointers were not equal", "~0~1", ep);

        dp = Json.decodePointer("~0~1");
        assertEquals("Decoded pointers were not equal", "~/", dp);

        p = Json.createPointer("/" + ep);
        json5 = Json.createValue("specialCharacters");
        json6 = (JsonString) p.getValue(json);
        assertEquals("Json Pointer value was not \"specialCharacters\"", json5, json6);

    }

    @Test
    public void testJSONPatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                        .add("firstName", "Steve")
                        .add("children", Json.createArrayBuilder().add("John"))
                        .build();

        //Test Adding an Object Member
        JsonObject json2 = Json.createObjectBuilder()
                        .add("firstName", "Steve")
                        .add("lastName", "Watson")
                        .add("children", Json.createArrayBuilder().add("John"))
                        .build();

        JsonPatch patch = Json.createPatchBuilder().add("/lastName", "Watson").build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);

        //Test Adding an Array Element
        json2 = Json.createObjectBuilder()
                        .add("firstName", "Steve")
                        .add("lastName", "Watson")
                        .add("children", Json.createArrayBuilder()
                                        .add("John")
                                        .add("Mark"))
                        .build();

        patch = Json.createPatchBuilder().add("/children/1", "Mark").build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);

        //Test Removing an Object Member
        json2 = Json.createObjectBuilder()
                        .add("firstName", "Steve")
                        .add("children", Json.createArrayBuilder()
                                        .add("John")
                                        .add("Mark"))
                        .build();

        patch = Json.createPatchBuilder().remove("/lastName").build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);

        //Test Moving an Array Element
        json2 = Json.createObjectBuilder()
                        .add("firstName", "Steve")
                        .add("children", Json.createArrayBuilder()
                                        .add("Mark")
                                        .add("John"))
                        .build();

        patch = Json.createPatchBuilder().move("/children/0", "/children/1").build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);

        //Test Removing an Array Element
        json2 = Json.createObjectBuilder()
                        .add("firstName", "Steve")
                        .add("children", Json.createArrayBuilder().add("John"))
                        .build();

        patch = Json.createPatchBuilder().remove("/children/0").build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);

        //Test Replacing a Value
        json2 = Json.createObjectBuilder()
                        .add("firstName", "Watson")
                        .add("children", Json.createArrayBuilder().add("John"))
                        .build();

        patch = Json.createPatchBuilder().replace("/firstName", "Watson").build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);

        //Test Moving a Value
        json2 = Json.createObjectBuilder()
                        .add("lastName", "Watson")
                        .add("children", Json.createArrayBuilder().add("John"))
                        .build();

        patch = Json.createPatchBuilder().move("/lastName", "/firstName").build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);

        //Testing a Value: Success
        json2 = Json.createObjectBuilder()
                        .add("lastName", "Watson")
                        .add("firstName", "Steve")
                        .add("children", Json.createArrayBuilder().add("John"))
                        .build();

        patch = Json.createPatchBuilder().test("/lastName", "Watson").add("/firstName", "Steve").build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);

        //Testing a Value: Failure
        json2 = Json.createObjectBuilder()
                        .add("lastName", "Watson")
                        .add("firstName", "Steve")
                        .add("children", Json.createArrayBuilder().add("John"))
                        .build();

        patch = Json.createPatchBuilder().test("/lastName", "Johnson").add("/middleName", "Thomas").build();
        try {
            json = patch.apply(json);
            assertTrue("Should have receieved exception", false);
        } catch (JsonException je) {
            assertEquals("Json Objects did not match", json2, json);
        }

        //Testing Adding a Nested Member Object
        json2 = Json.createObjectBuilder()
                        .add("lastName", "Watson")
                        .add("firstName", "Steve")
                        .add("children", Json.createArrayBuilder().add("John"))
                        .add("grandchildren", Json.createObjectBuilder().add("John", Json.createArrayBuilder()
                                        .add("Scott")
                                        .add("Bob")))
                        .build();

        patch = Json.createPatchBuilder().add("/grandchildren", Json.createObjectBuilder().add("John", Json.createArrayBuilder().add("Scott").add("Bob")).build()).build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);

        //Testing Adding to nonexistant target
        json2 = Json.createObjectBuilder()
                        .add("lastName", "Watson")
                        .add("firstName", "Steve")
                        .add("children", Json.createArrayBuilder().add("John"))
                        .add("grandchildren", Json.createObjectBuilder().add("John", Json.createArrayBuilder()
                                        .add("Scott")
                                        .add("Bob")))
                        .build();

        patch = Json.createPatchBuilder().add("/middleName/initial", "J").build();
        try {
            json = patch.apply(json);
            assertTrue("Should have receieved exception", false);
        } catch (JsonException je) {
            assertEquals("Json Objects did not match", json2, json);
        }

        //Testing Adding an Array Value
        json2 = Json.createObjectBuilder()
                        .add("lastName", "Watson")
                        .add("firstName", "Steve")
                        .add("children", Json.createArrayBuilder().add("John").add(Json.createArrayBuilder()
                                        .add("Scott")
                                        .add("Bob")))
                        .add("grandchildren", Json.createObjectBuilder().add("John", Json.createArrayBuilder()
                                        .add("Scott")
                                        .add("Bob")))
                        .build();

        patch = Json.createPatchBuilder().add("/children/-", Json.createArrayBuilder()
                        .add("Scott")
                        .add("Bob")
                        .build()).build();
        json = patch.apply(json);
        assertEquals("Json Objects did not match", json2, json);

    }

    @Test
    public void testJSONMergePatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                        .add("firstName", "Steve")
                        .add("lastName", "Watson")
                        .build();

        //Test Adding an Object Member
        JsonObject json2 = Json.createObjectBuilder()
                        .add("firstName", "John")
                        .add("lastName", "Watson")
                        .build();

        json = (JsonObject) Json.createMergePatch(Json.createObjectBuilder().add("firstName", "John").build()).apply(json);
        assertEquals("Json Objects did not match", json2, json);

    }
}
