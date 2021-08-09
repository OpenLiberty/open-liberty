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
package web.jsonbtest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.json.bind.spi.JsonbProvider;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JSONBTestServlet")
public class JSONBTestServlet extends FATServlet {

    public static final String PROVIDER_YASSON = "org.eclipse.yasson.JsonBindingProvider";
    public static final String PROVIDER_JOHNZON = "org.apache.johnzon.jsonb.JohnzonProvider";
    public static final String PROVIDER_GLASSFISH_JSONP = "org.glassfish.json.JsonProviderImpl";
    public static final String PROVIDER_JOHNZON_JSONP = "org.apache.johnzon.core.JsonProviderImpl";

    // Marshall and unmarshall application classes to/from JSON.
    // Choose a specific JSON-B provider by class name.
    // Verify renaming via @JsonbProperty, ordering via @JsonbPropertyOrder, custom constructor via @JsonbCreator.
    // Test application classes nested 1, 2, and 3 levels deep, including in arrays.
    // Also unmarshall as generic map and verify contents.
    public static void testApplicationClasses(String jsonbProvider) throws Exception {
        JsonbBuilder builder = JsonbBuilder.newBuilder(jsonbProvider);
        Jsonb jsonb = builder.build();
        String json;

        Pod H315 = new Pod();
        H315.setBuilding("50");
        H315.setFloor((short) 2);
        H315.setPodNumber("H315");
        H315.setStreetAddress("3605 Hwy 52 N");
        H315.setCity("Rochester");
        H315.setState("Minnesota");
        H315.setZipCode(55901);

        Squad blizzard = new Squad("Blizzard", (byte) 7, H315, 30.6f);

        json = jsonb.toJson(H315);
        System.out.println("JSON for Pod object: " + json);
        Pod pod = jsonb.fromJson(json, Pod.class);
        assertEquals(H315.toString(), pod.toString());

        int p = json.indexOf("\"podNumber\"");
        int a = json.indexOf("\"address\""); // renamed from streetAddress by @JsonbProperty
        int c = json.indexOf("\"Rochester\""); // value of city
        // @JsonpPropertyOrder requires podNumber before address before city
        assertTrue(json, p != -1 && p < a && a < c);

        json = jsonb.toJson(blizzard);
        System.out.println("JSON for Squad object: " + json);
        assertTrue(json, json.contains("address")); // renamed from streetAddress by @JsonbProperty
        Squad squad = jsonb.fromJson(json, Squad.class);
        assertEquals(blizzard.toString(), squad.toString());

        Pod H215 = new Pod();
        H215.setBuilding("50");
        H215.setFloor((short) 2);
        H215.setPodNumber("H215");
        H215.setStreetAddress("3605 Hwy 52 N");
        H215.setCity("Rochester");
        H215.setState("Minnesota");
        H215.setZipCode(55901);

        Squad zombieApocalypse = new Squad("Zombie Apocalypse", (byte) 9, H215, 23.0f);

        Pod H230 = new Pod();
        H230.setBuilding("50");
        H230.setFloor((short) 2);
        H230.setPodNumber("H230");
        H230.setStreetAddress("3605 Hwy 52 N");
        H230.setCity("Rochester");
        H230.setState("Minnesota");
        H230.setZipCode(55901);

        Squad wendigo = new Squad("Wendigo", (byte) 10, H230, 21.2f);

        Tribe appPlatform = new Tribe();
        appPlatform.name = "App Platform Server";
        appPlatform.squads.add(wendigo);
        appPlatform.squads.add(zombieApocalypse);

        json = jsonb.toJson(appPlatform);
        System.out.println("JSON for Tribe object: " + json);
        Tribe tribe = jsonb.fromJson(json, Tribe.class);
        assertEquals(appPlatform.toString(), tribe.toString());

        LinkedHashMap<?, ?> appPlatformMap = jsonb.fromJson(json, LinkedHashMap.class);
        System.out.println("Tribe object unmarshalled as LinkedHashMap: ");
        assertEquals("App Platform Server", appPlatformMap.get("name"));
        List<?> squadList = (List<?>) appPlatformMap.get("squads");
        assertEquals(2, squadList.size());
        Map<?, ?> wendigoMap = (Map<?, ?>) squadList.get(0);
        assertEquals("Wendigo", wendigoMap.get("name"));
        Map<?, ?> zombieMap = (Map<?, ?>) squadList.get(1);
        Map<?, ?> podMap = (Map<?, ?>) zombieMap.get("pod");
        assertEquals("H215", podMap.get("podNumber"));

        jsonb.close();
    }

