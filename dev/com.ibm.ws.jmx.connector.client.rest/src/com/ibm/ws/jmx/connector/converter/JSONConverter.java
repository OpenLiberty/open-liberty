/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.converter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeChangeNotificationFilter;
import javax.management.AttributeList;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import javax.management.relation.MBeanServerNotificationFilter;
import javax.management.relation.RelationNotification;
import javax.management.remote.JMXConnectionNotification;
import javax.management.timer.TimerNotification;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONArtifact;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import com.ibm.ws.jmx.connector.datatypes.ConversionException;
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
 * Class used to convert JSON data for use as input and output to the JMX/REST
 * connector.
 * <p>
 * Note that the internals of this class are NOT thread safe, but the contract
 * to obtain an instance is. Do not use the same instance of this class in
 * multiple threads. Obtain a new instance each time an action is required
 * using the following pattern:
 * <p> {@code JSONConverter converter = JSONConverter.getConverter();}<br> {@code ...}<br> {@code JSONConverter.returnConverter(converter); }
 */
public class JSONConverter {
    private static final Logger logger = Logger.getLogger(JSONConverter.class.getName());
    private static final boolean USE_BASE64_FOR_POJO = false;
    private static final boolean USE_BASE64_FOR_MBEANINFO = false;

    private static DefaultSerializationHelper defaultHelper = new DefaultSerializationHelper();
    private static SerializationHelper helper = defaultHelper;

    // All supported interfaces/classes.
    private static enum TYPE {
        _Byte, _Short, _Integer, _Long, _Float, _Double, _Character, _Boolean,
        Byte, Short, Integer, Long, Float, Double, Character, Boolean,
        String, BigInteger, BigDecimal, Date, ObjectName,

        Object,
        Collection, Map,
        CompositeData, TabularData,

        List, Set,
        ArrayList, LinkedList, Vector,
        HashMap, Hashtable, TreeMap,
        HashSet,
        CompositeDataSupport, TabularDataSupport
    }

    private static final Set<String> PrimitiveArrayTypes = new HashSet<String>();
    static {
        PrimitiveArrayTypes.add(boolean.class.getName());
        PrimitiveArrayTypes.add(Boolean.class.getName());
        PrimitiveArrayTypes.add(char.class.getName());
        PrimitiveArrayTypes.add(Character.class.getName());
        PrimitiveArrayTypes.add(byte.class.getName());
        PrimitiveArrayTypes.add(Byte.class.getName());
        PrimitiveArrayTypes.add(short.class.getName());
        PrimitiveArrayTypes.add(Short.class.getName());
        PrimitiveArrayTypes.add(int.class.getName());
        PrimitiveArrayTypes.add(Integer.class.getName());
        PrimitiveArrayTypes.add(long.class.getName());
        PrimitiveArrayTypes.add(Long.class.getName());
        PrimitiveArrayTypes.add(float.class.getName());
        PrimitiveArrayTypes.add(Float.class.getName());
        PrimitiveArrayTypes.add(double.class.getName());
        PrimitiveArrayTypes.add(Double.class.getName());
    }

    // Map from supported classes to Type enumeration, so that we can
    // switch on the returned value. Implementations classes corresponding
    // to the second half of the constants map to the base concepts:
    // Collection / Map / CompositeData / TabularData.
    // This map is used for writing and reading.
    private static final Map<Class<?>, TYPE> SupportedClasses = new HashMap<Class<?>, TYPE>();
    static {
        SupportedClasses.put(Byte.TYPE, TYPE._Byte);
        SupportedClasses.put(Short.TYPE, TYPE._Short);
        SupportedClasses.put(Integer.TYPE, TYPE._Integer);
        SupportedClasses.put(Long.TYPE, TYPE._Long);
        SupportedClasses.put(Float.TYPE, TYPE._Float);
        SupportedClasses.put(Double.TYPE, TYPE._Double);
        SupportedClasses.put(Character.TYPE, TYPE._Character);
        SupportedClasses.put(Boolean.TYPE, TYPE._Boolean);
        SupportedClasses.put(Byte.class, TYPE.Byte);
        SupportedClasses.put(Short.class, TYPE.Short);
        SupportedClasses.put(Integer.class, TYPE.Integer);
        SupportedClasses.put(Long.class, TYPE.Long);
        SupportedClasses.put(Float.class, TYPE.Float);
        SupportedClasses.put(Double.class, TYPE.Double);
        SupportedClasses.put(Character.class, TYPE.Character);
        SupportedClasses.put(Boolean.class, TYPE.Boolean);
        SupportedClasses.put(String.class, TYPE.String);
        SupportedClasses.put(BigInteger.class, TYPE.BigInteger);
        SupportedClasses.put(BigDecimal.class, TYPE.BigDecimal);
        SupportedClasses.put(Date.class, TYPE.Date);
        SupportedClasses.put(ObjectName.class, TYPE.ObjectName);

        SupportedClasses.put(Object.class, TYPE.Object);
        SupportedClasses.put(Collection.class, TYPE.Collection);
        SupportedClasses.put(Map.class, TYPE.Map);
        SupportedClasses.put(CompositeData.class, TYPE.CompositeData);
        SupportedClasses.put(TabularData.class, TYPE.TabularData);

        SupportedClasses.put(List.class, TYPE.Collection);
        SupportedClasses.put(Set.class, TYPE.Collection);
        SupportedClasses.put(ArrayList.class, TYPE.Collection);
        SupportedClasses.put(LinkedList.class, TYPE.Collection);
        SupportedClasses.put(Vector.class, TYPE.Collection);
        SupportedClasses.put(HashMap.class, TYPE.Map);
        SupportedClasses.put(Hashtable.class, TYPE.Map);
        SupportedClasses.put(TreeMap.class, TYPE.Map);
        SupportedClasses.put(HashSet.class, TYPE.Collection);
        SupportedClasses.put(CompositeDataSupport.class, TYPE.CompositeData);
        SupportedClasses.put(TabularDataSupport.class, TYPE.TabularData);
    }

    // Set of 13 simple value classes corresponding to values that can be
    // represented as Strings, hence used as keys in simple maps.
    // Unlike SimpleValues, this one doesn't include java.lang.Object.
    // This map is used only during writing.
    private static final Set<Class<?>> SimpleKeys = new HashSet<Class<?>>();
    static {
        SimpleKeys.add(Byte.class);
        SimpleKeys.add(Short.class);
        SimpleKeys.add(Integer.class);
        SimpleKeys.add(Long.class);
        SimpleKeys.add(Float.class);
        SimpleKeys.add(Double.class);
        SimpleKeys.add(Character.class);
        SimpleKeys.add(Boolean.class);
        SimpleKeys.add(String.class);
        SimpleKeys.add(BigInteger.class);
        SimpleKeys.add(BigDecimal.class);
        SimpleKeys.add(Date.class);
        SimpleKeys.add(ObjectName.class);
    }

    // Set of 8 primitive, 8 corresponding built-in, and String classes.
    // Array of these types can't contain instance of other classes.
    private static final Set<Class<?>> SimpleArrays = new HashSet<Class<?>>();
    static {
        SimpleArrays.add(Byte.TYPE);
        SimpleArrays.add(Short.TYPE);
        SimpleArrays.add(Integer.TYPE);
        SimpleArrays.add(Long.TYPE);
        SimpleArrays.add(Float.TYPE);
        SimpleArrays.add(Double.TYPE);
        SimpleArrays.add(Character.TYPE);
        SimpleArrays.add(Boolean.TYPE);
        SimpleArrays.add(Byte.class);
        SimpleArrays.add(Short.class);
        SimpleArrays.add(Integer.class);
        SimpleArrays.add(Long.class);
        SimpleArrays.add(Float.class);
        SimpleArrays.add(Double.class);
        SimpleArrays.add(Character.class);
        SimpleArrays.add(Boolean.class);
        SimpleArrays.add(String.class);
    }

    // Set of 14 simple value classes corresponding to values that can be
    // represented as Strings.
    // This map is used only during reading.
    private static final Map<String, TYPE> SimpleValues = new HashMap<String, TYPE>();
    static {
        SimpleValues.put(Byte.class.getName(), TYPE.Byte);
        SimpleValues.put(Short.class.getName(), TYPE.Short);
        SimpleValues.put(Integer.class.getName(), TYPE.Integer);
        SimpleValues.put(Long.class.getName(), TYPE.Long);
        SimpleValues.put(Float.class.getName(), TYPE.Float);
        SimpleValues.put(Double.class.getName(), TYPE.Double);
        SimpleValues.put(Character.class.getName(), TYPE.Character);
        SimpleValues.put(Boolean.class.getName(), TYPE.Boolean);
        SimpleValues.put(String.class.getName(), TYPE.String);
        SimpleValues.put(BigInteger.class.getName(), TYPE.BigInteger);
        SimpleValues.put(BigDecimal.class.getName(), TYPE.BigDecimal);
        SimpleValues.put(Date.class.getName(), TYPE.Date);
        SimpleValues.put(Object.class.getName(), TYPE.Object);
        SimpleValues.put(ObjectName.class.getName(), TYPE.ObjectName);
    }

    // The map from class names to the simple open types.
    // This map is only used during reading.
    private static final Map<String, SimpleType<?>> Name2SimpleTypes = new HashMap<String, SimpleType<?>>();
    static {
        // Don't need to worry about VOID
        Name2SimpleTypes.put(Byte.class.getName(), SimpleType.BYTE);
        Name2SimpleTypes.put(Short.class.getName(), SimpleType.SHORT);
        Name2SimpleTypes.put(Integer.class.getName(), SimpleType.INTEGER);
        Name2SimpleTypes.put(Long.class.getName(), SimpleType.LONG);
        Name2SimpleTypes.put(Float.class.getName(), SimpleType.FLOAT);
        Name2SimpleTypes.put(Double.class.getName(), SimpleType.DOUBLE);
        Name2SimpleTypes.put(Character.class.getName(), SimpleType.CHARACTER);
        Name2SimpleTypes.put(Boolean.class.getName(), SimpleType.BOOLEAN);
        Name2SimpleTypes.put(String.class.getName(), SimpleType.STRING);
        Name2SimpleTypes.put(BigInteger.class.getName(), SimpleType.BIGINTEGER);
        Name2SimpleTypes.put(BigDecimal.class.getName(), SimpleType.BIGDECIMAL);
        Name2SimpleTypes.put(Date.class.getName(), SimpleType.DATE);
        Name2SimpleTypes.put(ObjectName.class.getName(), SimpleType.OBJECTNAME);
    }

    // The simple open types to the type enumerations.
    // This map is only used during reading.
    private static final Map<SimpleType<?>, TYPE> SimpleOpenTypes = new HashMap<SimpleType<?>, TYPE>();
    static {
        // Don't need to worry about VOID
        SimpleOpenTypes.put(SimpleType.BYTE, TYPE.Byte);
        SimpleOpenTypes.put(SimpleType.SHORT, TYPE.Short);
        SimpleOpenTypes.put(SimpleType.INTEGER, TYPE.Integer);
        SimpleOpenTypes.put(SimpleType.LONG, TYPE.Long);
        SimpleOpenTypes.put(SimpleType.FLOAT, TYPE.Float);
        SimpleOpenTypes.put(SimpleType.DOUBLE, TYPE.Double);
        SimpleOpenTypes.put(SimpleType.CHARACTER, TYPE.Character);
        SimpleOpenTypes.put(SimpleType.BOOLEAN, TYPE.Boolean);
        SimpleOpenTypes.put(SimpleType.STRING, TYPE.String);
        SimpleOpenTypes.put(SimpleType.BIGINTEGER, TYPE.BigInteger);
        SimpleOpenTypes.put(SimpleType.BIGDECIMAL, TYPE.BigDecimal);
        SimpleOpenTypes.put(SimpleType.DATE, TYPE.Date);
        SimpleOpenTypes.put(SimpleType.OBJECTNAME, TYPE.ObjectName);
    }

    // Map from names of complex structure classes to the type enumerations.
    // This map is only used during reading.
    private static final Map<String, TYPE> StructuredClasses = new HashMap<String, TYPE>();
    static {
        StructuredClasses.put(ArrayList.class.getName(), TYPE.ArrayList);
        StructuredClasses.put(LinkedList.class.getName(), TYPE.LinkedList);
        StructuredClasses.put(Vector.class.getName(), TYPE.Vector);
        StructuredClasses.put(HashMap.class.getName(), TYPE.HashMap);
        StructuredClasses.put(Hashtable.class.getName(), TYPE.Hashtable);
        StructuredClasses.put(TreeMap.class.getName(), TYPE.TreeMap);
        StructuredClasses.put(HashSet.class.getName(), TYPE.HashSet);
        StructuredClasses.put(CompositeDataSupport.class.getName(), TYPE.CompositeDataSupport);
        StructuredClasses.put(TabularDataSupport.class.getName(), TYPE.TabularDataSupport);
    }

