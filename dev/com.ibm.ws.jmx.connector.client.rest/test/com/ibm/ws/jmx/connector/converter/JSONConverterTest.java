/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.converter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.management.Attribute;
import javax.management.AttributeChangeNotificationFilter;
import javax.management.AttributeList;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.relation.MBeanServerNotificationFilter;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONArtifact;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import com.ibm.ws.jmx.connector.datatypes.CreateMBean;
import com.ibm.ws.jmx.connector.datatypes.Invocation;
import com.ibm.ws.jmx.connector.datatypes.JMXServerInfo;
import com.ibm.ws.jmx.connector.datatypes.MBeanInfoWrapper;
import com.ibm.ws.jmx.connector.datatypes.MBeanQuery;
import com.ibm.ws.jmx.connector.datatypes.NotificationArea;
import com.ibm.ws.jmx.connector.datatypes.NotificationRegistration;
import com.ibm.ws.jmx.connector.datatypes.NotificationSettings;
import com.ibm.ws.jmx.connector.datatypes.ObjectInstanceWrapper;
import com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration;
import com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration.Operation;

/**
 *
 */
public class JSONConverterTest {

    //TODO: testWriteMBeanQuery test queryExp

    private final JSONConverter converter = JSONConverter.getConverter();

    // Names of JSON object members.
    private static final String N_API = "api";
    private static final String N_ATTRIBUTES = "attributes";
    private static final String N_ATTRIBUTES_URL = "attributes_URL";
    private static final String N_CLASSNAME = "className";
    private static final String N_CONSTRUCTORS = "constructors";
    private static final String N_CREATEMBEAN = "createMBean";
    private static final String N_DEFAULTDOMAIN = "defaultDomain";
    private static final String N_DELIVERYINTERVAL = "deliveryInterval";
    private static final String N_DESCRIPTION = "description";
    private static final String N_DESCRIPTOR = "descriptor";
    private static final String N_DOMAINS = "domains";
    private static final String N_ENABLED = "enabled";
    private static final String N_DISABLED = "disabled";
    private static final String N_ENTRIES = "entries";
    private static final String N_FILE_TRANSFER = "fileTransfer";
    private static final String N_FILTER = "filter";
    private static final String N_FILTERS = "filters";
    private static final String N_FILTERID = "filterID";
    private static final String N_GRAPH = "graph";
    private static final String N_HANDBACK = "handback";
    private static final String N_HANDBACKID = "handbackID";
    private static final String N_HOSTNAME = "hostName";
    private static final String N_IMPACT = "impact";
    private static final String N_INBOX = "inbox";
    private static final String N_INBOXEXPIRY = "inboxExpiry";
    private static final String N_INSTANCEOF = "instanceOf";
    private static final String N_ISIS = "isIs";
    private static final String N_ISREADABLE = "isReadable";
    private static final String N_ISWRITABLE = "isWritable";
    private static final String N_ITEMS = "items";
    private static final String N_KEY = "key";
    private static final String N_KEYTYPE = "keyType";
    private static final String N_LISTENER = "listener";
    private static final String N_LOADERNAME = "loaderName";
    private static final String N_MBEANCOUNT = "mbeanCount";
    private static final String N_MBEANS = "mbeans";
    private static final String N_MESSAGE = "message";
    private static final String N_NAME = "name";
    private static final String N_NAMES = "names";
    private static final String N_NOTIFICATIONS = "notifications";
    private static final String N_NOTIFTYPES = "notifTypes";
    private static final String N_OBJECTNAME = "objectName";
    private static final String N_OPERATION = "operation";
    private static final String N_OPERATIONS = "operations";
    private static final String N_PARAMS = "params";
    private static final String N_REGISTRATIONS = "registrations";
    private static final String N_RETURNTYPE = "returnType";
    private static final String N_SERIALIZED = "serialized";
    private static final String N_SERVERNAME = "serverName";
    private static final String N_SERVERREGISTRATIONS = "serverRegistrations";
    private static final String N_SERVERUSERDIR = "serverUserDir";
    private static final String N_SEQUENCENUMBER = "sequenceNumber";
    private static final String N_SIGNATURE = "signature";
    private static final String N_SIMPLEKEY = "simpleKey";
    private static final String N_SOURCE = "source";
    private static final String N_STACKTRACE = "stackTrace";
    private static final String N_EXCEPTION_MESSAGE = "error";
    private static final String N_THROWABLE = "throwable";
    private static final String N_TIMESTAMP = "timeStamp";
    private static final String N_TYPE = "type";
    private static final String N_TYPES = "types";
    private static final String N_URL = "URL";
    private static final String N_USELOADER = "useLoader";
    private static final String N_USERDATA = "userData";
    private static final String N_USESIGNATURE = "useSignature";
    private static final String N_VALUE = "value";
    private static final String N_VALUES = "values";
    private static final String N_VERSION = "version";

    private static final String OPEN_JSON_ARRAY = "[";
    private static final String CLOSE_JSON_ARRAY = "]";
    private static final char OPEN_JSON = '{';
    private static final char CLOSE_JSON = '}';
    private static final char ESCAPE = '\\';
    private static final char COMMA = ',';
    private static final char COLON = ':';
    private static final char EQUAL = '=';
    private static final String TRUE = "true";

    //Testing variables
    private static final int TEST_INT = 1234567890;
    private static final String TEST_INT_JSON = encloseJSON(Integer.toString(TEST_INT));
    private static final String TEST_STR = "abcdefghijklmnopqrstuvwxyz";
    private static final char[] SPECIAL_CHARS =
    {
     '\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006',
     '\u0007', '\u0008', '\u0009', '\u000b', '\u000c', '\u000e', '\u000f',
     '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016',
     '\u0017', '\u0018', '\u0019', '\u001a', '\u001b', '\u001c', '\u001d',
     '\u001e', '\u001f', '\\'
    };

    private static String specialCharString;

    private static final boolean TEST_BOOLEAN_FALSE = false;
    private static final boolean TEST_BOOLEAN_TRUE = true;

    private static final int TEST_JMX_VERSION = 1;
    private static final String TEST_JMX_MBEANS_URL = "http://testurl.com/mbeans";
    private static final String TEST_JMX_CREATEMBEAN_URL = "http://testurl.com/create";
    private static final String TEST_JMX_MBEANCOUNT_URL = "http://testurl.com/mbeanCount";
    private static final String TEST_JMX_DEFAULT_DOMAINS_URL = "http://testurl.com/defaultDomain";
    private static final String TEST_JMX_DOMAINS_URL = "http://testurl.com/domains";
    private static final String TEST_JMX_NOTIFICATIONS_URL = "http://testurl.com/notifications";
    private static final String TEST_JMX_INSTANCEOF_URL = "http://testurl.com/instanceOf";
    private static final String TEST_JMX_FILE_TRANSFER_URL = "http://testurl.com/file";
    private static final String TEST_JMX_API_URL = "http://testurl.com/api";
    private static final String TEST_JMX_GRAPH_URL = "http://testurl.com/graph";

    private static final String TEST_OBJ_INST_CLASS_NAME = "SomeClassName.class";
    private static final String TEST_OBJ_INST_OBJ_NAME = "SomeObjectName";
    private static final String TEST_OBJ_INST_OBJ_KEY = "objectInstanceObjectKey";
    private static final String TEST_OBJ_INST_OBJ_VALUE = "objectInstanceObjectVal";
    private static final String TEST_OBJ_INST_BEAN_INFO_URL = "http://testurl.com";

    private static final String TEST_MBEAN_QUERY_INSTANCE = "mBeanQueryInstance";
    private static final String TEST_MBEAN_QUERY_OBJ_NAME = "mBeanObjectName";
    private static final String TEST_MBEAN_QUERY_OBJ_KEY = "mBeanObjectKey";
    private static final String TEST_MBEAN_QUERY_OBJ_VALUE = "mBeanObjectValue";

    private static final String TEST_CREATE_MBEAN_OBJ_NAME = "createMBeanObjectName";
    private static final String TEST_CREATE_MBEAN_OBJ_KEY = "createMBeanObjectKey";
    private static final String TEST_CREATE_MBEAN_OBJ_VALUE = "createMBeanObjectValue";
    private static final String TEST_CREATE_MBEAN_LOADER_NAME = "createMBeanLoaderObjectName";
    private static final String TEST_CREATE_MBEAN_LOADER_KEY = "createMBeanLoaderObjectKey";
    private static final String TEST_CREATE_MBEAN_LOADER_VALUE = "createMBeanLoaderObjectValue";
    private static final String TEST_CREATE_MBEAN_CLASS_NAME = "mBeanClassName";
    private static final String TEST_CREATE_MBEAN_SIGNATURE_STRING = "createMBeanSignature";
    private static final boolean TEST_CREATE_MBEAN_USE_LOADER = true;
    private static final boolean TEST_CREATE_MBEAN_USE_SIGNATURE = true;

    //Test MBeanInfo
    //Wrapper
    private static final String TEST_MBEAN_INFO_CLASS_NAME = "MBeanInfoClassName";
    private static final String TEST_MBEAN_INFO_DESCRIPTION = "some description";
    private static final String TEST_MBEAN_INFO_URL = "http://someurl.com";
    private static final String TEST_MBEAN_INFO_OP_KEY = "mBeanOperationInfoName";
    private static final String TEST_MBEAN_INFO_OP_URL = "http://someurl.com";

    //AttributeInfo
    private static final String TEST_MBEAN_ATTR_INFO_NAME = "mBeanAttributeInfoName";
    private static final String TEST_MBEAN_ATTR_INFO_TYPE = "mBeanAttributeInfoType";
    private static final String TEST_MBEAN_ATTR_INFO_DESCRIPTION = "mBeanAttributeInfoDescription";
    private static final boolean TEST_MBEAN_ATTR_INFO_ISIS = false;
    private static final boolean TEST_MBEAN_ATTR_INFO_READABLE = true;
    private static final boolean TEST_MBEAN_ATTR_INFO_WRITABLE = true;
    private static final String TEST_MBEAN_ATTR_INFO_URL = "http://someurl.com";

    //MBeanConstructorInfo
    private static final String TEST_MBEAN_CONST_INFO_NAME = "mBeanConstructorInfoName";
    private static final String TEST_MBEAN_CONST_DESCRIPTION = "mBeanConstructorInfoDescription";

    //MBeanParameterInfo
    private static final String TEST_MBEAN_PARAM_INFO_NAME = "mBeanParamInfoName";
    private static final String TEST_MBEAN_PARAM_INFO_TYPE = "mBeanParamInfoType";
    private static final String TEST_MBEAN_PARAM_INFO_DESCRIPTION = "mBeanParamInfoDescription";

    //MBeanOperationinfo
    private static final String TEST_MBEAN_OP_INFO_NAME = "mBeanOperationInfoName";
    private static final String TEST_MBEAN_OP_INFO_DESCRIPTION = "mBeanOperationinfoDescription";
    private static final String TEST_MBEAN_OP_INFO_TYPE = "mBeanOperationInfoType";
    private static final int TEST_MBEAN_OP_INFO_IMPACT = 1;

    //MBeanNotificationInfo
    private static final String TEST_MBEAN_NOTIFICATION_NAME = "notInfoName";
    private static final String TEST_MBEAN_NOTIFICATION_DESCRIPTION = "notInfoDesc";
    private static final String TEST_MBEAN_NOTIFICATION_TYPE = "someType";
    private static final int TEST_MBEAN_NOTIFICATION_ARRAY_SIZE = 5;

    private static final String TEST_NOTIFICATION_AREA_REGISTRATION = "http://notificationarea.com";
    private static final String TEST_NOTIFICATION_AREA_INBOX = "http://notificationinbox.com";
    private static final String TEST_NOTIFICATION_AREA_SERVER_REGISTRATION = "http://notificationserver.registration";

    private static final String TEST_SERVER_NOTIF_REGISTRATION_OBJ_NAME = "objectName";
    private static final String TEST_SERVER_NOTIF_REGISTRATION_OBJ_KEY = "objectNameKey";
    private static final String TEST_SERVER_NOTIF_REGISTRATION_OBJ_VALUE = "objectNameValue";
    private static final String TEST_SERVER_NOTIF_REGISTRATION_LISTENER_NAME = "listenerName";
    private static final String TEST_SERVER_NOTIF_REGISTRATION_LISTENER_KEY = "listenerKey";
    private static final String TEST_SERVER_NOTIF_REGISTRATION_LISTENER_VALUE = "listenerValue";
    private static final String TEST_SERVER_NOTIF_REGISTRATION_HANDBACK = "some POJO Object";
    private static final int TEST_SERVER_NOTIF_REGISTRATION_FILTER_ID = 1;
    private static final int TEST_SERVER_NOTIF_REGISTRATION_HANDBACK_ID = 2;

    private static final String TEST_CREATE_NOTIFICATION_REGISTRATION_NAME = "objectName";
    private static final String TEST_CREATE_NOTIFICATION_REGISTRATION_KEY = "objectNameKey";
    private static final String TEST_CREATE_NOTIFICATION_REGISTRATION_VALUE = "objectNameValue";

    private static final String TEST_NOTIFICATION_TYPE = "notificationType";
    private static final String TEST_NOTIFICATION_OBJ_NAME = "objectName";
    private static final String TEST_NOTIFICATION_OBJECT_KEY = "objectKey";
    private static final String TEST_NOTIFICATION_OBJECT_VALUE = "objectValueForMBeanServer";
    private static final String TEST_NOTIFICATION_ATTRIBUTE_NAME = "attributeNameFornotifFilter";
    private static final String TEST_NOTIFICATION_SUPPORT_TYPE = "notificationTypeForNotifSupport";
    private static final long TEST_NOTIFICATION_SEQUENCE = 123213;
    private static final long TEST_NOTIFICATION_TIME_STAMP = 123123;
    private static final String TEST_NOTIFICATION_MESSAGE = "someMessage";

    private static final String TEST_NOTIFICATION_RECORD_HOST_NAME = "skywalker.torolab.ibm.com";
    private static final String TEST_NOTIFICATION_RECORD_SERVER_NAME = "myServer";
    private static final String TEST_NOTIFICATION_RECORD_SERVER_USER_DIR = "/dev/wlp/usr";

    private static final int TEST_NOTIFICATION_SETTING_DELIVERY_INTERVAL = 123;
    private static final int TEST_NOTIFICATION_SETTING_INBOX_EXPIRY = 234;

    private static final String TEST_ATTRIBUTE_NAME = "attributeName";

    private static final String TEST_THROWABLE_MESSAGE = "throwable stack trace";

    private static final String TEST_INVOCATION_SIGNATURE = "invocation signature";

