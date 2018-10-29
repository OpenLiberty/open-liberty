/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.class_scanner.ano;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.objectweb.asm.Type;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ArrayEntryType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ArrayInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.EnumerationInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ObjectFieldInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ObjectInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ObjectReferenceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ValueInstanceType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ValueType;

public class AsmObjectValueAnalyzer {
    private static final AtomicLong objIDCounter = new AtomicLong(0);
    private static final String PROPNAME_ACTUAL_TYPE = "actual.type";

    public static ValueInstanceType processValue(Object value) {
        return processValueInternal(value, new HashMap<Object, Long>());
    }

    public static ValueInstanceType processEnum(String name, String desc, String value) {
        return processEnumInternal(name, desc, value, new HashMap<Object, Long>());
    }

    private static ValueInstanceType processValueInternal(Object value, Map<Object, Long> objMap) {
        ValueInstanceType vit = new ValueInstanceType();

        if (value == null) {
            vit.setType(ValueType.NULL);
            return vit;
        }

        if ("org.objectweb.asm.Type".equals(value.getClass().getName())) {
            // This is a reference to a value of Class, ie, Class.class.  We will see this with
            // references to Entity listeners.
            org.objectweb.asm.Type type = (org.objectweb.asm.Type) value;

            ObjectInstanceType oit = new ObjectInstanceType();
            oit.setClassName(type.getClassName());
            vit.setObject(oit);
            vit.setType(ValueType.OBJECT);

            return vit;
        }

        final Class<?> cls = value.getClass();

        if (cls.isArray()) {
            vit.setType(ValueType.ARRAY);

            final ArrayInstanceType ait = new ArrayInstanceType();
            vit.setArray(ait);

            final int length = Array.getLength(value);
            ait.setLength(length);

            if (length > 0) {
                for (int index = 0; index < length; index++) {
                    ArrayEntryType aet = new ArrayEntryType();
                    aet.setIndex(index);
                    ait.getEntry().add(aet);

                    Object oVal = Array.get(value, index);
                    ValueInstanceType arrayVit = processValueInternal(oVal, objMap);
                    aet.setValue(arrayVit);
                }
            }

            return vit;
        }

        if (cls.isPrimitive()) {
            if (cls.equals(boolean.class)) {
                vit.setType(ValueType.BOOLEAN);
                vit.setSimple(String.format("%b", value));
            } else if (cls.equals(byte.class)) {
                vit.setType(ValueType.BYTE);
                vit.setSimple(String.format("%d", value));
            } else if (cls.equals(char.class)) {
                vit.setType(ValueType.CHAR);
                vit.setSimple(String.format("%c", value));
            } else if (cls.equals(double.class)) {
                vit.setType(ValueType.DOUBLE);
                vit.setSimple(String.format("%f", value)); // TODO: Make sure this supports double precision
            } else if (cls.equals(float.class)) {
                vit.setType(ValueType.FLOAT);
                vit.setSimple(String.format("%f", value));
            } else if (cls.equals(int.class)) {
                vit.setType(ValueType.INT);
                vit.setSimple(String.format("%d", value));
            } else if (cls.equals(long.class)) {
                vit.setType(ValueType.LONG);
                vit.setSimple(String.format("%d", value));
            } else if (cls.equals(short.class)) {
                vit.setType(ValueType.SHORT);
                vit.setSimple(String.format("%d", value));
            } else {
                vit.setType(ValueType.UNKNOWN);
            }

            return vit;
        }

        if (java.lang.Number.class.isAssignableFrom(cls)) {
            // AtomicInteger, AtomicLong, BigDecimal, BigInteger, Byte, Double, DoubleAccumulator,
            // DoubleAdder, Float, Integer, Long, LongAccumulator, LongAdder, Short

            if (cls.equals(java.lang.Byte.class)) {
                vit.setType(ValueType.JAVA_LANG_BYTE);
                vit.setSimple(String.format("%d", value));
            } else if (cls.equals(java.lang.Double.class)) {
                vit.setType(ValueType.JAVA_LANG_DOUBLE);
                vit.setSimple(String.format("%f", value));
            } else if (cls.equals(java.lang.Float.class)) {
                vit.setType(ValueType.JAVA_LANG_FLOAT);
                vit.setSimple(String.format("%f", value));
            } else if (cls.equals(java.lang.Integer.class)) {
                vit.setType(ValueType.JAVA_LANG_INTEGER);
                vit.setSimple(String.format("%d", value));
            } else if (cls.equals(java.lang.Long.class)) {
                vit.setType(ValueType.JAVA_LANG_LONG);
                vit.setSimple(String.format("%d", value));
            } else if (cls.equals(java.lang.Short.class)) {
                vit.setType(ValueType.JAVA_LANG_SHORT);
                vit.setSimple(String.format("%d", value));
            } else if (cls.equals(java.util.concurrent.atomic.AtomicInteger.class)) {
                vit.setType(ValueType.JAVA_LANG_INTEGER);
                vit.setSimple(String.format("%d", ((java.util.concurrent.atomic.AtomicInteger) value).get()));

                com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertiesType props = new com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertiesType();
                com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertyType prop = new com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertyType();
                prop.setName(PROPNAME_ACTUAL_TYPE);
                prop.setValue("java.util.concurrent.atomic.AtomicInteger");
                props.getProperty().add(prop);

                vit.setProperties(props);
            } else if (cls.equals(java.util.concurrent.atomic.AtomicLong.class)) {
                vit.setType(ValueType.JAVA_LANG_INTEGER);
                vit.setSimple(String.format("%d", ((java.util.concurrent.atomic.AtomicLong) value).get()));

                com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertiesType props = new com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertiesType();
                com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertyType prop = new com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertyType();
                prop.setName(PROPNAME_ACTUAL_TYPE);
                prop.setValue("java.util.concurrent.atomic.AtomicLong");
                props.getProperty().add(prop);

                vit.setProperties(props);
            } else if (cls.equals(java.math.BigDecimal.class)) {
                vit.setType(ValueType.JAVA_LANG_STRING);
                vit.setSimple(String.format("%s", ((java.math.BigDecimal) value).toString()));

                com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertiesType props = new com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertiesType();
                com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertyType prop = new com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertyType();
                prop.setName(PROPNAME_ACTUAL_TYPE);
                prop.setValue("java.math.BigDecimal");
                props.getProperty().add(prop);

                vit.setProperties(props);
            } else if (cls.equals(java.math.BigInteger.class)) {
                vit.setType(ValueType.JAVA_LANG_STRING);
                vit.setSimple(String.format("%s", ((java.math.BigInteger) value).toString()));

                com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertiesType props = new com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertiesType();
                com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertyType prop = new com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertyType();
                prop.setName(PROPNAME_ACTUAL_TYPE);
                prop.setValue("java.math.BigInteger");
                props.getProperty().add(prop);

                vit.setProperties(props);
            } else {
                vit.setType(ValueType.UNKNOWN);
                vit.setSimple(value.toString());

                com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertiesType props = new com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertiesType();
                com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertyType prop = new com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.PropertyType();
                prop.setName(PROPNAME_ACTUAL_TYPE);
                prop.setValue(value.getClass().getName());
                props.getProperty().add(prop);

                vit.setProperties(props);
            }

            return vit;
        }

        if (cls.equals(java.lang.Boolean.class)) {
            vit.setType(ValueType.JAVA_LANG_BOOLEAN);
            vit.setSimple(String.format("%b", value));
        } else if (cls.equals(java.lang.Character.class)) {
            vit.setType(ValueType.JAVA_LANG_CHARACTER);
            vit.setSimple(String.format("%c", value));
        } else if (cls.equals(java.lang.String.class)) {
            vit.setType(ValueType.JAVA_LANG_STRING);
            vit.setSimple(String.format("%s", value));
        } else {
            // Generic Object.
            vit.setType(ValueType.OBJECT);

            if (objMap != null && objMap.containsKey(value)) {
                // We have seen this Object before in a previous level of recursion.  Reference this
                // Object with a ObjectReferenceType
                final ObjectReferenceType ort = new ObjectReferenceType();
                ort.setRefId(objMap.get(value));
                vit.setObjectref(ort);
            } else {
                // We have not seen this Object before in a previous level of recursion.
                final Long id = objIDCounter.incrementAndGet();
                objMap.put(value, id);

                final ObjectInstanceType oit = new ObjectInstanceType();
                processObjectInternal(oit, value, objMap);
                vit.setObject(oit);
            }
        }

        return vit;
    }