    // Names of JSON object members.
    private static final String N_API = "api";
    private static final byte[] OM_API = { '"', 'a', 'p', 'i', '"', ':' };
    private static final String N_ATTRIBUTENAME = "attributeName";
    private static final byte[] OM_ATTRIBUTENAME = { '"', 'a', 't', 't', 'r', 'i', 'b', 'u', 't', 'e', 'N', 'a', 'm', 'e', '"', ':' };
    private static final String N_ATTRIBUTES = "attributes";
    private static final byte[] OM_ATTRIBUTES = { '"', 'a', 't', 't', 'r', 'i', 'b', 'u', 't', 'e', 's', '"', ':' };
    private static final String N_ATTRIBUTES_URL = "attributes_URL";
    private static final byte[] OM_ATTRIBUTES_URL = { '"', 'a', 't', 't', 'r', 'i', 'b', 'u', 't', 'e', 's', '_', 'U', 'R', 'L', '"', ':' };
    private static final String N_ATTRIBUTETYPE = "attributeType";
    private static final byte[] OM_ATTRIBUTETYPE = { '"', 'a', 't', 't', 'r', 'i', 'b', 'u', 't', 'e', 'T', 'y', 'p', 'e', '"', ':' };
    private static final String N_CLASSNAME = "className";
    private static final byte[] OM_CLASSNAME = { '"', 'c', 'l', 'a', 's', 's', 'N', 'a', 'm', 'e', '"', ':' };
    private static final String N_CLIENT = "client";
    private static final byte[] OM_CLIENT = { '"', 'c', 'l', 'i', 'e', 'n', 't', '"', ':' };
    private static final String N_CONNECTIONID = "connectionId";
    private static final byte[] OM_CONNECTIONID = { '"', 'c', 'o', 'n', 'n', 'e', 'c', 't', 'i', 'o', 'n', 'I', 'd', '"', ':' };
    private static final String N_CONSTRUCTORS = "constructors";
    private static final byte[] OM_CONSTRUCTORS = { '"', 'c', 'o', 'n', 's', 't', 'r', 'u', 'c', 't', 'o', 'r', 's', '"', ':' };
    private static final String N_CREATEMBEAN = "createMBean";
    private static final byte[] OM_CREATEMBEAN = { '"', 'c', 'r', 'e', 'a', 't', 'e', 'M', 'B', 'e', 'a', 'n', '"', ':' };
    private static final String N_DEFAULTDOMAIN = "defaultDomain";
    private static final byte[] OM_DEFAULTDOMAIN = { '"', 'd', 'e', 'f', 'a', 'u', 'l', 't', 'D', 'o', 'm', 'a', 'i', 'n', '"', ':' };
    private static final String N_DELIVERYINTERVAL = "deliveryInterval";
    private static final byte[] OM_DELIVERYINTERVAL = { '"', 'd', 'e', 'l', 'i', 'v', 'e', 'r', 'y', 'I', 'n', 't', 'e', 'r', 'v', 'a', 'l', '"', ':' };
    private static final String N_DESCRIPTION = "description";
    private static final byte[] OM_DESCRIPTION = { '"', 'd', 'e', 's', 'c', 'r', 'i', 'p', 't', 'i', 'o', 'n', '"', ':' };
    private static final String N_DESCRIPTOR = "descriptor";
    private static final byte[] OM_DESCRIPTOR = { '"', 'd', 'e', 's', 'c', 'r', 'i', 'p', 't', 'o', 'r', '"', ':' };
    private static final String N_DIMENSION = "dimension";
    private static final byte[] OM_DIMENSION = { '"', 'd', 'i', 'm', 'e', 'n', 's', 'i', 'o', 'n', '"', ':' };
    private static final String N_DISABLED = "disabled";
    private static final byte[] OM_DISABLED = { '"', 'd', 'i', 's', 'a', 'b', 'l', 'e', 'd', '"', ':' };
    private static final String N_DOMAINS = "domains";
    private static final byte[] OM_DOMAINS = { '"', 'd', 'o', 'm', 'a', 'i', 'n', 's', '"', ':' };
    private static final String N_ELEMENTTYPE = "elementType";
    private static final byte[] OM_ELEMENTTYPE = { '"', 'e', 'l', 'e', 'm', 'e', 'n', 't', 'T', 'y', 'p', 'e', '"', ':' };
    private static final String N_ENABLED = "enabled";
    private static final byte[] OM_ENABLED = { '"', 'e', 'n', 'a', 'b', 'l', 'e', 'd', '"', ':' };
    private static final String N_ENTRIES = "entries";
    private static final byte[] OM_ENTRIES = { '"', 'e', 'n', 't', 'r', 'i', 'e', 's', '"', ':' };
    private static final String N_FILE_TRANSFER = "fileTransfer";
    private static final byte[] OM_FILE_TRANSFER = { '"', 'f', 'i', 'l', 'e', 'T', 'r', 'a', 'n', 's', 'f', 'e', 'r', '"', ':' };
    private static final String N_FILTER = "filter";
    private static final byte[] OM_FILTER = { '"', 'f', 'i', 'l', 't', 'e', 'r', '"', ':' };
    private static final String N_FILTERID = "filterID";
    private static final byte[] OM_FILTERID = { '"', 'f', 'i', 'l', 't', 'e', 'r', 'I', 'D', '"', ':' };
    private static final String N_FILTERS = "filters";
    private static final byte[] OM_FILTERS = { '"', 'f', 'i', 'l', 't', 'e', 'r', 's', '"', ':' };
    private static final String N_GRAPH = "graph";
    private static final byte[] OM_GRAPH = { '"', 'g', 'r', 'a', 'p', 'h', '"', ':' };
    private static final String N_HANDBACK = "handback";
    private static final byte[] OM_HANDBACK = { '"', 'h', 'a', 'n', 'd', 'b', 'a', 'c', 'k', '"', ':' };
    private static final String N_HANDBACKID = "handbackID";
    private static final byte[] OM_HANDBACKID = { '"', 'h', 'a', 'n', 'd', 'b', 'a', 'c', 'k', 'I', 'D', '"', ':' };
    private static final String N_HOSTNAME = "hostName";
    private static final byte[] OM_HOSTNAME = { '"', 'h', 'o', 's', 't', 'N', 'a', 'm', 'e', '"', ':' };
    private static final String N_IMPACT = "impact";
    private static final byte[] OM_IMPACT = { '"', 'i', 'm', 'p', 'a', 'c', 't', '"', ':' };
    private static final String N_INBOX = "inbox";
    private static final byte[] OM_INBOX = { '"', 'i', 'n', 'b', 'o', 'x', '"', ':' };
    private static final String N_INBOXEXPIRY = "inboxExpiry";
    private static final byte[] OM_INBOXEXPIRTY = { '"', 'i', 'n', 'b', 'o', 'x', 'E', 'x', 'p', 'i', 'r', 'y', '"', ':' };
    private static final String N_INDEXNAMES = "indexNames";
    private static final byte[] OM_INDEXNAMES = { '"', 'i', 'n', 'd', 'e', 'x', 'N', 'a', 'm', 'e', 's', '"', ':' };
    private static final String N_INSTANCEOF = "instanceOf";
    private static final byte[] OM_INSTANCEOF = { '"', 'i', 'n', 's', 't', 'a', 'n', 'c', 'e', 'O', 'f', '"', ':' };
    private static final String N_ISIS = "isIs";
    private static final byte[] OM_ISIS = { '"', 'i', 's', 'I', 's', '"', ':' };
    private static final String N_ISREADABLE = "isReadable";
    private static final byte[] OM_ISREADABLE = { '"', 'i', 's', 'R', 'e', 'a', 'd', 'a', 'b', 'l', 'e', '"', ':' };
    private static final String N_ISWRITABLE = "isWritable";
    private static final byte[] OM_ISWRITABLE = { '"', 'i', 's', 'W', 'r', 'i', 't', 'a', 'b', 'l', 'e', '"', ':' };
    private static final String N_ITEMS = "items";
    private static final byte[] OM_ITEMS = { '"', 'i', 't', 'e', 'm', 's', '"', ':' };
    private static final String N_LISTENER = "listener";
    private static final byte[] OM_LISTENER = { '"', 'l', 'i', 's', 't', 'e', 'n', 'e', 'r', '"', ':' };
    private static final String N_LOADERNAME = "loaderName";
    private static final byte[] OM_LOADERNAME = { '"', 'l', 'o', 'a', 'd', 'e', 'r', 'N', 'a', 'm', 'e', '"', ':' };
    private static final String N_KEY = "key";
    private static final byte[] OM_KEY = { '"', 'k', 'e', 'y', '"', ':' };
    private static final String N_KEYTYPE = "keyType";
    private static final byte[] OM_KEYTYPE = { '"', 'k', 'e', 'y', 'T', 'y', 'p', 'e', '"', ':' };
    private static final String N_MBEANCOUNT = "mbeanCount";
    private static final byte[] OM_MBEANCOUNT = { '"', 'm', 'b', 'e', 'a', 'n', 'C', 'o', 'u', 'n', 't', '"', ':' };
    private static final String N_MBEANNAME = "mbeanName";
    private static final byte[] OM_MBEANNAME = { '"', 'm', 'b', 'e', 'a', 'n', 'N', 'a', 'm', 'e', '"', ':' };
    private static final String N_MBEANS = "mbeans";
    private static final byte[] OM_MBEANS = { '"', 'm', 'b', 'e', 'a', 'n', 's', '"', ':' };
    private static final String N_MBEANSTOUNREGISTER = "mbeansToUnregister";
    private static final byte[] OM_MBEANSTOUNREGISTER = { '"', 'm', 'b', 'e', 'a', 'n', 's', 'T', 'o', 'U', 'n', 'r', 'e', 'g', 'i', 's', 't', 'e', 'r', '"', ':' };
    private static final String N_MESSAGE = "message";
    private static final byte[] OM_MESSAGE = { '"', 'm', 'e', 's', 's', 'a', 'g', 'e', '"', ':' };
    private static final String N_NAME = "name";
    private static final byte[] OM_NAME = { '"', 'n', 'a', 'm', 'e', '"', ':' };
    private static final String N_NAMES = "names";
    private static final byte[] OM_NAMES = { '"', 'n', 'a', 'm', 'e', 's', '"', ':' };
    private static final String N_NEWROLEVALUE = "newRoleValue";
    private static final byte[] OM_NEWROLEVALUE = { '"', 'n', 'e', 'w', 'R', 'o', 'l', 'e', 'V', 'a', 'l', 'u', 'e', '"', ':' };
    private static final String N_NEWVALUE = "newValue";
    private static final byte[] OM_NEWVALUE = { '"', 'n', 'e', 'w', 'V', 'a', 'l', 'u', 'e', '"', ':' };
    private static final String N_NOTIFICATIONID = "notificationID";
    private static final byte[] OM_NOTIFICATIONID = { '"', 'n', 'o', 't', 'i', 'f', 'i', 'c', 'a', 't', 'i', 'o', 'n', 'I', 'D', '"', ':' };
    private static final String N_NOTIFICATIONS = "notifications";
    private static final byte[] OM_NOTIFICATIONS = { '"', 'n', 'o', 't', 'i', 'f', 'i', 'c', 'a', 't', 'i', 'o', 'n', 's', '"', ':' };
    private static final String N_NOTIFTYPES = "notifTypes";
    private static final byte[] OM_NOTIFTYPES = { '"', 'n', 'o', 't', 'i', 'f', 'T', 'y', 'p', 'e', 's', '"', ':' };
    private static final String N_OBJECTNAME = "objectName";
    private static final byte[] OM_OBJECTNAME = { '"', 'o', 'b', 'j', 'e', 'c', 't', 'N', 'a', 'm', 'e', '"', ':' };
    private static final String N_OLDROLEVALUE = "oldRoleValue";
    private static final byte[] OM_OLDROLEVALUE = { '"', 'o', 'l', 'd', 'R', 'o', 'l', 'e', 'V', 'a', 'l', 'u', 'e', '"', ':' };
    private static final String N_OLDVALUE = "oldValue";
    private static final byte[] OM_OLDVALUE = { '"', 'o', 'l', 'd', 'V', 'a', 'l', 'u', 'e', '"', ':' };
    private static final String N_OPENTYPE = "openType";
    private static final byte[] OM_OPENTYPE = { '"', 'o', 'p', 'e', 'n', 'T', 'y', 'p', 'e', '"', ':' };
    private static final String N_OPENTYPECLASS = "openTypeClass";
    private static final byte[] OM_OPENTYPECLASS = { '"', 'o', 'p', 'e', 'n', 'T', 'y', 'p', 'e', 'C', 'l', 'a', 's', 's', '"', ':' };
    private static final String N_OPENTYPES = "openTypes";
    private static final byte[] OM_OPENTYPES = { '"', 'o', 'p', 'e', 'n', 'T', 'y', 'p', 'e', 's', '"', ':' };
    private static final String N_OPERATION = "operation";
    private static final byte[] OM_OPERATION = { '"', 'o', 'p', 'e', 'r', 'a', 't', 'i', 'o', 'n', '"', ':' };
    private static final String N_OPERATIONS = "operations";
    private static final byte[] OM_OPERATIONS = { '"', 'o', 'p', 'e', 'r', 'a', 't', 'i', 'o', 'n', 's', '"', ':' };
    private static final String N_PARAMS = "params";
    private static final byte[] OM_PARAMS = { '"', 'p', 'a', 'r', 'a', 'm', 's', '"', ':' };
    private static final String N_QUERYEXP = "queryExp";
    private static final byte[] OM_QUERYEXP = { '"', 'q', 'u', 'e', 'r', 'y', 'E', 'x', 'p', '"', ':' };
    private static final String N_REGISTRATIONS = "registrations";
    private static final byte[] OM_REGISTRATIONS = { '"', 'r', 'e', 'g', 'i', 's', 't', 'r', 'a', 't', 'i', 'o', 'n', 's', '"', ':' };
    private static final String N_RELATIONID = "relationId";
    private static final byte[] OM_RELATIONID = { '"', 'r', 'e', 'l', 'a', 't', 'i', 'o', 'n', 'I', 'd', '"', ':' };
    private static final String N_RELATIONTYPENAME = "relationTypeName";
    private static final byte[] OM_RELATIONTYPENAME = { '"', 'r', 'e', 'l', 'a', 't', 'i', 'o', 'n', 'T', 'y', 'p', 'e', 'N', 'a', 'm', 'e', '"', ':' };
    private static final String N_RETURNTYPE = "returnType";
    private static final byte[] OM_RETURNTYPE = { '"', 'r', 'e', 't', 'u', 'r', 'n', 'T', 'y', 'p', 'e', '"', ':' };
    private static final String N_ROLENAME = "roleName";
    private static final byte[] OM_ROLENAME = { '"', 'r', 'o', 'l', 'e', 'N', 'a', 'm', 'e', '"', ':' };
    private static final String N_ROWTYPE = "rowType";
    private static final byte[] OM_ROWTYPE = { '"', 'r', 'o', 'w', 'T', 'y', 'p', 'e', '"', ':' };
    private static final String N_SEQUENCENUMBER = "sequenceNumber";
    private static final byte[] OM_SEQUENCENUMBER = { '"', 's', 'e', 'q', 'u', 'e', 'n', 'c', 'e', 'N', 'u', 'm', 'b', 'e', 'r', '"', ':' };
    private static final String N_SERIALIZED = "serialized";
    private static final byte[] OM_SERIALIZED = { '"', 's', 'e', 'r', 'i', 'a', 'l', 'i', 'z', 'e', 'd', '"', ':' };
    private static final String N_SERVERNAME = "serverName";
    private static final byte[] OM_SERVERNAME = { '"', 's', 'e', 'r', 'v', 'e', 'r', 'N', 'a', 'm', 'e', '"', ':' };
    private static final String N_SERVERREGISTRATIONS = "serverRegistrations";
    private static final byte[] OM_SERVERREGISTRATIONS = { '"', 's', 'e', 'r', 'v', 'e', 'r', 'R', 'e', 'g', 'i', 's', 't', 'r', 'a', 't', 'i', 'o', 'n', 's', '"', ':' };
    private static final String N_SERVERUSERDIR = "serverUserDir";
    private static final byte[] OM_SERVERUSERDIR = { '"', 's', 'e', 'r', 'v', 'e', 'r', 'U', 's', 'e', 'r', 'D', 'i', 'r', '"', ':' };
    private static final String N_SIGNATURE = "signature";
    private static final byte[] OM_SIGNATURE = { '"', 's', 'i', 'g', 'n', 'a', 't', 'u', 'r', 'e', '"', ':' };
    private static final String N_SIMPLEKEY = "simpleKey";
    private static final byte[] OM_SIMPLEKEY = { '"', 's', 'i', 'm', 'p', 'l', 'e', 'K', 'e', 'y', '"', ':' };
    private static final String N_SOURCE = "source";
    private static final byte[] OM_SOURCE = { '"', 's', 'o', 'u', 'r', 'c', 'e', '"', ':' };
    // "stacktrace" is not read by the Java client.
    //    private static final String N_STACKTRACE = "stackTrace";
    private static final byte[] OM_STACKTRACE = { '"', 's', 't', 'a', 'c', 'k', 'T', 'r', 'a', 'c', 'e', '"', ':' };
    // "error" is not read by the Java client
    private static final byte[] OM_EXCEPTION_MESSAGE = { '"', 'e', 'r', 'r', 'o', 'r', '"', ':' };
    private static final String N_THROWABLE = "throwable";
    private static final byte[] OM_THROWABLE = { '"', 't', 'h', 'r', 'o', 'w', 'a', 'b', 'l', 'e', '"', ':' };
    private static final String N_TIMESTAMP = "timeStamp";
    private static final byte[] OM_TIMESTAMP = { '"', 't', 'i', 'm', 'e', 'S', 't', 'a', 'm', 'p', '"', ':' };
    private static final String N_TYPE = "type";
    private static final byte[] OM_TYPE = { '"', 't', 'y', 'p', 'e', '"', ':' };
    private static final String N_TYPENAME = "typeName";
    private static final byte[] OM_TYPENAME = { '"', 't', 'y', 'p', 'e', 'N', 'a', 'm', 'e', '"', ':' };
    private static final String N_TYPES = "types";
    private static final byte[] OM_TYPES = { '"', 't', 'y', 'p', 'e', 's', '"', ':' };
    private static final String N_URL = "URL";
    private static final byte[] OM_URL = { '"', 'U', 'R', 'L', '"', ':' };
    private static final String N_USELOADER = "useLoader";
    private static final byte[] OM_USELOADER = { '"', 'u', 's', 'e', 'L', 'o', 'a', 'd', 'e', 'r', '"', ':' };
    private static final String N_USERDATA = "userData";
    private static final byte[] OM_USERDATA = { '"', 'u', 's', 'e', 'r', 'D', 'a', 't', 'a', '"', ':' };
    private static final String N_USESIGNATURE = "useSignature";
    private static final byte[] OM_USESIGNATURE = { '"', 'u', 's', 'e', 'S', 'i', 'g', 'n', 'a', 't', 'u', 'r', 'e', '"', ':' };
    private static final String N_VALUE = "value";
    private static final byte[] OM_VALUE = { '"', 'v', 'a', 'l', 'u', 'e', '"', ':' };
    private static final String N_VALUES = "values";
    private static final byte[] OM_VALUES = { '"', 'v', 'a', 'l', 'u', 'e', 's', '"', ':' };
    private static final String N_VERSION = "version";
    private static final byte[] OM_VERSION = { '"', 'v', 'e', 'r', 's', 'i', 'o', 'n', '"', ':' };

    private static final byte[] LONG_MIN = { '-', '9', '2', '2', '3', '3', '7',
                                             '2', '0', '3', '6', '8', '5', '4',
                                             '7', '7', '5', '8', '0', '8' };
    private static final byte[] INT_MIN = { '-', '2', '1', '4', '7', '4', '8',
                                            '3', '6', '4', '8' };
    private static final byte[] TRUE = { 't', 'r', 'u', 'e' };
    private static final byte[] FALSE = { 'f', 'a', 'l', 's', 'e' };
    private static final byte[] NULL = { 'n', 'u', 'l', 'l' };
    private static final Object OBJECT = new Object();

    // Maps used by base64 encoding/decoding
    private static final byte[] BASE64 = new byte[64];
    private static final byte[] BASE64_D = new byte[128];
    static {
        for (int i = 0; i < 128; i++) {
            BASE64_D[i] = -1;
        }
        for (byte i = 0; i < 26; i++) {
            BASE64[i] = (byte) (i + 'A');
            BASE64_D[i + 'A'] = i;
        }
        for (int i = 0; i < 26; i++) {
            BASE64[i + 26] = (byte) (i + 'a');
            BASE64_D[i + 'a'] = (byte) (i + 26);
        }
        for (int i = 0; i < 10; i++) {
            BASE64[i + 52] = (byte) (i + '0');
            BASE64_D[i + '0'] = (byte) (i + 52);
        }
        BASE64[62] = '+';
        BASE64[63] = '/';
        BASE64_D['+'] = 62;
        BASE64_D['/'] = 63;
        BASE64_D['='] = 0;
    }

    // Pool of converters, used by the server.
    private static final Stack<JSONConverter> POOL = new Stack<JSONConverter>();

    // Byte buffer for writing integers and encoding base64 values.
    // Make sure the buffer is long enough to hold LONG_MIN, and is divisible
    // by 3, for base64 encoding.
    private final byte[] BYTE_BUFFER = new byte[LONG_MIN.length / 3 * 3 + 3];

    // Whether the next array/object item is the first. If not, need to write ','.
    private boolean firstItem;

    // ByteArrayOutputStream used for encoding String to base64
    private final ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();

    // Base64OutputStream used to base64 encoding its content
    private final Base64OutputStream base64Stream = new Base64OutputStream();

    // Class for providing base64 encoding capability to an OutputStream
    private class Base64OutputStream extends OutputStream {
        private OutputStream out;
        private int count;

        private void start(OutputStream out) {
            this.out = out;
            this.count = 0;
        }

        @Override
        public void write(int b) throws IOException {
            // If the buffer is already full, base64-encode it.
            // The size is divisible by 3, so no padding is done.
            if (count == BYTE_BUFFER.length) {
                encodeBase64();
                count = 0;
            }
            BYTE_BUFFER[count++] = (byte) b;
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            int left = BYTE_BUFFER.length - count;
            // If we have more than what the buffer can hold
            while (len > left) {
                // Fill the buffer and base64 encode it
                // The size is divisible by 3, so no padding is done.
                System.arraycopy(b, off, BYTE_BUFFER, count, left);
                count = BYTE_BUFFER.length;
                encodeBase64();
                len -= left;
                off += left;
                left = BYTE_BUFFER.length;
            }
            // Copy the rest into the buffer
            System.arraycopy(b, off, BYTE_BUFFER, count, len);
            count += len;
        }

        @Override
        public void flush() throws IOException {
            // Encode and write out any pending bytes
            if (count > 0) {
                encodeBase64();
            }
        }

        private void end() throws IOException {
            flush();
            out = null;
        }

        private void encodeBase64() throws IOException {
            final int left = count % 3;
            int pos = 0, b1, b2, b3;
            // For every 3-byte sequence
            for (; pos < count - left;) {
                b1 = BYTE_BUFFER[pos++];
                b2 = BYTE_BUFFER[pos++];
                b3 = BYTE_BUFFER[pos++];
                // Write out 4 bytes
                out.write(BASE64[(b1 >> 2) & 0x3f]);
                out.write(BASE64[((b1 & 0x3) << 4) | ((b2 >> 4) & 0xf)]);
                out.write(BASE64[((b2 & 0xf) << 2) | ((b3 >> 6) & 0x3)]);
                out.write(BASE64[b3 & 0x3f]);
            }
            // 1 byte is left. Need 2 padding chars '=='
            if (left == 1) {
                b1 = BYTE_BUFFER[pos];
                out.write(BASE64[(b1 >> 2) & 0x3f]);
                out.write(BASE64[(b1 & 0x3) << 4]);
                out.write('=');
                out.write('=');
            }
            // 2 bytes are left. Need 1 padding char '='
            else if (left == 2) {
                b1 = BYTE_BUFFER[pos++];
                b2 = BYTE_BUFFER[pos++];
                out.write(BASE64[(b1 >> 2) & 0x3f]);
                out.write(BASE64[((b1 & 0x3) << 4) | ((b2 >> 4) & 0xf)]);
                out.write(BASE64[(b2 & 0xf) << 2]);
                out.write('=');
            }
            count = 0;
        }
    }

    /**
     * Retrieve an instance of the JSON converter. When the converter is no
     * longer needed, it can be returned to a global pool for reuse, by
     * calling {@link #returnConverter(JSONConverter)}.
     *
     * @return an instance of the JSONConverter. {@code null} is not returned.
     */
    public static JSONConverter getConverter() {
        synchronized (POOL) {
            if (!POOL.empty()) {
                return POOL.pop();
            }
        }
        return new JSONConverter();
    }

    /**
     * Returns the JSONConverter instance to the global pool for reuse later.
     *
     * @param converter the JSONConverter to return. {@code null} is not supported.
     */
    public static void returnConverter(JSONConverter converter) {
        if (!POOL.contains(converter))
            POOL.push(converter);
    }

    // Use getConverter()
    private JSONConverter() {}

    /**
     * Encode an integer value as JSON. An array is used to wrap the value:
     * [ Integer ]
     *
     * @param out The stream to write JSON to
     * @param value The integer value to encode
     * @throws IOException If an I/O error occurs
     * @see #readInt(InputStream)
     */
    public void writeInt(OutputStream out, int value) throws IOException {
        writeStartArray(out);
        writeIntInternal(out, value);
        writeEndArray(out);
    }

    /**
     * Decode a JSON document to retrieve an integer value.
     *
     * @param in The stream to read JSON from
     * @return The decoded integer value
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @see #writeInt(OutputStream, int)
     */
    public int readInt(InputStream in) throws ConversionException, IOException {
        JSONArray json = parseArray(in);
        if (json.size() != 1) {
            throwConversionException("readInt() expects one item in the array: [ Integer ].", json);
        }
        return readIntInternal(json.get(0));
    }

    /**
     * Encode a boolean value as JSON. An array is used to wrap the value:
     * [ true | false ]
     *
     * @param out The stream to write JSON to
     * @param value The boolean value to encode
     * @throws IOException If an I/O error occurs
     * @see #readBoolean(InputStream)
     */
    public void writeBoolean(OutputStream out, boolean value) throws IOException {
        writeStartArray(out);
        writeBooleanInternal(out, value);
        writeEndArray(out);
    }

