/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.transport.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import com.ibm.ws.repository.common.utils.internal.RepositoryCommonUtils;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;

/**
 * Handle serializing and deserializing client model objects to and from JSON.
 */
public class DataModelSerializer {
    /**
     * The package used to resolve any subobjects during deserialization, and to allow
     * objects during serialization.
     */
    private static final String DATA_MODEL_PACKAGE = "com.ibm.ws.repository.transport.model";
    /**
     * The date format to use for all string<>date transformations.
     */
    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'";
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // Make processJsonArray Exception messages visible for test use
    public final static String DATA_MODEL_ERROR_ARRAY = "Data Model Error: Simple parser does not understand nested arrays, or true/false in an array.";
    public final static String DATA_MODEL_ERROR_NUMBER = "Data Model Error: Simple parser does not understand numbers in arrays.";

    public static volatile boolean IGNORE_UNKNOWN_FIELDS = true;

    public static enum Verification {
        VERIFY, DO_NOT_VERIFY
    };

    /**
     * When we are deserializing a top level list we want to ignore elements with a bad version so we just get a list of stuff we understand. If the element with a bad version is a
     * nested list then we want to throw the exception back up the to root "deserialize" method so it can either be ignored (if it's a list as the root element) or thrown if it's
     * a single object.
     */
    private static enum ListVersionHandling {
        IGNORE_ELEMENT, THROW_EXCEPTION
    }

    /**
     * Uses a getter to obtain a value, then sets that value appropriately into a supplied JsonObject.
     *
     * @param getter
     * @param o
     * @param fieldName
     * @param j
     */
    private static void addFieldToJsonObject(Method getter, Object o,
                                             String fieldName, JsonObjectBuilder j) {
        try {
            Object fieldValue = getter.invoke(o);

            if (fieldValue == null) {
            } else if (fieldValue instanceof String) {
                j.add(fieldName, (String) fieldValue);
            } else if (fieldValue instanceof Boolean) {
                j.add(fieldName, (Boolean) fieldValue);
            } else if (fieldValue instanceof BigInteger) {
                j.add(fieldName, (BigInteger) fieldValue);
            } else if (fieldValue instanceof java.math.BigDecimal) {
                j.add(fieldName, (BigDecimal) fieldValue);
            } else if (fieldValue instanceof Double) {
                j.add(fieldName, (Double) fieldValue);
            } else if (fieldValue instanceof Integer) {
                j.add(fieldName, (Integer) fieldValue);
            } else if (fieldValue instanceof Long) {
                j.add(fieldName, (Long) fieldValue);
            } else if (fieldValue instanceof Short) {
                j.add(fieldName, (Short) fieldValue);
            } else if (fieldValue instanceof Byte) {
                j.add(fieldName, (Byte) fieldValue);

            } else if (fieldValue instanceof Enum) {
                try {
                    // enums need careful handling.. look for get/set Value to use.
                    Method getValueForEnumMethod = fieldValue.getClass().getMethod("getValue");
                    Object valueFromGetValueMethod = getValueForEnumMethod.invoke(fieldValue);
                    j.add(fieldName, valueFromGetValueMethod.toString());
                } catch (NoSuchMethodException e) {
                    // else fallback to toString
                    j.add(fieldName, fieldValue.toString());
                }
            } else if (fieldValue instanceof Collection
                       || fieldValue.getClass().getName().startsWith(DATA_MODEL_PACKAGE)) {
                JSONArtrifactPair fieldObjects = findFieldsToSerialize(fieldValue);
                j.add(fieldName, fieldObjects.mainObject);
                if (fieldObjects.incompatibleFieldsObject != null && !fieldObjects.incompatibleFieldsObject.isEmpty()) {
                    // As per the contract on the HasBreakingChanges interface store incompatible fields in a second object
                    j.add(fieldName + "2", fieldObjects.incompatibleFieldsObject);
                }
            } else if (fieldValue instanceof Calendar) {
                Date date = ((Calendar) fieldValue).getTime();
                j.add(fieldName, getDateFormat().format(date));
            } else if (fieldValue instanceof Date) {
                j.add(fieldName, getDateFormat().format((Date) fieldValue));
            } else if (fieldValue instanceof Locale) {
                //note that Locale toString is a bit fiddly to round trip, but it's the best we have for now.
                String localeString = ((Locale) fieldValue).toString();
                j.add(fieldName, localeString);
            } else {
                throw new IllegalStateException("Data Model Error: Unknown data model entity "
                                                + getter.getName() + " of type " + fieldValue.getClass().getName() + " on "
                                                + o.getClass().getName());
            }

        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Data Model Error: unable to invoke getter "
                                            + getter.getName() + " on "
                                            + o.getClass().getName(), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Data Model Error: unable to invoke getter "
                                            + getter.getName() + " on "
                                            + o.getClass().getName(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Data Model Error: unable to invoke getter "
                                            + getter.getName() + " on "
                                            + o.getClass().getName(), e);
        } catch (JsonException e) {
            // Shouldn't happen unless field name is null or the value cannot be converted
            throw new IllegalStateException("Data Model Error: unable to invoke getter "
                                            + getter.getName() + " on "
                                            + o.getClass().getName(), e);
        }
    }

