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
package com.ibm.websphere.jsonsupport.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.websphere.jsonsupport.test.User.Role;
import com.ibm.ws.jsonsupport.internal.JSONJacksonImpl;

public class CompareTest {

    private SuperUser setupUser() {

        //setup a POJO java object for comparison
        SuperUser su = new SuperUser();
        su.age = 19;
        su.firstName = "Jon";
        su.role = Role.Moderator;
        su.height = 5.9f;
        su.isActive = true;
        su.weight = 121.494323;

        //setup array and a list for testing
        ArrayList<User> managedUsers = new ArrayList<User>();
        User managedUser = new User();
        managedUser.age = 21;
        managedUser.role = Role.None;
        User managedUser2 = new User();
        managedUser2.age = 25;
        managedUser2.isActive = false;
        managedUser.role = Role.None;
        managedUsers.add(managedUser);
        managedUsers.add(managedUser2);
        su.managedUsers = new User[managedUsers.size()];
        su.managedUsersList = managedUsers;
        su.managedUsers = managedUsers.toArray(su.managedUsers);

        //setup hashmaps for tesitng		
        su.userMap = new HashMap<Object, User>();
        User u1 = new User();
        u1.height = 5.8f;
        User u2 = new User();
        u2.age = 34;
        su.userMap.put(1, u1);
        su.userMap.put("2", u2);

        su.testMap = new HashMap<Object, Object>();
        su.testMap.put("1", 1);
        su.testMap.put(2, "2");
        su.testMap.put(3.0, 3.0);
        su.testMap.put("isTrue", true);
        return su;
    }

    @Test
    public void marshallTest() throws JsonGenerationException, JsonMappingException, IOException, JSONMarshallException {

        //setup a POJO java object for comparison
        SuperUser td = setupUser();

        //Use ObjectMapper to JSONP 
        JSON j = new JSONJacksonImpl();
        String jsonPString = null;
        jsonPString = j.stringify(td);

        //Setup and use Jackson to create JSON String
        String jsonJackson = null;
        ObjectMapper m = new ObjectMapper();
        StringWriter sw = new StringWriter();
        m.writeValue(sw, td);
        jsonJackson = sw.toString();

        //System.out.println(jsonPString);
        //System.out.println(jsonJackson);

        assertEquals("String must be equal", jsonPString, jsonJackson);
    }

    @Ignore
    @Test
    public void marshallIgnoreNullTest()
                    throws JsonGenerationException, JsonMappingException, IOException, JSONMarshallException {

        //setup a POJO java object for comparison
        SuperUser td = setupUser();

        //Setup and use JSONP mapper 
        JSON j = new JSONJacksonImpl();
        String jsonPString = null;
        jsonPString = j.stringify(td);

        //Setup and use Jackson to create JSON String
        String jsonJackson = null;
        ObjectMapper m = new ObjectMapper();
        StringWriter sw = new StringWriter();
        m.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
        m.writeValue(sw, td);
        jsonJackson = sw.toString();

        //System.out.println(jsonPString);
        //System.out.println(jsonJackson);

        assertEquals("String must be equal", jsonPString, jsonJackson);
    }

    @Test
    public void unmarshallTest() throws JSONMarshallException, JsonParseException, JsonMappingException, IOException {

        String jsonString = "{\"age\":21, \"height\":6.2, \"weight\": 119.132465989, \"role\":\"Moderator\", \"firstName\":\"John\", " +
                            "\"lastName\":\"Smith\", \"isActive\":true, \"managedUsers\":[{\"height\":6.1},{\"weight\":111.21}]," +
                            "\"managedUsersList\":[{\"height\":6.1},{\"weight\":111.21}], \"userMap\":{\"id_1\":{\"age\":28}}," +
                            "\"testMap\":{\"1\":1, \"two\":2.0, \"map\": {\"testKey\": \"testValue\"} }}";

        JSON j = new JSONJacksonImpl();
        SuperUser suJsonP = j.parse(jsonString, SuperUser.class);

        //check that pojo was assigned correct values
        assertTrue(suJsonP != null);
        assertTrue(suJsonP.age == 21);
        assertTrue(suJsonP.height == 6.2f);
        assertTrue(suJsonP.weight == 119.132465989);
        assertTrue(suJsonP.role == Role.Moderator);
        assertTrue(suJsonP.firstName.equals("John"));
        assertTrue(suJsonP.getLastName().equals("Smith"));
        assertTrue(suJsonP.isActive == true);
        assertTrue(suJsonP.managedUsers != null);
        assertTrue(suJsonP.managedUsers.length == 2);
        assertTrue(suJsonP.managedUsers[0] != null);
        assertTrue(suJsonP.managedUsers[1] != null);
        assertTrue(suJsonP.managedUsers[0].height == 6.1f);
        assertTrue(suJsonP.managedUsers[1].weight == 111.21);
        assertTrue(suJsonP.managedUsersList != null);
        assertTrue(suJsonP.managedUsersList.size() == 2);
        assertTrue(suJsonP.managedUsersList.get(0).height == 6.1f);
        assertTrue(suJsonP.managedUsersList.get(1).weight == 111.21);
        assertTrue(suJsonP.userMap != null);
        assertTrue(suJsonP.userMap.size() == 1);
        assertTrue(suJsonP.userMap.get("id_1") != null);
        assertTrue(suJsonP.userMap.get("id_1").age == 28);
        assertTrue(suJsonP.testMap != null);
        assertTrue(suJsonP.testMap.get("1") != null);
        assertTrue(suJsonP.testMap.get("1").equals(1));
        assertTrue(suJsonP.testMap.get("two") != null);
        assertTrue(suJsonP.testMap.get("two").equals(2.0));
        assertTrue(suJsonP.testMap.get("map") != null);
        @SuppressWarnings("unchecked")
        HashMap<String, Object> hashMap = (HashMap<String, Object>) suJsonP.testMap.get("map");
        assertTrue(hashMap.get("testKey").equals("testValue"));

        /*
         * ObjectMapper m = new ObjectMapper();
         * SuperUser suJackson = m.readValue(jsonString, SuperUser.class);
         */
    }
}