    /**
     * Decode a JSON document to retrieve an boolean value.
     *
     * @param in The stream to read JSON from
     * @return The decoded boolean value
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @see #writeInt(OutputStream, int)
     */
    public boolean readBoolean(InputStream in) throws ConversionException, IOException {
        JSONArray json = parseArray(in);
        if (json.size() != 1) {
            throwConversionException("readBoolean() expects one item in the array: [ true | false ].", json);
        }
        return readBooleanInternal(json.get(0));
    }

    /**
     * Encode a String value as JSON. An array is used to wrap the value:
     * [ String ]
     *
     * @param out The stream to write JSON to
     * @param value The String value to encode.
     * @throws IOException If an I/O error occurs
     * @see #readString(InputStream)
     */
    public void writeString(OutputStream out, String value) throws IOException {
        writeStartArray(out);
        writeStringInternal(out, value);
        writeEndArray(out);
    }

    /**
     * Decode a JSON document to retrieve a String value.
     *
     * @param in The stream to read JSON from
     * @return The decoded String value
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @see #writeString(OutputStream, String)
     */
    public String readString(InputStream in) throws ConversionException, IOException {
        JSONArray json = parseArray(in);
        if (json.size() != 1) {
            throwConversionException("readString() expects one item in the array: [ String ].", json);
        }
        return readStringInternal(json.get(0));
    }

    /**
     * Encode a String array as JSON:
     * [ String* ]
     *
     * @param out The stream to write JSON to
     * @param value The String array to encode. Can't be null.
     * @throws IOException If an I/O error occurs
     */
    public void writeStringArray(OutputStream out, String[] value) throws IOException {
        writeStringArrayInternal(out, value);
    }

    /**
     * Decode a JSON document to retrieve a String array.
     *
     * @param in The stream to read JSON from
     * @return The decoded String array
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @see #writeStringArray(OutputStream, String[])
     */
    public String[] readStringArray(InputStream in) throws ConversionException, IOException {
        return readStringArrayInternal(parse(in));
    }

    /**
     * Encode an Object as JSON.
     *
     * Format:
     *
     * {
     * serialized: Base64, ? // If object has cyclic references. Otherwise use the following fields instead.
     * value: Value, ?
     * type: Type, ?
     * openTypes: [ OpenType * ], ? // Only if open types are used
     * }
     *
     * *** Value ***
     *
     * 1. For "null" value, or a value that's not expressible in JSON
     *
     * null
     *
     * 2. For a simple value (primitive, String, BigInteger/Decimal, Date,
     * ObjectName), or Object. For Object, "" is used.
     *
     * String
     *
     * 3. For array or collection
     *
     * [ Value* ]
     *
     * 4. For map where all keys are simple values
     *
     * {
     * String: Value *
     * }
     *
     * 5. For map with complex keys
     *
     * [ {key: Value, value: Value}* ]
     *
     * 6. For CompositeData
     *
     * {
     * String: Value *
     * }
     *
     * 7. and TabularData
     *
     * [ CompositeData* ]
     *
     * *** Type ***
     *
     * 1. For "null" value
     *
     * null
     *
     * 2. For a simple value, arrays of primitives or final simple values,
     * or Object. For Object, "" is used.
     *
     * String // The class name
     *
     * 3. Otherwise
     *
     * {
     * class: String, // Class name of the object
     * serialized: Base64 ? // Optional: If the class is not supported. The java serialization of the value.
     * }
     *
     * The following additional fields apply depending on the kind of value.
     *
     * 3.1 For complex array or collection classes:
     *
     * {
     * items: [ Type* ]
     * }
     *
     * 3.4 For maps
     *
     * {
     * simpleKey: boolean, // Whether all keys are simple. Then each entry has a "key" field.
     * entries: [
     * {
     * key: String, ? // For simple key
     * keyType: Type,
     * value: Type
     * } *
     * ]
     *
     * 3.5 For CompositeData and TabularData
     *
     * {
     * openType: Integer // Reference to the OpenType array in root Type
     * }
     *
     * *** OpenType ***
     *
     * 1. For SimpleType instances:
     *
     * String // The name of the simple type.
     *
     * 2. For all other OpenTypes:
     *
     * {
     * openTypeClass: String, // The OpenType class.
     * serialized: Base64 // Optional: If the class is not supported. The java serialization of the OpenType object.
     *
     * className: String,
     * typeName: String,
     * description: String,
     * }
     *
     * 2.1 ArrayType
     *
     * {
     * dimension: Integer,
     * elementType: Integer,
     * }
     *
     * 2.2 CompositeType
     *
     * {
     * items: { String: { description: String, type: Integer } *}
     * }
     *
     * 2.3 TabularType
     *
     * {
     * rowType: Integer,
     * indexNames: [String*]
     * }
     *
     * @param out The stream to write JSON to
     * @param value The Object to encode. Can be null.
     * @throws IOException If an I/O error occurs
     * @see #readPOJO(InputStream)
     */
    public void writePOJO(OutputStream out, Object value) throws IOException {
        writePOJOInternal(out, value);
    }

    /**
     * Decode a JSON document to retrieve an Object.
     *
     * @param in The stream to read JSON from
     * @return The decoded Object
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writePOJO(OutputStream, Object)
     */
    public Object readPOJO(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        return readPOJOInternal(parse(in));
    }

    /**
     * Encode a JMX instance as JSON:
     * {
     * "version" : Integer,
     * "mbeans" : URL,
     * "createMBean" : URL,
     * "mbeanCount" : URL,
     * "defaultDomain" : URL,
     * "domains" : URL,
     * "notifications" : URL,
     * "instanceOf" : URL
     * }
     *
     * @param out The stream to write JSON to
     * @param value The JMX instance to encode. Can't be null.
     * @throws IOException If an I/O error occurs
     * @see #readJMX(InputStream)
     */
    public void writeJMX(OutputStream out, JMXServerInfo value) throws IOException {
        writeStartObject(out);
        writeIntField(out, OM_VERSION, value.version);
        writeStringField(out, OM_MBEANS, value.mbeansURL);
        writeStringField(out, OM_CREATEMBEAN, value.createMBeanURL);
        writeStringField(out, OM_MBEANCOUNT, value.mbeanCountURL);
        writeStringField(out, OM_DEFAULTDOMAIN, value.defaultDomainURL);
        writeStringField(out, OM_DOMAINS, value.domainsURL);
        writeStringField(out, OM_NOTIFICATIONS, value.notificationsURL);
        writeStringField(out, OM_INSTANCEOF, value.instanceOfURL);
        writeStringField(out, OM_FILE_TRANSFER, value.fileTransferURL);
        writeStringField(out, OM_API, value.apiURL);
        writeStringField(out, OM_GRAPH, value.graphURL);
        writeEndObject(out);
    }

    /**
     * Decode a JSON document to retrieve a JMX instance.
     *
     * @param in The stream to read JSON from
     * @return The decoded JMX instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @see #writeJMX(OutputStream, JMXServerInfo)
     */
    public JMXServerInfo readJMX(InputStream in) throws ConversionException, IOException {
        JSONObject json = parseObject(in);
        JMXServerInfo ret = new JMXServerInfo();
        ret.version = readIntInternal(json.get(N_VERSION));
        ret.mbeansURL = readStringInternal(json.get(N_MBEANS));
        ret.createMBeanURL = readStringInternal(json.get(N_CREATEMBEAN));
        ret.mbeanCountURL = readStringInternal(json.get(N_MBEANCOUNT));
        ret.defaultDomainURL = readStringInternal(json.get(N_DEFAULTDOMAIN));
        ret.domainsURL = readStringInternal(json.get(N_DOMAINS));
        ret.notificationsURL = readStringInternal(json.get(N_NOTIFICATIONS));
        ret.instanceOfURL = readStringInternal(json.get(N_INSTANCEOF));
        ret.fileTransferURL = readStringInternal(json.get(N_FILE_TRANSFER));
        ret.apiURL = readStringInternal(json.get(N_API));
        ret.graphURL = readStringInternal(json.get(N_GRAPH));
        return ret;
    }

    /**
     * Encode an ObjectInstanceWrapper instance as JSON:
     * {
     * "objectName" : ObjectName,
     * "className" : String,
     * "URL" : URL,
     * }
     *
     * @param out The stream to write JSON to
     * @param value The ObjectInstanceWrapper instance to encode.
     *            The value and value.objectInstance can't be null.
     * @throws IOException If an I/O error occurs
     * @see #readObjectInstance(InputStream)
     */
    public void writeObjectInstance(OutputStream out, ObjectInstanceWrapper value) throws IOException {
        // ObjectInstance has no known sub-class.
        writeStartObject(out);
        writeObjectNameField(out, OM_OBJECTNAME, value.objectInstance.getObjectName());
        writeStringField(out, OM_CLASSNAME, value.objectInstance.getClassName());
        writeStringField(out, OM_URL, value.mbeanInfoURL);
        writeEndObject(out);
    }

    /**
     * Decode a JSON document to retrieve an ObjectInstanceWrapper instance.
     *
     * @param in The stream to read JSON from
     * @return The decoded ObjectInstanceWrapper instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @see #writeObjectInstance(OutputStream, ObjectInstanceWrapper)
     */
    public ObjectInstanceWrapper readObjectInstance(InputStream in) throws ConversionException, IOException {
        return readObjectInstanceInternal(parse(in));
    }

    /**
     * Encode an ObjectInstanceWrapper array as JSON:
     * [ ObjectInstanceWrapper* ]
     *
     * @param out The stream to write JSON to
     * @param value The ObjectInstanceWrapper array to encode. Can't be null.
     *            And none of its entries can be null.
     * @throws IOException If an I/O error occurs
     * @see #readObjectInstances(InputStream)
     * @see #writeObjectInstance(OutputStream, ObjectInstanceWrapper)
     */
    public void writeObjectInstanceArray(OutputStream out, ObjectInstanceWrapper[] value) throws IOException {
        writeStartArray(out);
        for (ObjectInstanceWrapper item : value) {
            writeArrayItem(out);
            writeObjectInstance(out, item);
        }
        writeEndArray(out);
    }

    /**
     * Decode a JSON document to retrieve an ObjectInstanceWrapper array.
     *
     * @param in The stream to read JSON from
     * @return The decoded ObjectInstanceWrapper array
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @see #writeObjectInstanceArray(OutputStream, ObjectInstanceWrapper[])
     * @see #readObjectInstance(InputStream)
     */
    public ObjectInstanceWrapper[] readObjectInstances(InputStream in) throws ConversionException, IOException {
        JSONArray json = parseArray(in);
        ObjectInstanceWrapper[] ret = new ObjectInstanceWrapper[json.size()];
        int pos = 0;
        for (Object item : json) {
            ret[pos++] = readObjectInstanceInternal(item);
        }
        return ret;
    }

    /**
     * Encode an MBeanQuery instance as JSON:
     * {
     * "objectName" : ObjectName,
     * "queryExp" : Base64,
     * "className" : String,
     * }
     *
     * @param out The stream to write JSON to
     * @param value The MBeanQuery instance to encode. Can't be null.
     * @throws IOException If an I/O error occurs
     * @see #readMBeanQuery(InputStream)
     */
    public void writeMBeanQuery(OutputStream out, MBeanQuery value) throws IOException {
        writeStartObject(out);
        writeObjectNameField(out, OM_OBJECTNAME, value.objectName);
        // TODO: Produce proper JSON for QueryExp?
        writeSerializedField(out, OM_QUERYEXP, value.queryExp);
        writeStringField(out, OM_CLASSNAME, value.className);
        writeEndObject(out);
    }

    /**
     * Decode a JSON document to retrieve an MBeanQuery instance.
     *
     * @param in The stream to read JSON from
     * @return The decoded MBeanQuery instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writeMBeanQuery(OutputStream, MBeanQuery)
     */
    public MBeanQuery readMBeanQuery(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        JSONObject json = parseObject(in);
        MBeanQuery ret = new MBeanQuery();
        ret.objectName = readObjectName(json.get(N_OBJECTNAME));
        Object queryExp = readSerialized(json.get(N_QUERYEXP));
        if (queryExp != null && !(queryExp instanceof QueryExp)) {
            throwConversionException("readMBeanQuery() receives an instance that's not a QueryExp.", json.get(N_QUERYEXP));
        }
        ret.queryExp = (QueryExp) queryExp;
        ret.className = readStringInternal(json.get(N_CLASSNAME));
        return ret;
    }

    /**
     * Encode a CreateMBean instance as JSON:
     * {
     * "className" : String,
     * "objectName" : ObjectName,
     * "loaderName" : ObjectName,
     * "params" : [ POJO* ],
     * "signature" : [ String* ],
     * "useLoader" : Boolean,
     * "useSignatue" : Boolean
     * }
     *
     * @param out The stream to write JSON to
     * @param value The CreateMBean instance to encode. Can't be null.
     * @throws IOException If an I/O error occurs
     * @see #readCreateMBean(InputStream)
     */
    public void writeCreateMBean(OutputStream out, CreateMBean value) throws IOException {
        writeStartObject(out);
        writeStringField(out, OM_CLASSNAME, value.className);
        writeObjectNameField(out, OM_OBJECTNAME, value.objectName);
        writeObjectNameField(out, OM_LOADERNAME, value.loaderName);
        writePOJOArrayField(out, OM_PARAMS, value.params);
        writeStringArrayField(out, OM_SIGNATURE, value.signature);
        writeBooleanField(out, OM_USELOADER, value.useLoader);
        writeBooleanField(out, OM_USESIGNATURE, value.useSignature);
        writeEndObject(out);
    }

    /**
     * Decode a JSON document to retrieve a CreateMBean instance.
     *
     * @param in The stream to read JSON from
     * @return The decoded CreateMBean instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writeCreateMBean(OutputStream, CreateMBean)
     */
    public CreateMBean readCreateMBean(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        JSONObject json = parseObject(in);
        CreateMBean ret = new CreateMBean();
        ret.objectName = readObjectName(json.get(N_OBJECTNAME));
        ret.className = readStringInternal(json.get(N_CLASSNAME));
        ret.loaderName = readObjectName(json.get(N_LOADERNAME));
        ret.params = readPOJOArray(json.get(N_PARAMS));
        ret.signature = readStringArrayInternal(json.get(N_SIGNATURE));
        ret.useLoader = readBooleanInternal(json.get(N_USELOADER));
        ret.useSignature = readBooleanInternal(json.get(N_USESIGNATURE));
        return ret;
    }

    /**
     * Encode an MBeanInfoWrapper instance as JSON:
     * {
     * "className" : String,
     * "description" : String,
     * "descriptor" : Descriptor,
     * "attributes" : [ MBeanAttributeInfo* ],
     * "attributes_URL" : URL,
     * "constructors" : [ MBeanConstructorInfo* ],
     * "notifications" : [ MBeanNotificationInfo* ],
     * "operations" : [ MBeanOperationInfo* ]
     * }
     *
     * Descriptor:
     * {
     * "names" : [ String* ],{
     * "values" : [ POJO* ]
     * }
     *
     * MBeanAttributeInfo:
     * {
     * "name" : String,
     * "type" : String,
     * "description" : String,
     * "descriptor" : Descriptor,
     * "isIs" : Boolean,
     * "isReadable" : Boolean,
     * "isWritable" : Boolean,
     * "URL" : URL
     * }
     *
     * MBeanConstructorInfo:
     * {
     * "name" : String,
     * "description" : String,
     * "descriptor" : Descriptor,
     * "signature" : [ MBeanParameterInfo* ]
     * }
     *
     * MBeanParameterInfo:
     * {
     * "name" : String,
     * "type" : String,
     * "description" : String,
     * "descriptor" : Descriptor
     * }
     *
     * MBeanNotificationInfo:
     * {
     * "name" : String,
     * "description" : String,
     * "descriptor" : Descriptor,
     * "notifTypes" [ String* ]
     * }
     *
     * MBeanOperationInfo:
     * {
     * "name" : String,
     * "description" : String,
     * "descriptor" : Descriptor,
     * "impact" : Integer,
     * "returnType" : String,
     * "signature" : [ MBeanParameterInfo* ],
     * "URI" : URI
     * }
     *
     * @param out The stream to write JSON to
     * @param value The MBeanInfoWrapper instance to encode.
     *            The value and its members can't be null.
     * @throws IOException If an I/O error occurs
     * @see #readMBeanInfo(InputStream)
     */
    public void writeMBeanInfo(OutputStream out, MBeanInfoWrapper value) throws IOException {
        // TODO: MBeanInfo has 2 sub-classes, Model*Support and Open*Support.
        // How to handle them? "Open" has references to OpenTMBean*Info.
        // Model has more convenience methods for retrieving individual
        // items, and methods to set the descriptors.
        // Same for subclasses of the various items.
        writeStartObject(out);
        if (USE_BASE64_FOR_MBEANINFO) {
            writeSerializedField(out, OM_SERIALIZED, value.mbeanInfo);
            writeStringField(out, OM_ATTRIBUTES_URL, value.attributesURL);
            writeSerializedField(out, OM_ATTRIBUTES, value.attributeURLs);
            writeSerializedField(out, OM_OPERATIONS, value.operationURLs);
            return;
        }
        if (value.mbeanInfo.getClass() != MBeanInfo.class) {
            writeSerializedField(out, OM_SERIALIZED, value.mbeanInfo);
        }
        writeStringField(out, OM_CLASSNAME, value.mbeanInfo.getClassName());
        writeStringField(out, OM_DESCRIPTION, value.mbeanInfo.getDescription());
        writeDescriptor(out, OM_DESCRIPTOR, value.mbeanInfo.getDescriptor());
        writeAttributes(out, OM_ATTRIBUTES, value.mbeanInfo.getAttributes(), value.attributeURLs);
        writeStringField(out, OM_ATTRIBUTES_URL, value.attributesURL);
        writeConstructors(out, OM_CONSTRUCTORS, value.mbeanInfo.getConstructors());
        writeNotifications(out, OM_NOTIFICATIONS, value.mbeanInfo.getNotifications());
        writeOperations(out, OM_OPERATIONS, value.mbeanInfo.getOperations(), value.operationURLs);
        writeEndObject(out);
    }

    /**
     * Decode a JSON document to retrieve an MBeanInfoWrapper instance.
     *
     * Note that all descriptors are of class ImmutableDescriptor.
     *
     * @param in The stream to read JSON from
     * @return The decoded MBeanInfoWrapper instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writeMBeanInfo(OutputStream, MBeanInfoWrapper)
     */
    @SuppressWarnings("unchecked")
    public MBeanInfoWrapper readMBeanInfo(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        JSONObject json = parseObject(in);
        MBeanInfoWrapper ret = new MBeanInfoWrapper();
        if (USE_BASE64_FOR_MBEANINFO) {
            Object o = readSerialized(json.get(N_SERIALIZED));
            if (!(o instanceof MBeanInfo)) {
                throwConversionException("readMBeanInfo() receives an instance that's not a MBeanInfo.", json.get(N_SERIALIZED));
            }
            ret.mbeanInfo = (MBeanInfo) o;
            ret.attributesURL = readStringInternal(json.get(N_ATTRIBUTES_URL));
            o = readSerialized(json.get(OM_ATTRIBUTES));
            if (!(o instanceof HashMap)) {
                throwConversionException("readMBeanInfo() receives an instance that's not a HashMap.", json.get(OM_ATTRIBUTES));
            }
            ret.attributeURLs = (Map<String, String>) o;
            o = readSerialized(json.get(OM_OPERATIONS));
            if (!(o instanceof HashMap)) {
                throwConversionException("readMBeanInfo() receives an instance that's not a HashMap.", json.get(OM_OPERATIONS));
            }
            ret.operationURLs = (Map<String, String>) o;
            return ret;
        }
        ret.attributeURLs = new HashMap<String, String>();
        ret.operationURLs = new HashMap<String, String>();
        String className = readStringInternal(json.get(N_CLASSNAME));
        String description = readStringInternal(json.get(N_DESCRIPTION));
        Descriptor descriptor = readDescriptor(json.get(N_DESCRIPTOR));
        MBeanAttributeInfo[] attributes = readAttributes(json.get(N_ATTRIBUTES), ret.attributeURLs);
        String attributeURL = readStringInternal(json.get(N_ATTRIBUTES_URL));
        MBeanConstructorInfo[] constructors = readConstructors(json.get(N_CONSTRUCTORS));
        MBeanNotificationInfo[] notifications = readNotifications(json.get(N_NOTIFICATIONS));
        MBeanOperationInfo[] operations = readOperations(json.get(N_OPERATIONS), ret.operationURLs);
        ret.attributesURL = attributeURL;
        Object o = json.get(N_SERIALIZED);
        if (o != null) {
            o = readSerialized(o);
            if (!(o instanceof MBeanInfo)) {
                throwConversionException("readMBeanInfo() receives an instance that's not a MBeanInfo.", json.get(N_SERIALIZED));
            }
            ret.mbeanInfo = (MBeanInfo) o;
        } else {
            ret.mbeanInfo = new MBeanInfo(className, description, attributes, constructors, operations, notifications, descriptor);
        }
        return ret;
    }