    /**
     * Use reflection to look inside Object to find fields that should be serialized into the JSON.
     * The fields are collected into an appropriate JsonStructure (either JsonObject, or JsonArray), and
     * returned for aggregation before serialization.
     *
     * @param o the Object to query, if o is an instance of Collection, then a JsonArray will be returned.
     * @return JSONArtrifactPair built from POJO.
     */
    @SuppressWarnings("unchecked")
    private static JSONArtrifactPair findFieldsToSerialize(Object o) {
        // if the object is a list, we'll return a JsonArray, containing each
        // thing in the List.
        if (o instanceof Collection) {
            Collection<? extends Object> listOfO = (Collection<? extends Object>) o;

            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (Object listObject : listOfO) {
                //currently, we support lists of strings, or lists of POJOs.
                if (listObject instanceof String) {
                    arrayBuilder.add((String) listObject);
                } else if (listObject.getClass().getName().startsWith(DATA_MODEL_PACKAGE)) {
                    arrayBuilder.add(findFieldsToSerialize(listObject).mainObject);
                } else {
                    throw new IllegalStateException("Data Model Error: serialization only supported for Collections of String, or other Data Model elements");
                }
            }

            JsonArray result = arrayBuilder.build();
            return new JSONArtrifactPair(result, null);
        }

        // object wasn't a collection.. better see what we can do with it.
        JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
        SortedMap<String, Method> gettersFromO = new TreeMap<String, Method>();
        Class<? extends Object> classOfO = o.getClass();

        // See if we have any breaking changes that need to go into a separate object
        JsonObjectBuilder incompatibleFieldsObjectBuilder = null;
        Collection<String> fieldsToPutInIncompatibleObject = null;
        boolean haveIncompatibleFields = false;
        if (HasBreakingChanges.class.isAssignableFrom(classOfO)) {
            fieldsToPutInIncompatibleObject = ((HasBreakingChanges) o).attributesThatCauseBreakingChanges();
            if (!fieldsToPutInIncompatibleObject.isEmpty()) {
                incompatibleFieldsObjectBuilder = Json.createObjectBuilder();
                haveIncompatibleFields = true;
            }
        }

        for (Method methodFromO : classOfO.getMethods()) {
            if (methodFromO.getName().startsWith("get") && methodFromO.getName().length() > 3 && methodFromO.getParameterTypes().length == 0) {
                Method old;
                if ((old = gettersFromO.put(methodFromO.getName(), methodFromO)) != null) {
                    throw new IllegalStateException("Data Model Error: duplicate getter for "
                                                    + methodFromO + "(" + old + ") on "
                                                    + o.getClass().getName());
                }
            }
        }

        for (Map.Entry<String, Method> entry : gettersFromO.entrySet()) {

            String getterName = entry.getKey();

            //not all getters are really for us ;p
            if ("getClass".equals(getterName)) {
                continue;
            }

            // If the field is marked as JSONIgnore then ignore it
            if (entry.getValue().isAnnotationPresent(JSONIgnore.class)) {
                continue;
            }

            String nameOfField = new StringBuilder().append(getterName.substring(3, 4).toLowerCase()).append(getterName.substring(4)).toString();

            if (haveIncompatibleFields && fieldsToPutInIncompatibleObject.contains(nameOfField)) {
                addFieldToJsonObject(entry.getValue(), o, nameOfField, incompatibleFieldsObjectBuilder);
            } else {
                addFieldToJsonObject(entry.getValue(), o, nameOfField, mainObjectBuilder);
            }
        }
        JsonObject mainObject = mainObjectBuilder.build();
        JsonObject incompatibleFieldsObject = null;
        if (incompatibleFieldsObjectBuilder != null) {
            incompatibleFieldsObject = incompatibleFieldsObjectBuilder.build();
        }

        return new JSONArtrifactPair(mainObject, incompatibleFieldsObject);
    }

