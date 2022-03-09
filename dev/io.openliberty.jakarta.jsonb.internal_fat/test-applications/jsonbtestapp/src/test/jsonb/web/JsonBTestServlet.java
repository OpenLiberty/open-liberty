/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jsonb.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JsonBTestServlet")
public class JsonBTestServlet extends FATServlet {

    Jsonb jsonb;

    @Override
    public void destroy() {
        try {
            jsonb.close();
        } catch (Exception x) {
            throw new Error(x);
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        jsonb = JsonbBuilder.create();
    }

    /**
     * JsonbNillable is allowed on methods and fields and determines if a JSON null value
     * is written versus omitting the property entirely.
     */
    @Test
    public void testNillableMethodsAndFields() throws Exception {
        TestNillableMethodsAndFields instance = new TestNillableMethodsAndFields();
        instance.firstName = "Me";
        instance.lastName = "Myself";

        String json = jsonb.toJson(instance);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = jsonb.fromJson(json, Map.class);

        assertTrue(json, map.containsKey("middleName")); // Nillable field
        assertTrue(json, map.containsKey("homePhone")); // Nillable method
        assertFalse(json, map.containsKey("cellPhone")); // Not nillable

        assertEquals("Me", map.get("firstName"));
        assertEquals(null, map.get("middleName"));
        assertEquals("Myself", map.get("lastName"));
        assertEquals(null, map.get("homePhone"));
    }

    public static class TestNillableMethodsAndFields {
        public String firstName, lastName;

        @JsonbNillable
        public String middleName;

        public Long cellPhone; // not nillable
        private Long homePhone;

        public Long getHomePhone() {
            return homePhone;
        }

        @JsonbNillable
        public void setHomePhone(Long value) {
            homePhone = value;
        }
    }

    /**
     * JSON null deserializes as JsonValue.NULL rather than Java null.
     *
     * https://github.com/eclipse-ee4j/jsonb-api/issues/181
     */
    @Test
    public void testNullJsonValue() throws Exception {
        TestNullJsonValue instance = jsonb.fromJson("{ \"jsonval\": null }", TestNullJsonValue.class);
        assertNotNull(instance);
        assertEquals(JsonValue.NULL, instance.jsonval);
    }

    public static class TestNullJsonValue {
        public JsonValue jsonval;
    }
}