    /**
     * Encode an AttributeList instance as JSON:
     * [ {
     * "name" : String,
     * "value" : POJO
     * }* ]
     *
     * @param out The stream to write JSON to
     * @param value The AttributeList instance to encode. Can be null,
     *            but its Attribute items can't be null.
     * @throws IOException If an I/O error occurs
     * @see #readAttributeList(InputStream)
     */
    public void writeAttributeList(OutputStream out, AttributeList value) throws IOException {
        // AttributeList has no known sub-class.
        writeStartArray(out);
        if (value != null) {
            for (Attribute item : value.asList()) {
                writeArrayItem(out);
                writeStartObject(out);
                writeStringField(out, OM_NAME, item.getName());
                writePOJOField(out, OM_VALUE, item.getValue());
                writeEndObject(out);
            }
        }
        writeEndArray(out);
    }

    /**
     * Decode a JSON document to retrieve an AttributeList instance.
     *
     * @param in The stream to read JSON from
     * @return The decoded AttributeList instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writeAttributeList(OutputStream, AttributeList)
     */
    public AttributeList readAttributeList(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        JSONArray json = parseArray(in);
        AttributeList ret = new AttributeList();
        for (Object item : json) {
            if (!(item instanceof JSONObject)) {
                throwConversionException("readAttributeList() receives an items that's not a JSONObject.", item);
            }
            JSONObject jo = (JSONObject) item;
            String name = readStringInternal(jo.get(N_NAME));
            Object value = readPOJOInternal(jo.get(N_VALUE));
            ret.add(new Attribute(name, value));
        }
        return ret;
    }

    /**
     * Encode an Invocation instance as JSON:
     * {
     * "params" : [ POJO* ],
     * "signature" : [ String* ]
     * }
     *
     * @param out The stream to write JSON to
     * @param value The Invocation instance to encode. Can't be null.
     * @throws IOException If an I/O error occurs
     * @see #readInvocation(InputStream)
     */
    public void writeInvocation(OutputStream out, Invocation value) throws IOException {
        writeStartObject(out);
        writePOJOArrayField(out, OM_PARAMS, value.params);
        writeStringArrayField(out, OM_SIGNATURE, value.signature);
        writeEndObject(out);
    }

    /**
     * Decode a JSON document to retrieve an Invocation instance.
     *
     * @param in The stream to read JSON from
     * @return The decoded Invocation instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writeInvocation(OutputStream, Invocation)
     */
    public Invocation readInvocation(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        JSONObject json = parseObject(in);
        Invocation ret = new Invocation();
        ret.params = readPOJOArray(json.get(N_PARAMS));
        ret.signature = readStringArrayInternal(json.get(N_SIGNATURE));
        return ret;
    }

    /**
     * Encode a NotificationArea instance as JSON:
     * {
     * "registrations" : URL,
     * "serverRegistrations" : URL,
     * "inbox" : URL
     * }
     *
     * @param out The stream to write JSON to
     * @param value The NotificationArea instance to encode. Can't be null.
     * @throws IOException If an I/O error occurs
     * @see #readNotificationArea(InputStream)
     */
    public void writeNotificationArea(OutputStream out, NotificationArea value) throws IOException {
        writeStartObject(out);
        writeStringField(out, OM_REGISTRATIONS, value.registrationsURL);
        writeStringField(out, OM_SERVERREGISTRATIONS, value.serverRegistrationsURL);
        writeStringField(out, OM_INBOX, value.inboxURL);
        writeStringField(out, OM_CLIENT, value.clientURL);
        writeEndObject(out);
    }

    /**
     * Decode a JSON document to retrieve a NotificationArea instance.
     *
     * @param in The stream to read JSON from
     * @return The decoded NotificationArea instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @see #writeNotificationArea(OutputStream, NotificationArea)
     */
    public NotificationArea readNotificationArea(InputStream in) throws ConversionException, IOException {
        JSONObject json = parseObject(in);
        NotificationArea ret = new NotificationArea();
        ret.registrationsURL = readStringInternal(json.get(N_REGISTRATIONS));
        ret.serverRegistrationsURL = readStringInternal(json.get(N_SERVERREGISTRATIONS));
        ret.inboxURL = readStringInternal(json.get(N_INBOX));
        ret.clientURL = readStringInternal(json.get(N_CLIENT));
        return ret;
    }

    /**
     * Encode a NotificationRegistration instance as JSON:
     * {
     * "objectName" : ObjectName,
     * "filters" : [ NotificationFilter* ]
     * }
     *
     * @param out The stream to write JSON to
     * @param value The NotificationRegistration instance to encode.
     *            Can't be null. See writeNotificationFilters() for
     *            requirements on the filters.
     * @throws IOException If an I/O error occurs
     * @see #readNotificationRegistration(InputStream)
     * @see #writeNotificationFilters(OutputStream, NotificationFilter[])
     */
    public void writeNotificationRegistration(OutputStream out, NotificationRegistration value) throws IOException {
        writeStartObject(out);
        writeObjectNameField(out, OM_OBJECTNAME, value.objectName);
        writeNotificationFiltersField(out, OM_FILTERS, value.filters);
        writeEndObject(out);
    }

    /**
     * Decode a JSON document to retrieve a NotificationRegistration instance.
     *
     * @param in The stream to read JSON from
     * @return The decoded NotificationRegistration instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writeNotificationRegistration(OutputStream, NotificationRegistration)
     */
    public NotificationRegistration readNotificationRegistration(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        JSONObject json = parseObject(in);
        NotificationRegistration ret = new NotificationRegistration();
        ret.objectName = readObjectName(json.get(N_OBJECTNAME));
        ret.filters = readNotificationFiltersInternal(json.get(N_FILTERS));
        return ret;
    }

    /**
     * Encode a ServerNotificationRegistration instance as JSON:
     * {
     * "operation" : ("Add" | "RemoveAll" | "RemoveSpecific")
     * "objectName" : ObjectName,
     * "listener" : ObjectName,
     * "filter" : NotificationFilter,
     * "handback" : POJO,
     * "filterID" : Integer,
     * "handbackID" : Integer
     * }
     *
     * @param out The stream to write JSON to
     * @param value The ServerNotificationRegistration instance to encode.
     *            Can't be null.
     * @throws IOException If an I/O error occurs
     * @see #readServerNotificationRegistration(InputStream)
     */
    public void writeServerNotificationRegistration(OutputStream out, ServerNotificationRegistration value) throws IOException {
        writeStartObject(out);
        boolean hasOperation = value.operation != null;
        if (hasOperation) {
            writeSimpleStringField(out, OM_OPERATION, value.operation.name());
        }
        writeObjectNameField(out, OM_OBJECTNAME, value.objectName);
        writeObjectNameField(out, OM_LISTENER, value.listener);
        writeNotificationFilterField(out, OM_FILTER, value.filter);
        writePOJOField(out, OM_HANDBACK, value.handback);
        if (hasOperation) {
            writeIntField(out, OM_FILTERID, value.filterID);
            writeIntField(out, OM_HANDBACKID, value.handbackID);
        }
        writeEndObject(out);
    }

    /**
     * Decode a JSON document to retrieve a ServerNotificationRegistration instance.
     *
     * @param in The stream to read JSON from
     * @return The decoded ServerNotificationRegistration instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writeServerNotificationRegistration(OutputStream, ServerNotificationRegistration)
     */
    public ServerNotificationRegistration readServerNotificationRegistration(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        JSONObject json = parseObject(in);
        ServerNotificationRegistration ret = new ServerNotificationRegistration();
        String name = readStringInternal(json.get(N_OPERATION));
        ret.operation = name != null ? Operation.valueOf(name) : null;
        ret.objectName = readObjectName(json.get(N_OBJECTNAME));
        ret.listener = readObjectName(json.get(N_LISTENER));
        ret.filter = readNotificationFilterInternal(json.get(N_FILTER), true);
        ret.handback = readPOJOInternal(json.get(N_HANDBACK));
        ret.filterID = readIntInternal(json.get(N_FILTERID));
        ret.handbackID = readIntInternal(json.get(N_HANDBACKID));
        return ret;
    }

    /**
     * Check if a NotificationFilter is a standard filter that can be
     * send to a JMX server.
     *
     * @param filter The filter to check. Can't be null.
     * @return Whether the filter is a standard one
     */
    public boolean isSupportedNotificationFilter(NotificationFilter filter) {
        Class<?> clazz = filter.getClass();
        return clazz == AttributeChangeNotificationFilter.class ||
               clazz == MBeanServerNotificationFilter.class ||
               clazz == NotificationFilterSupport.class;
    }

    /**
     * Encode a NotificationFilter array as JSON:
     * [ NotificationFilter* ]
     *
     * Format of NotificationFilter:
     * {
     * className: "AttributeChangeNotificationFilter" | "MBeanServerNotificationFilter" | "NotificationFilterSupport",
     * enabled: [ String* ], // For AttributeChangeNotificationFilter and MBeanServerNotificationFilter
     * disabled: [ String* ], // For MBeanServerNotificationFilter
     * types: [ String* ], // For MBeanServerNotificationFilter and NotificationFilterSupport
     * }
     *
     * @param out The stream to write JSON to
     * @param value The NotificationFilter array to encode. Can be null.
     *            The individual filters can't be null either. They must be of
     *            one of the 3 known filter classes.
     * @throws IOException If an I/O error occurs
     * @see #readNotificationFilters(InputStream)
     */
    public void writeNotificationFilters(OutputStream out, NotificationFilter[] value) throws IOException {
        writeNotificationFiltersInternal(out, value);
    }

    /**
     * Decode a JSON document to retrieve a NotificationFilter array.
     *
     * @param in The stream to read JSON from
     * @return The decoded NotificationFilter array
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writeNotificationFilters(OutputStream, NotificationFilter[])
     */
    public NotificationFilter[] readNotificationFilters(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        return readNotificationFiltersInternal(parseArray(in));
    }

    /**
     * Encode an array of Notification instance as JSON:
     *
     * @param out The stream to write JSON to
     * @param value The Notification array to encode. Value can be null,
     *            but its items can't be null.
     *            The "source" of the items must be an instance of ObjectName.
     * @throws IOException If an I/O error occurs
     * @see #readNotifications(InputStream)
     */
    public void writeNotifications(OutputStream out, Notification[] value) throws IOException {
        final NotificationRecord[] records;
        if (value != null) {
            records = new NotificationRecord[value.length];
            for (int i = 0; i < value.length; ++i) {
                Notification n = value[i];
                if (n != null) {
                    Object source = n.getSource();
                    NotificationRecord nr;
                    if (source instanceof ObjectName) {
                        nr = new NotificationRecord(n, (ObjectName) source);
                    } else {
                        nr = new NotificationRecord(n, (source != null) ? source.toString() : null);
                    }
                    records[i] = nr;
                }
            }
        } else {
            records = null;
        }
        writeNotificationRecords(out, records);
    }

    /**
     * Encode an array of NotificationRecord instance as JSON:
     * [ {
     * "className" : "String", // The class name of the Notification object
     * "serialized" : "Base64", // If the object is not of one of the known
     * // classes, then base64 serialize it.
     * "type" : String,
     * "source" : POJO,
     * "sequenceNumber" : Long,
     * "timeStamp" : Long,
     * "message" : String,
     * "userData" : POJO,
     *
     * "hostName" : String, // For routed Notifications only
     * "serverName" : String,
     * "serverUserDir" : String,
     *
     * "attributeName" : String, // For AttributeChangeNotification only
     * "attributeType" : String,
     * "oldValue", POJO,
     * "newValue", POJO,
     *
     * "connectionId" : String, // For JMXConnectionNotification
     *
     * "mbeanName" : String, // For MBeanServerNotification
     *
     * "relationId" : String, // For RelationNotification
     * "relationTypeName" : String,
     * "objectName" : String,
     * "mbeansToUnregister" : [ String* ],
     * "roleName" : String,
     * "oldRoleValue" : [String*],
     * "newRoleValue" : [String*],
     *
     * "notificationID" : Integer, // For TimerNotification
     * } * ]
     *
     * @param out The stream to write JSON to
     * @param value The NotificationRecord array to encode. Value can be null,
     *            but its items can't be null.
     *            The "source" of the items must be an instance of ObjectName.
     * @throws IOException If an I/O error occurs
     * @see #readNotificationRecords(InputStream)
     */
    public void writeNotificationRecords(OutputStream out, NotificationRecord[] value) throws IOException {
        writeStartArray(out);
        if (value == null) {
            writeEndArray(out);
            return;
        }

        for (NotificationRecord nr : value) {
            Notification item = nr.getNotification();
            writeArrayItem(out);
            writeStartObject(out);

            // Fields common to all notification classes
            Class<?> clazz = item.getClass();
            writeSimpleStringField(out, OM_CLASSNAME, clazz.getName());
            writeStringField(out, OM_TYPE, item.getType());
            ObjectName on = null;
            if (item.getSource() instanceof String) {
                try {
                    on = new ObjectName((String) item.getSource());
                } catch (Exception e) {
                }
            } else {
                on = (ObjectName) item.getSource();
            }

            writeObjectNameField(out, OM_SOURCE, on);
            writeLongField(out, OM_SEQUENCENUMBER, item.getSequenceNumber());
            writeLongField(out, OM_TIMESTAMP, item.getTimeStamp());
            writeStringField(out, OM_MESSAGE, item.getMessage());
            writePOJOField(out, OM_USERDATA, item.getUserData());

            // Write routing information (host name, server name, server user dir) if this is a routed notification.
            final Map<String, Object> routingInfo = nr.getNotificationTargetInformation().getRoutingInformation();
            if (routingInfo != null) {
                String hostName = (String) routingInfo.get(ClientProvider.ROUTING_KEY_HOST_NAME);
                String serverName = (String) routingInfo.get(ClientProvider.ROUTING_KEY_SERVER_NAME);
                String serverUserDir = (String) routingInfo.get(ClientProvider.ROUTING_KEY_SERVER_USER_DIR);

                writeStringField(out, OM_HOSTNAME, hostName);
                writeStringField(out, OM_SERVERNAME, serverName);
                writeStringField(out, OM_SERVERUSERDIR, serverUserDir);
            }

            if (clazz == Notification.class) {
            } else if (clazz == AttributeChangeNotification.class) {
                AttributeChangeNotification v = (AttributeChangeNotification) item;
                writeStringField(out, OM_ATTRIBUTENAME, v.getAttributeName());
                writeStringField(out, OM_ATTRIBUTETYPE, v.getAttributeType());
                writePOJOField(out, OM_OLDVALUE, v.getOldValue());
                writePOJOField(out, OM_NEWVALUE, v.getNewValue());
            } else if (clazz == JMXConnectionNotification.class) {
                JMXConnectionNotification v = (JMXConnectionNotification) item;
                writeStringField(out, OM_CONNECTIONID, v.getConnectionId());
            } else if (clazz == MBeanServerNotification.class) {
                MBeanServerNotification v = (MBeanServerNotification) item;
                writeObjectNameField(out, OM_MBEANNAME, v.getMBeanName());
            } else if (clazz == RelationNotification.class) {
                RelationNotification v = (RelationNotification) item;
                writeStringField(out, OM_RELATIONID, v.getRelationId());
                writeStringField(out, OM_RELATIONTYPENAME, v.getRelationTypeName());
                writeObjectNameField(out, OM_OBJECTNAME, v.getObjectName());
                String roleName = v.getRoleName();
                if (roleName == null) {
                    writeObjectNameListField(out, OM_MBEANSTOUNREGISTER, v.getMBeansToUnregister());
                } else {
                    writeStringField(out, OM_ROLENAME, v.getRoleName());
                    writeObjectNameListField(out, OM_OLDROLEVALUE, v.getOldRoleValue());
                    writeObjectNameListField(out, OM_NEWROLEVALUE, v.getNewRoleValue());
                }
            } else if (clazz == TimerNotification.class) {
                TimerNotification v = (TimerNotification) item;
                writeIntField(out, OM_NOTIFICATIONID, v.getNotificationID());
            } else {
                // MonitorNotification and TimerAlarmClockNotification are
                // known, but either the class or the constructor is not
                // visible, and we have to use base64.
                // SnmpTableEntryNotification is a sun specific implementation.
                writeSerializedField(out, OM_SERIALIZED, item);
            }

            writeEndObject(out);
        }
        writeEndArray(out);
    }

    /**
     * Decode a JSON document to retrieve an array of Notification instances.
     *
     * @param in The stream to read JSON from
     * @return The decoded Notification array
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writeNotifications(OutputStream, Notification[])
     */
    public Notification[] readNotifications(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        final NotificationRecord[] records = readNotificationRecords(in);
        final Notification[] ret = new Notification[records.length];
        for (int i = 0; i < records.length; ++i) {
            ret[i] = records[i].getNotification();
        }
        return ret;
    }