    /**
     * Pair of {@link JsonStructure}s
     */
    public static class JSONArtrifactPair {

        /**
         * The first object of the pair
         */
        public final JsonStructure mainObject;

        /**
         * The second object of the pair
         */
        public final JsonObject incompatibleFieldsObject;

        /**
         * Construct the pair
         *
         * @param mainObject
         *            The first object to store
         * @param incompatibleFieldsObject
         *            The object containing all the incompatible errors
         */
        public JSONArtrifactPair(JsonStructure mainObject, JsonObject incompatibleFieldsObject) {
            this.mainObject = mainObject;
            this.incompatibleFieldsObject = incompatibleFieldsObject;
        }

    }

    /**
     * Convert a POJO into Serialized JSON form.
     *
     * @param o the POJO to serialize
     * @return a String containing the JSON data.
     * @throws IOException when there are problems creating the Json.
     */
    public static String serializeAsString(Object o) throws IOException {
        try {
            JsonStructure builtJsonObject = findFieldsToSerialize(o).mainObject;
            return builtJsonObject.toString();
        } catch (IllegalStateException ise) {
            // the reflective attempt to build the object failed.
            throw new IOException("Unable to build JSON for Object", ise);
        } catch (JsonException e) {
            throw new IOException("Unable to build JSON for Object", e);
        }
    }

    /**
     * Convert a POJO into Serialized JSON form.
     *
     * @param o the POJO to serialize
     * @param s the OutputStream to write to..
     * @throws IOException when there are problems creating the JSON.
     */
    public static void serializeAsStream(Object o, OutputStream s) throws IOException {
        try {
            JsonStructure builtJsonObject = findFieldsToSerialize(o).mainObject;
            Map<String, Object> config = new HashMap<String, Object>();
            config.put(JsonGenerator.PRETTY_PRINTING, true);
            JsonWriterFactory writerFactory = Json.createWriterFactory(config);
            JsonWriter streamWriter = writerFactory.createWriter(s);
            streamWriter.write(builtJsonObject);
        } catch (IllegalStateException ise) {
            // the reflective attempt to build the object failed.
            throw new IOException("Unable to build JSON for Object", ise);
        } catch (JsonException e) {
            throw new IOException("Unable to build JSON for Object", e);
        }
    }

    /**
     * Convert a POJO into a serialized JsonValue object
     *
     * @param o the POJO to serialize
     * @return
     */
    public static JsonValue serializeAsJson(Object o) throws IOException {
        try {
            return findFieldsToSerialize(o).mainObject;
        } catch (IllegalStateException ise) {
            // the reflective attempt to build the object failed.
            throw new IOException("Unable to build JSON for Object", ise);
        } catch (JsonException e) {
            throw new IOException("Unable to build JSON for Object", e);
        }
    }