    @BeforeClass
    public static void setUp() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < SPECIAL_CHARS.length; i++) {
            str.append(SPECIAL_CHARS[i]);
        }
        specialCharString = str.toString();
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#getConverter()}.
     */
    @Test
    public void testGetConverter() {
        JSONConverter jsonConverter = JSONConverter.getConverter();

        assertTrue(jsonConverter instanceof JSONConverter);

    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#returnConverter(com.ibm.ws.jmx.connector.converter.JSONConverter)}.
     */
    @Test
    public void testReturnConverter() {
        JSONConverter jsonConverter = JSONConverter.getConverter();

        JSONConverter.returnConverter(jsonConverter);

        JSONConverter jsonConverterIn = JSONConverter.getConverter();

        //should be the same object as the first converter we pushed in
        assertSame(jsonConverter, jsonConverterIn);
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeInt(java.io.OutputStream, int)}.
     */
    @Test
    public void testWriteInt() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            converter.writeInt(out, TEST_INT);
            assertEquals(out.toString(), TEST_INT_JSON);
        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readInt(java.io.InputStream)}.
     */
    @Test
    public void testReadInt() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;
        try {
            converter.writeInt(out, TEST_INT);
            in = new ByteArrayInputStream(out.toByteArray());
            assertEquals(converter.readInt(in), TEST_INT);
        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    @Test
    public void testWriteBoolean() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            converter.writeBoolean(out, TEST_BOOLEAN_TRUE);
            assertEquals(out.toString(), encloseJSON(TEST_BOOLEAN_TRUE));

            out = new ByteArrayOutputStream();
            converter.writeBoolean(out, TEST_BOOLEAN_FALSE);
            assertEquals(out.toString(), encloseJSON(TEST_BOOLEAN_FALSE));

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }

    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readBoolean(java.io.InputStream)}.
     */
    @Test
    public void testReadBoolean() {
        ByteArrayOutputStream out;
        InputStream in;

        try {
            out = new ByteArrayOutputStream();
            converter.writeBoolean(out, TEST_BOOLEAN_TRUE);
            in = new ByteArrayInputStream(out.toByteArray());
            assertEquals(converter.readBoolean(in), TEST_BOOLEAN_TRUE);

            out = new ByteArrayOutputStream();
            converter.writeBoolean(out, TEST_BOOLEAN_FALSE);
            in = new ByteArrayInputStream(out.toByteArray());
            assertEquals(converter.readBoolean(in), TEST_BOOLEAN_FALSE);

        } catch (Exception e) {

        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeString(java.io.OutputStream, java.lang.String)}.
     */
    @Test
    public void testWriteString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            converter.writeString(out, TEST_STR);
            assertEquals(out.toString(), encloseJSON(TEST_STR));
        } catch (Exception e) {
            fail("Exception encountered " + e);
        }

    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readString(java.io.InputStream)}.
     */
    @Test
    public void testReadString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;
        try {
            converter.writeString(out, TEST_STR);
            in = new ByteArrayInputStream(out.toByteArray());
            assertEquals(converter.readString(in), TEST_STR);
            out.close();
            in.close();
            out = new ByteArrayOutputStream();
            converter.writeString(out, specialCharString);
            in = new ByteArrayInputStream(out.toByteArray());
            assertEquals(specialCharString, converter.readString(in));
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void testWriteEmptyStringArray() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        String[] stringArray = new String[] {};
        JSONArray json = new JSONArray();

        converter.writeStringArray(out, stringArray);
        assertEquals(out.toString(), parseJSONToString(json));
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readStringArray(java.io.InputStream)}.
     */
    @Test
    public void testReadEmptyStringArray() {

        String[] strArray = new String[] {};

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        try {
            converter.writeStringArray(out, strArray);
            in = new ByteArrayInputStream(out.toByteArray());
            String[] inStreamOutput = converter.readStringArray(in);
            assertArrayEquals(inStreamOutput, strArray);
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void testWriteSingleElementStringArray() {
        int arraySize = 1;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        String[] stringArray = new String[arraySize];
        JSONArray json = new JSONArray();

        for (int i = 0; i < arraySize; i++) {
            stringArray[i] = TEST_STR;
            json.add(TEST_STR);
        }

        try {
            converter.writeStringArray(out, stringArray);
            assertEquals(out.toString(), parseJSONToString(json));
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readStringArray(java.io.InputStream)}.
     */
    @Test
    public void testReadSingleElementStringArray() {
        int arraySize = 1;

        JSONArray json = new JSONArray();
        String[] strArray = new String[arraySize];

        for (int i = 0; i < arraySize; i++) {
            json.add(TEST_STR);
            strArray[i] = TEST_STR;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        try {
            converter.writeStringArray(out, strArray);
            in = new ByteArrayInputStream(out.toByteArray());
            String[] inStreamOutput = converter.readStringArray(in);
            for (int i = 0; i < arraySize; i++) {
                assertEquals(inStreamOutput[i], strArray[i]);
            }
            assertArrayEquals(inStreamOutput, strArray);
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }

    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void testWriteStringArray() {
        int arraySize = 5;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        String[] stringArray = new String[arraySize];
        JSONArray json = new JSONArray();

        for (int i = 0; i < arraySize; i++) {
            stringArray[i] = TEST_STR;
            json.add(TEST_STR);
        }

        try {
            converter.writeStringArray(out, stringArray);
            assertEquals(out.toString(), parseJSONToString(json));
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readStringArray(java.io.InputStream)}.
     */
    @Test
    public void testReadStringArray() {
        int arraySize = 5;

        JSONArray json = new JSONArray();
        String[] strArray = new String[arraySize];

        for (int i = 0; i < arraySize; i++) {
            json.add(TEST_STR);
            strArray[i] = TEST_STR;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        try {
            converter.writeStringArray(out, strArray);
            in = new ByteArrayInputStream(out.toByteArray());
            String[] inStreamOutput = converter.readStringArray(in);
            for (int i = 0; i < arraySize; i++) {
                assertEquals(inStreamOutput[i], strArray[i]);
            }
            assertArrayEquals(inStreamOutput, strArray);
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }

    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void testConvertEmptyHashMap() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Map<Object, Object> map = new HashMap<Object, Object>();

        converter.writePOJO(out, map);
        Object pojo = converter.readPOJO(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("FAIL: conversion of the empty map failed", map, pojo);
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void testConvertSingleElementHashMap() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put(1, "a");

        converter.writePOJO(out, map);
        Object pojo = converter.readPOJO(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("FAIL: conversion of the single element map failed", map, pojo);
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void testConvertMultipleElementHashMap() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put(1, "a");
        map.put("b", 2);
        map.put(0L, 1);

        converter.writePOJO(out, map);
        Object pojo = converter.readPOJO(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("FAIL: conversion of the multiple element map failed", map, pojo);
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writePOJO(java.io.OutputStream, java.lang.Object)}.
     */
    @Test
    public void testWritePOJO() {
        //TODO: Complex keys? what is that?

        OutputStream out;
        StringBuilder str;

        /**
         * Variables -- add test cases here
         */

        //primitive
        Object[] testCases = { null, Integer.MAX_VALUE, "string", 'a' };
        Integer[] intTest = { 1, 2, 3, 4, 5, 6 };
        String[] stringTest = { "a", "b", "c", "d", "e", "f", "abcdef" };
        Double[] doubleTest = { 1.1, 2.2, 3.3, 4.0, 5.1 };
        Character[] charTest = { 'a', 'b', 'c', 'd' };

        //TODO:Add more primitive arrays here, and test cases
        //Char, float?, etc.
        Object[] primitiveArrays =
        {
         intTest,
         stringTest,
         doubleTest,
         charTest
        };

        //Map
        Map<Object, Object> mapTest = new HashMap<Object, Object>();
        mapTest.put("key 1", "value 1");
        mapTest.put("key 2", "value 2");
        mapTest.put(1, "value 3");
        mapTest.put("asdf", 1);

        //Serializability
        List<Object> listTest = new ArrayList<Object>();

        listTest.add(1);
        listTest.add("abc");
        listTest.add(listTest);

        /**
         * Start Tests -- tests the variables above and their POJO output
         */
        try {
            //null case
            out = new ByteArrayOutputStream();
            str = new StringBuilder();

            str.append(OPEN_JSON);
            str.append(encloseString(N_VALUE, null));
            str.append(COMMA);
            str.append(encloseString(N_TYPE, null));
            converter.writePOJO(out, null);
            str.append(CLOSE_JSON);

            assertEquals(out.toString(), str.toString());

            //primitives
            for (int i = 0; i < testCases.length; i++) {
                out = new ByteArrayOutputStream();
                str = new StringBuilder();

                str.append(OPEN_JSON);
                if (testCases[i] != null) {
                    str.append(encloseString(N_VALUE, testCases[i].toString()));
                } else {
                    str.append(encloseString(N_VALUE, null));
                }
                str.append(COMMA);
                if (testCases[i] != null) {
                    str.append(encloseString(N_TYPE, testCases[i].getClass().getName()));
                } else {
                    str.append(encloseString(N_TYPE, null));
                }
                str.append(CLOSE_JSON);

                converter.writePOJO(out, testCases[i]);

                assertEquals(out.toString(), str.toString());
            }

            //primitive type arrays
            for (int i = 0; i < primitiveArrays.length; i++) {
                out = new ByteArrayOutputStream();
                str = new StringBuilder();

                str.append(OPEN_JSON);
                str.append(encloseQuote(N_VALUE));
                str.append(COLON);
                str.append(parseObjectToString((Object[]) primitiveArrays[i]));
                str.append(COMMA);
                str.append(encloseString(N_TYPE, primitiveArrays[i].getClass().getName()));
                str.append(CLOSE_JSON);

                converter.writePOJO(out, primitiveArrays[i]);

                assertEquals(out.toString(), str.toString());

            }

            //object arrays
            out = new ByteArrayOutputStream();
            str = new StringBuilder();
            str.append(OPEN_JSON);
            //value
            str.append(encloseQuote(N_VALUE));
            str.append(COLON);
            str.append(parseObjectToString(testCases));
            str.append(COMMA);
            //type
            str.append(encloseQuote(N_TYPE));
            str.append(COLON);
            str.append(OPEN_JSON);
            //type.classname
            str.append(encloseString(N_CLASSNAME, testCases.getClass().getName()));
            str.append(COMMA);
            //type.items
            str.append(encloseQuote(N_ITEMS));
            str.append(COLON);
            str.append(parseObjectTypeToString(testCases));

            str.append(CLOSE_JSON);
            str.append(CLOSE_JSON);

            converter.writePOJO(out, testCases);

            assertEquals(out.toString(), str.toString());

            //Map Test
            out = new ByteArrayOutputStream();
            str = new StringBuilder();

            str.append(OPEN_JSON);

            //value
            str.append(encloseQuote(N_VALUE));
            str.append(COLON);
            str.append(mapToJSON(mapTest));
            str.append(COMMA);

            //type
            str.append(encloseQuote(N_TYPE));
            str.append(COLON);
            str.append(OPEN_JSON);
            //type.classname
            str.append(encloseString(N_CLASSNAME, mapTest.getClass().getName()));
            str.append(COMMA);
            //type.simpleKey
            str.append(encloseQuote(N_SIMPLEKEY));
            str.append(COLON);
            str.append(TRUE);
            str.append(COMMA);
            //type.entries
            str.append(encloseQuote(N_ENTRIES));
            str.append(COLON);
            str.append(mapTypeToJSON(mapTest));

            str.append(CLOSE_JSON);

            str.append(CLOSE_JSON);

            converter.writePOJO(out, mapTest);
            assertEquals(out.toString(), str.toString());

            //Test Serialized
            out = new ByteArrayOutputStream();
            str = new StringBuilder();
            converter.writePOJO(out, listTest);

            str.append(ESCAPE);
            str.append(OPEN_JSON);
            str.append(encloseString(N_SERIALIZED, ".*"));
            str.append(CLOSE_JSON);

            assertTrue(Pattern.matches(str.toString(), out.toString()));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readPOJO(java.io.InputStream)}.
     */
    @Test
    public void testReadPOJO() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;
        Object POJOObj;

        List<Object> serializeTest = new ArrayList<Object>();
        serializeTest.add(1);
        serializeTest.add("abcd");
        serializeTest.add(serializeTest);

        try {
            //setup output stream to get data.
            POJOObj = makePOJOObject();
            converter.writePOJO(out, POJOObj);
            String POJOOutputString = out.toString();

            //see if server can read the output stream output.
            in = new ByteArrayInputStream(POJOOutputString.getBytes());
            Object POJOObjIn = converter.readPOJO(in);

            assertTrue(testObjects(POJOObjIn, POJOObj));

            //test serialize
            out = new ByteArrayOutputStream();
            converter.writePOJO(out, serializeTest);
            in = new ByteArrayInputStream(out.toByteArray());
            Object temp = converter.readPOJO(in);

            assertEquals(temp.getClass().getName(), serializeTest.getClass().getName());

            for (int i = 0; i < serializeTest.size(); i++) {
                assertEquals(((List<?>) temp).get(i).getClass().getName(), serializeTest.get(i).getClass().getName());
            }
        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeJMX(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.JMXServerInfo)}.
     */
    @Test
    public void testWriteJMX() {
        JMXServerInfo jmx = new JMXServerInfo();
        jmx.version = TEST_JMX_VERSION;
        jmx.mbeansURL = TEST_JMX_MBEANS_URL;
        jmx.createMBeanURL = TEST_JMX_CREATEMBEAN_URL;
        jmx.mbeanCountURL = TEST_JMX_MBEANCOUNT_URL;
        jmx.defaultDomainURL = TEST_JMX_DEFAULT_DOMAINS_URL;
        jmx.domainsURL = TEST_JMX_DOMAINS_URL;
        jmx.notificationsURL = TEST_JMX_NOTIFICATIONS_URL;
        jmx.instanceOfURL = TEST_JMX_INSTANCEOF_URL;
        jmx.fileTransferURL = TEST_JMX_FILE_TRANSFER_URL;
        jmx.apiURL = TEST_JMX_API_URL;
        jmx.graphURL = TEST_JMX_GRAPH_URL;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder str = new StringBuilder();

        //build the string to check against
        try {
            str.append(OPEN_JSON);
            str.append(encloseString(N_VERSION, Integer.toString(jmx.version)));
            str.append(COMMA);
            str.append(encloseString(N_MBEANS, jmx.mbeansURL));
            str.append(COMMA);
            str.append(encloseString(N_CREATEMBEAN, jmx.createMBeanURL));
            str.append(COMMA);
            str.append(encloseString(N_MBEANCOUNT, jmx.mbeanCountURL));
            str.append(COMMA);
            str.append(encloseString(N_DEFAULTDOMAIN, jmx.defaultDomainURL));
            str.append(COMMA);
            str.append(encloseString(N_DOMAINS, jmx.domainsURL));
            str.append(COMMA);
            str.append(encloseString(N_NOTIFICATIONS, jmx.notificationsURL));
            str.append(COMMA);
            str.append(encloseString(N_INSTANCEOF, jmx.instanceOfURL));
            str.append(COMMA);
            str.append(encloseString(N_FILE_TRANSFER, jmx.fileTransferURL));
            str.append(COMMA);
            str.append(encloseString(N_API, jmx.apiURL));
            str.append(COMMA);
            str.append(encloseString(N_GRAPH, jmx.graphURL));
            str.append(CLOSE_JSON);

        } catch (Exception e) {
            fail("Error setting up comparison string");
        }

        try {
            converter.writeJMX(out, jmx);
            assertEquals(out.toString(), str.toString());
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readJMX(java.io.InputStream)}.
     */
    @Test
    public void testReadJMX() {
        JMXServerInfo jmx = new JMXServerInfo();
        jmx.version = TEST_JMX_VERSION;
        jmx.mbeansURL = TEST_JMX_MBEANS_URL;
        jmx.createMBeanURL = TEST_JMX_CREATEMBEAN_URL;
        jmx.mbeanCountURL = TEST_JMX_MBEANCOUNT_URL;
        jmx.defaultDomainURL = TEST_JMX_DEFAULT_DOMAINS_URL;
        jmx.domainsURL = TEST_JMX_DOMAINS_URL;
        jmx.notificationsURL = TEST_JMX_NOTIFICATIONS_URL;
        jmx.instanceOfURL = TEST_JMX_INSTANCEOF_URL;
        jmx.fileTransferURL = TEST_JMX_FILE_TRANSFER_URL;
        jmx.apiURL = TEST_JMX_API_URL;
        jmx.graphURL = TEST_JMX_GRAPH_URL;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        try {
            //setup output stream to get data.
            converter.writeJMX(out, jmx);
            String jmxOutputStreamString = out.toString();

            //see if server can read the output stream output.
            in = new ByteArrayInputStream(jmxOutputStreamString.getBytes());
            JMXServerInfo jmxIn = converter.readJMX(in);

            assertTrue(testObjects(jmxIn, jmx));
            assertEquals(jmxIn.version, jmx.version);
            assertEquals(jmxIn.mbeansURL, jmx.mbeansURL);
            assertEquals(jmxIn.createMBeanURL, jmx.createMBeanURL);
            assertEquals(jmxIn.mbeanCountURL, jmx.mbeanCountURL);
            assertEquals(jmxIn.defaultDomainURL, jmx.defaultDomainURL);
            assertEquals(jmxIn.domainsURL, jmx.domainsURL);
            assertEquals(jmxIn.notificationsURL, jmx.notificationsURL);
            assertEquals(jmxIn.instanceOfURL, jmx.instanceOfURL);
            assertEquals(jmxIn.fileTransferURL, jmx.fileTransferURL);
            assertEquals(jmxIn.apiURL, jmx.apiURL);
            assertEquals(jmxIn.graphURL, jmx.graphURL);

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeObjectInstance(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.ObjectInstanceWrapper)}.
     */
    @Test
    public void testWriteObjectInstance() {
        ObjectInstanceWrapper wrapper = new ObjectInstanceWrapper();
        StringBuilder str = new StringBuilder();
        ObjectName objectName;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        //setup test params
        try {
            objectName = new ObjectName(TEST_OBJ_INST_OBJ_NAME, TEST_OBJ_INST_OBJ_KEY, TEST_OBJ_INST_OBJ_VALUE);
            ObjectInstance temp = new ObjectInstance(objectName, TEST_OBJ_INST_CLASS_NAME);

            wrapper.objectInstance = temp;
            wrapper.mbeanInfoURL = TEST_OBJ_INST_BEAN_INFO_URL;

            str.append(OPEN_JSON);
            str.append(encloseString(N_OBJECTNAME.toString(),
                                     TEST_OBJ_INST_OBJ_NAME + COLON +
                                                     TEST_OBJ_INST_OBJ_KEY + EQUAL +
                                                     TEST_OBJ_INST_OBJ_VALUE));
            str.append(COMMA);
            str.append(encloseString(N_CLASSNAME.toString(), TEST_OBJ_INST_CLASS_NAME));
            str.append(COMMA);
            str.append(encloseString(N_URL.toString(), TEST_OBJ_INST_BEAN_INFO_URL));
            str.append(CLOSE_JSON);

        } catch (Exception e) {

            fail("Exception encountered while setting up Object Instance Test parameters " + e);
        }

        //actual test
        try {
            converter.writeObjectInstance(out, wrapper);
            assertEquals(out.toString(), str.toString());
        } catch (Exception e) {
            fail("Exception encoutered during testing " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readObjectInstance(java.io.InputStream)}.
     */
    @Test
    public void testReadObjectInstance() {
        ObjectInstanceWrapper wrapper = new ObjectInstanceWrapper();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;
        ObjectName objectName;

        //setup parameters
        try {
            objectName = new ObjectName(TEST_OBJ_INST_OBJ_NAME, TEST_OBJ_INST_OBJ_KEY, TEST_OBJ_INST_OBJ_VALUE);
            ObjectInstance temp = new ObjectInstance(objectName, TEST_OBJ_INST_CLASS_NAME);

            wrapper.objectInstance = temp;
            wrapper.mbeanInfoURL = TEST_OBJ_INST_BEAN_INFO_URL;
        } catch (Exception e) {
            fail("Exception encoutered while setting up test parameters " + e);
        }

        //actual test
        try {
            converter.writeObjectInstance(out, wrapper);
            in = new ByteArrayInputStream(out.toByteArray());
            ObjectInstanceWrapper inWrap = converter.readObjectInstance(in);

            assertEquals(inWrap.objectInstance.getClassName(),
                         wrapper.objectInstance.getClassName());

            assertEquals(inWrap.objectInstance.getObjectName(),
                         wrapper.objectInstance.getObjectName());

            assertEquals(inWrap.objectInstance.getObjectName().getKeyPropertyListString(),
                         wrapper.objectInstance.getObjectName().getKeyPropertyListString());

            assertEquals(inWrap.mbeanInfoURL, wrapper.mbeanInfoURL);

        } catch (Exception e) {
            fail("Exception encountered during testing " + e);
        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeObjectInstanceArray(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.ObjectInstanceWrapper[])}.
     */
    @Test
    public void testWriteObjectInstanceArray() {
        int arraySize = 5;

        ObjectInstanceWrapper[] wrapper = new ObjectInstanceWrapper[arraySize];
        StringBuilder str = new StringBuilder();
        ObjectName objectName;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        //setup test params
        try {
            objectName = new ObjectName(TEST_OBJ_INST_OBJ_NAME, TEST_OBJ_INST_OBJ_KEY, TEST_OBJ_INST_OBJ_VALUE);
            ObjectInstance temp = new ObjectInstance(objectName, TEST_OBJ_INST_CLASS_NAME);

            for (int i = 0; i < arraySize; i++) {
                wrapper[i] = new ObjectInstanceWrapper();
                wrapper[i].objectInstance = temp;
                wrapper[i].mbeanInfoURL = TEST_OBJ_INST_BEAN_INFO_URL;
            }

            str.append('[');
            for (int i = 0; i < arraySize; i++) {
                str.append(OPEN_JSON);
                str.append(encloseString(N_OBJECTNAME.toString(),
                                         TEST_OBJ_INST_OBJ_NAME + COLON +
                                                         TEST_OBJ_INST_OBJ_KEY + EQUAL +
                                                         TEST_OBJ_INST_OBJ_VALUE));
                str.append(COMMA);
                str.append(encloseString(N_CLASSNAME.toString(), TEST_OBJ_INST_CLASS_NAME));
                str.append(COMMA);
                str.append(encloseString(N_URL.toString(), TEST_OBJ_INST_BEAN_INFO_URL));
                str.append(CLOSE_JSON);

                if (i != arraySize - 1) {
                    str.append(COMMA);
                }
            }
            str.append(']');

        } catch (Exception e) {

            fail("Exception encountered while setting up Object Instance Test parameters " + e);
        }

        //actual test
        try {
            converter.writeObjectInstanceArray(out, wrapper);
            for (int i = 0; i < arraySize; i++) {
                assertEquals(out.toString(), str.toString());
            }
        } catch (Exception e) {
            fail("Exception encoutered during testing " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readObjectInstances(java.io.InputStream)}.
     */
    @Test
    public void testReadObjectInstances() {
        int arraySize = 5;

        ObjectInstanceWrapper[] wrapper = new ObjectInstanceWrapper[arraySize];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;
        ObjectName objectName;

        //setup parameters
        try {
            objectName = new ObjectName(TEST_OBJ_INST_OBJ_NAME, TEST_OBJ_INST_OBJ_KEY, TEST_OBJ_INST_OBJ_VALUE);
            ObjectInstance temp = new ObjectInstance(objectName, TEST_OBJ_INST_CLASS_NAME);

            for (int i = 0; i < arraySize; i++) {
                wrapper[i] = new ObjectInstanceWrapper();
                wrapper[i].objectInstance = temp;
                wrapper[i].mbeanInfoURL = TEST_OBJ_INST_BEAN_INFO_URL;
            }
        } catch (Exception e) {
            fail("Exception encoutered while setting up test parameters " + e);
        }

        //actual test
        try {
            converter.writeObjectInstanceArray(out, wrapper);
            in = new ByteArrayInputStream(out.toByteArray());
            ObjectInstanceWrapper[] inWrap = converter.readObjectInstances(in);

            for (int i = 0; i < arraySize; i++) {
                assertEquals(inWrap[i].objectInstance.getClassName(),
                             wrapper[i].objectInstance.getClassName());

                assertEquals(inWrap[i].objectInstance.getObjectName(),
                             wrapper[i].objectInstance.getObjectName());

                assertEquals(inWrap[i].objectInstance.getObjectName().getKeyPropertyListString(),
                             wrapper[i].objectInstance.getObjectName().getKeyPropertyListString());

                assertEquals(inWrap[i].mbeanInfoURL,
                             wrapper[i].mbeanInfoURL);
            }

        } catch (Exception e) {
            fail("Exception encountered during testing " + e);
        }

    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeMBeanQuery(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.MBeanQuery)}.
     */
    @Test
    public void testWriteMBeanQuery() {
        MBeanQuery mBean = new MBeanQuery();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder str = new StringBuilder();

        //test params
        try {

            mBean.className = TEST_MBEAN_QUERY_INSTANCE;
            mBean.objectName = new ObjectName(TEST_MBEAN_QUERY_OBJ_NAME,
                            TEST_MBEAN_QUERY_OBJ_KEY,
                            TEST_MBEAN_QUERY_OBJ_VALUE);

            str.append(OPEN_JSON);
            str.append(encloseString(N_OBJECTNAME,
                                     mBean.objectName.getCanonicalName()));
            str.append(COMMA);
            str.append(encloseString(N_CLASSNAME, mBean.className));
            str.append(CLOSE_JSON);

        } catch (Exception e) {
            fail("Exception encountered while setting up test parameters" + e);
        }

        //actual test
        try {
            converter.writeMBeanQuery(out, mBean);
            assertEquals(out.toString(), str.toString());
        } catch (Exception e) {
            fail("Exception encountered during testing " + e);
        }

    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readMBeanQuery(java.io.InputStream)}.
     */
    @Test
    public void testReadMBeanQuery() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MBeanQuery mBean = new MBeanQuery();

        InputStream in;

        try {

            mBean.className = TEST_MBEAN_QUERY_INSTANCE;
            mBean.objectName = new ObjectName(TEST_MBEAN_QUERY_OBJ_NAME,
                            TEST_MBEAN_QUERY_OBJ_KEY,
                            TEST_MBEAN_QUERY_OBJ_VALUE);

        } catch (Exception e) {
            fail("Exception encoutered while setting up test parameters " + e);
        }

        try {
            //setup output stream to get data.
            converter.writeMBeanQuery(out, mBean);
            String mBeanOutputStreamString = out.toString();

            //see if server can read the output stream output.
            in = new ByteArrayInputStream(mBeanOutputStreamString.getBytes());
            MBeanQuery inMBean = converter.readMBeanQuery(in);

            assertEquals(inMBean.className, mBean.className);
            assertEquals(inMBean.objectName.toString(), mBean.objectName.toString());
            assertEquals(inMBean.objectName.getCanonicalName(), mBean.objectName.getCanonicalName());
            assertEquals(inMBean.objectName.getKeyProperty(TEST_MBEAN_QUERY_OBJ_KEY), TEST_MBEAN_QUERY_OBJ_VALUE);

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeCreateMBean(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.CreateMBean)}.
     */
    @Test
    public void testWriteCreateMBean() {
        int signatureSize = 5;
        int paramSize = 5;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream pojoOut = new ByteArrayOutputStream();
        StringBuilder str = new StringBuilder();

        CreateMBean mBean = new CreateMBean();
        //Setup test parameters
        try {
            mBean.className = TEST_CREATE_MBEAN_CLASS_NAME;
            mBean.objectName = new ObjectName(TEST_CREATE_MBEAN_OBJ_NAME,
                            TEST_CREATE_MBEAN_OBJ_KEY,
                            TEST_CREATE_MBEAN_OBJ_VALUE);
            mBean.loaderName = new ObjectName(TEST_CREATE_MBEAN_LOADER_NAME,
                            TEST_CREATE_MBEAN_LOADER_KEY,
                            TEST_CREATE_MBEAN_LOADER_VALUE);

            mBean.signature = new String[signatureSize];

            mBean.params = new Object[paramSize];

            for (int i = 0; i < paramSize; i++) {
                mBean.params[i] = makePOJOObject();
            }

            for (int i = 0; i < signatureSize; i++) {
                mBean.signature[i] = TEST_CREATE_MBEAN_SIGNATURE_STRING;
            }

            mBean.useLoader = TEST_CREATE_MBEAN_USE_LOADER;

            mBean.useSignature = TEST_CREATE_MBEAN_USE_SIGNATURE;

            str.append(OPEN_JSON);
            str.append(encloseString(N_CLASSNAME, TEST_CREATE_MBEAN_CLASS_NAME));
            str.append(COMMA);
            str.append(encloseString(N_OBJECTNAME,
                                     mBean.objectName.getCanonicalName()));
            str.append(COMMA);
            str.append(encloseString(N_LOADERNAME,
                                     mBean.loaderName.getCanonicalName()));
            str.append(COMMA);
            str.append(encloseQuote(N_PARAMS));
            str.append(COLON);
            str.append(OPEN_JSON_ARRAY);
            for (int j = 0; j < mBean.params.length; j++) {
                pojoOut = new ByteArrayOutputStream();
                converter.writePOJO(pojoOut, mBean.params[j]);
                str.append(pojoOut.toString());
                str.append(COMMA);
            }
            if (str.charAt(str.length() - 1) == COMMA) {
                str.deleteCharAt(str.length() - 1);
            }
            str.append(CLOSE_JSON_ARRAY);
            str.append(COMMA);

            str.append(encloseQuote(N_SIGNATURE));
            str.append(COLON);
            str.append(parseJSONToString(mBean.signature));
            str.append(COMMA);

            str.append(encloseString(N_USELOADER, mBean.useLoader));
            str.append(COMMA);

            str.append(encloseString(N_USESIGNATURE, mBean.useSignature));
            str.append(CLOSE_JSON);

        } catch (Exception e) {
            fail("Exception encountered while setting up test parameters " + e);
        }

        try {
            converter.writeCreateMBean(out, mBean);
            assertEquals(out.toString(), str.toString());
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readCreateMBean(java.io.InputStream)}.
     */
    @Test
    public void testReadCreateMBean() {
        int signatureSize = 5;
        int paramSize = 5;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CreateMBean mBean = new CreateMBean();
        InputStream in;

        try {
            mBean.className = TEST_CREATE_MBEAN_CLASS_NAME;
            mBean.objectName = new ObjectName(TEST_CREATE_MBEAN_OBJ_NAME,
                            TEST_CREATE_MBEAN_OBJ_KEY,
                            TEST_CREATE_MBEAN_OBJ_VALUE);
            mBean.loaderName = new ObjectName(TEST_CREATE_MBEAN_LOADER_NAME,
                            TEST_CREATE_MBEAN_LOADER_KEY,
                            TEST_CREATE_MBEAN_LOADER_VALUE);

            mBean.signature = new String[signatureSize];

            mBean.params = new Object[paramSize];

            for (int i = 0; i < paramSize; i++) {
                mBean.params[i] = makePOJOObject();
            }

            for (int i = 0; i < signatureSize; i++) {
                mBean.signature[i] = TEST_CREATE_MBEAN_SIGNATURE_STRING;
            }

            mBean.useLoader = TEST_CREATE_MBEAN_USE_LOADER;

            mBean.useSignature = TEST_CREATE_MBEAN_USE_SIGNATURE;

        } catch (Exception e) {
            fail("Exception encountered while setting up test parameters " + e);
        }

        try {
            //setup output stream to get data.
            converter.writeCreateMBean(out, mBean);
            String createMBeanOutputString = out.toString();

            //see if server can read the output stream output.
            in = new ByteArrayInputStream(createMBeanOutputString.getBytes());
            CreateMBean mBeanIn = converter.readCreateMBean(in);

            assertTrue(testObjects(mBeanIn, mBean));
            assertEquals(mBeanIn.className, mBean.className);
            assertEquals(mBeanIn.objectName.toString(),
                         mBean.objectName.toString());
            assertEquals(mBeanIn.loaderName.toString(),
                         mBean.loaderName.toString());

            for (int i = 0; i < signatureSize; i++) {
                assertEquals(mBeanIn.signature[i],
                             mBean.signature[i]);
            }

            for (int i = 0; i < paramSize; i++) {
                assertTrue(testObjects(mBeanIn.params[i],
                                       mBean.params[i]));
            }

            assertEquals(mBeanIn.useLoader, mBean.useLoader);
            assertEquals(mBeanIn.useSignature, mBean.useSignature);

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeMBeanInfo(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.MBeanInfoWrapper)}.
     */
    @Test
    public void testWriteMBeanInfo() {
        try {
            int arraySize = 5;

            MBeanAttributeInfo mBeanAttrInfo = null;

            mBeanAttrInfo = new MBeanAttributeInfo(
                            TEST_MBEAN_ATTR_INFO_NAME,
                            TEST_MBEAN_ATTR_INFO_TYPE,
                            TEST_MBEAN_ATTR_INFO_DESCRIPTION,
                            TEST_MBEAN_ATTR_INFO_READABLE,
                            TEST_MBEAN_ATTR_INFO_WRITABLE,
                            TEST_MBEAN_ATTR_INFO_ISIS,
                            createDescriptor());

            MBeanParameterInfo mBeanParamInfo = new MBeanParameterInfo(
                            TEST_MBEAN_PARAM_INFO_NAME,
                            TEST_MBEAN_PARAM_INFO_TYPE,
                            TEST_MBEAN_PARAM_INFO_DESCRIPTION,
                            createDescriptor());

            MBeanParameterInfo[] mBeanParamInfoArray = new MBeanParameterInfo[arraySize];
            for (int i = 0; i < arraySize; i++) {
                mBeanParamInfoArray[i] = mBeanParamInfo;

            }

            MBeanConstructorInfo mBeanConstInfo = new MBeanConstructorInfo(
                            TEST_MBEAN_CONST_INFO_NAME,
                            TEST_MBEAN_CONST_DESCRIPTION,
                            mBeanParamInfoArray,
                            createDescriptor());

            MBeanOperationInfo mBeanOperationInfo = new MBeanOperationInfo(
                            TEST_MBEAN_OP_INFO_NAME,
                            TEST_MBEAN_OP_INFO_DESCRIPTION,
                            mBeanParamInfoArray,
                            TEST_MBEAN_OP_INFO_TYPE,
                            TEST_MBEAN_OP_INFO_IMPACT,
                            createDescriptor());

            String[] types = new String[TEST_MBEAN_NOTIFICATION_ARRAY_SIZE];

            for (int i = 0; i < TEST_MBEAN_NOTIFICATION_ARRAY_SIZE; i++) {
                types[i] = TEST_MBEAN_NOTIFICATION_TYPE;
            }
            MBeanNotificationInfo mBeanNotificationInfo = new MBeanNotificationInfo(
                            types,
                            TEST_MBEAN_NOTIFICATION_NAME,
                            TEST_MBEAN_NOTIFICATION_DESCRIPTION);

            MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[arraySize];
            MBeanConstructorInfo[] constructors = new MBeanConstructorInfo[arraySize];
            MBeanOperationInfo[] operations = new MBeanOperationInfo[arraySize];
            MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[arraySize];

            for (int i = 0; i < arraySize; i++) {
                attributes[i] = mBeanAttrInfo;
                constructors[i] = mBeanConstInfo;
                operations[i] = mBeanOperationInfo;
                notifications[i] = mBeanNotificationInfo;
            }

            MBeanInfo mBeanInfo = new MBeanInfo(
                            TEST_MBEAN_INFO_CLASS_NAME,
                            TEST_MBEAN_INFO_DESCRIPTION,
                            attributes,
                            constructors,
                            operations,
                            notifications,
                            createDescriptor());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            StringBuilder str = new StringBuilder();

            MBeanInfoWrapper mBean = new MBeanInfoWrapper();
            mBean.attributesURL = TEST_MBEAN_INFO_URL;
            mBean.attributesURL = TEST_MBEAN_ATTR_INFO_URL;
            mBean.attributeURLs = new HashMap<String, String>();
            mBean.attributeURLs.put(TEST_MBEAN_INFO_OP_KEY, TEST_MBEAN_INFO_OP_URL);
            mBean.operationURLs = new HashMap<String, String>();
            mBean.operationURLs.put(TEST_MBEAN_INFO_OP_KEY, TEST_MBEAN_INFO_OP_URL);

            mBean.mbeanInfo = mBeanInfo;

            str.append(OPEN_JSON);
            //Class Name
            str.append(encloseString(N_CLASSNAME, mBean.mbeanInfo.getClassName()));
            str.append(COMMA);

            //Description
            str.append(encloseString(N_DESCRIPTION, mBean.mbeanInfo.getDescription()));
            str.append(COMMA);

            //Descriptor
            str.append(getDescriptorString(mBean.mbeanInfo.getDescriptor()));
            str.append(COMMA);

            //Attributes
            str.append(encloseQuote(N_ATTRIBUTES));
            str.append(COLON);
            str.append(OPEN_JSON_ARRAY);

            for (int i = 0; i < mBean.mbeanInfo.getAttributes().length; i++) {
                str.append(OPEN_JSON);
                str.append(encloseString(N_NAME, mBean.mbeanInfo.getAttributes()[i].getName()));
                str.append(COMMA);
                str.append(encloseString(N_TYPE, mBean.mbeanInfo.getAttributes()[i].getType()));
                str.append(COMMA);
                str.append(encloseString(N_DESCRIPTION, mBean.mbeanInfo.getAttributes()[i].getDescription()));
                str.append(COMMA);
                str.append(getDescriptorString(mBean.mbeanInfo.getAttributes()[i].getDescriptor()));
                str.append(COMMA);
                str.append(encloseString(N_ISIS, mBean.mbeanInfo.getAttributes()[i].isIs()));
                str.append(COMMA);
                str.append(encloseString(N_ISREADABLE, mBean.mbeanInfo.getAttributes()[i].isReadable()));
                str.append(COMMA);
                str.append(encloseString(N_ISWRITABLE, mBean.mbeanInfo.getAttributes()[i].isWritable()));
                str.append(CLOSE_JSON);
                str.append(COMMA);
            }

            str.deleteCharAt(str.length() - 1);
            str.append(CLOSE_JSON_ARRAY);
            str.append(COMMA);

            //Attributes URL
            str.append(encloseString(N_ATTRIBUTES_URL, mBean.attributesURL));
            str.append(COMMA);

            //constructors
            str.append(encloseQuote(N_CONSTRUCTORS));
            str.append(COLON);
            str.append(OPEN_JSON_ARRAY);
            for (int i = 0; i < mBean.mbeanInfo.getConstructors().length; i++) {
                str.append(OPEN_JSON);
                str.append(encloseString(N_NAME, mBean.mbeanInfo.getConstructors()[i].getName()));
                str.append(COMMA);
                str.append(encloseString(N_DESCRIPTION, mBean.mbeanInfo.getConstructors()[i].getDescription()));
                str.append(COMMA);
                str.append(getDescriptorString(mBean.mbeanInfo.getConstructors()[i].getDescriptor()));
                str.append(COMMA);
                str.append(getSignatureString(mBean.mbeanInfo.getConstructors()[i].getSignature()));
                str.append(CLOSE_JSON);
                str.append(COMMA);

            }
            str.deleteCharAt(str.length() - 1);
            str.append(CLOSE_JSON_ARRAY);
            str.append(COMMA);

            //Notifications
            str.append(encloseQuote(N_NOTIFICATIONS));
            str.append(COLON);
            str.append(OPEN_JSON_ARRAY);
            for (int i = 0; i < mBean.mbeanInfo.getNotifications().length; i++) {
                str.append(OPEN_JSON);
                str.append(encloseString(N_NAME, mBean.mbeanInfo.getNotifications()[i].getName()));
                str.append(COMMA);
                str.append(encloseString(N_DESCRIPTION, mBean.mbeanInfo.getNotifications()[i].getDescription()));
                str.append(COMMA);
                str.append(getDescriptorString(mBean.mbeanInfo.getNotifications()[i].getDescriptor()));
                str.append(COMMA);
                //Notification Types
                str.append(encloseQuote(N_NOTIFTYPES));
                str.append(COLON);
                str.append(parseObjectToString(mBean.mbeanInfo.getNotifications()[i].getNotifTypes()));
                str.append(CLOSE_JSON);
                str.append(COMMA);
            }
            str.deleteCharAt(str.length() - 1);
            str.append(CLOSE_JSON_ARRAY);
            str.append(COMMA);

            //Operations
            str.append(encloseQuote(N_OPERATIONS));
            str.append(COLON);
            str.append(OPEN_JSON_ARRAY);
            for (int i = 0; i < mBean.mbeanInfo.getOperations().length; i++) {
                str.append(OPEN_JSON);
                str.append(encloseString(N_NAME, mBean.mbeanInfo.getOperations()[i].getName()));
                str.append(COMMA);
                str.append(encloseString(N_DESCRIPTION, mBean.mbeanInfo.getOperations()[i].getDescription()));
                str.append(COMMA);
                str.append(getDescriptorString(mBean.mbeanInfo.getOperations()[i].getDescriptor()));
                str.append(COMMA);
                str.append(encloseString(N_IMPACT, mBean.mbeanInfo.getOperations()[i].getImpact()));
                str.append(COMMA);
                str.append(encloseString(N_RETURNTYPE, mBean.mbeanInfo.getOperations()[i].getReturnType()));
                str.append(COMMA);
                str.append(getSignatureString(mBean.mbeanInfo.getOperations()[i].getSignature()));
                str.append(COMMA);
                if (mBean.operationURLs.containsKey(mBean.mbeanInfo.getOperations()[i].getName())) {
                    str.append(encloseString(N_URL,
                                             mBean.operationURLs.get(mBean.mbeanInfo.getOperations()[i].getName())));
                }
                str.append(CLOSE_JSON);
                str.append(COMMA);

            }
            str.deleteCharAt(str.length() - 1);
            str.append(CLOSE_JSON_ARRAY);
            str.append(CLOSE_JSON);

            converter.writeMBeanInfo(out, mBean);
            assertEquals(out.toString(), str.toString());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readMBeanInfo(java.io.InputStream)}.
     */
    @Test
    public void testReadMBeanInfo() {
        int arraySize = 5;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;
        MBeanInfoWrapper mBean = null;

        try {

            MBeanAttributeInfo mBeanAttrInfo = null;

            mBeanAttrInfo = new MBeanAttributeInfo(
                            TEST_MBEAN_ATTR_INFO_NAME,
                            TEST_MBEAN_ATTR_INFO_TYPE,
                            TEST_MBEAN_ATTR_INFO_DESCRIPTION,
                            TEST_MBEAN_ATTR_INFO_READABLE,
                            TEST_MBEAN_ATTR_INFO_WRITABLE,
                            TEST_MBEAN_ATTR_INFO_ISIS);

            MBeanParameterInfo mBeanParamInfo = new MBeanParameterInfo(
                            TEST_MBEAN_PARAM_INFO_NAME,
                            TEST_MBEAN_PARAM_INFO_TYPE,
                            TEST_MBEAN_PARAM_INFO_DESCRIPTION);

            MBeanParameterInfo[] mBeanParamInfoArray = new MBeanParameterInfo[arraySize];
            for (int i = 0; i < arraySize; i++) {
                mBeanParamInfoArray[i] = mBeanParamInfo;

            }

            MBeanConstructorInfo mBeanConstInfo = new MBeanConstructorInfo(
                            TEST_MBEAN_CONST_INFO_NAME,
                            TEST_MBEAN_CONST_DESCRIPTION,
                            mBeanParamInfoArray);

            MBeanOperationInfo mBeanOperationInfo = new MBeanOperationInfo(
                            TEST_MBEAN_OP_INFO_NAME,
                            TEST_MBEAN_OP_INFO_DESCRIPTION,
                            mBeanParamInfoArray,
                            TEST_MBEAN_OP_INFO_TYPE,
                            TEST_MBEAN_OP_INFO_IMPACT);

            String[] types = new String[TEST_MBEAN_NOTIFICATION_ARRAY_SIZE];

            for (int i = 0; i < TEST_MBEAN_NOTIFICATION_ARRAY_SIZE; i++) {
                types[i] = TEST_MBEAN_NOTIFICATION_TYPE;
            }
            MBeanNotificationInfo mBeanNotificationInfo = new MBeanNotificationInfo(
                            types,
                            TEST_MBEAN_NOTIFICATION_NAME,
                            TEST_MBEAN_NOTIFICATION_DESCRIPTION);

            MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[arraySize];
            MBeanConstructorInfo[] constructors = new MBeanConstructorInfo[arraySize];
            MBeanOperationInfo[] operations = new MBeanOperationInfo[arraySize];
            MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[arraySize];

            for (int i = 0; i < arraySize; i++) {
                attributes[i] = mBeanAttrInfo;
                constructors[i] = mBeanConstInfo;
                operations[i] = mBeanOperationInfo;
                notifications[i] = mBeanNotificationInfo;
            }

            MBeanInfo mBeanInfo = new MBeanInfo(
                            TEST_MBEAN_INFO_CLASS_NAME,
                            TEST_MBEAN_INFO_DESCRIPTION,
                            attributes,
                            constructors,
                            operations,
                            notifications);

            mBean = new MBeanInfoWrapper();
            mBean.attributesURL = TEST_MBEAN_INFO_URL;
            mBean.attributesURL = TEST_MBEAN_ATTR_INFO_URL;
            mBean.attributeURLs = new HashMap<String, String>();
            mBean.attributeURLs.put(TEST_MBEAN_ATTR_INFO_NAME, TEST_MBEAN_ATTR_INFO_URL);
            mBean.operationURLs = new HashMap<String, String>();
            mBean.operationURLs.put(TEST_MBEAN_INFO_OP_KEY, TEST_MBEAN_INFO_OP_URL);

            mBean.mbeanInfo = mBeanInfo;

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception encoutered " + e);
        }

        try {
            //setup output stream to get data.
            converter.writeMBeanInfo(out, mBean);
            String mBeanOutputString = out.toString();

            //see if server can read the output stream output.
            in = new ByteArrayInputStream(mBeanOutputString.getBytes());
            MBeanInfoWrapper mBeanIn = converter.readMBeanInfo(in);

            //start actual assertions
            //check all fields
            assertTrue(testObjects(mBeanIn, mBean));

            //check strings

            assertEquals(mBeanIn.mbeanInfo.getClassName(),
                         mBean.mbeanInfo.getClassName());

            assertEquals(mBeanIn.mbeanInfo.getDescription(),
                         mBean.mbeanInfo.getDescription());

            //check attributes
            assertTrue(testObjects(mBeanIn.mbeanInfo.getAttributes(),
                                   mBean.mbeanInfo.getAttributes()));

            for (int i = 0; i < arraySize; i++) {
                assertEquals(mBeanIn.mbeanInfo.getAttributes()[i].getDescription(),
                             mBean.mbeanInfo.getAttributes()[i].getDescription());

                assertEquals(mBeanIn.mbeanInfo.getAttributes()[i].getDescription(),
                             mBean.mbeanInfo.getAttributes()[i].getDescription());

                assertEquals(mBeanIn.mbeanInfo.getAttributes()[i].getName(),
                             mBean.mbeanInfo.getAttributes()[i].getName());

                assertEquals(mBeanIn.mbeanInfo.getAttributes()[i].getType(),
                             mBean.mbeanInfo.getAttributes()[i].getType());
            }

            //check constructors
            assertTrue(testObjects(mBeanIn.mbeanInfo.getConstructors(),
                                   mBean.mbeanInfo.getConstructors()));

            for (int i = 0; i < arraySize; i++) {
                assertEquals(mBeanIn.mbeanInfo.getConstructors()[i].getDescription(),
                             mBean.mbeanInfo.getConstructors()[i].getDescription());

                assertEquals(mBeanIn.mbeanInfo.getConstructors()[i].getName(),
                             mBean.mbeanInfo.getConstructors()[i].getName());

                assertTrue(testObjects(mBeanIn.mbeanInfo.getConstructors()[i].getSignature(),
                                       mBean.mbeanInfo.getConstructors()[i].getSignature()));

                for (int j = 0; j < arraySize; j++) {
                    assertEquals(mBeanIn.mbeanInfo.getConstructors()[i].getSignature()[j].getDescription(),
                                 mBean.mbeanInfo.getConstructors()[i].getSignature()[j].getDescription());

                    assertEquals(mBeanIn.mbeanInfo.getConstructors()[i].getSignature()[j].getName(),
                                 mBean.mbeanInfo.getConstructors()[i].getSignature()[j].getName());

                    assertEquals(mBeanIn.mbeanInfo.getConstructors()[i].getSignature()[j].getType(),
                                 mBean.mbeanInfo.getConstructors()[i].getSignature()[j].getType());
                }
            }

            //Notificatons
            assertTrue(testObjects(mBeanIn.mbeanInfo.getNotifications(),
                                   mBean.mbeanInfo.getNotifications()));

            for (int i = 0; i < arraySize; i++) {
                assertEquals(mBeanIn.mbeanInfo.getNotifications()[i].getDescription(),
                             mBean.mbeanInfo.getNotifications()[i].getDescription());

                assertEquals(mBeanIn.mbeanInfo.getNotifications()[i].getName(),
                             mBean.mbeanInfo.getNotifications()[i].getName());

                assertArrayEquals(mBeanIn.mbeanInfo.getNotifications()[i].getNotifTypes(),
                                  mBean.mbeanInfo.getNotifications()[i].getNotifTypes());
            }

            //Operations
            assertTrue(testObjects(mBeanIn.mbeanInfo.getOperations(),
                                   mBean.mbeanInfo.getOperations()));
            for (int i = 0; i < arraySize; i++) {
                assertEquals(mBeanIn.mbeanInfo.getOperations()[i].getDescription(),
                             mBean.mbeanInfo.getOperations()[i].getDescription());

                assertEquals(mBeanIn.mbeanInfo.getOperations()[i].getImpact(),
                             mBean.mbeanInfo.getOperations()[i].getImpact());

                assertEquals(mBeanIn.mbeanInfo.getOperations()[i].getName(),
                             mBean.mbeanInfo.getOperations()[i].getName());

                assertEquals(mBeanIn.mbeanInfo.getOperations()[i].getReturnType(),
                             mBean.mbeanInfo.getOperations()[i].getReturnType());

                assertTrue(testObjects(mBeanIn.mbeanInfo.getOperations()[i].getSignature(),
                                       mBean.mbeanInfo.getOperations()[i].getSignature()));
                for (int j = 0; j < arraySize; j++) {
                    assertEquals(mBeanIn.mbeanInfo.getOperations()[i].getSignature()[j].getDescription(),
                                 mBean.mbeanInfo.getOperations()[i].getSignature()[j].getDescription());

                    assertEquals(mBeanIn.mbeanInfo.getOperations()[i].getSignature()[j].getName(),
                                 mBean.mbeanInfo.getOperations()[i].getSignature()[j].getName());

                    assertEquals(mBeanIn.mbeanInfo.getOperations()[i].getSignature()[j].getType(),
                                 mBean.mbeanInfo.getOperations()[i].getSignature()[j].getType());
                }
            }

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeAttributeList(java.io.OutputStream, javax.management.AttributeList)}.
     */
    @Test
    public void testWriteAttributeList() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream pojoOut;
        StringBuilder str = new StringBuilder();

        //Setup test parameters
        AttributeList attributeList = null;

        try {
            attributeList = new AttributeList();
            Object o = makePOJOObject();
            Object o2 = makePOJOObject();
            Array.set(o2, 0, new Boolean[3]);

            Attribute attribute = new Attribute(TEST_ATTRIBUTE_NAME, o);
            Attribute attribute2 = new Attribute(TEST_ATTRIBUTE_NAME + "2", o2);

            attributeList.add(attribute);
            attributeList.add(attribute2);

            str.append(OPEN_JSON_ARRAY);
            for (Attribute item : attributeList.asList()) {
                str.append(OPEN_JSON);
                str.append(encloseString(N_NAME, item.getName()));
                str.append(COMMA);
                str.append(encloseQuote(N_VALUE));
                str.append(COLON);
                pojoOut = new ByteArrayOutputStream();
                converter.writePOJO(pojoOut, item.getValue());
                str.append(pojoOut.toString());
                str.append(CLOSE_JSON);
                str.append(COMMA);
            }
            str.deleteCharAt(str.length() - 1);
            str.append(CLOSE_JSON_ARRAY);

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }

        try {

            //setup output stream to get data.
            converter.writeAttributeList(out, attributeList);
            assertEquals(out.toString(), str.toString());
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readAttributeList(java.io.InputStream)}.
     */
    @Test
    public void testReadAttributeList() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        AttributeList attributeList = null;

        try {
            attributeList = new AttributeList();
            Object o = makePOJOObject();
            Object o2 = makePOJOObject();
            Array.set(o2, 0, new Boolean[3]);

            Attribute attribute = new Attribute(TEST_ATTRIBUTE_NAME, o);
            Attribute attribute2 = new Attribute(TEST_ATTRIBUTE_NAME, o2);

            attributeList.add(attribute);
            attributeList.add(attribute2);

            //setup output stream to get data.
            converter.writeAttributeList(out, attributeList);
            String attributeListOutputString = out.toString();

            //see if server can read the output stream output.
            in = new ByteArrayInputStream(attributeListOutputString.getBytes());
            AttributeList attributeListIn = converter.readAttributeList(in);

            if (attributeListIn.asList().size() == attributeList.asList().size()) {
                for (int i = 0; i < attributeListIn.size(); i++) {
                    //test name
                    assertEquals(attributeListIn.asList().get(i).getName(),
                                 attributeList.asList().get(i).getName());
                    //test POJO
                    assertTrue(testObjects(attributeListIn.asList().get(i).getValue(),
                                           attributeList.asList().get(i).getValue()));
                }
            } else {
                fail("Attribute list returned the wrong size");
            }

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeInvocation(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.Invocation)}.
     */
    @Test
    public void testWriteInvocation() {
        int arraySize = 5;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream pojoOut;
        StringBuilder str = new StringBuilder();

        //Setup test parameters
        String[] signature = new String[arraySize];
        Object[] params = new Object[arraySize];

        Invocation invocation = new Invocation();

        try {

            for (int i = 0; i < arraySize; i++) {
                signature[i] = TEST_INVOCATION_SIGNATURE;
                params[i] = makePOJOObject();
            }

            invocation.params = params;
            invocation.signature = signature;

            str.append(OPEN_JSON);
            str.append(encloseQuote(N_PARAMS));
            str.append(COLON);
            str.append(OPEN_JSON_ARRAY);
            for (int i = 0; i < invocation.params.length; i++) {
                pojoOut = new ByteArrayOutputStream();
                converter.writePOJO(pojoOut, invocation.params[i]);
                str.append(pojoOut.toString());
                str.append(COMMA);
            }
            str.deleteCharAt(str.length() - 1);
            str.append(CLOSE_JSON_ARRAY);
            str.append(COMMA);
            str.append(encloseQuote(N_SIGNATURE));
            str.append(COLON);
            str.append(parseObjectToString(invocation.signature));
            str.append(CLOSE_JSON);

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }

        try {
            converter.writeInvocation(out, invocation);
            assertEquals(out.toString(), str.toString());
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readInvocation(java.io.InputStream)}.
     */
    @Test
    public void testReadInvocation() {
        int arraySize = 5;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        String[] signature = new String[arraySize];
        Object[] params = new Object[arraySize];

        Invocation invocation = new Invocation();

        try {

            for (int i = 0; i < arraySize; i++) {
                signature[i] = TEST_INVOCATION_SIGNATURE;
                params[i] = makePOJOObject();
            }

            invocation.params = params;
            invocation.signature = signature;

            //setup output stream to get data.
            converter.writeInvocation(out, invocation);
            String invocationOutputString = out.toString();

            //see if server can read the output stream output.
            in = new ByteArrayInputStream(invocationOutputString.getBytes());
            Invocation invocationIn = converter.readInvocation(in);

            assertArrayEquals(invocation.signature, invocation.signature);
            assertTrue(testObjects(invocationIn.params, invocation.params));

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeNotificationArea(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.NotificationArea)}.
     */
    @Test
    public void testWriteNotificationArea() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder str = new StringBuilder();

        NotificationArea notification = new NotificationArea();
        //Setup test parameters
        try {
            notification.inboxURL = TEST_NOTIFICATION_AREA_INBOX;
            notification.registrationsURL = TEST_NOTIFICATION_AREA_REGISTRATION;
            notification.serverRegistrationsURL = TEST_NOTIFICATION_AREA_SERVER_REGISTRATION;

            str.append(OPEN_JSON);
            str.append(encloseString(N_REGISTRATIONS, notification.registrationsURL));
            str.append(COMMA);
            str.append(encloseString(N_SERVERREGISTRATIONS, notification.serverRegistrationsURL));
            str.append(COMMA);
            str.append(encloseString(N_INBOX, notification.inboxURL));
            str.append(CLOSE_JSON);
        } catch (Exception e) {
            fail("Exception encountered while setting up test parameters " + e);
        }

        try {
            converter.writeNotificationArea(out, notification);
            assertEquals(out.toString(), str.toString());
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readNotificationArea(java.io.InputStream)}.
     */
    @Test
    public void testReadNotificationArea() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        NotificationArea notification = new NotificationArea();

        try {
            notification.inboxURL = TEST_NOTIFICATION_AREA_INBOX;
            notification.registrationsURL = TEST_NOTIFICATION_AREA_REGISTRATION;
            notification.serverRegistrationsURL = TEST_NOTIFICATION_AREA_SERVER_REGISTRATION;

            //setup output stream to get data.
            converter.writeNotificationArea(out, notification);
            String notificationAreaOutputString = out.toString();

            //see if server can read the output stream output.
            in = new ByteArrayInputStream(notificationAreaOutputString.getBytes());
            NotificationArea notificationIn = converter.readNotificationArea(in);

            assertEquals(notificationIn.inboxURL, notification.inboxURL);
            assertEquals(notificationIn.registrationsURL, notification.registrationsURL);
            assertEquals(notificationIn.serverRegistrationsURL, notification.serverRegistrationsURL);

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeServerNotificationRegistration(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration)}
     * .
     */
    @Test
    public void testWriteServerNotificationRegistration() {
        ServerNotificationRegistration registration = new ServerNotificationRegistration();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder str = new StringBuilder();

        try {
            registration.objectName = new ObjectName(TEST_SERVER_NOTIF_REGISTRATION_OBJ_NAME,
                            TEST_SERVER_NOTIF_REGISTRATION_OBJ_KEY,
                            TEST_SERVER_NOTIF_REGISTRATION_OBJ_VALUE);

            registration.listener = new ObjectName(TEST_SERVER_NOTIF_REGISTRATION_LISTENER_NAME,
                            TEST_SERVER_NOTIF_REGISTRATION_LISTENER_KEY,
                            TEST_SERVER_NOTIF_REGISTRATION_LISTENER_VALUE);

            registration.filter = new MBeanServerNotificationFilter();

            registration.operation = Operation.Add;

            registration.handback = TEST_SERVER_NOTIF_REGISTRATION_HANDBACK;
            registration.filterID = TEST_SERVER_NOTIF_REGISTRATION_FILTER_ID;
            registration.handbackID = TEST_SERVER_NOTIF_REGISTRATION_HANDBACK_ID;

            str.append(OPEN_JSON);
            str.append(encloseString(N_OPERATION, registration.operation));
            str.append(COMMA);
            str.append(encloseString(N_OBJECTNAME, registration.objectName.getCanonicalName()));
            str.append(COMMA);
            str.append(encloseString(N_LISTENER, registration.listener.getCanonicalName()));
            str.append(COMMA);
            str.append(encloseQuote(N_FILTER));
            str.append(COLON);
            str.append(OPEN_JSON);
            str.append(encloseString(N_CLASSNAME, registration.filter.getClass().getName()));
            str.append(COMMA);
            str.append(encloseQuote(N_ENABLED));
            str.append(COLON);
            str.append(OPEN_JSON_ARRAY);
            for (ObjectName e : ((MBeanServerNotificationFilter) registration.filter).getEnabledObjectNames()) {
                str.append(encloseQuote(N_OBJECTNAME + COLON + e.getCanonicalKeyPropertyListString()));
                str.append(COMMA);
            }
            if (str.charAt(str.length() - 1) == COMMA) {
                str.deleteCharAt(str.length() - 1);
            }

            str.append(CLOSE_JSON_ARRAY);
            str.append(COMMA);
            str.append(encloseQuote(N_TYPES));
            str.append(COLON);
            str.append(OPEN_JSON_ARRAY);
            for (String e : ((MBeanServerNotificationFilter) registration.filter).getEnabledTypes()) {
                str.append(encloseQuote(e));
                str.append(COMMA);
            }
            if (str.charAt(str.length() - 1) == COMMA) {
                str.deleteCharAt(str.length() - 1);
            }
            str.append(CLOSE_JSON_ARRAY);
            str.append(CLOSE_JSON);
            str.append(COMMA);
            OutputStream pojoOut = new ByteArrayOutputStream();
            converter.writePOJO(pojoOut, registration.handback);
            str.append(encloseQuote(N_HANDBACK));
            str.append(COLON);
            str.append(pojoOut.toString());
            str.append(COMMA);
            str.append(encloseString(N_FILTERID, registration.filterID));
            str.append(COMMA);
            str.append(encloseString(N_HANDBACKID, registration.handbackID));

            str.append(CLOSE_JSON);

            converter.writeServerNotificationRegistration(out, registration);

            assertEquals(out.toString(), str.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception encountered " + e);
        }

    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readServerNotificationRegistration(java.io.InputStream)}.
     */
    @Test
    public void testReadServerNotificationRegistration() {
        ServerNotificationRegistration registration = new ServerNotificationRegistration();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        try {
            registration.objectName = new ObjectName(TEST_SERVER_NOTIF_REGISTRATION_OBJ_NAME,
                            TEST_SERVER_NOTIF_REGISTRATION_OBJ_KEY,
                            TEST_SERVER_NOTIF_REGISTRATION_OBJ_VALUE);

            registration.listener = new ObjectName(TEST_SERVER_NOTIF_REGISTRATION_LISTENER_NAME,
                            TEST_SERVER_NOTIF_REGISTRATION_LISTENER_KEY,
                            TEST_SERVER_NOTIF_REGISTRATION_LISTENER_VALUE);

            registration.filter = new MBeanServerNotificationFilter();

            registration.operation = Operation.Add;

            registration.handback = TEST_SERVER_NOTIF_REGISTRATION_HANDBACK;
            registration.filterID = TEST_SERVER_NOTIF_REGISTRATION_FILTER_ID;
            registration.handbackID = TEST_SERVER_NOTIF_REGISTRATION_HANDBACK_ID;

            converter.writeServerNotificationRegistration(out, registration);
            in = new ByteArrayInputStream(out.toByteArray());
            ServerNotificationRegistration registrationIn =
                            converter.readServerNotificationRegistration(in);

            assertEquals(registrationIn.objectName.getCanonicalName(), registration.objectName.getCanonicalName());
            assertEquals(registrationIn.listener.getCanonicalName(), registration.listener.getCanonicalName());
            assertEquals(registrationIn.filter.getClass().getName(), registration.filter.getClass().getName());
            assertEquals(registrationIn.operation, registration.operation);
            assertEquals(registrationIn.handback, registration.handback);
            assertEquals(registrationIn.filterID, registration.filterID);
            assertEquals(registrationIn.handbackID, registration.handbackID);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeCreateNotificationRegistration(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.NotificationRegistration)}
     * .
     */
    @Test
    public void testWriteNotificationRegistration() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder str = new StringBuilder();

        NotificationRegistration registration = new NotificationRegistration();

        ObjectName obj = null;

        try {
            obj = new ObjectName(TEST_CREATE_NOTIFICATION_REGISTRATION_NAME,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_KEY,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_VALUE);

            str.append(OPEN_JSON);
            str.append(encloseString(N_OBJECTNAME, obj.getCanonicalName()));
            str.append(CLOSE_JSON);
        } catch (Exception e) {
            fail("Exception encountered while setting up parameters " + e);
        }

        registration.objectName = obj;

        try {
            converter.writeNotificationRegistration(out, registration);
            assertEquals(out.toString(), str.toString());
        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readCreateNotificationRegistration(java.io.InputStream)}.
     */
    @Test
    public void testReadNotificationRegistration() {
        int arraySize = 5;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        NotificationRegistration registration = new NotificationRegistration();

        ObjectName obj = null;

        try {
            obj = new ObjectName(TEST_CREATE_NOTIFICATION_REGISTRATION_NAME,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_KEY,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_VALUE);
        } catch (Exception e) {
            fail("Exception encountered while setting up parameters " + e);
        }

        registration.objectName = obj;

        NotificationFilter[] mBeanServer = new NotificationFilter[arraySize];
        NotificationFilter[] notifChange = new NotificationFilter[arraySize];
        NotificationFilter[] notifSupport = new NotificationFilter[arraySize];
        for (int i = 0; i < arraySize; i++) {
            mBeanServer[i] = new MBeanServerNotificationFilter();
            notifChange[i] = new AttributeChangeNotificationFilter();
            notifSupport[i] = new NotificationFilterSupport();
        }

        try {
            //setup output stream to get data.
            registration.filters = mBeanServer;
            converter.writeNotificationRegistration(out, registration);
            String registrationOutString = out.toString();

            //see if server can read the output stream output.
            //MBeanServerNotificationFilter
            in = new ByteArrayInputStream(registrationOutString.getBytes());
            NotificationRegistration registrationIn = converter.readNotificationRegistration(in);

            assertEquals(registrationIn.objectName.toString(), registration.objectName.toString());
            assertEquals(registrationIn.objectName.getCanonicalKeyPropertyListString(), registration.objectName.getCanonicalKeyPropertyListString());
            assertEquals(registrationIn.objectName.getCanonicalName(), registration.objectName.getCanonicalName());

            for (int i = 0; i < arraySize; i++) {
                assertEquals(registrationIn.getClass(), registration.getClass());
            }

            //AttributeChangeNotificatonFilter
            registration.filters = notifChange;
            out = new ByteArrayOutputStream();
            converter.writeNotificationRegistration(out, registration);

            in = new ByteArrayInputStream(out.toByteArray());
            registrationIn = converter.readNotificationRegistration(in);

            assertEquals(registrationIn.objectName.toString(), registration.objectName.toString());
            assertEquals(registrationIn.objectName.getCanonicalKeyPropertyListString(), registration.objectName.getCanonicalKeyPropertyListString());
            assertEquals(registrationIn.objectName.getCanonicalName(), registration.objectName.getCanonicalName());

            for (int i = 0; i < arraySize; i++) {
                assertEquals(registrationIn.getClass(), registration.getClass());
            }

            //NotificationFilterSupport
            registration.filters = notifSupport;
            out = new ByteArrayOutputStream();
            converter.writeNotificationRegistration(out, registration);

            in = new ByteArrayInputStream(out.toByteArray());
            registrationIn = converter.readNotificationRegistration(in);

            assertEquals(registrationIn.objectName.toString(), registration.objectName.toString());
            assertEquals(registrationIn.objectName.getCanonicalKeyPropertyListString(), registration.objectName.getCanonicalKeyPropertyListString());
            assertEquals(registrationIn.objectName.getCanonicalName(), registration.objectName.getCanonicalName());

            for (int i = 0; i < arraySize; i++) {
                assertEquals(registrationIn.getClass(), registration.getClass());
            }

        } catch (Exception e) {

            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#isSupportedNotificationFilter(javax.management.NotificationFilter)}.
     */
    @Test
    public void testIsSupportedNotificationFilter() {
        assertTrue(converter.isSupportedNotificationFilter(new AttributeChangeNotificationFilter()));
        assertTrue(converter.isSupportedNotificationFilter(new MBeanServerNotificationFilter()));
        assertTrue(converter.isSupportedNotificationFilter(new NotificationFilterSupport()));
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeNotificationFilters(java.io.OutputStream, javax.management.NotificationFilter[])}.
     */
    @Test
    public void testWriteNotificationFilters() {
        int arraySize = 5;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder str = new StringBuilder();
        ObjectName objectName = null;

        try {
            objectName = new ObjectName(TEST_NOTIFICATION_OBJ_NAME,
                            TEST_NOTIFICATION_OBJECT_KEY,
                            TEST_NOTIFICATION_OBJECT_VALUE);
        } catch (Exception e) {
            fail("ObjectName setup error");
        }

        //Setup test parameters
        NotificationFilter[] mBeanServer = new NotificationFilter[arraySize];
        NotificationFilter[] notifChange = new NotificationFilter[arraySize];
        NotificationFilter[] notifSupport = new NotificationFilter[arraySize];
        for (int i = 0; i < arraySize; i++) {
            mBeanServer[i] = new MBeanServerNotificationFilter();
            ((MBeanServerNotificationFilter) mBeanServer[i]).enableObjectName(objectName);

            notifChange[i] = new AttributeChangeNotificationFilter();
            ((AttributeChangeNotificationFilter) notifChange[i]).
                            enableAttribute(TEST_NOTIFICATION_ATTRIBUTE_NAME);
            notifSupport[i] = new NotificationFilterSupport();
            ((NotificationFilterSupport) notifSupport[i]).enableType(TEST_NOTIFICATION_SUPPORT_TYPE);
        }

        try {
            //setup output stream to get data.

            //MBeanServerNotificationFilter
            str.append(OPEN_JSON_ARRAY);
            for (int i = 0; i < mBeanServer.length; i++) {
                str.append(OPEN_JSON);
                str.append(encloseString(N_CLASSNAME, mBeanServer[i].getClass().getName()));
                str.append(COMMA);
                str.append(encloseQuote(N_ENABLED));
                str.append(COLON);
                str.append(OPEN_JSON_ARRAY);
                for (ObjectName e : ((MBeanServerNotificationFilter) mBeanServer[i]).getEnabledObjectNames()) {
                    str.append(encloseQuote(N_OBJECTNAME + COLON + e.getCanonicalKeyPropertyListString()));
                    str.append(COMMA);
                }
                if (str.charAt(str.length() - 1) == COMMA) {
                    str.deleteCharAt(str.length() - 1);
                }

                str.append(CLOSE_JSON_ARRAY);
                str.append(COMMA);
                str.append(encloseQuote(N_TYPES));
                str.append(COLON);
                str.append(OPEN_JSON_ARRAY);
                for (String e : ((MBeanServerNotificationFilter) mBeanServer[i]).getEnabledTypes()) {
                    str.append(encloseQuote(e));
                    str.append(COMMA);
                }
                if (str.charAt(str.length() - 1) == COMMA) {
                    str.deleteCharAt(str.length() - 1);
                }
                str.append(CLOSE_JSON_ARRAY);
                str.append(CLOSE_JSON);
                str.append(COMMA);
            }
            if (str.charAt(str.length() - 1) == COMMA) {
                str.deleteCharAt(str.length() - 1);
            }
            str.append(CLOSE_JSON_ARRAY);

            converter.writeNotificationFilters(out, mBeanServer);
            assertEquals(out.toString(), str.toString());

            //AttributeChangeNotificatonFilter

            out = new ByteArrayOutputStream();
            str = new StringBuilder();

            str.append(OPEN_JSON_ARRAY);
            for (int i = 0; i < mBeanServer.length; i++) {
                str.append(OPEN_JSON);
                str.append(encloseString(N_CLASSNAME, notifChange[i].getClass().getName()));
                str.append(COMMA);
                str.append(encloseQuote(N_ENABLED));
                str.append(COLON);
                str.append(OPEN_JSON_ARRAY);
                for (String e : ((AttributeChangeNotificationFilter) notifChange[i]).getEnabledAttributes()) {
                    str.append(encloseQuote(e));
                    str.append(COMMA);
                }
                if (str.charAt(str.length() - 1) == COMMA) {
                    str.deleteCharAt(str.length() - 1);
                }

                str.append(CLOSE_JSON_ARRAY);

                str.append(CLOSE_JSON);
                str.append(COMMA);
            }
            if (str.charAt(str.length() - 1) == COMMA) {
                str.deleteCharAt(str.length() - 1);
            }
            str.append(CLOSE_JSON_ARRAY);

            converter.writeNotificationFilters(out, notifChange);
            assertEquals(out.toString(), str.toString());

            //NotificationFilterSupport

            out = new ByteArrayOutputStream();
            str = new StringBuilder();

            str.append(OPEN_JSON_ARRAY);
            for (int i = 0; i < mBeanServer.length; i++) {
                str.append(OPEN_JSON);
                str.append(encloseString(N_CLASSNAME, notifSupport[i].getClass().getName()));
                str.append(COMMA);
                str.append(encloseQuote(N_TYPES));
                str.append(COLON);
                str.append(OPEN_JSON_ARRAY);
                for (String e : ((NotificationFilterSupport) notifSupport[i]).getEnabledTypes()) {
                    str.append(encloseQuote(e));
                    str.append(COMMA);
                }
                if (str.charAt(str.length() - 1) == COMMA) {
                    str.deleteCharAt(str.length() - 1);
                }
                str.append(CLOSE_JSON_ARRAY);
                str.append(CLOSE_JSON);
                str.append(COMMA);
            }
            if (str.charAt(str.length() - 1) == COMMA) {
                str.deleteCharAt(str.length() - 1);
            }
            str.append(CLOSE_JSON_ARRAY);

            converter.writeNotificationFilters(out, notifSupport);
            assertEquals(out.toString(), str.toString());

            //null case
            out = new ByteArrayOutputStream();
            str = new StringBuilder();

            converter.writeNotificationFilters(out, null);
            str.append(OPEN_JSON_ARRAY);
            str.append(CLOSE_JSON_ARRAY);
            assertEquals(out.toString(), str.toString());

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readNotificationFilters(java.io.InputStream)}.
     */
    @Test
    public void testReadNotificationFilters() {

        int arraySize = 5;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        ObjectName objectName = null;

        try {
            objectName = new ObjectName(TEST_NOTIFICATION_OBJ_NAME,
                            TEST_NOTIFICATION_OBJECT_KEY,
                            TEST_NOTIFICATION_OBJECT_VALUE);
        } catch (Exception e) {
            fail("ObjectName setup error");
        }

        NotificationFilter[] mBeanServer = new NotificationFilter[arraySize];
        NotificationFilter[] notifChange = new NotificationFilter[arraySize];
        NotificationFilter[] notifSupport = new NotificationFilter[arraySize];
        for (int i = 0; i < arraySize; i++) {
            mBeanServer[i] = new MBeanServerNotificationFilter();
            ((MBeanServerNotificationFilter) mBeanServer[i]).enableObjectName(objectName);

            notifChange[i] = new AttributeChangeNotificationFilter();
            ((AttributeChangeNotificationFilter) notifChange[i]).
                            enableAttribute(TEST_NOTIFICATION_ATTRIBUTE_NAME);
            notifSupport[i] = new NotificationFilterSupport();
            ((NotificationFilterSupport) notifSupport[i]).enableType(TEST_NOTIFICATION_SUPPORT_TYPE);
        }
        try {
            //setup output stream to get data.

            converter.writeNotificationFilters(out, mBeanServer);

            //see if server can read the output stream output.
            //MBeanServerNotificationFilter
            in = new ByteArrayInputStream(out.toByteArray());
            NotificationFilter[] mBeanServerIn = converter.readNotificationFilters(in);

            for (int i = 0; i < arraySize; i++) {
                assertEquals(mBeanServerIn[i].getClass(), mBeanServer[i].getClass());
                for (ObjectName e : ((MBeanServerNotificationFilter) mBeanServerIn[i]).getEnabledObjectNames()) {
                    assertEquals(e.getCanonicalKeyPropertyListString(), objectName.getCanonicalKeyPropertyListString());
                }
            }

            //AttributeChangeNotificatonFilter

            out = new ByteArrayOutputStream();
            converter.writeNotificationFilters(out, notifChange);

            in = new ByteArrayInputStream(out.toByteArray());
            NotificationFilter[] notifChangeIn = converter.readNotificationFilters(in);

            for (int i = 0; i < arraySize; i++) {
                assertEquals(notifChangeIn[i].getClass(), notifChange[i].getClass());
                for (String e : ((AttributeChangeNotificationFilter) notifChangeIn[i]).getEnabledAttributes()) {
                    assertEquals(e, TEST_NOTIFICATION_ATTRIBUTE_NAME);
                }
            }

            //NotificationFilterSupport

            out = new ByteArrayOutputStream();
            converter.writeNotificationFilters(out, notifSupport);

            in = new ByteArrayInputStream(out.toByteArray());
            NotificationFilter[] notifSupportIn = converter.readNotificationFilters(in);

            for (int i = 0; i < arraySize; i++) {
                assertEquals(notifSupportIn[i].getClass(), notifSupport[i].getClass());
                for (String e : ((NotificationFilterSupport) notifSupportIn[i]).getEnabledTypes()) {
                    assertEquals(e, TEST_NOTIFICATION_SUPPORT_TYPE);
                }
            }

        } catch (Exception e) {

            fail("Exception encountered " + e);
        }

    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeNotifications(java.io.OutputStream, javax.management.Notification)}.
     */
    @Test
    public void testWriteNotifications() {

        int arraySize = 5;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream pojoOut = new ByteArrayOutputStream();
        StringBuilder str = new StringBuilder();

        //Setup test parameters
        Notification[] notification = new Notification[arraySize];
        ObjectName obj;

        try {
            obj = new ObjectName(TEST_CREATE_NOTIFICATION_REGISTRATION_NAME,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_KEY,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_VALUE);

            for (int i = 0; i < arraySize; i++) {
                notification[i] = new Notification(
                                TEST_NOTIFICATION_TYPE + i,
                                obj,
                                TEST_NOTIFICATION_SEQUENCE,
                                TEST_NOTIFICATION_TIME_STAMP,
                                TEST_NOTIFICATION_MESSAGE);

                notification[i].setUserData(makePOJOObject());
            }

            str.append(OPEN_JSON_ARRAY);
            for (int i = 0; i < arraySize; i++) {
                str.append(OPEN_JSON);
                str.append(encloseString(N_CLASSNAME, notification[i].getClass().getName()));
                str.append(COMMA);
                str.append(encloseString(N_TYPE, notification[i].getType()));
                str.append(COMMA);
                str.append(encloseString(N_SOURCE, notification[i].getSource()));
                str.append(COMMA);
                str.append(encloseString(N_SEQUENCENUMBER, notification[i].getSequenceNumber()));
                str.append(COMMA);
                str.append(encloseString(N_TIMESTAMP, notification[i].getTimeStamp()));
                str.append(COMMA);
                str.append(encloseString(N_MESSAGE, notification[i].getMessage()));
                str.append(COMMA);
                pojoOut = new ByteArrayOutputStream();
                converter.writePOJO(pojoOut, notification[i].getUserData());
                str.append(encloseQuote(N_USERDATA));
                str.append(COLON);
                str.append(pojoOut.toString());
                str.append(CLOSE_JSON);
                str.append(COMMA);

            }
            if (str.charAt(str.length() - 1) == COMMA) {
                str.deleteCharAt(str.length() - 1);
            }
            str.append(CLOSE_JSON_ARRAY);

            converter.writeNotifications(out, notification);
            assertEquals(out.toString(), str.toString());

        } catch (Exception e) {
            fail("Exception encountered  " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeNotificationRecords(java.io.OutputStream, NotificationRecord[])}.
     */
    @Test
    public void testWriteNotificationRecords() {

        int arraySize = 5;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream pojoOut = new ByteArrayOutputStream();
        StringBuilder str = new StringBuilder();

        //Setup test parameters
        NotificationRecord[] records = new NotificationRecord[arraySize];
        ObjectName obj;

        try {
            obj = new ObjectName(TEST_CREATE_NOTIFICATION_REGISTRATION_NAME,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_KEY,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_VALUE);

            for (int i = 0; i < arraySize; i++) {
                Notification n = new Notification(
                                TEST_NOTIFICATION_TYPE + i,
                                obj,
                                TEST_NOTIFICATION_SEQUENCE,
                                TEST_NOTIFICATION_TIME_STAMP,
                                TEST_NOTIFICATION_MESSAGE);

                n.setUserData(makePOJOObject());
                records[i] = new NotificationRecord(n, obj,
                                TEST_NOTIFICATION_RECORD_HOST_NAME,
                                TEST_NOTIFICATION_RECORD_SERVER_NAME + i,
                                TEST_NOTIFICATION_RECORD_SERVER_USER_DIR);
            }

            str.append(OPEN_JSON_ARRAY);
            for (int i = 0; i < arraySize; i++) {
                Notification n = records[i].getNotification();
                Map<String, Object> map = records[i].getNotificationTargetInformation().getRoutingInformation();

                str.append(OPEN_JSON);
                str.append(encloseString(N_CLASSNAME, n.getClass().getName()));
                str.append(COMMA);
                str.append(encloseString(N_TYPE, n.getType()));
                str.append(COMMA);
                str.append(encloseString(N_SOURCE, n.getSource()));
                str.append(COMMA);
                str.append(encloseString(N_SEQUENCENUMBER, n.getSequenceNumber()));
                str.append(COMMA);
                str.append(encloseString(N_TIMESTAMP, n.getTimeStamp()));
                str.append(COMMA);
                str.append(encloseString(N_MESSAGE, n.getMessage()));
                str.append(COMMA);
                pojoOut = new ByteArrayOutputStream();
                converter.writePOJO(pojoOut, n.getUserData());
                str.append(encloseQuote(N_USERDATA));
                str.append(COLON);
                str.append(pojoOut.toString());
                str.append(COMMA);
                str.append(encloseString(N_HOSTNAME, map.get(ClientProvider.ROUTING_KEY_HOST_NAME)));
                str.append(COMMA);
                str.append(encloseString(N_SERVERNAME, map.get(ClientProvider.ROUTING_KEY_SERVER_NAME)));
                str.append(COMMA);
                str.append(encloseString(N_SERVERUSERDIR, map.get(ClientProvider.ROUTING_KEY_SERVER_USER_DIR)));
                str.append(CLOSE_JSON);
                str.append(COMMA);

            }
            if (str.charAt(str.length() - 1) == COMMA) {
                str.deleteCharAt(str.length() - 1);
            }
            str.append(CLOSE_JSON_ARRAY);

            converter.writeNotificationRecords(out, records);
            assertEquals(out.toString(), str.toString());

        } catch (Exception e) {
            fail("Exception encountered  " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readNotifications(java.io.InputStream)}.
     */
    @Test
    public void testReadNotifications() {
        int arraySize = 5;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        Notification[] notification = new Notification[arraySize];
        ObjectName obj;

        try {
            obj = new ObjectName(TEST_CREATE_NOTIFICATION_REGISTRATION_NAME,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_KEY,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_VALUE);

            for (int i = 0; i < arraySize; i++) {
                notification[i] = new Notification(
                                TEST_NOTIFICATION_TYPE,
                                obj,
                                TEST_NOTIFICATION_SEQUENCE,
                                TEST_NOTIFICATION_TIME_STAMP,
                                TEST_NOTIFICATION_MESSAGE);

                notification[i].setUserData(makePOJOObject());

            }
        } catch (Exception e) {

            fail("Exception encountered while setting up test parameters " + e);
        }

        try {
            //setup output stream to get data.
            converter.writeNotifications(out, notification);
            String notificationOutputString = out.toString();

            //see if server can read the output stream output.
            in = new ByteArrayInputStream(notificationOutputString.getBytes());
            Notification[] notificationIn = converter.readNotifications(in);

            for (int i = 0; i < arraySize; i++) {
                assertEquals(notificationIn[i].getType(), notification[i].getType());
                assertEquals(notificationIn[i].getTimeStamp(), notification[i].getTimeStamp());
                assertEquals(notificationIn[i].getSequenceNumber(), notification[i].getSequenceNumber());
                assertEquals(notificationIn[i].getMessage(), notification[i].getMessage());
                assertTrue(testObjects(notificationIn[i].getUserData(), notification[i].getUserData()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readNotificationRecords(java.io.InputStream)}.
     */
    @Test
    public void testReadNotificationRecords() {
        int arraySize = 5;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        //Setup test parameters
        NotificationRecord[] records = new NotificationRecord[arraySize];
        ObjectName obj;

        try {
            obj = new ObjectName(TEST_CREATE_NOTIFICATION_REGISTRATION_NAME,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_KEY,
                            TEST_CREATE_NOTIFICATION_REGISTRATION_VALUE);

            for (int i = 0; i < arraySize; i++) {
                Notification n = new Notification(
                                TEST_NOTIFICATION_TYPE,
                                obj,
                                TEST_NOTIFICATION_SEQUENCE,
                                TEST_NOTIFICATION_TIME_STAMP,
                                TEST_NOTIFICATION_MESSAGE);

                n.setUserData(makePOJOObject());
                records[i] = new NotificationRecord(n, obj,
                                TEST_NOTIFICATION_RECORD_HOST_NAME,
                                TEST_NOTIFICATION_RECORD_SERVER_NAME,
                                TEST_NOTIFICATION_RECORD_SERVER_USER_DIR);
            }
        } catch (Exception e) {

            fail("Exception encountered while setting up test parameters " + e);
        }

        try {
            //setup output stream to get data.
            converter.writeNotificationRecords(out, records);
            String notificationOutputString = out.toString();

            //see if server can read the output stream output.
            in = new ByteArrayInputStream(notificationOutputString.getBytes());
            NotificationRecord[] recordsIn = converter.readNotificationRecords(in);

            for (int i = 0; i < arraySize; i++) {
                Notification notificationIn = recordsIn[i].getNotification();
                Map<String, Object> routingInfoIn = recordsIn[i].getNotificationTargetInformation().getRoutingInformation();

                Notification notification = records[i].getNotification();
                Map<String, Object> routingInfo = records[i].getNotificationTargetInformation().getRoutingInformation();

                assertEquals(notificationIn.getType(), notification.getType());
                assertEquals(notificationIn.getTimeStamp(), notification.getTimeStamp());
                assertEquals(notificationIn.getSequenceNumber(), notification.getSequenceNumber());
                assertEquals(notificationIn.getMessage(), notification.getMessage());
                assertTrue(testObjects(notificationIn.getUserData(), notification.getUserData()));

                assertEquals(routingInfoIn.get(ClientProvider.ROUTING_KEY_HOST_NAME), routingInfo.get(ClientProvider.ROUTING_KEY_HOST_NAME));
                assertEquals(routingInfoIn.get(ClientProvider.ROUTING_KEY_SERVER_NAME), routingInfo.get(ClientProvider.ROUTING_KEY_SERVER_NAME));
                assertEquals(routingInfoIn.get(ClientProvider.ROUTING_KEY_SERVER_USER_DIR), routingInfo.get(ClientProvider.ROUTING_KEY_SERVER_USER_DIR));
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeNotificationSettings(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.NotificationSettings)}.
     */
    @Test
    public void testWriteNotificationSettings() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NotificationSettings settings = new NotificationSettings();
        StringBuilder str = new StringBuilder();
        settings.deliveryInterval = TEST_NOTIFICATION_SETTING_DELIVERY_INTERVAL;
        settings.inboxExpiry = TEST_NOTIFICATION_SETTING_INBOX_EXPIRY;

        str.append(OPEN_JSON);
        str.append(encloseString(N_DELIVERYINTERVAL, settings.deliveryInterval));
        str.append(COMMA);
        str.append(encloseString(N_INBOXEXPIRY, settings.inboxExpiry));
        str.append(CLOSE_JSON);

        try {
            converter.writeNotificationSettings(out, settings);
            assertEquals(out.toString(), str.toString());
        } catch (Exception e) {
            fail("Exception encountered " + e);
        }

    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readNotificationSettings(java.io.InputStream)}.
     */
    @Test
    public void testReadNotificationSettings() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NotificationSettings settings = new NotificationSettings();
        InputStream in;

        settings.deliveryInterval = TEST_NOTIFICATION_SETTING_DELIVERY_INTERVAL;
        settings.inboxExpiry = TEST_NOTIFICATION_SETTING_INBOX_EXPIRY;

        try {
            converter.writeNotificationSettings(out, settings);
            in = new ByteArrayInputStream(out.toByteArray());
            NotificationSettings settingsIn = converter.readNotificationSettings(in);

            assertEquals(settingsIn.deliveryInterval, settings.deliveryInterval);
            assertEquals(settingsIn.inboxExpiry, settingsIn.inboxExpiry);

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeThrowable(java.io.OutputStream, java.lang.Throwable)}.
     */
    @Test
    public void testWriteThrowable() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream tempOut = new ByteArrayOutputStream();
        StringBuilder regex = new StringBuilder();
        StringWriter stackTrace = new StringWriter();

        Throwable throwable = new Throwable(TEST_THROWABLE_MESSAGE);
        //Setup test parameters
        try {
            regex.append(ESCAPE);
            regex.append(OPEN_JSON);
            regex.append(encloseString(N_THROWABLE, ".+"));
            regex.append(COMMA);
            regex.append(encloseString(N_EXCEPTION_MESSAGE, ".+"));
            regex.append(ESCAPE);
            regex.append(CLOSE_JSON);
        } catch (Exception e) {
            fail("Exception encountered while setting up test parameters " + e);
        }

        //check message
        try {
            converter.writeThrowable(out, throwable);

            assertTrue(Pattern.matches(regex.toString(), out.toString()));

            JSONArtifact art = JSON.parse(out.toString());
            JSONObject obj = (JSONObject) art;

            assertEquals(obj.get(N_EXCEPTION_MESSAGE), TEST_THROWABLE_MESSAGE);

        } catch (Exception e) {
            fail("Exception encoutered " + e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readThrowable(java.io.InputStream)}.
     */
    @Test
    public void testReadThrowable() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;

        Throwable throwable = new Throwable(TEST_THROWABLE_MESSAGE);

        try {
            //setup output stream to get data.
            converter.writeThrowable(out, throwable);
            String throwableOutputString = out.toString();

            //see if server can read the output stream output.
            in = new ByteArrayInputStream(throwableOutputString.getBytes());
            Throwable throwableIn = converter.readThrowable(in);

            assertEquals(throwableIn.getMessage(), throwable.getMessage());
            assertArrayEquals(throwableIn.getStackTrace(), throwable.getStackTrace());

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }
    }

    /**
     * Test method for serializing to de-serializing filters
     * {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeNotificationRegistration(java.io.OutputStream, com.ibm.ws.jmx.connector.datatypes.NotificationRegistration)}.
     * {@link com.ibm.ws.jmx.connector.converter.JSONConverter#readNotificationRegistration(java.io.InputStream)}.
     */
    @Test
    public void testWriteReadNotificationFilters() {
        //Serializing the Filter
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder expectedStr = new StringBuilder();

        MBeanServerNotificationFilter myFilter = new MBeanServerNotificationFilter();
        myFilter.enableAllObjectNames();

        NotificationRegistration registration = new NotificationRegistration();
        registration.filters = new NotificationFilter[] { myFilter };

        try {
            JSONConverter conv = JSONConverter.getConverter();
            conv.writeNotificationRegistration(baos, registration);

            String json = new String(baos.toByteArray());

            expectedStr.append(OPEN_JSON);
            expectedStr.append(encloseQuote(N_FILTERS));
            expectedStr.append(COLON);
            expectedStr.append(OPEN_JSON_ARRAY);
            expectedStr.append(OPEN_JSON);
            expectedStr.append(encloseString(N_CLASSNAME, myFilter.getClass().getName()));
            expectedStr.append(COMMA);
            expectedStr.append(encloseQuote(N_DISABLED));
            expectedStr.append(COLON);

            expectedStr.append(myFilter.getDisabledObjectNames());
            expectedStr.append(COMMA);
            expectedStr.append(encloseQuote(N_TYPES));
            expectedStr.append(COLON);
            expectedStr.append(OPEN_JSON_ARRAY);

            for (String e : myFilter.getEnabledTypes()) {
                expectedStr.append(encloseQuote(e));
                expectedStr.append(COMMA);
            }
            if (expectedStr.charAt(expectedStr.length() - 1) == COMMA) {
                expectedStr.deleteCharAt(expectedStr.length() - 1);
            }
            expectedStr.append(CLOSE_JSON_ARRAY);
            expectedStr.append(CLOSE_JSON);
            expectedStr.append(CLOSE_JSON_ARRAY);
            expectedStr.append(CLOSE_JSON);

            assertEquals(expectedStr.toString(), json.toString());

            ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
            MBeanServerNotification notif = new MBeanServerNotification(MBeanServerNotification.REGISTRATION_NOTIFICATION,
                            new ObjectName("team.server.monitoring.jts_9443:type=Monitoring"), 10,
                            new ObjectName("team.server.monitoring.jts_9443:type=Problem Activity,activityId=someId"));

            //De-serializing the Filter
            NotificationRegistration reg = conv.readNotificationRegistration(bais);

            for (NotificationFilter filter : reg.filters) {
                assertTrue(filter.isNotificationEnabled(notif));
                for (NotificationFilter expectedFilter : registration.filters) {
                    assertEquals(filter.getClass().getName(), expectedFilter.getClass().getName());
                    assertTrue(expectedFilter.isNotificationEnabled(notif));
                }
            }

        } catch (Exception e) {
            fail("Exception encountered " + e);
        }

    }

    //End of tests

    /**
     * encloses a single string into a JSON array format
     * 
     * @param data
     * @return
     */
    private static String encloseJSON(Object data) {
        if (data == null)
            return OPEN_JSON_ARRAY + "null" + CLOSE_JSON_ARRAY;
        if (data instanceof Boolean)
            return OPEN_JSON_ARRAY + data.toString() + CLOSE_JSON_ARRAY;

        return OPEN_JSON_ARRAY + "\"" + data + "\"" + CLOSE_JSON_ARRAY;
    }

    /**
     * Encloses a string array in a JSON array format
     * 
     * @param str
     * @return
     */
    private static String parseJSONToString(String[] str) {
        JSONArray json = new JSONArray();
        for (int i = 0; i < str.length; i++) {
            json.add(str[i]);
        }
        return parseJSONToString(json);
    }

    /**
     * Encloses a JSONArray into a JSON array format
     * 
     * @param json
     * @return
     */
    private static String parseJSONToString(JSONArray json) {
        StringBuilder str = new StringBuilder();
        str.append('[');
        if (json.size() > 1) {
            for (int i = 0; i < json.size() - 1; i++) {
                str.append('"');
                str.append(json.get(i));
                str.append("\",");
            }
        }
        if (json.size() > 0) {
            str.append("\"");
            str.append(json.get(json.size() - 1));
            str.append("\"");
        }
        str.append("]");
        return str.toString();
    }

    /**
     * Parses an object array to a JSON format
     * 
     * @param obj
     * @return
     */
    private static String parseObjectToString(Object[] obj) {
        if (obj.length == 0) {
            return "[]";
        }
        StringBuilder str = new StringBuilder();
        str.append('[');
        for (int i = 0; i < obj.length; i++) {
            if (obj[i] == null) {
                str.append("null");
                str.append(COMMA);
            } else {
                str.append('"');
                str.append(obj[i]);
                str.append("\",");
            }
        }
        str.deleteCharAt(str.length() - 1);
        str.append(']');
        return str.toString();

    }

    /**
     * Parses object array obj's types and returns into a JSON format
     * 
     * @param obj
     * @return
     */
    private static String parseObjectTypeToString(Object[] obj) {
        StringBuilder str = new StringBuilder();
        str.append('[');
        for (int i = 0; i < obj.length; i++) {
            if (obj[i] == null) {
                str.append("null");
                str.append(COMMA);
            } else {
                str.append('"');
                str.append(obj[i].getClass().getName());
                str.append("\",");
            }
        }
        str.deleteCharAt(str.length() - 1);
        str.append(']');
        return str.toString();

    }

    /**
     * Encloses value in quotaton marks. Will not enclose "null" in
     * quotation marks.
     * 
     * @param value
     * @return
     */
    private static String encloseQuote(String value) {
        if (value == null) {
            return "null";
        }

        return "\"" + value + "\"";
    }

    /**
     * Encloses key and value in the format "key":"value" for the common case
     * when value is null, method will return "key":null
     * when value is a boolean, method will return "key":true or false depending
     * on the boolean
     * 
     * @param key
     * @param value
     * @return
     */
    private static String encloseString(String key, Object value) {
        StringBuilder str = new StringBuilder();
        str.append('"');
        str.append(key);
        str.append("\":");

        if (value == null) {
            str.append("null");
            return str.toString();
        }

        if (value instanceof Boolean) {
            str.append(value.toString());
            return str.toString();
        }
        str.append('"');
        str.append(value.toString());
        str.append('"');
        return str.toString();
    }

    /**
     * Converts a map into a JSON Format:
     * {
     * (Key:value)*
     * }
     * 
     * @param map
     * @return
     */
    private static String mapToJSON(Map<Object, Object> map) {
        StringBuilder str = new StringBuilder();
        str.append(OPEN_JSON);
        for (Object key : map.keySet()) {

            str.append(encloseString(key.toString(), map.get(key).toString()));

            str.append(COMMA);
        }
        str.deleteCharAt(str.length() - 1);
        str.append(CLOSE_JSON);

        return str.toString();
    }

    /**
     * Creates a descriptor object.
     * 
     * @return
     */
    private static Descriptor createDescriptor() {
        //Change fieldNames/fieldValues to add more or less objects to the descriptor to test
        String[] fieldNames = { "field 1", "field 2", "field 3" };
        Object[] fieldValues = { 1, "value 2", '3' };
        return new DescriptorSupport(fieldNames, fieldValues);
    }

    /**
     * Converts a Descriptor object to the expected JSON string
     * 
     * @param descriptor
     * @return
     */
    private static String getDescriptorString(Descriptor descriptor) {
        StringBuilder str = new StringBuilder();

        //Descriptor
        str.append(encloseQuote(N_DESCRIPTOR));
        str.append(COLON);
        str.append(OPEN_JSON);
        //Descriptor.names
        str.append(encloseQuote(N_NAMES));
        str.append(COLON);
        str.append(parseObjectToString(descriptor.getFieldNames()));
        str.append(COMMA);
        //Descriptor.values
        str.append(encloseQuote(N_VALUES));
        str.append(COLON);
        str.append(OPEN_JSON_ARRAY);
        for (int i = 0; i < descriptor.getFieldNames().length; i++) {
            str.append(OPEN_JSON);
            str.append(encloseString(N_VALUE, descriptor.getFieldValue(descriptor.getFieldNames()[i]).toString()));
            str.append(COMMA);
            str.append(encloseString(N_TYPE, descriptor.getFieldValue(descriptor.getFieldNames()[i]).getClass().getName()));
            str.append(CLOSE_JSON);
            str.append(COMMA);
        }
        if (descriptor.getFieldNames().length != 0)
            str.deleteCharAt(str.length() - 1);
        str.append(CLOSE_JSON_ARRAY);
        str.append(CLOSE_JSON);

        return str.toString();
    }

    /**
     * Converts an MBeanParameterInfo array to its expected JSON output.
     * 
     * @param signature
     * @return
     */
    private static String getSignatureString(MBeanParameterInfo[] signature) {
        StringBuilder str = new StringBuilder();
        str.append(encloseQuote(N_SIGNATURE));
        str.append(COLON);
        //constructor.signature
        str.append(OPEN_JSON_ARRAY);
        for (int j = 0; j < signature.length; j++) {
            str.append(OPEN_JSON);
            str.append(encloseString(N_NAME, signature[j].getName()));
            str.append(COMMA);
            str.append(encloseString(N_TYPE, signature[j].getType()));
            str.append(COMMA);
            str.append(encloseString(N_DESCRIPTION, signature[j].getDescription()));
            str.append(COMMA);
            str.append(getDescriptorString(signature[j].getDescriptor()));
            str.append(CLOSE_JSON);
            str.append(COMMA);
        }
        str.deleteCharAt(str.length() - 1);
        str.append(CLOSE_JSON_ARRAY);
        return str.toString();
    }

    /**
     * Convert a Map to its expected JSON String
     * 
     * For Type.Entries
     * 
     * @param map
     * @return
     */
    private static String mapTypeToJSON(Map<Object, Object> map) {
        StringBuilder str = new StringBuilder();
        str.append(OPEN_JSON_ARRAY);

        for (Object key : map.keySet()) {
            str.append(OPEN_JSON);
            str.append(encloseQuote(N_KEY));
            str.append(COLON);
            str.append(encloseQuote(key.toString()));
            str.append(COMMA);
            str.append(encloseString(N_KEYTYPE, key.getClass().getName()));
            str.append(COMMA);
            str.append(encloseString(N_VALUE, map.get(key).getClass().getName()));
            str.append(CLOSE_JSON);
            str.append(COMMA);
        }
        str.deleteCharAt(str.length() - 1);
        str.append(CLOSE_JSON_ARRAY);
        return str.toString();
    }

    /**
     * Creates a POJO object
     * 
     * @return
     */
    private static Object makePOJOObject() {
        Object[][] array2d = new Object[3][];
        array2d[0] = new Boolean[5];
        array2d[1] = new Object[2];
        array2d[1][0] = new int[5];
        array2d[2] = new Collection[1];
        List<Map<String, int[]>> list = new ArrayList<Map<String, int[]>>();
        array2d[2][0] = list;
        Map<String, int[]> map = new HashMap<String, int[]>();
        list.add(map);
        map.put("abc", new int[5]);

        //converter.writePOJO(out, array2d);
        //JSON.parse(new String(out.toByteArray(), UTF8)).serialize(System.out, true);
        return array2d;
    }

    //Supported types
    private static final String[] PRIMITIVE_TYPES = { "Integer", "Float", "Boolean", "Double", "Character", "Byte", "Short", "Long" };
    private static final String[] COLLECTION_TYPES = { "Collection", "List", "Set", "ArrayList", "LinkedList", "Vector", "HashSet" };
    private static final String[] MAP_TYPES = { "Map", "HashMap", "Hashtable", "TreeMap" };

    /**
     * Tests returns true if POJOin and POJO contain the same data in the objects. Objects can be
     * any object that is supported by the read/write methods above.
     * 
     * @param POJOin
     * @param POJO
     * @return
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    private boolean testObjects(Object POJOin, Object POJO) throws IllegalAccessException, NoSuchFieldException {

        if ((POJOin == null) ^ (POJO == null)) { //return if one of the objects are null
            return false;
        } else if ((POJOin == null) && (POJO == null)) { //both objects are null -> same
            return true;
        }

        if (POJOin.getClass().getSimpleName().equals("ObjectName")) { // skip objectName for now
            return true;
        }

        if (POJOin.getClass().getName().equals(POJO.getClass().getName())) {
            if (POJOin.getClass().isArray()) { //POJO array
                if (Array.getLength(POJOin) == Array.getLength(POJO)) {
                    for (int i = 0; i < Array.getLength(POJOin); i++) {
                        if (!testObjects(Array.get(POJOin, i), Array.get(POJO, i))) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            } else if (Arrays.asList(PRIMITIVE_TYPES).contains(POJOin.getClass().getSimpleName())) { //Primitives
                if (POJOin.equals(POJO)) {
                    return true;
                }
                return false;
            } else if (POJOin instanceof String) {
                if (((String) POJOin).equals(POJO)) {
                    return true;
                }
                return false;
            } else if (Arrays.asList(COLLECTION_TYPES).contains(POJOin.getClass().getSimpleName())) { //ListTypes
                return testObjects(((Collection<?>) POJOin).toArray(), ((Collection<?>) POJO).toArray());
            } else if (Arrays.asList(MAP_TYPES).contains(POJOin.getClass().getSimpleName())) {
                //return map, key&&value
                if (testObjects(((Map<?, ?>) POJOin).keySet().toArray(), ((Map<?, ?>) POJO).keySet().toArray()) &&
                    testObjects(((Map<?, ?>) POJOin).values().toArray(), ((Map<?, ?>) POJO).values().toArray())) {
                    return true;

                }
                return false;
            } else { //some object
                Field[] fields = POJOin.getClass().getFields();
                for (int i = 0; i < fields.length; i++) {
                    if (!testObjects(fields[i].get(POJOin), fields[i].get(POJO))) {

                        return false;
                    }
                }

                return true;
            }
        }
        return false;
    }
}