    /**
     * Decode a JSON document to retrieve an array of NotificationRecord instances.
     *
     * @param in The stream to read JSON from
     * @return The non-null decoded NotificationRecord array
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writeNotificationRecords(OutputStream, NotificationRecord[])
     */
    public NotificationRecord[] readNotificationRecords(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        JSONArray json = parseArray(in);
        int size = json.size();
        NotificationRecord[] ret = new NotificationRecord[size];
        for (int i = 0; i < size; i++) {
            Object o = json.get(i);
            if (!(o instanceof JSONObject)) {
                throwConversionException("readNotifications() expects a JSONObject.", o);
            }
            JSONObject obj = (JSONObject) o;

            // Fields common to all Notification classes.
            String className = readStringInternal(obj.get(N_CLASSNAME));
            String type = readStringInternal(obj.get(N_TYPE));
            ObjectName source = readObjectName(obj.get(N_SOURCE));
            long sequenceNumber = readLongInternal(obj.get(N_SEQUENCENUMBER));
            long timeStamp = readLongInternal(obj.get(N_TIMESTAMP));
            String message = readStringInternal(obj.get(N_MESSAGE));
            Object userData = readPOJOInternal(obj.get(N_USERDATA));

            // Read routing information (host name, server name, server user dir) if this is a routed notification.
            String hostName = readStringInternal(obj.get(N_HOSTNAME));
            String serverName;
            String serverUserDir;
            if (hostName != null) {
                serverName = readStringInternal(obj.get(N_SERVERNAME));
                serverUserDir = readStringInternal(obj.get(N_SERVERUSERDIR));
            } else {
                serverName = null;
                serverUserDir = null;
            }

            Notification n;
            if ("javax.management.Notification".equals(className)) {
                n = new Notification(type, source, sequenceNumber, timeStamp, message);
            } else if ("javax.management.AttributeChangeNotification".equals(className)) {
                if (!AttributeChangeNotification.ATTRIBUTE_CHANGE.equals(type)) {
                    throwConversionException("Type for AttributeChangeNotification should be ATTRIBUTE_CHANGE", o);
                }
                String attributeName = readStringInternal(obj.get(N_ATTRIBUTENAME));
                String attributeType = readStringInternal(obj.get(N_ATTRIBUTETYPE));
                Object oldValue = readPOJOInternal(obj.get(N_OLDVALUE));
                Object newValue = readPOJOInternal(obj.get(N_NEWVALUE));
                n = new AttributeChangeNotification(source, sequenceNumber, timeStamp, message, attributeName, attributeType, oldValue, newValue);
            } else if ("javax.management.remote.JMXConnectionNotification".equals(className)) {
                String connectionId = readStringInternal(obj.get(N_CONNECTIONID));
                n = new JMXConnectionNotification(type, source, connectionId, sequenceNumber, message, userData);
                // Replace the value set by the constructor
                n.setTimeStamp(timeStamp);
            } else if ("javax.management.MBeanServerNotification".equals(className)) {
                ObjectName objectName = readObjectName(obj.get(N_MBEANNAME));
                n = new MBeanServerNotification(type, source, sequenceNumber, objectName);
                // Replace the value set by the constructor
                n.setTimeStamp(timeStamp);
            } else if ("javax.management.relation.RelationNotification".equals(className)) {
                String relationId = readStringInternal(obj.get(N_RELATIONID));
                String typeName = readStringInternal(obj.get(N_RELATIONTYPENAME));
                ObjectName objectName = readObjectName(obj.get(N_OBJECTNAME));
                String roleName = readStringInternal(obj.get(N_ROLENAME));
                if (roleName == null) {
                    List<ObjectName> mbeansToUnregister = readObjectNameList(obj.get(N_MBEANSTOUNREGISTER));
                    n = new RelationNotification(type, source, sequenceNumber, timeStamp, message, relationId, typeName, objectName, mbeansToUnregister);
                } else {
                    List<ObjectName> oldValue = readObjectNameList(obj.get(N_OLDROLEVALUE));
                    List<ObjectName> newValue = readObjectNameList(obj.get(N_NEWROLEVALUE));
                    n = new RelationNotification(type, source, sequenceNumber, timeStamp, message, relationId, typeName, objectName, roleName, oldValue, newValue);
                }
            } else if ("javax.management.timer.TimerNotification".equals(className)) {
                Integer notificationID = readIntInternal(obj.get(N_NOTIFICATIONID));
                n = new TimerNotification(type, source, sequenceNumber, timeStamp, message, notificationID);
            } else {
                Object serialized = readSerialized(obj.get(N_SERIALIZED));
                if (!(serialized instanceof Notification)) {
                    throwConversionException("readNotifications() expects a Notification.", o);
                }
                n = (Notification) serialized;
            }

            n.setUserData(userData);

            final NotificationRecord nr;
            if (hostName != null) {
                nr = new NotificationRecord(n, source, hostName, serverName, serverUserDir);
            } else {
                nr = new NotificationRecord(n, source);
            }
            ret[i] = nr;
        }
        return ret;
    }

    /**
     * Encode a NotificationSettings instance as JSON:
     * {
     * "deliveryInterval" : Integer
     * }
     *
     * @param out The stream to write JSON to
     * @param value The NotificationSettings instance to encode. Can't be null.
     * @throws IOException If an I/O error occurs
     * @see #readNotificationSettings(InputStream)
     */
    public void writeNotificationSettings(OutputStream out, NotificationSettings value) throws IOException {
        writeStartObject(out);
        writeIntField(out, OM_DELIVERYINTERVAL, value.deliveryInterval);
        writeIntField(out, OM_INBOXEXPIRTY, value.inboxExpiry);
        writeEndObject(out);
    }

    /**
     * Decode a JSON document to retrieve a NotificationSettings instance.
     *
     * @param in The stream to read JSON from
     * @return The decoded NotificationSettings instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @see #writeNotificationSettings(OutputStream, NotificationSettings)
     */
    public NotificationSettings readNotificationSettings(InputStream in) throws ConversionException, IOException {
        JSONObject json = parseObject(in);
        NotificationSettings ret = new NotificationSettings();
        ret.deliveryInterval = readIntInternal(json.get(N_DELIVERYINTERVAL));
        ret.inboxExpiry = readIntInternal(json.get(N_INBOXEXPIRY));
        return ret;
    }

    /**
     * Encode a Throwable instance as JSON:
     * {
     * "throwable" : Base64,
     * "error" : String - the exception message
     * }
     *
     * @param out The stream to write JSON to
     * @param value The Throwable instance to encode. Can't be null.
     * @throws IOException If an I/O error occurs
     * @see #readThrowable(InputStream)
     */
    public void writeThrowable(OutputStream out, Throwable value) throws IOException {
        writeStartObject(out);
        writeSerializedField(out, OM_THROWABLE, value);
        writeStringField(out, OM_EXCEPTION_MESSAGE, value.getMessage());
        writeEndObject(out);
    }

    /**
     * Decode a JSON document to retrieve a Throwable instance.
     *
     * @param in The stream to read JSON from
     * @return The decoded Throwable instance
     * @throws ConversionException If JSON uses unexpected structure/format
     * @throws IOException If an I/O error occurs or if JSON is ill-formed.
     * @throws ClassNotFoundException If needed class can't be found.
     * @see #writeThrowable(OutputStream, Throwable)
     */
    public Throwable readThrowable(InputStream in) throws ConversionException, IOException, ClassNotFoundException {
        byte[] byteInputStream = convertInputStreamToBytes(in);
        ByteArrayInputStream bais = new ByteArrayInputStream(byteInputStream);
        JSONObject json = null;
        try {
            json = parseObject(bais);
        } catch (IOException ex) {
            bais.reset();
            throw new RuntimeException(convertStreamToString(bais));
        }
        Object t = readSerialized(json.get(N_THROWABLE));
        if (!(t instanceof Throwable)) {
            throwConversionException("readThrowable() receives an instance that's not a Throwable.", json.get(N_THROWABLE));
        }
        return (Throwable) t;
    }

    /**
     * Converts inputstream to bytearray
     *
     * @param in
     * @return
     * @throws IOException
     */
    private byte[] convertInputStreamToBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int len;
        byte[] data = new byte[16384];

        while ((len = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, len);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * Converts inputstream to String
     *
     * @param in
     * @return
     * @throws IOException
     */

    private String convertStreamToString(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder sb = new StringBuilder();
        String line = null;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        input.close();
        return sb.toString();
    }

    /**
     * Encode a String in base64. The content of the string is first encoded
     * as UTF-8 bytes, the bytes are then base64 encoded. The resulting base64
     * value is returned as a String.
     *
     * @param value The String to encode
     * @return The encoded base64 String
     * @throws ConversionException If the String content can not be UTF-8 encoded.
     */
    public String encodeStringAsBase64(String value) throws ConversionException {
        try {
            return encodeStringAsBase64Internal(value);
        } catch (IOException e) {
            // Will never happen
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private String encodeStringAsBase64Internal(String value) throws ConversionException, IOException {
        final Base64OutputStream out = base64Stream;
        byteArrayOS.reset();
        out.start(byteArrayOS);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < 0x80) {
                // 1-byte
                out.write(ch);
            } else if (ch < 0x800) {
                // 2-byte
                out.write(0xC0 | (ch >> 6));
                out.write(0x80 | (0x3F & ch));
            } else if (ch < 0xD800 || ch >= 0xE000) {
                // 3-byte
                out.write(0xE0 | (ch >> 12));
                out.write(0x80 | (0x3F & (ch >> 6)));
                out.write(0x80 | (0x3F & ch));
            } else if (ch < 0xDC00) {
                // 4-byte. Surrogate pair.
                if (i == value.length() - 1) {
                    // Error if second half is missing
                    utf8EncodeError(value);
                } else {
                    char ch2 = value.charAt(i + 1);
                    if (ch2 >= 0xDC00 && ch2 < 0xE000) {
                        int c = 0x00010000 + ((ch - 0xD800) << 10) + (ch2 - 0xDC00);
                        out.write(0xF0 | (c >> 18)); /* no mask needed */
                        out.write(0x80 | (0x3F & (c >> 12)));
                        out.write(0x80 | (0x3F & (c >> 6)));
                        out.write(0x80 | (0x3F & c));
                        i++;
                    } else {
                        // Error if second half is out of range
                        utf8EncodeError(value);
                    }
                }
            } else {
                // Error if first half is out of range
                utf8EncodeError(value);
            }
        }
        out.end();
        // Base64 encoded String only has ASCII characters.
        return byteArrayOS.toString(0);
    }

    private void writeStartObject(OutputStream out) throws IOException {
        out.write('{');
        firstItem = true;
    }

    private void writeFieldName(OutputStream out, byte[] name) throws IOException {
        if (firstItem) {
            firstItem = false;
        } else {
            out.write(',');
        }
        out.write(name);
    }

    private void writeSimpleFieldName(OutputStream out, String name) throws IOException {
        if (firstItem) {
            firstItem = false;
        } else {
            out.write(',');
        }
        writeSimpleString(out, name);
        out.write(':');
    }

    private void escapeFieldName(OutputStream out, String name) throws IOException {
        if (firstItem) {
            firstItem = false;
        } else {
            out.write(',');
        }
        writeStringInternal(out, name);
        out.write(':');
    }

    private void writeEndObject(OutputStream out) throws IOException {
        out.write('}');
        firstItem = false;
    }

    private void writeStartArray(OutputStream out) throws IOException {
        out.write('[');
        firstItem = true;
    }

    private void writeArrayItem(OutputStream out) throws IOException {
        if (firstItem) {
            firstItem = false;
        } else {
            out.write(',');
        }
    }

    private void writeEndArray(OutputStream out) throws IOException {
        out.write(']');
        firstItem = false;
    }

    /**
     * Debug (dump) the contents of an input stream. This effectively copies
     * the input stream into a byte array, dumps one and then returns another.
     * If logging is not enabled, nothing happens.
     *
     * @param in
     * @throws IOException
     */
    private InputStream debugInputStream(InputStream in) throws IOException {
        if (logger.isLoggable(Level.FINEST)) {
            final byte[] buffer = new byte[1024];
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len;
            String line;

            logger.finest("[START] Dumping the InputStream that will be passed to JSON.parse()");
            while ((len = in.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();

            ByteArrayInputStream dumpIn = new ByteArrayInputStream(baos.toByteArray());
            BufferedReader br = new BufferedReader(new InputStreamReader(dumpIn));
            while ((line = br.readLine()) != null) {
                logger.finest(line);
            }
            logger.finest("[END] Dumping the InputStream that will be passed to JSON.parse()");

            return new ByteArrayInputStream(baos.toByteArray());
        } else {
            return in;

        }
    }

    /**
     * Parse (and possible debug) the InputStream as a JSONArtifact.
     *
     * @param in
     * @return
     * @throws IOException
     */
    private JSONArtifact debugAndParse(InputStream in) throws IOException {
        return JSON.parse(debugInputStream(in));
    }

    private Object parse(InputStream in) throws IOException {
        return debugAndParse(in);
    }

    private JSONObject parseObject(InputStream in) throws ConversionException, IOException {
        JSONArtifact json = debugAndParse(in);
        if (!(json instanceof JSONObject)) {
            throwConversionException("parseObject() receives an instance that's not a JSONObject.", json);
        }
        return (JSONObject) json;
    }

    private JSONArray parseArray(InputStream in) throws ConversionException, IOException {
        JSONArtifact json = debugAndParse(in);
        if (!(json instanceof JSONArray)) {
            throwConversionException("parseArray() receives an instance that's not a JSONArray.", json);
        }
        return (JSONArray) json;
    }

    private byte readByteInternal(Object in) throws ConversionException {
        int value = readIntInternal(in);
        if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            throwConversionException("readByteInternal() receives an out-of-range value.", in);
        }
        return (byte) value;
    }

    private short readShortInternal(Object in) throws ConversionException {
        int value = readIntInternal(in);
        if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            throwConversionException("readShortInternal() receives an out-of-range value.", in);
        }
        return (short) value;
    }

    private void writeIntField(OutputStream out, byte[] name, int value) throws IOException {
        writeFieldName(out, name);
        writeIntInternal(out, value);
    }

    private void writeIntInternal(OutputStream out, int value) throws IOException {
        out.write('"');
        encodeInt(out, value);
        out.write('"');
    }

    private void encodeInt(OutputStream out, int value) throws IOException {
        if (value < 0) {
            // -Integer.MIN_VALUE is out of "int" range. Special-casing it.
            if (value == Integer.MIN_VALUE) {
                out.write(INT_MIN);
                return;
            }

            // Turn value to positive so that % and / work as expected.
            out.write('-');
            value = -value;
        }

        // Write from right to left
        final byte[] bytes = BYTE_BUFFER;
        int pos = BYTE_BUFFER.length;
        do {
            int q = value / 10;
            int r = value - (q << 3) - (q << 1); // same as (value - q * 10)
            value = q;
            bytes[--pos] = (byte) (r + '0');
        } while (value != 0);

        out.write(bytes, pos, BYTE_BUFFER.length - pos);
    }

    private int readIntInternal(Object in) throws ConversionException {
        if (in == null) {
            return 0;
        }

        if (!(in instanceof String)) {
            throwConversionException("readIntInternal() expects a String.", in);
        }
        try {
            return Integer.parseInt((String) in);
        } catch (NumberFormatException e) {
            throwConversionException(e, in);
            return 0;
        }
    }

    private void writeLongField(OutputStream out, byte[] name, long value) throws IOException {
        writeFieldName(out, name);
        writeLongInternal(out, value);
    }

    private void writeLongInternal(OutputStream out, long value) throws IOException {
        out.write('"');
        encodeLong(out, value);
        out.write('"');
    }

    private void encodeLong(OutputStream out, long value) throws IOException {
        if (value < 0) {
            // -Long.MIN_VALUE is out of "long" range. Special-casing it.
            if (value == Long.MIN_VALUE) {
                out.write(LONG_MIN);
                return;
            }

            // Turn value to positive so that % and / work as expected.
            out.write('-');
            value = -value;
        }

        // Write from right to left
        final byte[] bytes = BYTE_BUFFER;
        int pos = BYTE_BUFFER.length;
        do {
            long q = value / 10;
            long r = value - (q << 3) - (q << 1); // same as (value - q * 10)
            value = q;
            bytes[--pos] = (byte) (r + '0');
        } while (value != 0);

        out.write(bytes, pos, BYTE_BUFFER.length - pos);
    }

    private long readLongInternal(Object in) throws ConversionException {
        if (!(in instanceof String)) {
            throwConversionException("readLongInternal() expects a String.", in);
        }
        try {
            return Long.parseLong((String) in);
        } catch (NumberFormatException e) {
            throwConversionException(e, in);
            return 0;
        }
    }

    private float readFloatInternal(Object in) throws ConversionException {
        if (!(in instanceof String)) {
            throwConversionException("readFloatInternal() expects a String.", in);
        }
        try {
            return Float.parseFloat((String) in);
        } catch (NumberFormatException e) {
            throwConversionException(e, in);
            return 0;
        }
    }

    private double readDoubleInternal(Object in) throws ConversionException {
        if (!(in instanceof String)) {
            throwConversionException("readDoubleInternal() expects a String.", in);
        }
        try {
            return Double.parseDouble((String) in);
        } catch (NumberFormatException e) {
            throwConversionException(e, in);
            return 0;
        }
    }

    private char readCharInternal(Object in) throws ConversionException {
        if (!(in instanceof String)) {
            throwConversionException("readCharInternal() expects a String.", in);
        }
        String str = (String) in;
        if (str.length() != 1) {
            throwConversionException("readCharInternal() expects a String of length 1.", in);
        }
        return str.charAt(0);
    }

    private void writeBooleanField(OutputStream out, byte[] name, boolean value) throws IOException {
        writeFieldName(out, name);
        writeBooleanInternal(out, value);
    }

    private void writeBooleanInternal(OutputStream out, boolean value) throws IOException {
        out.write(value ? TRUE : FALSE);
    }

    private boolean readBooleanInternal(Object in) throws ConversionException {
        if (!(in instanceof Boolean)) {
            throwConversionException("readBooleanInternal() expects a Boolean.", in);
        }
        return ((Boolean) in).booleanValue();
    }

    private void writeSimpleStringField(OutputStream out, byte[] name, CharSequence value) throws IOException {
        writeFieldName(out, name);
        writeSimpleString(out, value);
    }

    // Called for ASCII Strings that don't contain any need-to-escape characters.
    // The value can't be null.
    private void writeSimpleString(OutputStream out, CharSequence value) throws IOException {
        out.write('"');
        for (int i = 0; i < value.length(); i++) {
            out.write(value.charAt(i));
        }
        out.write('"');
    }

    private void writeStringField(OutputStream out, byte[] name, String value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeStringInternal(out, value);
    }

    private void writeStringInternal(OutputStream out, String value) throws IOException {
        if (value == null) {
            out.write(NULL);
            return;
        }

        out.write('"');
        escapeString(out, value);
        out.write('"');
    }

    private void escapeString(OutputStream out, String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            // Escape '"' and '\'
            if (ch == '"') {
                out.write('\\');
                out.write('"');
            } else if (ch == '\\') {
                out.write('\\');
                out.write('\\');
            } else if (ch >= 0x80) {
                // Multi-byte chars
                if (ch < 0x800) {
                    // 2-byte
                    out.write(0xC0 | (ch >> 6));
                    out.write(0x80 | (0x3F & ch));
                } else if (ch < 0xD800 || ch >= 0xE000) {
                    // 3-byte
                    out.write(0xE0 | (ch >> 12));
                    out.write(0x80 | (0x3F & (ch >> 6)));
                    out.write(0x80 | (0x3F & ch));
                } else if (ch < 0xDC00) {
                    // 4-byte. Surrogate pair.
                    if (i == value.length() - 1) {
                        // Error if second half is missing
                        writeInvalidChar(out, ch);
                    } else {
                        char ch2 = value.charAt(i + 1);
                        if (ch2 >= 0xDC00 && ch2 < 0xE000) {
                            int c = 0x00010000 + ((ch - 0xD800) << 10) + (ch2 - 0xDC00);
                            out.write(0xF0 | (c >> 18)); /* no mask needed */
                            out.write(0x80 | (0x3F & (c >> 12)));
                            out.write(0x80 | (0x3F & (c >> 6)));
                            out.write(0x80 | (0x3F & c));
                            i++;
                        } else {
                            // Error if second half is out of range
                            writeInvalidChar(out, ch);
                        }
                    }
                } else {
                    // Error if first half is out of range
                    writeInvalidChar(out, ch);
                }
            } else if (ch >= 0x20) {
                // Common case: ASCII and not special
                out.write(ch);
            } else {
                // Escape control characters.
                out.write('\\');
                switch (ch) {
                    case 8:
                        out.write('b');
                        break;
                    case 9:
                        out.write('t');
                        break;
                    case 0xA:
                        out.write('n');
                        break;
                    case 0xC:
                        out.write('f');
                        break;
                    case 0xD:
                        out.write('r');
                        break;
                    default:
                        out.write('u');
                        out.write('0');
                        out.write('0');
                        out.write(hex(ch >> 4));
                        out.write(hex(ch & 0xf));
                }
            }
        }
    }

    private void writeInvalidChar(OutputStream out, char value) throws IOException {
        out.write('\\');
        out.write('u');
        out.write(hex(value >> 12));
        out.write(hex((value >> 8) & 0xf));
        out.write(hex((value >> 4) & 0xf));
        out.write(hex(value & 0xf));
    }

