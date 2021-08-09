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
package com.ibm.ws.security.jwt.utils;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;


import test.common.SharedOutputManager;

public class JwtUtilsTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.jwt.*=all");

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        //System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
        //System.out.println("Exiting test: " + testName.getMethodName());
    }

    @Test
    public void testClaimsFromJsonObject() {
        String jsonString1 = "{ \"schemas\":[\"urn:scim:schemas:core:2.0:User\"],\"userName\":\"janedoe\", " +
                "\"externalId\":\"janedoe\", " +
                "\"name\":{ \"formatted\":\"Ms. Jane J Doe \", \"familyName\":\"Doe\", \"givenName\":\"Jane\" }, " +
                "\"password\":\"asdf\", " +
                "\"phoneNumbers\":[ { \"value\": \"555-555-8377\", \"type\":\"work\" }, { \"value\": \"555-555-8378\", \"type\":\"fax\" } ], " +
                "\"emails\":[ { \"value\": \"jdoe@rabitmail.com\", \"type\":\"personal\" }, { \"value\": \"jdoe@turtlemail.com\", \"type\":\"work\" } ]," + 
                "\"addresses\": [ { \"type\": \"work\", \"streetAddress\": \"100 Universal City Plaza\", \"locality\": \"Hollywood\", " +
                "\"region\": \"CA\", \"postalCode\": \"91608\", \"country\": \"US\", \"formatted\": \"100 Universal City Plaza, Hollywood, CA 91608 US\" }, " +
                "{ \"type\": \"home\", \"streetAddress\": \"911 Universal City Plaza\", \"locality\": \"Hollywood\", \"region\": \"CA\"," +
                " \"postalCode\": \"91608\", \"country\": \"US\", \"formatted\": \"911 Universal City Plaza, Hollywood, CA 91608 US\" }] }";
        
//        String jsonString = "{\"emails\" : [{\"email\":\"cliang2007@yahoo.com\",\"primary\":true,\"verified\":true,\"visibility\":\"public\"},{\"email\":\"c_liang2000@aol.com\",\"primary\":false,\"verified\":false,\"visibility\":null}]}";
//        String json = "{ \"schemas\":[\"urn:scim:schemas:core:2.0:User\"]}";
        try {
            Map map = JwtUtils.claimsFromJsonObject(jsonString1);
            //Map map = JwtUtils.claimsFromJson(jsonString1, null);
//            Set<Entry<String, Object>> entries = map.entrySet();
//            Iterator<Entry<String, Object>> it = entries.iterator();
//            while (it.hasNext()) {
//                Entry<String, Object> entry = it.next();
//                System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
//            }
            Map map2 = (Map) map.get("name");
            String familyName = (String) map2.get("familyName");
            String givenName = (String) map2.get("givenName");
            Assert.assertEquals("The expected family name is Doe", "Doe", familyName);
            Assert.assertEquals("The expected given name is Jane","Jane", givenName);
            
            Object list = map.get("schemas");
            if (list instanceof List) {
                List list2 = (List) list;
                for (int i = 0 ; i < list2.size() ; i++) {
                    if (list2.get(i) instanceof Map) {
                        fail("schemas is a list but the element of the list is not a correct data type");
                    }
                    else {
                        Assert.assertEquals("The expected schemas element is not found", "urn:scim:schemas:core:2.0:User", list2.get(i));
                    }  
                }
            }
            
            Object arrayOfMaps = map.get("phoneNumbers");
            if (arrayOfMaps instanceof ArrayList<?>) {
                ArrayList<Map> mapsList = (ArrayList<Map>)arrayOfMaps;
                for (int i =  0 ; i < mapsList.size() ; i ++) {
                    Map map3 = (Map) mapsList.get(i);
                    if ("work".equals((String)map3.get("type"))) {
                        String number = (String)map3.get("value");
                        Assert.assertEquals("The expected ph.no is 555-555-8377", "555-555-8377", number);
                    }
                }
            }
            
            Object emails = map.get("emails");
            if (emails instanceof List) {
                List emailslist = (List)emails;
                String email = null;
                if (emailslist.get(0) instanceof Map) {
                    for (int i = 0 ; i < emailslist.size() ; i++) {
                        Map map4 = (Map) emailslist.get(i);
                        if ("personal".equals((String)map4.get("type"))) {
                            email = (String) map4.get("value");
                            break;
                        }  
                    }
                    Assert.assertEquals("The expected personal email is", "jdoe@rabitmail.com", email);
                }
                else {
                    fail("emails list does not have the expected type object");
                }   
            }   
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }
    
    @Test
    public void testIsJsonJsonObj() {
        String jsonString = "{\"emails\" : [{\"email\":\"cliang2007@yahoo.com\",\"primary\":true,\"verified\":true,\"visibility\":\"public\"},{\"email\":\"c_liang2000@aol.com\",\"primary\":false,\"verified\":false,\"visibility\":null}]}";
        
        boolean isJson = JwtUtils.isJson(jsonString);
        
        Assert.assertTrue("This should be a good json formatted string and expecting true", isJson);
    }
    
    @Test
    public void testIsJsonJsonArrayOfJsonObjs() {
        String jsonArray = "[{\"email\":\"cliang2007@yahoo.com\",\"primary\":true,\"verified\":true,\"visibility\":\"public\"},{\"email\":\"c_liang2000@aol.com\",\"primary\":false,\"verified\":false,\"visibility\":null}]";
        
        boolean isJson = JwtUtils.isJson(jsonArray);
       
        Assert.assertTrue("This should be a good json array and expecting true", isJson);
    }
    
    @Test
    public void testIsJsonJsonArrayOfArrays() {
        String jsonArray = "[[\"str1\", \"str2\", \"str3\"],[\"str6\", \"str7\", \"str8\"]]";
        
        boolean isJson = JwtUtils.isJson(jsonArray);
        
        Assert.assertTrue("This should be a good json array and expecting true", isJson);
    }
    
    @Test
    public void testIsJsonJsonArrayOfNumbers() {
        String jsonArray = "[1234, 5678, 9876]";
        
        boolean isJson = JwtUtils.isJson(jsonArray);
        
        Assert.assertTrue("This should be a good json array and expecting true", isJson);
    }

}