    @Test
    public void testJsonbDemo() throws Exception {
        // Convert Java Object --> JSON
        Team zombies = new Team();
        zombies.name = "Zombies";
        zombies.size = 9;
        zombies.winLossRatio = 0.85f;
        Jsonb jsonb = JsonbProvider.provider().create().build();
        String teamJson = jsonb.toJson(zombies);
        System.out.println(teamJson);
        assertTrue(teamJson.contains("\"name\":\"Zombies\""));
        assertTrue(teamJson.contains("\"size\":9"));
        assertTrue(teamJson.contains("\"winLossRatio\":0.8"));

        // Convert JSON --> Java Object
        Team rangers = jsonb.fromJson("{\"name\":\"Rangers\",\"size\":7,\"winLossRatio\":0.6}", Team.class);
        System.out.println(rangers.name);
        assertEquals("Rangers", rangers.name);
        assertEquals(7, rangers.size);
        assertEquals(0.6f, rangers.winLossRatio, 0.01f);
    }

    // Marshall and unmarshall arrays to/from JSON, of various types and including an empty one.
    // Use various JSON-B methods that write the JSON to strings, output streams, and writers,
    // and which unmarshall the JSON from strings, input streams and readers.
    // This is a general sampling of the JSON-B API, not anything comprehensive.
    @Test
    public void testArrays() throws Exception {
        Jsonb jsonb = JsonbBuilder.create();
        String json;

        String[] strings = new String[] { "s1", "s2", "s3" };
        json = jsonb.toJson(strings);
        System.out.println("JSON for string[]: " + json);
        String[] s = jsonb.fromJson(json, String[].class);
        assertArrayEquals(strings, s);

        Object[] objects = new Object[] { 'o', "o1", 2, strings };
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        jsonb.toJson(objects, bout);
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        bout.close();
        Object[] o = jsonb.fromJson(bin, Object[].class);
        bin.close();
        System.out.println("Deserialized Object[]: " + Arrays.deepToString(o));
        assertEquals(objects.length, o.length);
        assertEquals("o", o[0]);
        assertEquals("o1", o[1]);
        assertEquals(2, ((Number) o[2]).intValue());
        assertEquals(Arrays.asList(strings), o[3]);

        if (!jsonb.getClass().getName().startsWith("org.apache.johnzon")) {
            byte[] bytes = "This is a test of byte arrays in JSON-B.".getBytes();
            CharArrayWriter cw = new CharArrayWriter();
            jsonb.toJson(bytes, cw);
            System.out.println("JSON for byte[]: " + cw.toString());
            CharArrayReader cr = new CharArrayReader(cw.toCharArray());
            byte[] b = jsonb.fromJson(cr, byte[].class);
            assertArrayEquals(bytes, b);

            cr.reset();
            s = jsonb.fromJson(cr, String[].class);
            cr.close();
            assertEquals(bytes.length, s.length);
            for (int i = 0; i < bytes.length; i++)
                assertEquals("failed at position " + i, Byte.toString(bytes[i]), s[i]);
        }

        long[] empty = new long[] {};
        json = jsonb.toJson(empty);
        System.out.println("JSON For empty short[]: " + json);
        StringReader sr = new StringReader(json);
        long[] l = jsonb.fromJson(sr, long[].class);
        sr.close();
        assertArrayEquals(empty, l);

        jsonb.close();
    }

    // Unmarshall JSON into Java object where one of the fields is an interface
    // and JsonbAdapter disambiguates which subclass should be used.
    // Use JsonbProvider to specify a provider to obtain the JsonbBuilder.
    public static void testJsonbAdapter(String jsonbProvider) throws Exception {
        ReservableRoom A101 = new ReservableRoom();
        A101.setBuilding("50");
        A101.setFloor((short) 2);
        A101.setRoomNumber("A101");
        A101.setRoomName("Orchard");
        A101.setCapacity((short) 20);
        A101.setStreetAddress("3605 Hwy 52 N");
        A101.setCity("Rochester");
        A101.setState("Minnesota");
        A101.setZipCode(55901);

        Meeting meeting = new Meeting();
        meeting.title = "Iteration Planning Meeting";
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("US/Central"));
        cal.set(2017, 6, 1, 15, 0, 0);
        meeting.start = cal.getTime();
        cal.set(2017, 6, 1, 16, 0, 0);
        meeting.end = cal.getTime();
        meeting.location = A101;

