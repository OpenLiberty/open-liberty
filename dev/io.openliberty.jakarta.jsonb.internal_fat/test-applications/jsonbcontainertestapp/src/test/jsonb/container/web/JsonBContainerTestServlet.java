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
package test.jsonb.container.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbSubtype;
import jakarta.json.bind.annotation.JsonbTypeInfo;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JsonBContainerTestServlet")
public class JsonBContainerTestServlet extends FATServlet {

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

    public static class TestCreatorParameters {
        private final String name;

        @JsonbCreator
        public TestCreatorParameters(@JsonbProperty("first-name") String first, //
                                     @JsonbProperty("middle-name") String middle, //
                                     @JsonbProperty("last-name") String last) {
            name = first + (middle == null ? ' ' : ' ' + middle + ' ') + last;
        }

        public String getFirstName() {
            return name.substring(0, name.indexOf(' '));
        }

        public String getLastName() {
            return name.substring(name.lastIndexOf(' ') + 1);
        }

        public String getMiddleName() {
            int start = name.indexOf(' ') + 1;
            int end = name.lastIndexOf(' ');
            return end > start ? name.substring(start, end) : null;
        }
    }

    /**
     * jsonbContainer feature is used to add a fake JSON-B provider that switches the
     * default for CREATOR_PARAMETERS_REQUIRED to true.
     */
    @Test
    public void testCreatorParametersRequired() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            TestCreatorParameters instance = new TestCreatorParameters("First", null, "Last");

            String json = jsonb.toJson(instance);
            assertFalse(json, json.contains("middle-name"));

            TestCreatorParameters copy = jsonb.fromJson(json, TestCreatorParameters.class);
            fail("JsonbCreator parameter was not required. " + copy.name);
        } catch (JsonbException x) {
            // expect: JsonbCreator parameter middle-name is missing in json document.
            if (x.getMessage() == null || !x.getMessage().contains("middle"))
                throw x;
        }
    }

    /**
     * JsonbNillable is allowed on methods and fields and determines if a JSON null value
     * is written versus omitting the property entirely.
     * The jsonbContainer is used to add a fake JSON-B provider that switches the
     * default PROPERTY_NAMING_STRATEGY to LOWER_CASE_WITH_DASHES.
     */
    @Test
    public void testNillableMethodsAndFields() throws Exception {
        TestNillableMethodsAndFields instance = new TestNillableMethodsAndFields();
        instance.firstName = "Me";
        instance.lastName = "Myself";

        String json = jsonb.toJson(instance);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = jsonb.fromJson(json, Map.class);

        assertTrue(json, map.containsKey("middle-name")); // Nillable field
        assertTrue(json, map.containsKey("home-phone")); // Nillable method
        assertFalse(json, map.containsKey("cell-phone")); // Not nillable

        assertEquals("Me", map.get("first-name"));
        assertEquals(null, map.get("middle-name"));
        assertEquals("Myself", map.get("last-name"));
        assertEquals(null, map.get("home-phone"));
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

    // TODO make different
    /**
     * Tests polymorphic types in JSON-B.
     *
     * https://github.com/eclipse-ee4j/jsonb-api/issues/147
     */
    @Test
    public void testPolymorphism() throws Exception {
        TestPolymorphism.LifeForm[] list = new TestPolymorphism.LifeForm[5];

        TestPolymorphism.Animal m = new TestPolymorphism.Animal();
        m.species = "Alces alces";
        m.maxSpeedMPH = 35;
        list[0] = m;

        TestPolymorphism.Animal b = new TestPolymorphism.Animal();
        b.species = "Ursus americanus";
        b.maxSpeedMPH = 35;
        list[1] = b;

        TestPolymorphism.Plant h = new TestPolymorphism.Plant();
        h.species = "Corylus americana";
        h.pollination = "wind";
        list[2] = h;

        TestPolymorphism.Plant t = new TestPolymorphism.Plant();
        t.species = "Erythronium albidum";
        t.pollination = "insects";
        list[3] = t;

        TestPolymorphism.Animal l = new TestPolymorphism.Animal();
        l.species = "Gavia immer";
        l.maxSpeedMPH = 70;
        list[4] = l;

        String json = jsonb.toJson(list);

        System.out.println("testPolymorphism JSON:");
        System.out.println(json);

        TestPolymorphism.LifeForm[] lifeForms = jsonb.fromJson(json, TestPolymorphism.LifeForm[].class);

        TestPolymorphism.Animal a = (TestPolymorphism.Animal) lifeForms[0];
        assertEquals(m.species, a.species);
        assertEquals(m.maxSpeedMPH, a.maxSpeedMPH);

        TestPolymorphism.Plant p = (TestPolymorphism.Plant) lifeForms[2];
        assertEquals(h.species, p.species);
        assertEquals(h.pollination, p.pollination);
    }

    public static interface TestPolymorphism {
        @JsonbTypeInfo({
                         @JsonbSubtype(alias = "animal", type = Animal.class),
                         @JsonbSubtype(alias = "plant", type = Plant.class)
        })
        public static class LifeForm {
            public String species;
        }

        public static class Animal extends LifeForm {
            public int maxSpeedMPH;
        }

        public static class Plant extends LifeForm {
            public String pollination;
        }
    }
}