    private static void processObjectInternal(ObjectInstanceType oit, Object value, Map<Object, Long> objMap) {
        final Class<?> c = value.getClass();
        oit.setClassName(c.getName());

        final List<ObjectFieldInstanceType> ofitList = oit.getField();

        // Get all of the fields that are retained by this Object.
        final Set<Field> fieldsSet = new HashSet<Field>();
        Class<?> classWalker = c;
        Set<Class> historySet = new HashSet<Class>(); // Prevent loops
        int breakoutCount = 200;
        while ((--breakoutCount > 0) && !historySet.contains(classWalker) && classWalker != null && !Object.class.equals(classWalker)) {
            historySet.add(classWalker);
            // TODO: Need to be able to work within a Java 2 Security Enable environment, so doPriv() stuff.
            Field[] declaredFields = classWalker.getDeclaredFields();
            if (declaredFields != null && declaredFields.length > 0) {
                for (Field f : declaredFields) {
                    fieldsSet.add(f);
                }
            }
            classWalker = classWalker.getSuperclass();
        }

        // Walk through each field found
        for (Field f : fieldsSet) {
            final ObjectFieldInstanceType ofit = new ObjectFieldInstanceType();
            ofitList.add(ofit);

            final boolean accessible = f.isAccessible();
            try {
                f.setAccessible(true);
                Class<?> fClass = f.getType();
                ofit.setClassName(fClass.getName());
                ofit.setName(f.getName());

                Object fValue = f.get(value);
                ValueInstanceType vit = processValueInternal(fValue, objMap);
                ofit.setValue(vit);
            } catch (Exception e) {
                FFDCFilter.processException(e, AsmObjectValueAnalyzer.class.getName() + ".processObjectInternal", "220");
                continue;
            } finally {
                f.setAccessible(accessible);
            }
        }
    }

    private static ValueInstanceType processEnumInternal(String name, String desc, String value, Map<Object, Long> objMap) {
        final ValueInstanceType vit = new ValueInstanceType();
        vit.setType(ValueType.ENUM);

        final EnumerationInstanceType eit = new EnumerationInstanceType();
        vit.setEnum(eit);

        final Type type = Type.getType(desc);
        if (type != null) {
            String processedName = AsmHelper.normalizeClassName(type.getClassName());
            eit.setClassName(processedName);
            eit.setValue(value);;
        }

        return vit;
    }

}