        try (Jsonb jsonb = JsonbProvider.provider(jsonbProvider).create().build()) {
            String json = jsonb.toJson(meeting);
            System.out.println("JSON for Meeting object: " + json);

            int p = json.indexOf("\"A101\""); // value of roomNumber
            int a = json.indexOf("\"address\"");
            int c = json.indexOf("\"Rochester\""); // value of city

            // Order of LinkedHashMap is roomNumber before address before city
            assertTrue(json, p != -1 && p < a && a < c);
            assertTrue(json, json.contains("\"A101\""));

            Meeting m = jsonb.fromJson(json, Meeting.class);
            assertEquals(meeting.toString(), m.toString());
        }
    }

    // Unmarshall JSON into Java object where one of the fields is an interface
    // and JsonbDeserializer disambiguates which subclass should be used.
    // Use JsonbProvider default provider to obtain the JsonbBuilder.
    @Test
    public void testJsonbDeserializer() throws Exception {
        ReservableRoom G105 = new ReservableRoom();
        G105.setBuilding("50");
        G105.setFloor((short) 2);
        G105.setRoomNumber("G105");
        G105.setRoomName("Harvest");
        G105.setCapacity((short) 16);
        G105.setStreetAddress("3605 Hwy 52 N");
        G105.setCity("Rochester");
        G105.setState("Minnesota");
        G105.setZipCode(55901);

        Scrum scrum = new Scrum();
        scrum.squadName = "Zombie Apocalypse";
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("US/Central"));
        cal.set(2017, 5, 30, 15, 0, 0);
        scrum.start = cal.getTime();
        scrum.location = G105;

        try (Jsonb jsonb = JsonbProvider.provider().create().build()) {
            String json = jsonb.toJson(scrum);
            System.out.println("JSON for Scrum object: " + json);
            assertTrue(json, json.contains("\"G105\""));
            Scrum s = jsonb.fromJson(json, Scrum.class);
            assertEquals(scrum.toString(), s.toString());
        }
    }

    // Verify that the specified JsonbProvider is available,
    // and that its package matches the package name of the expected provider.
    public static void testJsonbProviderAvailable(String jsonbProvider) throws Exception {
        String expectedPackage = jsonbProvider.substring(0, jsonbProvider.lastIndexOf('.'));

        JsonbProvider provider = JsonbProvider.provider(jsonbProvider);

        String observedPackage = provider.getClass().getPackage().getName();
        assertTrue(observedPackage, observedPackage.startsWith(expectedPackage));
    }

    // Verify that the specified JsonbProvider is not available.
    public static void testJsonbProviderNotAvailable(String jsonbProvider) throws Exception {
        try {
            JsonbProvider provider = JsonbProvider.provider(jsonbProvider);
            fail("Provider class " + jsonbProvider + " should not be available as " + provider);
        } catch (JsonbException x) {
            if (x.getMessage().indexOf(jsonbProvider) < 0)
                throw x;
        }
    }

    // Load a JSON-B provider via thread context classloader and then use it to marshal/unmarshall JSON to/from Java objects.
    public static void testThreadContextClassLoader(String jsonbProvider) throws Exception {
        @SuppressWarnings("unchecked")
        Class<JsonbProvider> providerClass = (Class<JsonbProvider>) Thread.currentThread().getContextClassLoader().loadClass(jsonbProvider);

        JsonbProvider provider = providerClass.newInstance();
        assertEquals(jsonbProvider, provider.getClass().getName());

        Jsonb jsonb = JsonbBuilder.newBuilder(provider).build();

        ReservableRoom H115 = new ReservableRoom();
        H115.setBuilding("50");
        H115.setFloor((short) 2);
        H115.setRoomNumber("H115");
        H115.setRoomName("Bushel");
        H115.setCapacity((short) 10);
        H115.setStreetAddress("3605 Hwy 52 N");
        H115.setCity("Rochester");
        H115.setState("Minnesota");
        H115.setZipCode(55901);

        String json = jsonb.toJson(H115);
        System.out.println("JSON for ReservableRoom object: " + json);
        assertTrue(json, json.contains("\"H115\""));
        assertTrue(json, json.contains("\"Rochester\""));

        ReservableRoom rr = jsonb.fromJson(json, ReservableRoom.class);
        jsonb.close();
        assertEquals(H115.toString(), rr.toString());
    }
}
