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
package test.jsonb.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Type;
import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbSubtype;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeInfo;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
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

    public static class TestCreatorParameters {
        private final String name;

        @JsonbCreator
        public TestCreatorParameters(@JsonbProperty("firstName") String first, //
                                     @JsonbProperty("middleName") String middle, //
                                     @JsonbProperty("lastName") String last) {
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
     * JsonbCreator parameters are optional by default.
     *
     * https://github.com/eclipse-ee4j/jsonb-api/issues/121
     */
    @Test
    public void testCreatorParametersOptional() throws Exception {
        TestCreatorParameters instance = new TestCreatorParameters("First", null, "Last");

        String json = jsonb.toJson(instance);
        assertFalse(json, json.contains("middleName"));

        TestCreatorParameters copy = jsonb.fromJson(json, TestCreatorParameters.class);
        assertEquals(instance.name, copy.name);
    }

    /**
     * JsonbCreator parameters are required when withCreatorParametersRequired is specified.
     *
     * https://github.com/eclipse-ee4j/jsonb-api/issues/121
     */
    @Test
    public void testCreatorParametersRequired() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withCreatorParametersRequired(true))) {
            TestCreatorParameters instance = new TestCreatorParameters("First", null, "Last");

            String json = jsonb.toJson(instance);
            assertFalse(json, json.contains("middleName"));

            TestCreatorParameters copy = jsonb.fromJson(json, TestCreatorParameters.class);
            fail("JsonbCreator parameter was not required. " + copy.name);
        } catch (JsonbException x) {
            // expect: JsonbCreator parameter middleName is missing in json document.
            if (x.getMessage() == null || !x.getMessage().contains("middleName"))
                throw x;
        }
    }

    /**
     * JsonbTypeAdapter can be used on JsonbCreator parameters.
     *
     * https://github.com/eclipse-ee4j/jsonb-api/issues/71
     */
    @Test
    public void testCreatorWithTypeAdapter() throws Exception {
        ZonedDateTime time = ZonedDateTime.of(2021, 1, 31, 10, 30, 0, 0, ZoneId.of("America/Chicago"));

        TestCreatorWithTypeAdapter instance = new TestCreatorWithTypeAdapter(time);

        String json = jsonb.toJson(instance);

        TestCreatorWithTypeAdapter copy = jsonb.fromJson(json, TestCreatorWithTypeAdapter.class);
        assertEquals(time, copy.zdt);
    }

    public static class TestCreatorWithTypeAdapter {
        private final ZonedDateTime zdt;

        @JsonbCreator
        public TestCreatorWithTypeAdapter(//
                                          @JsonbProperty("timestamp") //
                                          @JsonbTypeAdapter(Adapter.class) //
                                          ZonedDateTime zdt) {
            this.zdt = zdt;
        }

        // Ensure that @JsonbTypeAdapter from @JsonbCreator parameter will be required to deserialize
        public Map<String, ?> getTimestamp() {
            return new Adapter().adaptToJson(zdt);
        }

        public static class Adapter implements JsonbAdapter<ZonedDateTime, Map<String, ?>> {
            @Override
            public ZonedDateTime adaptFromJson(Map<String, ?> map) {
                return ZonedDateTime.of(((Number) map.get("year")).intValue(),
                                        ((Number) map.get("month")).intValue(),
                                        ((Number) map.get("day")).intValue(),
                                        ((Number) map.get("hour")).intValue(),
                                        ((Number) map.get("minute")).intValue(),
                                        ((Number) map.get("second")).intValue(),
                                        0,
                                        ZoneId.of((String) map.get("zone")));
            }

            @Override
            public Map<String, ?> adaptToJson(ZonedDateTime d) {
                LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
                map.put("year", d.getYear());
                map.put("month", d.getMonth().getValue());
                map.put("day", d.getDayOfMonth());
                map.put("hour", d.getHour());
                map.put("minute", d.getMinute());
                map.put("second", d.getSecond());
                map.put("zone", d.getZone().getId());
                return map;
            }
        }
    }

    /**
     * JsonbTypeDeserializer can be used on JsonbCreator parameters.
     *
     * https://github.com/eclipse-ee4j/jsonb-api/issues/71
     */
    @Test
    public void testCreatorWithTypeDeserializer() throws Exception {
        ZonedDateTime time = ZonedDateTime.of(2022, 2, 22, 14, 22, 0, 0, ZoneId.of("America/Chicago"));

        TestCreatorWithTypeDeserializer instance = new TestCreatorWithTypeDeserializer(time);

        String json = jsonb.toJson(instance);

        TestCreatorWithTypeDeserializer copy = jsonb.fromJson(json, TestCreatorWithTypeDeserializer.class);
        assertEquals(time, copy.zdt);
    }

    public static class TestCreatorWithTypeDeserializer {
        private final ZonedDateTime zdt;

        @JsonbCreator
        public TestCreatorWithTypeDeserializer(//
                                               @JsonbProperty("time") //
                                               @JsonbTypeDeserializer(Deserializer.class) //
                                               ZonedDateTime zdt) {
            this.zdt = zdt;
        }

        // Ensure that @JsonbTypeAdapter from @JsonbTypeDeserializer parameter will be required to deserialize
        public Map<String, ?> getTime() {
            return new TestCreatorWithTypeAdapter.Adapter().adaptToJson(zdt);
        }

        public static class Deserializer implements JsonbDeserializer<ZonedDateTime> {
            @Override
            public ZonedDateTime deserialize(JsonParser parser, DeserializationContext dctx, Type type) {
                Map<?, ?> map = dctx.deserialize(Map.class, parser);
                return ZonedDateTime.of(((Number) map.get("year")).intValue(),
                                        ((Number) map.get("month")).intValue(),
                                        ((Number) map.get("day")).intValue(),
                                        ((Number) map.get("hour")).intValue(),
                                        ((Number) map.get("minute")).intValue(),
                                        ((Number) map.get("second")).intValue(),
                                        0,
                                        ZoneId.of((String) map.get("zone")));
            }
        }
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

    /**
     * Tests polymorphic types in JSON-B.
     *
     * https://github.com/eclipse-ee4j/jsonb-api/issues/147
     */
    @Test
    public void testPolymorphism() throws Exception {
        TestPolymorphism[] list = new TestPolymorphism[5];

        TestPolymorphism.State mn = new TestPolymorphism.State();
        mn.name = "Minnesota";
        mn.population = 5707390;
        mn.capital = "St. Paul";
        list[0] = mn;

        TestPolymorphism.City rochester = new TestPolymorphism.City();
        rochester.name = "Rochester";
        rochester.population = 121395;
        rochester.state = "Minnesota";
        list[1] = rochester;

        TestPolymorphism.Employee employee = new TestPolymorphism.Employee();
        employee.firstName = "I";
        employee.lastName = "Myself";
        employee.location = rochester;
        list[2] = employee;

        TestPolymorphism.Location northAmerica = new TestPolymorphism.Location();
        northAmerica.name = "North America";
        northAmerica.population = 601074700;
        list[3] = northAmerica;

        String json = jsonb.toJson(list);

        System.out.println("testPolymorphism JSON:");
        System.out.println(json);

        TestPolymorphism[] copy = jsonb.fromJson(json, TestPolymorphism[].class);

        TestPolymorphism.State state = (TestPolymorphism.State) copy[0];
        assertEquals(mn.name, state.name);
        assertEquals(mn.population, state.population);
        assertEquals(mn.capital, state.capital);

        TestPolymorphism.City city = (TestPolymorphism.City) copy[1];
        assertEquals(rochester.name, city.name);
        assertEquals(rochester.population, city.population);
        assertEquals(rochester.state, city.state);

        TestPolymorphism.Employee emp = (TestPolymorphism.Employee) copy[2];
        assertEquals(employee.firstName, emp.firstName);
        assertEquals(employee.lastName, emp.lastName);
        city = (TestPolymorphism.City) emp.location;
        assertEquals(rochester.name, city.name);
        assertEquals(rochester.population, city.population);
        assertEquals(rochester.state, city.state);

        TestPolymorphism.Location location = (TestPolymorphism.Location) copy[3];
        assertEquals(location.name, northAmerica.name);
        assertEquals(location.population, northAmerica.population);
    }

    @JsonbTypeInfo({
                     @JsonbSubtype(alias = "employee", type = TestPolymorphism.Employee.class),
                     @JsonbSubtype(alias = "location", type = TestPolymorphism.Location.class)
    })
    public static interface TestPolymorphism {
        public static class Employee implements TestPolymorphism {
            public String firstName, lastName;
            public Location location;
        }

        @JsonbTypeInfo(key = "@loctype", value = {
                                                   @JsonbSubtype(alias = "city", type = TestPolymorphism.City.class),
                                                   @JsonbSubtype(alias = "state", type = TestPolymorphism.State.class)
        })
        public static class Location implements TestPolymorphism {
            public String name;
            public long population;
        }

        public static class City extends Location {
            public String state;
        }

        public static class State extends Location {
            public String capital;
        }
    }

    /**
     * Test to ensure that when the name of a transient field is used as the
     * name for another field that JSONB does not attempt to serialize that
     * field into the incorrect class.
     *
     * In this example order is transient, and orderLink is given the property name order.
     * In previous yasson releases they treated the key "order" as reserved even through
     * the field was transient.
     */
    @Test
    public void testPropertyAnnotationCollision() {
        Order myOrder = new Order();
        myOrder.setName("Kyle");

        Coffee myCoffee = new Coffee();
        myCoffee.setOrder(myOrder);
        myCoffee.setOrderLink(URI.create("http://my.coffee.shop/"));

        String myOrderJson = jsonb.toJson(myOrder);
        String myCoffeeJson = jsonb.toJson(myCoffee);

        assertEquals(myOrderJson, "{\"name\":\"Kyle\"}");
        assertEquals(myCoffeeJson, "{\"order\":\"http://my.coffee.shop/\"}");

        Order resultOrder = jsonb.fromJson(myOrderJson, Order.class);
        Coffee resultCoffee = jsonb.fromJson(myCoffeeJson, Coffee.class);

        assertEquals(resultOrder.getName(), myOrder.getName());
        assertEquals(resultCoffee.getOrderLink(), myCoffee.getOrderLink());
        assertNull(resultCoffee.getOrder());

    }

    public static class Coffee {
        @JsonbTransient
        private Order order;

        @JsonbProperty("order")
        private URI orderLink;

        public Order getOrder() {
            return order;
        }

        public void setOrder(Order order) {
            this.order = order;
        }

        public URI getOrderLink() {
            return orderLink;
        }

        public void setOrderLink(URI orderLink) {
            this.orderLink = orderLink;
        }
    }

    public static class Order {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
