/*******************************************************************************
 * Copyright (c) 2022,2026 IBM Corporation and others.
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
import java.time.Month;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
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
import jakarta.json.bind.annotation.JsonbDateFormat;
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
     * Write MonthDay values to JSON and read them back in as Java objects.
     */
    @Test
    public void testMonthDay() {
        TestMonthDay taxDeadline = new TestMonthDay();
        taxDeadline.setMonthDay(MonthDay.of(Month.APRIL, 15));
        String jsonTaxDeadline = jsonb.toJson(taxDeadline);

        TestMonthDay independenceDay = new TestMonthDay();
        independenceDay.setMonthDay(MonthDay.of(7, 4));
        String jsonIndependenceDay = jsonb.toJson(independenceDay);

        taxDeadline = jsonb.fromJson(jsonTaxDeadline, TestMonthDay.class);
        independenceDay = jsonb.fromJson(jsonIndependenceDay, TestMonthDay.class);

        assertEquals(MonthDay.of(4, 15), taxDeadline.monthDay);
        assertEquals(MonthDay.of(Month.JULY, 4), independenceDay.monthDay);

        assertEquals("{\"monthDay\":\"--04-15\"}", jsonTaxDeadline);
        assertEquals("{\"monthDay\":\"--07-04\"}", jsonIndependenceDay);
    }

    public static class TestMonthDay {
        private MonthDay monthDay;

        public MonthDay getMonthDay() {
            return monthDay;
        }

        public void setMonthDay(MonthDay value) {
            monthDay = value;
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

    /**
     * Write Year values to JSON and read them back in as Java objects.
     */
    @Test
    public void testYear() {
        TestYear year2026 = new TestYear();
        year2026.setYear(Year.of(2026));
        String json2026 = jsonb.toJson(year2026);

        TestYear year2025 = new TestYear();
        year2025.setYear(Year.of(2025));
        String json2025 = jsonb.toJson(year2025);

        // TODO See jsonb-api 397 regarding whether this behavior will be added
        //year2026 = jsonb.fromJson(json2026, TestYear.class);
        //year2025 = jsonb.fromJson(json2025, TestYear.class);

        //assertEquals(Year.of(2026), year2026.year);
        //assertEquals(Year.of(2025), year2025.year);

        //assertEquals("{\"year\":\"2026\"}", json2026);
        //assertEquals("{\"year\":\"2025\"}", json2025);

        // Current behavior for writing to JSON is:
        assertEquals("{\"year\":{\"leap\":false,\"value\":2026}}", json2026);
        assertEquals("{\"year\":{\"leap\":false,\"value\":2025}}", json2025);

        // Current behavior for reading from JSON is:
        // jakarta.json.bind.JsonbException: Cannot create instance of a class: class java.time.Year, No default constructor found.
        // at org.eclipse.yasson.internal.deserializer.DefaultObjectInstanceCreator.<init>(DefaultObjectInstanceCreator.java:44)
        // at org.eclipse.yasson.internal.deserializer.DeserializationModelCreator.createObjectDeserializer(DeserializationModelCreator.java:251)
        // at org.eclipse.yasson.internal.deserializer.DeserializationModelCreator.deserializerChainInternal(DeserializationModelCreator.java:193)
        // at org.eclipse.yasson.internal.deserializer.DeserializationModelCreator.deserializerChain(DeserializationModelCreator.java:135)
        // at org.eclipse.yasson.internal.deserializer.DeserializationModelCreator.createNewChain(DeserializationModelCreator.java:488)
        // at org.eclipse.yasson.internal.deserializer.DeserializationModelCreator.typeProcessor(DeserializationModelCreator.java:477)
        // at org.eclipse.yasson.internal.deserializer.DeserializationModelCreator.typeProcessor(DeserializationModelCreator.java:430)
        // at org.eclipse.yasson.internal.deserializer.DeserializationModelCreator.memberTypeProcessor(DeserializationModelCreator.java:423)
        // at org.eclipse.yasson.internal.deserializer.DeserializationModelCreator.createObjectDeserializer(DeserializationModelCreator.java:222)
        // at org.eclipse.yasson.internal.deserializer.DeserializationModelCreator.deserializerChainInternal(DeserializationModelCreator.java:193)
        // at org.eclipse.yasson.internal.deserializer.DeserializationModelCreator.deserializerChain(DeserializationModelCreator.java:135)
        // at org.eclipse.yasson.internal.deserializer.DeserializationModelCreator.deserializerChain(DeserializationModelCreator.java:123)
        // at org.eclipse.yasson.internal.DeserializationContextImpl.deserializeItem(DeserializationContextImpl.java:137)
    }

    public static class TestYear {
        private Year year;

        public Year getYear() {
            return year;
        }

        public void setYear(Year value) {
            year = value;
        }
    }

    /**
     * Write YearMonth values to JSON and read them back in as Java objects.
     */
    @Test
    public void testYearMonth() {
        TestYearMonth july2026 = new TestYearMonth();
        july2026.setYearMonth(YearMonth.of(2026, Month.JULY));
        String jsonJuly2026 = jsonb.toJson(july2026);

        TestYearMonth dec2026 = new TestYearMonth();
        dec2026.setYearMonth(YearMonth.of(2026, 12));
        String jsonDec2026 = jsonb.toJson(dec2026);

        TestYearMonth jan2025 = new TestYearMonth();
        jan2025.setYearMonth(YearMonth.of(2025, Month.JANUARY));
        String jsonJan2025 = jsonb.toJson(jan2025);

        TestYearMonth feb0051 = new TestYearMonth();
        feb0051.setYearMonth(YearMonth.of(51, Month.FEBRUARY));
        String jsonFeb0051 = jsonb.toJson(feb0051);

        TestYearMonth aug7654321 = new TestYearMonth();
        aug7654321.setYearMonth(YearMonth.of(7654321, Month.AUGUST));
        String jsonAug7654321 = jsonb.toJson(aug7654321);

        TestYearMonth dec0005bc = new TestYearMonth();
        dec0005bc.setYearMonth(YearMonth.of(-5, Month.DECEMBER));
        String jsonDec0005bc = jsonb.toJson(dec0005bc);

        july2026 = jsonb.fromJson(jsonJuly2026, TestYearMonth.class);
        dec2026 = jsonb.fromJson(jsonDec2026, TestYearMonth.class);
        jan2025 = jsonb.fromJson(jsonJan2025, TestYearMonth.class);
        feb0051 = jsonb.fromJson(jsonFeb0051, TestYearMonth.class);
        aug7654321 = jsonb.fromJson(jsonAug7654321, TestYearMonth.class);
        dec0005bc = jsonb.fromJson(jsonDec0005bc, TestYearMonth.class);

        assertEquals(YearMonth.of(2026, Month.DECEMBER), dec2026.yearMonth);
        assertEquals(YearMonth.of(2026, 7), july2026.yearMonth);
        assertEquals(YearMonth.of(2025, 1), jan2025.yearMonth);
        assertEquals(YearMonth.of(51, 2), feb0051.yearMonth);
        assertEquals(YearMonth.of(7654321, 8), aug7654321.yearMonth);
        assertEquals(YearMonth.of(-5, 12), dec0005bc.yearMonth);

        assertEquals("{\"yearMonth\":\"2026-12\"}", jsonDec2026);
        assertEquals("{\"yearMonth\":\"2026-07\"}", jsonJuly2026);
        assertEquals("{\"yearMonth\":\"2025-01\"}", jsonJan2025);
        assertEquals("{\"yearMonth\":\"0051-02\"}", jsonFeb0051);
        assertEquals("{\"yearMonth\":\"+7654321-08\"}", jsonAug7654321);
        assertEquals("{\"yearMonth\":\"-0005-12\"}", jsonDec0005bc);
    }

    public static class TestYearMonth {
        // Without this annotation, year within the common era is written,
        // so a value like -5 (which represents 6 BCE) is written as 0006.
        // And 0006 is read back in as 6.
        @JsonbDateFormat("uuuu-MM")
        private YearMonth yearMonth;

        public YearMonth getYearMonth() {
            return yearMonth;
        }

        public void setYearMonth(YearMonth value) {
            yearMonth = value;
        }
    }
}