    /**
     * Simple class to associate a Class and a Method for passing around.
     */
    private static class ClassAndMethod {
        Class<?> cls;
        Method m;
    }

    /**
     * A little utility to convert from Type to Class. <br>
     * The method is only complete enough for the usage within this class, it won't handle [] arrays, or primitive types, etc.
     *
     * @param t the Type to return a Class for, if the Type is parameterized, the actual type is returned (eg, List<String> returns String)
     * @return
     */
    private static Class<?> getClassForType(Type t) {
        if (t instanceof Class) {
            return (Class<?>) t;
        } else if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            return getClassForType(pt.getActualTypeArguments()[0]);
        } else if (t instanceof GenericArrayType) {
            throw new IllegalStateException("Data Model Error: Simple deserializer does not handle arrays, please use Collection instead.");
        }
        throw new IllegalStateException("Data Model Error: Unknown type " + t.toString() + ".");
    }

    /**
     * Gets a class for a JSON field name, by looking in a given Class for an appropriate setter.
     * The setter is assumed not to be a Collection.
     *
     * @param fieldName the name to use to search for the getter.
     * @param classToLookForFieldIn the Class to search for the getter within.
     * @return instance of ClassAndMethod if one is found, null otherwise
     */
    private static ClassAndMethod getClassForFieldName(String fieldName, Class<?> classToLookForFieldIn) {
        return internalGetClassForFieldName(fieldName, classToLookForFieldIn, false);
    }

    /**
     * Gets a class for a JSON field name, by looking in a given Class for an appropriate setter.
     * The setter is assumed to be a Collection.
     *
     * @param fieldName the name to use to search for the getter.
     * @param classToLookForFieldIn the Class to search for the getter within.
     * @return instance of ClassAndMethod if one is found, null otherwise
     */
    private static ClassAndMethod getClassForCollectionOfFieldName(String fieldName, Class<?> classToLookForFieldIn) {
        return internalGetClassForFieldName(fieldName, classToLookForFieldIn, true);
    }

    /**
     * Utility method, given a JSON field name, and an associated POJO, it will hunt for the appropriate setter to use, and if located return
     * the type the setter expects, along with a reflected reference to the method itself.
     *
     * @param fieldName the name of the json key being queried.
     * @param classToLookForFieldIn the object to hunt within for appropriate setters.
     * @param isForArray true, if the setter is expected to be for a collection, based on if the json data was an array.
     * @return instance of ClassAndMethod associating the class and method for the setter.
     */
    private static ClassAndMethod internalGetClassForFieldName(String fieldName, Class<?> classToLookForFieldIn, boolean isForArray) {
        Method found = null;

        //precalc the field name as a setter to use for each method test.
        String fieldNameAsASetter = new StringBuilder("set").append(fieldName.substring(0, 1).toUpperCase()).append(fieldName.substring(1)).toString();

        //hunt for any matching setter in the object
        for (Method m : classToLookForFieldIn.getMethods()) {
            String methodName = m.getName();

            //eligible setters must only accept a single argument.
            if (m.getParameterTypes().length == 1 && methodName.equals(fieldNameAsASetter)) {
                found = m;
                break;
            }
        }
        //at the mo, if we don't match a setter, we sysout a warning, this will likely need to become a toggle.
        if (found == null) {
            if (DataModelSerializer.IGNORE_UNKNOWN_FIELDS) {
                return null;
            } else {
                throw new IllegalStateException("Data Model Error: Found unexpected JSON field " + fieldName + " supposedly for in class " + classToLookForFieldIn.getName());
            }
        } else {
            ClassAndMethod cm = new ClassAndMethod();
            cm.m = found;
            if (isForArray) {
                //for an array we return the type of the collection, eg String for List<String> instead of List.
                Type t = found.getGenericParameterTypes()[0];
                cm.cls = getClassForType(t);
                return cm;
            } else {
                cm.cls = found.getParameterTypes()[0];
                return cm;
            }
        }
    }

    private static void invokeSetter(Method setter, Object targetObject, Object value) {
        try {
            // Only invoke the setter if it is not annotated with @ignore
            if (!setter.isAnnotationPresent(JSONIgnore.class)) {
                setter.invoke(targetObject, value);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Data Model Error: unable to invoke setter for data model element " + setter.getName() + " on " + targetObject.getClass().getName(), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Data Model Error: unable to invoke setter for data model element " + setter.getName() + " on " + targetObject.getClass().getName(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Data Model Error: unable to invoke setter for data model element " + setter.getName() + " on " + targetObject.getClass().getName(), e);
        }
    }

    /**
     * Reads every field from a JsonObject, and creates an instance of typeOfObject, and sets the data into that object.
     *
     * @param json JsonObject from JSONP
     * @param typeOfObject The class of the object to create
     * @param verify Specifies if we should check the JSON is something we know how to process
     * @return The object we created
     * @throws IOException
     * @throws BadVersionException
     */
    private static <T> T processJsonObjectBackIntoDataModelInstance(JsonObject json, Class<? extends T> typeOfObject, Verification verify) throws IOException, BadVersionException {
        Set<Map.Entry<String, JsonValue>> jsonSet = json.entrySet();

        // Make a new instance and make sure we know how to process it
        T targetObject = null;
        try {
            targetObject = typeOfObject.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("Data Model Error: unable to instantiate data model element " + typeOfObject.getName(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Data Model Error: unable to instantiate data model element " + typeOfObject.getName(), e);
        }

        if (verify.equals(Verification.VERIFY) && targetObject instanceof VersionableContent) {
            String version = null;
            try {
                version = json.getString(((VersionableContent) targetObject).nameOfVersionAttribute(), null);
            } catch (NullPointerException e) {
                // Ignore
            }
            if (version != null) {
                ((VersionableContent) targetObject).validate(version);
            }
        }

        for (Map.Entry<String, JsonValue> keyEntry : jsonSet) {
            String keyString = keyEntry.getKey();
            JsonValue value = keyEntry.getValue();

            if (value == null || value.getValueType().equals(ValueType.NULL))
                continue;

            //When calling toString() on a JsonString the value is returned in quotation marks, so we need to call getString() on JsonString to avoid this
            String valueString = null;
            if (value.getValueType().equals(ValueType.STRING)) {
                valueString = ((JsonString) value).getString();
            } else {
                valueString = value.toString();
            }

            //values in the JsonValue can be; JsonObject, JsonArray, or simple data.
            //each is handled with an instanceof block.

            if (value instanceof JsonObject) {
                //easy one, just find out the type for the new child, instantiate it, populate it, and set it.
                ClassAndMethod fieldType = getClassForFieldName(keyString, targetObject.getClass());
                if (fieldType != null) {
                    if (HasBreakingChanges.class.isAssignableFrom(fieldType.cls)) {
                        // It's something with a breaking change so see if we have a matching "2" element
                        JsonObject incompatibleFields = json.getJsonObject(keyString + "2");
                        if (incompatibleFields != null) {
                            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                            Set<Map.Entry<String, JsonValue>> valueSet = ((JsonObject) value).entrySet();
                            for (Map.Entry<String, JsonValue> entry : valueSet) {
                                jsonObjectBuilder.add(entry.getKey(), entry.getValue());
                            }
                            Set<Map.Entry<String, JsonValue>> incompatibleFieldsSet = incompatibleFields.entrySet();
                            for (Map.Entry<String, JsonValue> entry : incompatibleFieldsSet) {
                                jsonObjectBuilder.add(entry.getKey(), entry.getValue());
                            }
                            value = jsonObjectBuilder.build();
                        }
                    }
                    Object newChild = processJsonObjectBackIntoDataModelInstance((JsonObject) value, fieldType.cls, verify);
                    invokeSetter(fieldType.m, targetObject, newChild);
                }
            } else if (value instanceof JsonArray) {
                //slightly more tricky, we must determine the type for the collection to hold the data, instantiate a collection
                //then process each element in the array into the collection, and set that into the targetObject.
                ClassAndMethod fieldType = getClassForFieldName(keyString, targetObject.getClass());
                if (fieldType != null) {
                    if (fieldType.cls.equals(Collection.class) ||
                        fieldType.cls.equals(List.class)) {

                        List<Object> newList = new ArrayList<Object>();

                        //this entry in the json object was an array we need to look at the targetObject to determine type information.
                        ClassAndMethod listElementType = getClassForCollectionOfFieldName(keyString, targetObject.getClass());

                        if (listElementType == null) {
                            throw new IllegalStateException("Data Model Error: unable to deserialize a JSON array into a field with no generic information. " + keyString);
                        }

                        // Process the nested array and tell it to throw any bad version exceptions as this is a nested array so if this is a get single by ID we may want to throw it
                        processJsonArray((JsonArray) value, newList, listElementType.cls, verify, ListVersionHandling.THROW_EXCEPTION);

                        invokeSetter(fieldType.m, targetObject, newList);
                    } else {
                        throw new IllegalStateException("Data Model Error: unable to deserialize a JSON array into a field that is not of type List/Collection " + keyString);
                    }
                }
            } else {
                //this entry in the json object was a normal value, if it's not an enum, it's easy, if it's an enum.. well..
                ClassAndMethod fieldType = getClassForFieldName(keyString, targetObject.getClass());
                if (fieldType != null) {
                    if (fieldType.cls.isEnum()) {
                        //enums are kind of tricky.. cannot set the value directly, must build the correct enum instance.
                        //approach 1.. uppercase value, and look for matching enum.
                        Object o = null;
                        if (keyString.indexOf(' ') == -1) {
                            try {
                                Field enumInstance = fieldType.cls.getField(valueString.toUpperCase());
                                if (enumInstance != null) {
                                    o = enumInstance.get(null);
                                }
                            } catch (NoSuchFieldException e) {
                                //handled later
                            } catch (SecurityException e) {
                                //handled later
                            } catch (IllegalArgumentException e) {
                                //handled later
                            } catch (IllegalAccessException e) {
                                //handled later
                            }
                        }
                        if (o == null) {
                            //approach 2.. look for a method on enum taking string..
                            for (Method m : fieldType.cls.getMethods()) {
                                if (Modifier.isStatic(m.getModifiers()) &&
                                    m.getParameterTypes().length == 1 &&
                                    m.getParameterTypes()[0].equals(String.class)) {
                                    //enums have a valueOf method.. which may work.. but if not..
                                    //there may be another method to use yet.
                                    if ("valueOf".equals(m.getName())) {
                                        try {
                                            o = m.invoke(null, valueString);
                                        } catch (IllegalArgumentException e) {
                                            //ignore.. maybe another method will work?
                                        } catch (InvocationTargetException e) {
                                            //ignore.. maybe another method will work?
                                        } catch (IllegalAccessException e) {
                                            //ignore.. maybe another method will work?
                                        }
                                    } else {
                                        try {
                                            o = m.invoke(null, valueString);
                                        } catch (IllegalAccessException e) {
                                            throw new IllegalStateException("Data Model Error: unable to invoke setter " + fieldType.m.getName() + " for data model element "
                                                                            + fieldType.cls.getName() + " on " + targetObject.getClass().getName(), e);
                                        } catch (IllegalArgumentException e) {
                                            throw new IllegalStateException("Data Model Error: unable to invoke setter " + fieldType.m.getName() + " for data model element "
                                                                            + fieldType.cls.getName() + " on " + targetObject.getClass().getName(), e);
                                        } catch (InvocationTargetException e) {
                                            throw new IllegalStateException("Data Model Error: unable to invoke setter " + fieldType.m.getName() + " for data model element "
                                                                            + fieldType.cls.getName() + " on " + targetObject.getClass().getName(), e);
                                        }
                                    }
                                    if (o != null)
                                        break;
                                }
                            }
                        }
                        if (o == null) {
                            throw new IllegalStateException("Data Model Error: unable to handle Enum value of " + value + " for enum " + fieldType.cls.getName());
                        }
                        invokeSetter(fieldType.m, targetObject, o);
                    } else if (fieldType.cls.isPrimitive()) {
                        //primitives need more careful handling..
                        //all primitives in the current data model are numbers..
                        String type = fieldType.cls.getName();
                        if ("int".equals(type)) {
                            JsonNumber n = (JsonNumber) value;
                            invokeSetter(fieldType.m, targetObject, n.intValue());
                        } else if ("byte".equals(type)) {
                            JsonNumber n = (JsonNumber) value;
                            invokeSetter(fieldType.m, targetObject, (byte) n.intValue());
                        } else if ("short".equals(type)) {
                            JsonNumber n = (JsonNumber) value;
                            invokeSetter(fieldType.m, targetObject, (short) n.intValue());
                        } else if ("long".equals(type)) {
                            JsonNumber n = (JsonNumber) value;
                            invokeSetter(fieldType.m, targetObject, n.longValue());
                        } else if ("boolean".equals(type)) {
                            Boolean b = Boolean.valueOf(valueString);
                            invokeSetter(fieldType.m, targetObject, b);
                        } else {
                            throw new IllegalStateException("Data Model Error: unsupported primitive type used " + type + " in setter " + fieldType.m.getName());
                        }

                    } else if (fieldType.cls.equals(Calendar.class)) {
                        try {
                            Date d = getDateFormat().parse(valueString);
                            Calendar c = Calendar.getInstance();
                            c.setTime(d);
                            invokeSetter(fieldType.m, targetObject, c);
                        } catch (ParseException e) {
                            throw new IllegalStateException("JSON Error: date was not correctly formatted, got " + value, e);
                        }
                    } else if (fieldType.cls.equals(Date.class)) {
                        try {
                            Date d = getDateFormat().parse(valueString);
                            invokeSetter(fieldType.m, targetObject, d);
                        } catch (ParseException e) {
                            throw new IllegalStateException("JSON Error: date was not correctly formatted, got " + value, e);
                        }
                    } else if (fieldType.cls.equals(Locale.class)) {
                        Locale l = RepositoryCommonUtils.localeForString(valueString);
                        invokeSetter(fieldType.m, targetObject, l);
                    } else if (fieldType.cls.equals(String.class)) {
                        invokeSetter(fieldType.m, targetObject, valueString);
                    } else {
                        throw new IllegalArgumentException("Data Model Error: unable to invoke setter for data model element "
                                                           + fieldType.m.getName() + " on "
                                                           + targetObject.getClass().getName());
                    }
                }
            }
        }
        return targetObject;
    }

    /**
     * Processes an instance of JsonArray created by JSONP.<br>
     * Assumes each element of the array is the same sort of object, and that nested arrays are forbidden.
     * Handles arrays of simple values, and arrays of complex objects.
     *
     * @param jsonArray the JSONP instance to read data from
     * @param list the list to add the POJOs/simple data to.
     * @param listClass the type of data in the list (required to instantiate elements for pojos)
     * @throws BadVersionException
     * @throws IOException
     */
    private static <T> void processJsonArray(JsonArray jsonArray, List<T> list, Class<? extends T> listClass, Verification verify,
                                             ListVersionHandling versionHandler) throws IOException, BadVersionException {

        for (JsonValue value : jsonArray) {
            switch (value.getValueType()) {
                case TRUE:
                case FALSE:
                case ARRAY:
                    // array had another array as an element or a JSON true/false.
                    // message reads "Data Model Error: Simple parser does not understand nested arrays, or true/false in an array."
                    throw new IllegalStateException(DATA_MODEL_ERROR_ARRAY);
                case OBJECT:
                    //array had a complex object as an element.
                    try {
                        T newArrayElement = processJsonObjectBackIntoDataModelInstance((JsonObject) value, listClass, verify);
                        list.add(newArrayElement);
                    } catch (BadVersionException e) {
                        // versionHandler tells us what to do...
                        if (!ListVersionHandling.IGNORE_ELEMENT.equals(versionHandler)) {
                            // Default and THROW_EXCEPTION mean we should rethrow this
                            throw e;
                        } // Else ignore this
                    }
                    break;
                case STRING:
                    list.add(listClass.cast(((JsonString) value).getString()));
                    break;
                case NULL:
                    list.add(null);
                    break;
                case NUMBER:
                    // message reads "Data Model Error: Simple parser does not understand numbers in arrays.";
                    throw new IllegalStateException(DATA_MODEL_ERROR_NUMBER);
                default:
                    throw new IllegalStateException("Data Model Error: Unknown Json value type returned: " + value.getValueType().toString());
            }
        }
    }

    public static <T> T deserializeObject(InputStream i, Class<? extends T> typeOfObject) throws IOException, BadVersionException {
        return doDeserializeObject(i, typeOfObject, Verification.VERIFY);
    }

    public static <T> T deserializeObjectWithoutVerification(InputStream i, Class<? extends T> typeOfObject) throws IOException {
        T result = null;
        try {
            result = doDeserializeObject(i, typeOfObject, Verification.DO_NOT_VERIFY);
        } catch (BadVersionException bvx) {
            // This is unreachable
        }
        return result;
    }

    public static <T> T deserializeObject(JsonObject jsonObject, Class<? extends T> typeOfObject, Verification verify) throws IOException, BadVersionException {
        return processJsonObjectBackIntoDataModelInstance(jsonObject, typeOfObject, verify);
    }

    private static <T> T doDeserializeObject(InputStream i, Class<? extends T> typeOfObject, Verification verify) throws IOException, BadVersionException {
        try {
            JsonReader jsonReader = Json.createReader(i);
            JsonObject parsedObject = jsonReader.readObject();
            jsonReader.close();
            T newT = processJsonObjectBackIntoDataModelInstance(parsedObject, typeOfObject, verify);
            return newT;
        } catch (JsonException e) {
            throw new IOException("Failed to deserialize object of type " + typeOfObject.getName(), e);
        }
    }

    public static <T> List<T> deserializeList(InputStream i, Class<? extends T> listElementType) throws IOException {
        List<T> newT = new ArrayList<T>();

        try {
            JsonReader jsonReader = Json.createReader(i);
            JsonArray parsedArray = jsonReader.readArray();
            jsonReader.close();
            // Process the array, if it comes across any elements that are at an invalid version tell it to ignore them rather than throw an exception
            processJsonArray(parsedArray, newT, listElementType, Verification.VERIFY, ListVersionHandling.IGNORE_ELEMENT);
        } catch (BadVersionException e) {
            // We've told the Array handle to ignore these exception so this should never happen but it is in the method signature for nested array processing
        } catch (JsonException e) {
            throw new IOException("Failed to deserialize list", e);
        }
        return newT;
    }

    /**
     * Return a date format object for writing and parsing dates to JSON.
     * <p>
     * This method always creates a new instance because DateFormats are not thread safe.
     *
     * @return a date format for reading and writing JSON dates
     */
    private static DateFormat getDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);
        dateFormat.setTimeZone(UTC);
        return dateFormat;
    }
}