    private byte hex(int value) {
        return (byte) (value < 10 ? value + '0' : value + 'A' - 10);
    }

    private String readStringInternal(Object in) throws ConversionException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof String)) {
            throwConversionException("readStringInternal() expects a String.", in);
        }
        // The JSON parser already decoded the string.
        return (String) in;
    }

    private void writeStringArrayField(OutputStream out, byte[] name, String[] value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeStringArrayInternal(out, value);
    }

    private void writeStringArrayInternal(OutputStream out, String[] value) throws IOException {
        writeStartArray(out);
        if (value != null) {
            for (String s : value) {
                writeArrayItem(out);
                writeStringInternal(out, s);
            }
        }
        writeEndArray(out);
    }

    private String[] readStringArrayInternal(Object in) throws ConversionException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONArray)) {
            throwConversionException("readStringArrayInternal() expects a JSONArray.", in);
        }
        JSONArray json = (JSONArray) in;
        int size = json.size();
        String[] ret = new String[size];
        for (int i = 0; i < size; i++) {
            ret[i] = readStringInternal(json.get(i));
        }
        return ret;
    }

    private void writeSerializedField(OutputStream out, byte[] name, Object value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeSerialized(out, value);
    }

    private void writeSerialized(OutputStream out, Object value) throws IOException {
        if (value == null) {
            out.write(NULL);
            return;
        }
        out.write('"');
        base64Stream.start(out);
        ObjectOutputStream objectOut = new ObjectOutputStream(base64Stream);
        objectOut.writeObject(value);
        base64Stream.end();
        out.write('"');
    }

    private Object readSerialized(Object in) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof String)) {
            throwConversionException("readSerialized() expects a String.", in);
        }
        String value = (String) in;
        int slen = value.length();
        if (slen == 0 || slen / 4 * 4 != slen) {
            base64DecodeError(in);
        }
        int blen = slen / 4 * 3;
        byte[] binary = new byte[blen];
        int c1 = 0, c2 = 0, c3 = 0, c4 = 0, pos = 0;
        for (int i = 0; i < slen;) {
            c1 = value.charAt(i++);
            c2 = value.charAt(i++);
            c3 = value.charAt(i++);
            c4 = value.charAt(i++);
            if (c1 >= BASE64_D.length || BASE64_D[c1] == -1 ||
                c2 >= BASE64_D.length || BASE64_D[c2] == -1 ||
                c2 >= BASE64_D.length || BASE64_D[c3] == -1 ||
                c2 >= BASE64_D.length || BASE64_D[c4] == -1) {
                base64DecodeError(in);
            }
            c1 = BASE64_D[c1];
            c2 = BASE64_D[c2];
            c3 = BASE64_D[c3];
            c4 = BASE64_D[c4];
            binary[pos++] = (byte) ((c1 << 2) | (c2 >> 4));
            binary[pos++] = (byte) ((c2 << 4) | (c3 >> 2));
            binary[pos++] = (byte) ((c3 << 6) | c4);
        }

        if (value.charAt(slen - 1) == '=') {
            if (binary[--blen] != 0) {
                base64DecodeError(in);
            }
            if (value.charAt(slen - 2) == '=') {
                if (binary[--blen] != 0) {
                    base64DecodeError(in);
                }
            }
        }

        return helper.readObject(in, blen, binary);
    }

    public static void setSerializationHelper(SerializationHelper sh) {
        if (null == sh) {
            helper = defaultHelper;
        } else {
            helper = sh;
        }
    }

    private void writePOJOField(OutputStream out, byte[] name, Object value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writePOJOInternal(out, value);
    }

    private void writePOJOInternal(OutputStream out, Object value) throws IOException {
        if (USE_BASE64_FOR_POJO) {
            writeStartObject(out);
            if (value != null) {
                writeSimpleStringField(out, OM_CLASSNAME, value.getClass().getName());
                writeSerializedField(out, OM_SERIALIZED, value);
            }
            writeEndObject(out);
            return;
        }

        writeStartObject(out);
        // Merge this with the "writeType" method?
        // Then need to write "type" content into the byateArrayOS.
        if (hasCycle(value, new Stack<Object>())) {
            // If the value object contains cycle, then java-serialize it.
            writeSerializedField(out, OM_SERIALIZED, value);
        } else {
            writeFieldName(out, OM_VALUE);
            writePOJOValue(out, value);
            writeFieldName(out, OM_TYPE);
            List<OpenType<?>> openTypes = new ArrayList<OpenType<?>>();
            writePOJOType(out, value, openTypes);
            writeOpenTypes(out, openTypes);
        }
        writeEndObject(out);
    }

    private boolean hasCycle(Object value, Stack<Object> seen) {
        if (value == null) {
            return false;
        }

        for (Object o : seen) {
            if (o == value) {
                return true;
            }
        }

        Class<?> clazz = value.getClass();

        if (clazz.isArray()) {
            Class<?> componentType = clazz.getComponentType();
            if (componentType.isPrimitive()) {
                return false;
            }

            Class<?> leaf = componentType;
            while (leaf.isArray()) {
                leaf = leaf.getComponentType();
            }
            if (SupportedClasses.get(leaf) == null) {
                return false;
            }

            if (SimpleArrays.contains(leaf)) {
                return false;
            }

            seen.push(value);
            for (Object o : (Object[]) value) {
                if (hasCycle(o, seen)) {
                    return true;
                }
            }
            seen.pop();
            return false;
        }

        TYPE type = SupportedClasses.get(clazz);
        if (type == null) {
            return false;
        }

        switch (type) {
            case Byte:
            case Short:
            case Integer:
            case Long:
            case Float:
            case Double:
            case Character:
            case Boolean:
            case String:
            case BigInteger:
            case BigDecimal:
            case Date:
            case Object:
                return false;
            case Collection:
                seen.push(value);
                for (Object o : (Collection<?>) value) {
                    if (hasCycle(o, seen)) {
                        return true;
                    }
                }
                seen.pop();
                return false;
            case Map:
                Map<?, ?> map = (Map<?, ?>) value;
                seen.push(value);
                for (Entry<?, ?> entry : map.entrySet()) {
                    if (hasCycle(entry.getKey(), seen)) {
                        return true;
                    }
                    if (hasCycle(entry.getValue(), seen)) {
                        return true;
                    }
                }
                seen.pop();
                return false;
            case CompositeData:
                CompositeData composite = (CompositeData) value;
                seen.push(value);
                for (String key : composite.getCompositeType().keySet()) {
                    if (hasCycle(composite.get(key), seen)) {
                        return true;
                    }
                }
                seen.pop();
                return false;
            case TabularData:
                TabularData tabular = (TabularData) value;
                seen.push(value);
                for (Object o : tabular.values()) {
                    if (hasCycle(o, seen)) {
                        return true;
                    }
                }
                seen.pop();
                return false;
        }
        return false;
    }

    private void writePOJOValue(OutputStream out, Object value) throws IOException {
        if (value == null) {
            // - For "null" value:
            //   value: null
            out.write(NULL);
            return;
        }

        Class<?> clazz = value.getClass();

        if (clazz.isArray()) {
            // - For array
            //   value: [ Value* ]
            Class<?> componentType = clazz.getComponentType();
            if (componentType.isPrimitive()) {
                writePrimitiveArray(out, value, componentType);
                return;
            }

            writeStartArray(out);
            for (Object o : (Object[]) value) {
                writeArrayItem(out);
                writePOJOValue(out, o);
            }
            writeEndArray(out);
            return;
        }

        TYPE type = SupportedClasses.get(clazz);
        if (type == null) {
            if (value instanceof TabularData) {
                type = TYPE.TabularData;
            } else if (value instanceof CompositeData) {
                type = TYPE.CompositeData;
            } else if (value instanceof Map) {
                type = TYPE.Map;
            } else if (value instanceof Collection) {
                type = TYPE.Collection;
            } else if (value instanceof BigInteger) {
                type = TYPE.BigInteger;
            } else if (value instanceof BigDecimal) {
                type = TYPE.BigDecimal;
            } else if (value instanceof Date) {
                type = TYPE.Date;
            } else if (value instanceof ObjectName) {
                type = TYPE.ObjectName;
            } else {
                // - For a value that's not expressible in JSON:
                //   null
                out.write(NULL);
                return;
            }
        }

        switch (type) {
            case Byte:
            case Short:
            case Integer:
            case Long:
            case Float:
            case Double:
            case Boolean:
            case BigInteger:
            case BigDecimal:
                // These value are ASCII without \ or " character, so no
                // escaping is needed.
                // - For a simple value
                //   value: String representation
                writeSimpleString(out, value.toString());
                return;
            case String:
                writeStringInternal(out, (String) value);
                return;
            case ObjectName:
                writeObjectName(out, (ObjectName) value);
                return;
            case Date:
                writeLongInternal(out, ((Date) value).getTime());
                return;
            case Object:
                // - For an Object:
                //   value: "". "null" isn't very accurate.
                writeSimpleString(out, "");
                return;
            case Character:
                // Could be special; may need to escape.
                writeStringInternal(out, value.toString());
                return;
            case Collection:
                // - For Collection
                //   value: [ Value* ]
                writeStartArray(out);
                for (Object o : (Collection<?>) value) {
                    writeArrayItem(out);
                    writePOJOValue(out, o);
                }
                writeEndArray(out);
                return;
            case Map:
                Map<?, ?> map = (Map<?, ?>) value;
                if (map.isEmpty()) {
                    writeStartObject(out);
                    writeEndObject(out);
                    return;
                }
                boolean simpleKey = true;
                for (Object key : map.keySet()) {
                    // If a key is "null", then treat as complex key
                    if (key == null || !SimpleKeys.contains(key.getClass())) {
                        simpleKey = false;
                        break;
                    }
                }
                if (simpleKey) {
                    // - For map with simple key:
                    //   value: {
                    //     String: Value *
                    //   }
                    writeStartObject(out);
                    for (Entry<?, ?> entry : map.entrySet()) {
                        Object key = entry.getKey();
                        Object v = entry.getValue();
                        if (key instanceof String) {
                            escapeFieldName(out, (String) key);
                        } else {
                            writeSimpleFieldName(out, key.toString());
                        }
                        writePOJOValue(out, v);
                    }
                    writeEndObject(out);
                } else {
                    // -For map with complex keys
                    //  value: [ {key: Value, value: Value}* ]
                    writeStartArray(out);
                    for (Entry<?, ?> entry : map.entrySet()) {
                        Object key = entry.getKey();
                        writeArrayItem(out);
                        writeStartObject(out);
                        writeFieldName(out, OM_KEY);
                        writePOJOValue(out, key);
                        writeFieldName(out, OM_VALUE);
                        writePOJOValue(out, entry.getValue());
                        writeEndObject(out);
                    }
                    writeEndArray(out);
                }
                return;
            case CompositeData:
                // - For CompositeData
                //   value: {
                //     String: Value * // Key can't be null
                //   }
                CompositeData composite = (CompositeData) value;
                writeStartObject(out);
                for (String key : composite.getCompositeType().keySet()) {
                    escapeFieldName(out, key);
                    writePOJOValue(out, composite.get(key));
                }
                writeEndObject(out);
                return;
            case TabularData:
                // - TabularData
                //   value: [ CompositeData * ]
                TabularData tabular = (TabularData) value;
                writeStartArray(out);
                for (Object item : tabular.values()) {
                    writeArrayItem(out);
                    writePOJOValue(out, item);
                }
                writeEndArray(out);
                return;
        }
    }

    private void writePrimitiveArray(OutputStream out, Object value, Class<?> component) throws IOException {
        switch (SupportedClasses.get(component)) {
            case _Byte:
            case Byte:
                writeStartArray(out);
                for (byte v : (byte[]) value) {
                    writeArrayItem(out);
                    writeIntInternal(out, v);
                }
                writeEndArray(out);
                break;
            case _Short:
            case Short:
                writeStartArray(out);
                for (short v : (short[]) value) {
                    writeArrayItem(out);
                    writeIntInternal(out, v);
                }
                writeEndArray(out);
                break;
            case _Integer:
            case Integer:
                writeStartArray(out);
                for (int v : (int[]) value) {
                    writeArrayItem(out);
                    writeIntInternal(out, v);
                }
                writeEndArray(out);
                break;
            case _Long:
            case Long:
                writeStartArray(out);
                for (long v : (long[]) value) {
                    writeArrayItem(out);
                    writeLongInternal(out, v);
                }
                writeEndArray(out);
                break;
            case _Float:
            case Float:
                writeStartArray(out);
                for (Float v : (Float[]) value) {
                    writeArrayItem(out);
                    writeSimpleString(out, Float.toString(v));
                }
                writeEndArray(out);
                break;
            case _Double:
            case Double:
                writeStartArray(out);
                for (Double v : (Double[]) value) {
                    writeArrayItem(out);
                    writeSimpleString(out, Double.toString(v));
                }
                writeEndArray(out);
                break;
            case _Character:
            case Character:
                writeStartArray(out);
                for (char v : (char[]) value) {
                    writeArrayItem(out);
                    writeIntInternal(out, v);
                }
                writeEndArray(out);
                break;
            case _Boolean:
            case Boolean:
                writeStartArray(out);
                for (boolean v : (boolean[]) value) {
                    writeArrayItem(out);
                    out.write(v ? TRUE : FALSE);
                }
                writeEndArray(out);
                break;
            default:
                break;
        }
    }

    private void writePOJOType(OutputStream out, Object value, List<OpenType<?>> openTypes) throws IOException {
        if (value == null) {
            // - For "null" value:
            //   type: null
            out.write(NULL);
            return;
        }

        Class<?> clazz = value.getClass();

        if (clazz.isArray()) {
            // - For simple array
            //   type: String
            // - For complex array
            //   type: {
            //     className: String, ?
            //     value: Base64, ?
            //     items: [ Type* ] ?
            //   }
            Class<?> componentType = clazz.getComponentType();
            if (componentType.isPrimitive()) {
                writeSimpleString(out, clazz.getName());
                return;
            }

            Class<?> leaf = componentType;
            while (leaf.isArray()) {
                leaf = leaf.getComponentType();
            }
            if (SupportedClasses.get(leaf) == null) {
                // Unknown array type. Java serialize it
                writeStartObject(out);
                writeSimpleStringField(out, OM_CLASSNAME, clazz.getName());
                writeSerializedField(out, OM_VALUE, value);
                writeEndObject(out);
                // And do not produce type information for the subtree.
                return;
            }

            if (SimpleArrays.contains(leaf)) {
                writeSimpleString(out, clazz.getName());
                // Don't need further type information for simple arrays
                return;
            }

            writeStartObject(out);
            writeSimpleStringField(out, OM_CLASSNAME, clazz.getName());
            writeFieldName(out, OM_ITEMS);
            writeStartArray(out);

            for (Object o : (Object[]) value) {
                writeArrayItem(out);
                writePOJOType(out, o, openTypes);
            }
            writeEndArray(out);
            writeEndObject(out);
            return;
        }

        TYPE type = SupportedClasses.get(clazz);
        if (type == null) {
            // Unknown array type. Java serialize it
            //   type: {
            //     className: String, ?
            //     value: Base64, ?
            //   }
            writeStartObject(out);
            writeSimpleStringField(out, OM_CLASSNAME, clazz.getName());
            writeSerializedField(out, OM_VALUE, value);
            writeEndObject(out);
            // And do not produce type information for the subtree.
            return;
        }

        switch (type) {
            case Byte:
            case Short:
            case Integer:
            case Long:
            case Float:
            case Double:
            case Character:
            case Boolean:
            case String:
            case BigInteger:
            case BigDecimal:
            case Date:
            case ObjectName:
            case Object:
                // - For a simple value
                //   type: String (class name)
                // These value are ASCII without \ or " character, so no
                // escaping is needed.
                writeSimpleString(out, clazz.getName());
                return;
            case Collection:
                // - For Collection
                //   type: {
                //     className: String,
                //     items: [ Type* ]
                //   }
                writeStartObject(out);
                writeSimpleStringField(out, OM_CLASSNAME, clazz.getName());
                writeFieldName(out, OM_ITEMS);
                writeStartArray(out);
                for (Object o : (Collection<?>) value) {
                    writeArrayItem(out);
                    writePOJOType(out, o, openTypes);
                }
                writeEndArray(out);
                writeEndObject(out);
                return;
            case Map:
                Map<?, ?> map = (Map<?, ?>) value;
                // - For maps
                //   type: {
                //     className: String,
                //     simpleKey: boolean,
                //     entries: [
                //       {
                //         key:String, ? // For simple key
                //         keyType:Type,
                //         value:Type
                //       } *
                //     ]
                //   }
                writeStartObject(out);
                writeSimpleStringField(out, OM_CLASSNAME, clazz.getName());
                if (map.isEmpty()) {
                    writeEndObject(out);
                    return;
                }

                boolean simpleKey = true;
                for (Object key : map.keySet()) {
                    // If a key is "null", then treat as complex key
                    if (key == null || !SimpleKeys.contains(key.getClass())) {
                        simpleKey = false;
                        break;
                    }
                }

                writeBooleanField(out, OM_SIMPLEKEY, simpleKey);
                writeFieldName(out, OM_ENTRIES);
                writeStartArray(out);
                for (Entry<?, ?> entry : map.entrySet()) {
                    Object key = entry.getKey();
                    writeArrayItem(out);
                    writeStartObject(out);
                    if (simpleKey) {
                        if (key instanceof String) {
                            writeStringField(out, OM_KEY, (String) key);
                        } else {
                            writeSimpleStringField(out, OM_KEY, key.toString());
                        }
                    }
                    writeFieldName(out, OM_KEYTYPE);
                    writePOJOType(out, key, openTypes);
                    writeFieldName(out, OM_VALUE);
                    writePOJOType(out, entry.getValue(), openTypes);
                    writeEndObject(out);
                }
                writeEndArray(out);
                writeEndObject(out);
                return;
            case CompositeData:
                // - For CompositeData
                //   type: {
                //     className: String,
                //     openType: Integer // Reference to the OpenType array in root Type
                //   }
                CompositeData composite = (CompositeData) value;
                writeStartObject(out);
                writeSimpleStringField(out, OM_CLASSNAME, clazz.getName());
                CompositeType ct = composite.getCompositeType();
                writeOpenTypeField(out, OM_OPENTYPE, ct, openTypes);
                writeEndObject(out);
                return;
            case TabularData:
                // - TabularData
                //   type: {
                //     className: String,
                //     openType: Integer // Reference to the OpenType array in root Type
                //   }
                TabularData tabular = (TabularData) value;
                writeStartObject(out);
                writeSimpleStringField(out, OM_CLASSNAME, clazz.getName());
                TabularType tt = tabular.getTabularType();
                writeOpenTypeField(out, OM_OPENTYPE, tt, openTypes);
                writeEndObject(out);
                break;
            default:
                break;
        }
    }

    private void writeOpenTypeField(OutputStream out, byte[] name, OpenType<?> openType, List<OpenType<?>> openTypes) throws IOException {
        int idx = openTypes.indexOf(openType);
        if (idx < 0) {
            writeIntField(out, name, openTypes.size());
            openTypes.add(openType);
        } else {
            writeIntField(out, name, idx);
        }
    }

    private void writeOpenTypes(OutputStream out, List<OpenType<?>> openTypes) throws IOException {
        if (openTypes.isEmpty()) {
            return;
        }

        writeFieldName(out, OM_OPENTYPES);
        writeStartArray(out);
        for (int i = 0; i < openTypes.size(); i++) {
            writeArrayItem(out);
            OpenType<?> ot = openTypes.get(i);
            Class<?> clazz = ot.getClass();
            if (clazz == SimpleType.class) {
                // - For SimpleType instances:
                //   String // The name of the simple type.
                writeSimpleString(out, ((SimpleType<?>) ot).getTypeName());
            } else {
                // - For all other OpenTypes:
                //   {
                //     openTypeClass: String,
                //     className: String,
                //     typeName: String,
                //     description: String,
                //   }
                writeStartObject(out);
                writeSimpleStringField(out, OM_OPENTYPECLASS, clazz.getName());
                writeSimpleStringField(out, OM_CLASSNAME, ot.getClassName());
                writeStringField(out, OM_TYPENAME, ot.getTypeName());
                writeStringField(out, OM_DESCRIPTION, ot.getDescription());
                if (clazz == ArrayType.class) {
                    // - ArrayType
                    //   {
                    //     dimension: Integer,
                    //     elementType: Integer,
                    //   }
                    ArrayType<?> at = (ArrayType<?>) ot;
                    writeIntField(out, OM_DIMENSION, at.getDimension());
                    OpenType<?> elemntType = at.getElementOpenType();
                    writeOpenTypeField(out, OM_ELEMENTTYPE, elemntType, openTypes);
                } else if (clazz == CompositeType.class) {
                    // - CompositeType
                    //   {
                    //     items: [ { key: String, description: String, type: Integer } *]
                    //   }
                    CompositeType ct = (CompositeType) ot;
                    writeFieldName(out, OM_ITEMS);
                    writeStartArray(out);
                    for (String key : ct.keySet()) {
                        writeArrayItem(out);
                        writeStartObject(out);
                        writeStringField(out, OM_KEY, key);
                        writeStringField(out, OM_DESCRIPTION, ct.getDescription(key));
                        writeOpenTypeField(out, OM_TYPE, ct.getType(key), openTypes);
                        writeEndObject(out);
                    }
                    writeEndArray(out);
                } else if (clazz == TabularType.class) {
                    // - TabularType
                    //   {
                    //     rowType: Integer,
                    //     indexNames: [String*]
                    //   }
                    TabularType tt = (TabularType) ot;
                    writeOpenTypeField(out, OM_ROWTYPE, tt.getRowType(), openTypes);
                    writeFieldName(out, OM_INDEXNAMES);
                    writeStartArray(out);
                    for (String name : tt.getIndexNames()) {
                        writeArrayItem(out);
                        writeStringInternal(out, name);
                    }
                    writeEndArray(out);
                } else {
                    // - For unknown OpenTypes
                    //   {
                    //     serialized: Base64
                    //   }
                    writeSerializedField(out, OM_SERIALIZED, ot);
                }
                writeEndObject(out);
            }
        }
        writeEndArray(out);
    }

    private Object readPOJOInternal(Object in) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONObject)) {
            throwConversionException("readPOJOInternal() expects a JSONObject.", in);
        }
        JSONObject json = (JSONObject) in;
        Object o = json.get(N_SERIALIZED);
        if (o != null) {
            // If the original object contains cycle, then java-deserialize it.
            return readSerialized(o);
        }
        if (USE_BASE64_FOR_POJO) {
            return null;
        }
        OpenType<?>[] openTypes = readOpenTypes(json.get(N_OPENTYPES));
        return readPOJOValue(json.get(N_VALUE), json.get(N_TYPE), openTypes);
    }

    private OpenType<?>[] readOpenTypes(Object in) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONArray)) {
            throwConversionException("readOpenTypes() expects a JSONArray.", in);
        }

        JSONArray json = (JSONArray) in;
        int size = json.size();
        OpenType<?>[] openTypes = new OpenType[size];
        try {
            for (int i = 0; i < size; i++) {
                readOpenType(json, i, openTypes);
            }
        } catch (OpenDataException e) {
            throwConversionException(e, in);
        }
        return openTypes;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private OpenType<?> readOpenType(JSONArray json, int i, OpenType<?>[] openTypes) throws ConversionException, ClassNotFoundException, OpenDataException {
        if (openTypes[i] != null) {
            return openTypes[i];
        }
        Object o = json.get(i);
        if (o instanceof String) {
            openTypes[i] = Name2SimpleTypes.get(o);
            if (openTypes[i] == null) {
                throwConversionException("readOpenType() received an unknown simple type name.", o);
            }
            return openTypes[i];
        }

        if (!(o instanceof JSONObject)) {
            throwConversionException("readOpenType() expects a JSONObject.", o);
        }

        JSONObject type = (JSONObject) o;
        Object serialized = type.get(N_SERIALIZED);
        if (serialized != null) {
            Object ret = readSerialized(serialized);
            if (!(ret instanceof OpenType<?>)) {
                throwConversionException("readOpenType() expects an OpenType.", serialized);
            }
            return openTypes[i] = (OpenType<?>) ret;
        }

        String openTypeClass = readStringInternal(type.get(N_OPENTYPECLASS));
        if ("javax.management.openmbean.ArrayType".equals(openTypeClass)) {
            int dimension = readIntInternal(type.get(N_DIMENSION));
            int elementType = readIntInternal(type.get(N_ELEMENTTYPE));
            if (elementType < 0 || elementType >= openTypes.length) {
                throwConversionException("readOpenType() receives an out-of-range open type index.", type.get(N_ELEMENTTYPE));
            }
            OpenType<?> etype = readOpenType(json, elementType, openTypes);
            if (etype instanceof SimpleType<?>) {
                return openTypes[i] = new ArrayType((SimpleType<?>) etype, PrimitiveArrayTypes.contains(etype.getClassName()));
            } else {
                return openTypes[i] = new ArrayType(dimension, etype);
            }
        }

        if ("javax.management.openmbean.CompositeType".equals(openTypeClass)) {
            String typeName = readStringInternal(type.get(N_TYPENAME));
            String description = readStringInternal(type.get(N_DESCRIPTION));

            o = type.get(N_ITEMS);
            if (!(o instanceof JSONArray)) {
                throwConversionException("readOpenType() expects a JSONArray.", o);
            }

            JSONArray items = (JSONArray) o;
            int size = items.size();
            String[] itemNames = new String[size];
            String[] itemDescriptions = new String[size];
            OpenType<?>[] itemTypes = new OpenType[size];
            for (int p = 0; p < size; p++) {
                o = items.get(p);
                if (!(o instanceof JSONObject)) {
                    throwConversionException("readOpenType() expects a JSONObject.", o);
                }

                JSONObject item = (JSONObject) o;
                itemNames[p] = readStringInternal(item.get(N_KEY));
                itemDescriptions[p] = readStringInternal(item.get(N_DESCRIPTION));
                int itemType = readIntInternal(item.get(N_TYPE));
                if (itemType < 0 || itemType >= openTypes.length) {
                    throwConversionException("readOpenType() receives an out-of-range open type index.", item.get(N_TYPE));
                }
                itemTypes[p] = readOpenType(json, itemType, openTypes);
            }
            return openTypes[i] = new CompositeType(typeName, description, itemNames, itemDescriptions, itemTypes);
        }

        if ("javax.management.openmbean.TabularType".equals(openTypeClass)) {
            String typeName = readStringInternal(type.get(N_TYPENAME));
            String description = readStringInternal(type.get(N_DESCRIPTION));
            int rowType = readIntInternal(type.get(N_ROWTYPE));
            if (rowType < 0 || rowType >= openTypes.length) {
                throwConversionException("readOpenType() receives an out-of-range open type index.", type.get(N_ROWTYPE));
            }

            OpenType<?> rtype = readOpenType(json, rowType, openTypes);
            if (!(rtype instanceof CompositeType)) {
                throwConversionException("readOpenType() expects a CompositeType.", rtype);
            }

            o = type.get(N_INDEXNAMES);
            if (!(o instanceof JSONArray)) {
                throwConversionException("readOpenType() expects a JSONArray.", o);
            }

            JSONArray names = (JSONArray) o;
            int size = names.size();
            String[] indexNames = new String[size];
            for (int p = 0; p < size; p++) {
                // Service change 119678:
                // Original code: indexNames[i] = readStringInternal(names.get(p));
                indexNames[p] = readStringInternal(names.get(p));
            }
            return openTypes[i] = new TabularType(typeName, description, (CompositeType) rtype, indexNames);
        }

        throwConversionException("readOpenType() received an unknown open type class.", openTypeClass);
        return null;
    }

    private Object readPOJOValue(Object value, Object type, OpenType<?>[] openTypes) throws ConversionException, ClassNotFoundException {
        if (type == null) {
            return null;
        }

        if (type instanceof String) {
            String tstr = (String) type;
            TYPE t = SimpleValues.get(tstr);
            if (t == null) {
                if (tstr.length() > 0 && tstr.charAt(0) == '[') {
                    return readSimpleArray(value, Class.forName(tstr));
                }
                throwConversionException("readPOJOValue() received an unknown class name.", type);
            }
            return readSimpleValue(value, t);
        }

        if (!(type instanceof JSONObject)) {
            throwConversionException("readPOJOValue() expects a JSONObject.", type);
        }

        JSONObject tjson = (JSONObject) type;
        Object o = tjson.get(N_VALUE);
        if (o != null) {
            return readSerialized(o);
        }

        o = tjson.get(N_CLASSNAME);
        if (o == null || !(o instanceof String)) {
            throwConversionException("readPOJOValue() expects a String.", o);
        }

        String className = (String) o;
        TYPE t = StructuredClasses.get(className);
        if (t == null) {
            if (className.length() > 0 && className.charAt(0) == '[') {
                return readComplexArray(value, Class.forName(className), tjson.get(N_ITEMS), openTypes);
            }
            throwConversionException("readPOJOValue() received an unknown class name.", o);
        }
        switch (t) {
            case ArrayList:
                return readCollectionValue(value, tjson, new ArrayList<Object>(), openTypes);
            case LinkedList:
                return readCollectionValue(value, tjson, new LinkedList<Object>(), openTypes);
            case Vector:
                return readCollectionValue(value, tjson, new Vector<Object>(), openTypes);
            case HashSet:
                return readCollectionValue(value, tjson, new HashSet<Object>(), openTypes);
            case HashMap:
                return readMapValue(value, tjson, new HashMap<Object, Object>(), openTypes);
            case Hashtable:
                return readMapValue(value, tjson, new Hashtable<Object, Object>(), openTypes);
            case TreeMap:
                return readMapValue(value, tjson, new TreeMap<Object, Object>(), openTypes);
            case CompositeDataSupport:
            case TabularDataSupport:
                int ot = readIntInternal(tjson.get(N_OPENTYPE));
                return readOpenData(value, openTypes[ot]);
        }

        return null;
    }

    private Object readSimpleValue(Object value, TYPE t) throws ConversionException {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throwConversionException("readSimpleValue() expects a String.", value);
        }
        String vstr = (String) value;
        switch (t) {
            case Byte:
                return Byte.valueOf(vstr);
            case Short:
                return Short.valueOf(vstr);
            case Integer:
                return Integer.valueOf(vstr);
            case Long:
                return Long.valueOf(vstr);
            case Float:
                return Float.valueOf(vstr);
            case Double:
                return Double.valueOf(vstr);
            case Character:
                if (vstr.length() != 1) {
                    throwConversionException("readSimpleValue() expects a String of length 1 for Characgter.", value);
                }
                return Character.valueOf(vstr.charAt(0));
            case Boolean:
                return Boolean.valueOf(vstr);
            case String:
                return vstr;
            case BigInteger:
                return new BigInteger(vstr);
            case BigDecimal:
                return new BigDecimal(vstr);
            case Date:
                return new Date(readLongInternal(vstr));
            case ObjectName:
                return readObjectName(vstr);
            case Object:
                if (vstr.length() != 0) {
                    throwConversionException("readSimpleValue() expects an empty String for Object.", value);
                }
                return OBJECT;
        }
        return null;
    }

    private Object readSimpleArray(Object value, Class<?> array) throws ConversionException {
        if (value == null) {
            return null;
        }
        if (!(value instanceof JSONArray)) {
            throwConversionException("readSimpleArray() expects a JSONArray.", value);
        }
        JSONArray json = (JSONArray) value;
        int size = json.size();
        Class<?> component = array.getComponentType();
        if (component.isArray()) {
            Object[] ret = (Object[]) Array.newInstance(component, size);
            for (int i = 0; i < size; i++) {
                ret[i] = readSimpleArray(json.get(i), component);
            }
            return ret;
        }
        if (component.isPrimitive()) {
            return readPrimitiveArray(json, SupportedClasses.get(component));
        }

        Object[] ret = (Object[]) Array.newInstance(component, size);
        for (int i = 0; i < size; i++) {
            ret[i] = readSimpleValue(json.get(i), SupportedClasses.get(component));
        }
        return ret;
    }

    private Object readPrimitiveArray(JSONArray value, TYPE type) throws ConversionException {
        int size = value.size();
        switch (type) {
            case _Byte:
            case Byte:
                byte[] bytes = new byte[size];
                for (int i = 0; i < size; i++) {
                    bytes[i] = readByteInternal(value.get(i));
                }
                return bytes;
            case _Short:
            case Short:
                short[] shorts = new short[size];
                for (int i = 0; i < size; i++) {
                    shorts[i] = readShortInternal(value.get(i));
                }
                return shorts;
            case _Integer:
            case Integer:
                int[] ints = new int[size];
                for (int i = 0; i < size; i++) {
                    ints[i] = readIntInternal(value.get(i));
                }
                return ints;
            case _Long:
            case Long:
                long[] longs = new long[size];
                for (int i = 0; i < size; i++) {
                    longs[i] = readLongInternal(value.get(i));
                }
                return longs;
            case _Float:
            case Float:
                float[] floats = new float[size];
                for (int i = 0; i < size; i++) {
                    floats[i] = readFloatInternal(value.get(i));
                }
                return floats;
            case _Double:
            case Double:
                double[] doubles = new double[size];
                for (int i = 0; i < size; i++) {
                    doubles[i] = readDoubleInternal(value.get(i));
                }
                return doubles;
            case _Character:
            case Character:
                char[] chars = new char[size];
                for (int i = 0; i < size; i++) {
                    chars[i] = readCharInternal(value.get(i));
                }
                return chars;
            case _Boolean:
            case Boolean:
                boolean[] bools = new boolean[size];
                for (int i = 0; i < size; i++) {
                    bools[i] = readBooleanInternal(value.get(i));
                }
                return bools;
        }
        return null;
    }

    private Object readComplexArray(Object value, Class<?> array, Object type, OpenType<?>[] openTypes) throws ConversionException, ClassNotFoundException {
        if (!(value instanceof JSONArray)) {
            throwConversionException("readComplexArray() expects a JSONArray.", value);
        }
        if (!(type instanceof JSONArray)) {
            throwConversionException("readComplexArray() expects a JSONArray.", type);
        }
        JSONArray vjson = (JSONArray) value;
        JSONArray tjson = (JSONArray) type;
        int size = tjson.size();
        if (size != vjson.size()) {
            throwConversionException("readComplexArray() expects same size from value and type arrays.", null);
        }

        Object[] ret = (Object[]) Array.newInstance(array.getComponentType(), size);
        for (int i = 0; i < size; i++) {
            ret[i] = readPOJOValue(vjson.get(i), tjson.get(i), openTypes);
        }
        return ret;
    }

    private Object readCollectionValue(Object value, JSONObject t, Collection<Object> ret, OpenType<?>[] openTypes) throws ConversionException, ClassNotFoundException {
        Object type = t.get(N_ITEMS);
        if (!(value instanceof JSONArray)) {
            throwConversionException("readCollectionValue() expects a JSONArray.", value);
        }
        if (!(type instanceof JSONArray)) {
            throwConversionException("readCollectionValue() expects a JSONArray.", type);
        }

        JSONArray vjson = (JSONArray) value;
        JSONArray tjson = (JSONArray) type;
        if (vjson.size() != tjson.size()) {
            throwConversionException("readCollectionValue() expects same size from value and type arrays.", null);
        }
        for (int i = 0; i < tjson.size(); i++) {
            ret.add(readPOJOValue(vjson.get(i), tjson.get(i), openTypes));
        }
        return ret;
    }

    private Object readMapValue(Object value, JSONObject t, Map<Object, Object> ret, OpenType<?>[] openTypes) throws ConversionException, ClassNotFoundException {
        if (value == null) {
            return ret;
        }

        Object simple = t.get(N_SIMPLEKEY);

        if (simple == null) {
            return ret;
        }

        if (!(simple instanceof Boolean)) {
            throwConversionException("readMapValue() expects a Boolean.", simple);
        }
        boolean simpleKey = ((Boolean) simple).booleanValue();

        Object type = t.get(N_ENTRIES);
        if (!(type instanceof JSONArray)) {
            throwConversionException("readMapValue() expects a JSONArray.", type);
        }
        JSONArray tjson = (JSONArray) type;

        if (simpleKey) {
            if (!(value instanceof JSONObject)) {
                throwConversionException("readMapValue() expects a JSONObject.", value);
            }
            JSONObject vjson = (JSONObject) value;
            if (tjson.size() != vjson.size()) {
                throwConversionException("readMapValue() expects same size from value and type arrays.", null);
            }
            for (Object e : tjson) {
                if (!(e instanceof JSONObject)) {
                    throwConversionException("readMapValue() expects a JSONObject.", e);
                }
                JSONObject entry = (JSONObject) e;
                Object k = entry.get(N_KEY);
                if (!(k instanceof String)) {
                    throwConversionException("readMapValue() expects a String.", k);
                }
                String key = (String) k;
                ret.put(readPOJOValue(key, entry.get(N_KEYTYPE), openTypes),
                        readPOJOValue(vjson.get(key), entry.get(N_VALUE), openTypes));
            }
        } else {
            if (!(value instanceof JSONArray)) {
                throwConversionException("readMapValue() expects a JSONArray.", value);
            }
            JSONArray vjson = (JSONArray) value;
            if (tjson.size() != vjson.size()) {
                throwConversionException("readMapValue() expects same size from value and type arrays.", null);
            }
            for (int i = 0; i < tjson.size(); i++) {
                Object te = tjson.get(i);
                if (!(te instanceof JSONObject)) {
                    throwConversionException("readMapValue() expects a JSONObject.", te);
                }
                JSONObject tentry = (JSONObject) te;
                Object ve = vjson.get(i);
                if (!(ve instanceof JSONObject)) {
                    throwConversionException("readMapValue() expects a JSONObject.", ve);
                }
                JSONObject ventry = (JSONObject) ve;
                ret.put(readPOJOValue(ventry.get(N_KEY), tentry.get(N_KEYTYPE), openTypes),
                        readPOJOValue(ventry.get(N_VALUE), tentry.get(N_VALUE), openTypes));
            }
        }
        return ret;
    }

    private Object readOpenData(Object value, OpenType<?> openType) throws ConversionException {
        if (value == null) {
            return null;
        }
        if (openType instanceof SimpleType) {
            return readSimpleValue(value, SimpleOpenTypes.get(openType));
        }
        if (openType instanceof ArrayType) {
            ArrayType<?> at = (ArrayType<?>) openType;
            OpenType<?> elementOT = at.getElementOpenType();
            Class<?> elementType = null;
            try {
                elementType = Class.forName(elementOT.getClassName());
            } catch (ClassNotFoundException e) {
                // Should never happen. All open type names are known.
            }

            if (!(value instanceof JSONArray)) {
                throwConversionException("readOpenData() expects a JSONArray.", value);
            }
            JSONArray json = (JSONArray) value;

            if (at.isPrimitiveArray()) {
                return readPrimitiveArray(json, SupportedClasses.get(elementType));
            }

            int size = json.size();
            Object[] ret = (Object[]) Array.newInstance(elementType, size);
            for (int i = 0; i < size; i++) {
                ret[i] = readOpenData(json.get(i), elementOT);
            }
            return ret;
        } else if (openType instanceof CompositeType) {
            return readCompositeData(value, (CompositeType) openType);
        } else if (openType instanceof TabularType) {
            TabularType tt = (TabularType) openType;
            CompositeType row = tt.getRowType();
            if (!(value instanceof JSONArray)) {
                throwConversionException("readOpenData() expects a JSONArray.", value);
            }
            JSONArray json = (JSONArray) value;

            TabularDataSupport ret = new TabularDataSupport(tt);
            for (Object o : json) {
                ret.put(readCompositeData(o, row));
            }
            return ret;
        }
        return null;
    }

    private CompositeData readCompositeData(Object value, CompositeType ct) throws ConversionException {
        Set<String> keys = ct.keySet();
        if (!(value instanceof JSONObject)) {
            throwConversionException("readCompositeData() expects a JSONObject.", value);
        }
        JSONObject json = (JSONObject) value;

        int size = keys.size();
        if (size != json.size()) {
            throwConversionException("readCompositeData() expects the same number of entries as in the type.", json);
        }

        String[] names = new String[size];
        Object[] values = new Object[size];
        int i = 0;
        for (String key : keys) {
            names[i] = key;
            values[i++] = readOpenData(json.get(key), ct.getType(key));
        }
        try {
            return new CompositeDataSupport(ct, names, values);
        } catch (OpenDataException e) {
            // Should never happen. All names/values are constructed based
            // on the open type.
        }
        return null;
    }

    private void writePOJOArrayField(OutputStream out, byte[] name, Object[] value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writePOJOArray(out, value);
    }

    private void writePOJOArray(OutputStream out, Object[] value) throws IOException {
        writeStartArray(out);
        for (Object o : value) {
            writeArrayItem(out);
            writePOJOInternal(out, o);
        }
        writeEndArray(out);
    }

    private Object[] readPOJOArray(Object in) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONArray)) {
            throwConversionException("readPOJOArray() expects a JSONArray.", in);
        }
        JSONArray json = (JSONArray) in;
        int size = json.size();
        Object[] ret = new Object[size];
        for (int i = 0; i < size; i++) {
            ret[i] = readPOJOInternal(json.get(i));
        }
        return ret;
    }

    private void writeObjectNameListField(OutputStream out, byte[] name, List<ObjectName> value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeStartArray(out);
        for (ObjectName item : value) {
            writeArrayItem(out);
            writeObjectName(out, item);
        }
        writeEndArray(out);
    }

    private List<ObjectName> readObjectNameList(Object in) throws ConversionException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONArray)) {
            throwConversionException("readObjectNameList() expects a JSONArray.", in);
        }
        JSONArray json = (JSONArray) in;
        int size = json.size();
        List<ObjectName> ret = new ArrayList<ObjectName>(size);
        for (Object o : json) {
            ret.add(readObjectName(o));
        }
        return ret;
    }

    private void writeObjectNameField(OutputStream out, byte[] name, ObjectName value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeObjectName(out, value);
    }

    private void writeObjectName(OutputStream out, ObjectName value) throws IOException {
        // ObjectName has no known sub-class.
        writeStringInternal(out, value.toString());
    }

    private ObjectName readObjectName(Object in) throws ConversionException {
        if (in == null) {
            return null;
        }
        try {
            return new ObjectName(readStringInternal(in));
        } catch (MalformedObjectNameException e) {
            throwConversionException(e, in);
            return null;
        }
    }

    private ObjectInstanceWrapper readObjectInstanceInternal(Object in) throws ConversionException {
        if (!(in instanceof JSONObject)) {
            throwConversionException("readObjectInstanceInternal() expects a JSONObject.", in);
        }
        JSONObject json = (JSONObject) in;
        ObjectName objectName = readObjectName(json.get(N_OBJECTNAME));
        String className = readStringInternal(json.get(N_CLASSNAME));
        ObjectInstanceWrapper ret = new ObjectInstanceWrapper();
        ret.objectInstance = new ObjectInstance(objectName, className);
        ret.mbeanInfoURL = readStringInternal(json.get(N_URL));
        return ret;
    }

    private void writeDescriptor(OutputStream out, byte[] name, Descriptor value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeStartObject(out);
        String[] names = value.getFieldNames();
        writeStringArrayField(out, OM_NAMES, names);
        writePOJOArrayField(out, OM_VALUES, value.getFieldValues(names));
        writeEndObject(out);
    }

    private Descriptor readDescriptor(Object in) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONObject)) {
            throwConversionException("readDescriptor() expects a JSONObject.", in);
        }
        JSONObject json = (JSONObject) in;
        String[] names = readStringArrayInternal(json.get(N_NAMES));
        Object[] values = readPOJOArray(json.get(N_VALUES));
        // All descriptors (on the client side) are immutable
        return new ImmutableDescriptor(names, values);
    }

    private void writeAttributes(OutputStream out, byte[] name, MBeanAttributeInfo[] value, Map<String, String> urls) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeStartArray(out);
        for (MBeanAttributeInfo item : value) {
            writeArrayItem(out);
            writeStartObject(out);
            writeStringField(out, OM_NAME, item.getName());
            writeStringField(out, OM_TYPE, item.getType());
            writeStringField(out, OM_DESCRIPTION, item.getDescription());
            writeDescriptor(out, OM_DESCRIPTOR, item.getDescriptor());
            writeBooleanField(out, OM_ISIS, item.isIs());
            writeBooleanField(out, OM_ISREADABLE, item.isReadable());
            writeBooleanField(out, OM_ISWRITABLE, item.isWritable());
            writeStringField(out, OM_URL, urls.get(item.getName()));
            writeEndObject(out);
        }
        writeEndArray(out);
    }

    private MBeanAttributeInfo[] readAttributes(Object in, Map<String, String> urls) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONArray)) {
            throwConversionException("readAttributes() expects a JSONArray.", in);
        }
        JSONArray json = (JSONArray) in;
        MBeanAttributeInfo[] ret = new MBeanAttributeInfo[json.size()];
        int pos = 0;
        for (Object item : json) {
            if (!(item instanceof JSONObject)) {
                throwConversionException("readAttributes() expects a JSONObject.", item);
            }
            JSONObject value = (JSONObject) item;
            String name = readStringInternal(value.get(N_NAME));
            String type = readStringInternal(value.get(N_TYPE));
            String description = readStringInternal(value.get(N_DESCRIPTION));
            boolean isReadable = readBooleanInternal(value.get(N_ISREADABLE));
            boolean isWritable = readBooleanInternal(value.get(N_ISWRITABLE));
            boolean isIs = readBooleanInternal(value.get(N_ISIS));
            Descriptor descriptor = readDescriptor(value.get(N_DESCRIPTOR));
            ret[pos++] = new MBeanAttributeInfo(name, type, description, isReadable, isWritable, isIs, descriptor);
            urls.put(name, readStringInternal(value.get(N_URL)));
        }
        return ret;
    }

    private void writeConstructors(OutputStream out, byte[] name, MBeanConstructorInfo[] value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeStartArray(out);
        for (MBeanConstructorInfo item : value) {
            writeArrayItem(out);
            writeStartObject(out);
            writeStringField(out, OM_NAME, item.getName());
            writeStringField(out, OM_DESCRIPTION, item.getDescription());
            writeDescriptor(out, OM_DESCRIPTOR, item.getDescriptor());
            writeParameters(out, OM_SIGNATURE, item.getSignature());
            writeEndObject(out);
        }
        writeEndArray(out);
    }

    private MBeanConstructorInfo[] readConstructors(Object in) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONArray)) {
            throwConversionException("readConstructors() expects a JSONArray.", in);
        }
        JSONArray json = (JSONArray) in;
        MBeanConstructorInfo[] ret = new MBeanConstructorInfo[json.size()];
        int pos = 0;
        for (Object item : json) {
            if (!(item instanceof JSONObject)) {
                throwConversionException("readConstructors() expects a JSONObject.", item);
            }
            JSONObject value = (JSONObject) item;
            String name = readStringInternal(value.get(N_NAME));
            String description = readStringInternal(value.get(N_DESCRIPTION));
            MBeanParameterInfo[] signature = readParameters(value.get(N_SIGNATURE));
            Descriptor descriptor = readDescriptor(value.get(N_DESCRIPTOR));
            ret[pos++] = new MBeanConstructorInfo(name, description, signature, descriptor);
        }
        return ret;
    }

    private void writeParameters(OutputStream out, byte[] name, MBeanParameterInfo[] value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeStartArray(out);
        for (MBeanParameterInfo item : value) {
            writeArrayItem(out);
            writeStartObject(out);
            writeStringField(out, OM_NAME, item.getName());
            writeStringField(out, OM_TYPE, item.getType());
            writeStringField(out, OM_DESCRIPTION, item.getDescription());
            writeDescriptor(out, OM_DESCRIPTOR, item.getDescriptor());
            writeEndObject(out);
        }
        writeEndArray(out);
    }

    private MBeanParameterInfo[] readParameters(Object in) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONArray)) {
            throwConversionException("readParameters() expects a JSONArray.", in);
        }
        JSONArray json = (JSONArray) in;
        MBeanParameterInfo[] ret = new MBeanParameterInfo[json.size()];
        int pos = 0;
        for (Object item : json) {
            if (!(item instanceof JSONObject)) {
                throwConversionException("readParameters() expects a JSONObject.", item);
            }
            JSONObject value = (JSONObject) item;
            String name = readStringInternal(value.get(N_NAME));
            String type = readStringInternal(value.get(N_TYPE));
            String description = readStringInternal(value.get(N_DESCRIPTION));
            Descriptor descriptor = readDescriptor(value.get(N_DESCRIPTOR));
            ret[pos++] = new MBeanParameterInfo(name, type, description, descriptor);
        }
        return ret;
    }

    private void writeNotifications(OutputStream out, byte[] name, MBeanNotificationInfo[] value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeStartArray(out);
        for (MBeanNotificationInfo item : value) {
            writeArrayItem(out);
            writeStartObject(out);
            writeStringField(out, OM_NAME, item.getName());
            writeStringField(out, OM_DESCRIPTION, item.getDescription());
            writeDescriptor(out, OM_DESCRIPTOR, item.getDescriptor());
            writeStringArrayField(out, OM_NOTIFTYPES, item.getNotifTypes());
            writeEndObject(out);
        }
        writeEndArray(out);
    }

    private MBeanNotificationInfo[] readNotifications(Object in) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONArray)) {
            throwConversionException("readNotifications() expects a JSONArray.", in);
        }
        JSONArray json = (JSONArray) in;
        MBeanNotificationInfo[] ret = new MBeanNotificationInfo[json.size()];
        int pos = 0;
        for (Object item : json) {
            if (!(item instanceof JSONObject)) {
                throwConversionException("readNotifications() expects a JSONObject.", item);
            }
            JSONObject value = (JSONObject) item;
            String name = readStringInternal(value.get(N_NAME));
            String description = readStringInternal(value.get(N_DESCRIPTION));
            String[] notifTypes = readStringArrayInternal(value.get(N_NOTIFTYPES));
            Descriptor descriptor = readDescriptor(value.get(N_DESCRIPTOR));
            ret[pos++] = new MBeanNotificationInfo(notifTypes, name, description, descriptor);
        }
        return ret;
    }

    private void writeOperations(OutputStream out, byte[] name, MBeanOperationInfo[] value, Map<String, String> urls) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeStartArray(out);
        for (MBeanOperationInfo item : value) {
            writeArrayItem(out);
            writeStartObject(out);
            writeStringField(out, OM_NAME, item.getName());
            writeStringField(out, OM_DESCRIPTION, item.getDescription());
            writeDescriptor(out, OM_DESCRIPTOR, item.getDescriptor());
            writeIntField(out, OM_IMPACT, item.getImpact());
            writeStringField(out, OM_RETURNTYPE, item.getReturnType());
            writeParameters(out, OM_SIGNATURE, item.getSignature());
            writeStringField(out, OM_URL, urls.get(item.getName()));
            writeEndObject(out);
        }
        writeEndArray(out);
    }

    private MBeanOperationInfo[] readOperations(Object in, Map<String, String> urls) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONArray)) {
            throwConversionException("readOperations() expects a JSONArray.", in);
        }
        JSONArray json = (JSONArray) in;
        MBeanOperationInfo[] ret = new MBeanOperationInfo[json.size()];
        int pos = 0;
        for (Object item : json) {
            if (!(item instanceof JSONObject)) {
                throwConversionException("readOperations() expects a JSONObject.", item);
            }
            JSONObject value = (JSONObject) item;
            String description = readStringInternal(value.get(N_DESCRIPTION));
            String name = readStringInternal(value.get(N_NAME));
            int impact = readIntInternal(value.get(N_IMPACT));
            String returnType = readStringInternal(value.get(N_RETURNTYPE));
            MBeanParameterInfo[] signature = readParameters(value.get(N_SIGNATURE));
            Descriptor descriptor = readDescriptor(value.get(N_DESCRIPTOR));
            ret[pos++] = new MBeanOperationInfo(name, description, signature, returnType, impact, descriptor);
            urls.put(name, readStringInternal(value.get(N_URL)));
        }
        return ret;
    }

    private void writeNotificationFiltersField(OutputStream out, byte[] name, NotificationFilter[] value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeNotificationFiltersInternal(out, value);
    }

    private void writeNotificationFiltersInternal(OutputStream out, NotificationFilter[] value) throws IOException {
        writeStartArray(out);
        if (value != null) {
            for (NotificationFilter item : value) {
                writeArrayItem(out);
                writeNotificationFilterInternal(out, item, false);
            }
        }
        writeEndArray(out);
    }

    private void writeNotificationFilterInternal(OutputStream out, NotificationFilter value, boolean allowOther) throws IOException {
        writeStartObject(out);
        Class<?> clazz = value.getClass();
        writeSimpleStringField(out, OM_CLASSNAME, clazz.getName());
        if (clazz == AttributeChangeNotificationFilter.class) {
            writeFieldName(out, OM_ENABLED);
            writeStartArray(out);
            for (String e : ((AttributeChangeNotificationFilter) value).getEnabledAttributes()) {
                writeArrayItem(out);
                writeStringInternal(out, e);
            }
            writeEndArray(out);
        } else if (clazz == MBeanServerNotificationFilter.class) {
            MBeanServerNotificationFilter filter = (MBeanServerNotificationFilter) value;
            byte[] fieldName = OM_ENABLED;
            Vector<ObjectName> list = filter.getEnabledObjectNames();
            if (list == null) {
                fieldName = OM_DISABLED;
                list = filter.getDisabledObjectNames();
            }
            writeFieldName(out, fieldName);
            writeStartArray(out);
            for (ObjectName e : list) {
                writeArrayItem(out);
                writeStringInternal(out, e.toString());
            }
            writeEndArray(out);
            writeFieldName(out, OM_TYPES);
            writeStartArray(out);
            for (String e : ((MBeanServerNotificationFilter) value).getEnabledTypes()) {
                writeArrayItem(out);
                writeStringInternal(out, e);
            }
            writeEndArray(out);
        } else if (clazz == NotificationFilterSupport.class) {
            writeFieldName(out, OM_TYPES);
            writeStartArray(out);
            for (String e : ((NotificationFilterSupport) value).getEnabledTypes()) {
                writeArrayItem(out);
                writeStringInternal(out, e);
            }
            writeEndArray(out);
        } else if (allowOther) {
            writeSerializedField(out, OM_SERIALIZED, value);
        } else {
            // The caller guarantees that only the known classes are used.
        }
        writeEndObject(out);
    }

    private void writeNotificationFilterField(OutputStream out, byte[] name, NotificationFilter value) throws IOException {
        if (value == null) {
            return;
        }
        writeFieldName(out, name);
        writeNotificationFilterInternal(out, value, true);
    }

    private NotificationFilter[] readNotificationFiltersInternal(Object in) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONArray)) {
            throwConversionException("readNotificationFiltersInternal() expects a JSONArray.", in);
        }
        JSONArray json = (JSONArray) in;
        NotificationFilter[] ret = new NotificationFilter[json.size()];
        int pos = 0;
        for (Object item : json) {
            if ((ret[pos++] = readNotificationFilterInternal(item, false)) == null) {
                throwConversionException("readNotificationFilterInternal() received a null NotificationListener.", in);
            }
        }
        return ret;
    }

    private NotificationFilter readNotificationFilterInternal(Object in, boolean allowOther) throws ConversionException, ClassNotFoundException {
        if (in == null) {
            return null;
        }
        if (!(in instanceof JSONObject)) {
            throwConversionException("readNotificationFilterInternal() expects a JSONObject.", in);
        }
        JSONObject json = (JSONObject) in;
        String className = readStringInternal(json.get(N_CLASSNAME));
        if ("javax.management.AttributeChangeNotificationFilter".equals(className)) {
            AttributeChangeNotificationFilter filter = new AttributeChangeNotificationFilter();
            String[] enabled = readStringArrayInternal(json.get(N_ENABLED));
            for (String item : enabled) {
                filter.enableAttribute(item);
            }
            return filter;
        } else if ("javax.management.relation.MBeanServerNotificationFilter".equals(className)) {
            MBeanServerNotificationFilter filter = new MBeanServerNotificationFilter();
            String[] enabled = readStringArrayInternal(json.get(N_ENABLED));
            if (enabled != null) {
                for (String item : enabled) {
                    filter.enableObjectName(readObjectName(item));
                }
            } else {
                String[] disabled = readStringArrayInternal(json.get(N_DISABLED));

                if (disabled != null) {
                    if (disabled.length == 0) {
                        filter.enableAllObjectNames();
                    } else {
                        for (String item : disabled) {
                            filter.disableObjectName(readObjectName(item));
                        }
                    }
                }
            }
            String[] types = readStringArrayInternal(json.get(N_TYPES));
            for (String item : types) {
                filter.enableType(item);
            }
            return filter;
        } else if ("javax.management.NotificationFilterSupport".equals(className)) {
            NotificationFilterSupport filter = new NotificationFilterSupport();
            String[] types = readStringArrayInternal(json.get(N_TYPES));
            for (String item : types) {
                filter.enableType(item);
            }
            return filter;
        } else if (allowOther) {
            Object o = readSerialized(json.get(N_SERIALIZED));
            if (!(o instanceof NotificationFilter)) {
                throwConversionException("readNotificationFilterInternal() expects a NotificationFilter.", in);
            }
            return (NotificationFilter) o;
        }
        throwConversionException("readNotificationFilterInternal() received an unknown filter class.", className);
        return null;
    }

    private void utf8EncodeError(Object json) throws ConversionException {
        throwConversionException("encodeStringAsBase64Internal() can't encode the value in UTF-8.", json);
    }

    private void base64DecodeError(Object json) throws ConversionException {
        throwConversionException("readSerialized() received invalid base64 string.", json);
    }

    private static String combineErrorMessage(String message, Object json) throws ConversionException {
        try {
            if (json instanceof JSONArtifact) {
                message = message + "\n\t" + ((JSONArtifact) json).serialize(true);
            } else if (json != null) {
                message = message + "\n\t" + json.toString();
            }
        } catch (IOException e) {
            // Should never happen
        }
        return message;
    }

    private static void throwConversionException(String message, Object json) throws ConversionException {
        throw new ConversionException(combineErrorMessage(message, json));
    }

    public static void throwConversionException(Throwable t, Object json) throws ConversionException {
        throw new ConversionException(combineErrorMessage(t.getMessage(), json), t);
    }

    static class DefaultSerializationHelper implements SerializationHelper {

        @Override
        public Object readObject(Object in, int blen, byte[] binary) throws ClassNotFoundException, ConversionException {
            try {
                return new ObjectInputStream(new ByteArrayInputStream(binary, 0, blen)).readObject();
            } catch (IOException e) {
                throwConversionException(e, in);
                return null;
            }
        }
    }
}
