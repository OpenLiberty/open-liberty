/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.processor;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.annotation.Resources;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;
import com.ibm.ws.injectionengine.TestHelper;
import com.ibm.ws.injectionengine.TestHelper.EnvEntryImpl;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;

public class EnvEntryTest
{
    @Test
    public void testSimpleAnnotation()
                    throws Exception
    {
        TestSimpleAnnotation instance = new TestSimpleAnnotation();
        TestHelper helper = new TestHelper()
                        .setClassLoader()
                        .setJavaColonCompEnvMap()
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivStringField"), "StringField")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "string"), "StringMethod")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivBooleanField"), "true")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivBooleanObjectField"), "true")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "boolean"), "true")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "booleanObject"), "true")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivByteField"), "1")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivByteObjectField"), "2")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "byte"), "3")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "byteObject"), "4")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivShortField"), "1")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivShortObjectField"), "2")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "short"), "3")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "shortObject"), "4")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivCharField"), "A")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivCharacterField"), "B")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "char"), "C")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "character"), "D")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivIntField"), "1")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivIntegerField"), "2")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "int"), "3")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "integer"), "4")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivFloatField"), "1.5")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivFloatObjectField"), "2.5")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "float"), "3.5")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "floatObject"), "4.5")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivLongField"), "1")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivLongObjectField"), "2")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "long"), "3")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "longObject"), "4")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivDoubleField"), "1.5")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivDoubleObjectField"), "2.5")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "double"), "3.5")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "doubleObject"), "4.5")
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivClassField"), TestSimpleAnnotation.class.getName())
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "class"), TestSimpleAnnotation.class.getName())
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "ivEnumField"), TestSimpleEnum.AAA.name())
                        .addEnvEntryValue(TestHelper.envName(TestSimpleAnnotation.class, "enum"), TestSimpleEnum.AAA.name())
                        .processAndInject(instance);
        Map<String, InjectionBinding<?>> bindings = helper.getJavaColonCompEnvMap();

        Assert.assertEquals("StringField", instance.ivStringField);
        Assert.assertEquals("StringField", bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivStringField")).getInjectionObject());
        Assert.assertEquals("StringMethod", instance.ivStringMethod);
        Assert.assertEquals("StringMethod", bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "string")).getInjectionObject());
        Assert.assertEquals(true, instance.ivBooleanField);
        Assert.assertEquals(true, bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivBooleanField")).getInjectionObject());
        Assert.assertEquals(true, instance.ivBooleanObjectField);
        Assert.assertEquals(true, bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivBooleanObjectField")).getInjectionObject());
        Assert.assertEquals(true, instance.ivBooleanMethod);
        Assert.assertEquals(true, bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "boolean")).getInjectionObject());
        Assert.assertEquals(true, instance.ivBooleanObjectMethod);
        Assert.assertEquals(true, bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "booleanObject")).getInjectionObject());
        Assert.assertEquals(1, instance.ivByteField);
        Assert.assertEquals(1, (byte) (Byte) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivByteField")).getInjectionObject());
        Assert.assertEquals(2, (byte) instance.ivByteObjectField);
        Assert.assertEquals(2, (byte) (Byte) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivByteObjectField")).getInjectionObject());
        Assert.assertEquals(3, instance.ivByteMethod);
        Assert.assertEquals(3, (byte) (Byte) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "byte")).getInjectionObject());
        Assert.assertEquals(4, (byte) instance.ivByteObjectMethod);
        Assert.assertEquals(4, (byte) (Byte) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "byteObject")).getInjectionObject());
        Assert.assertEquals(1, instance.ivShortField);
        Assert.assertEquals(1, (short) (Short) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivShortField")).getInjectionObject());
        Assert.assertEquals(2, (short) instance.ivShortObjectField);
        Assert.assertEquals(2, (short) (Short) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivShortObjectField")).getInjectionObject());
        Assert.assertEquals(3, instance.ivShortMethod);
        Assert.assertEquals(3, (short) (Short) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "short")).getInjectionObject());
        Assert.assertEquals(4, (short) instance.ivShortObjectMethod);
        Assert.assertEquals(4, (short) (Short) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "shortObject")).getInjectionObject());
        Assert.assertEquals('A', instance.ivCharField);
        Assert.assertEquals('A', (char) (Character) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivCharField")).getInjectionObject());
        Assert.assertEquals('B', (char) instance.ivCharacterField);
        Assert.assertEquals('B', (char) (Character) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivCharacterField")).getInjectionObject());
        Assert.assertEquals('C', instance.ivCharMethod);
        Assert.assertEquals('C', (char) (Character) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "char")).getInjectionObject());
        Assert.assertEquals('D', (char) instance.ivCharacterMethod);
        Assert.assertEquals('D', (char) (Character) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "character")).getInjectionObject());
        Assert.assertEquals(1, instance.ivIntField);
        Assert.assertEquals(1, (int) (Integer) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivIntField")).getInjectionObject());
        Assert.assertEquals(2, (int) instance.ivIntegerField);
        Assert.assertEquals(2, (int) (Integer) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivIntegerField")).getInjectionObject());
        Assert.assertEquals(3, instance.ivIntMethod);
        Assert.assertEquals(3, (int) (Integer) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "int")).getInjectionObject());
        Assert.assertEquals(4, (int) instance.ivIntegerMethod);
        Assert.assertEquals(4, (int) (Integer) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "integer")).getInjectionObject());
        Assert.assertEquals(1.5f, instance.ivFloatField, 0);
        Assert.assertEquals(1.5f, (Float) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivFloatField")).getInjectionObject(), 0);
        Assert.assertEquals(2.5f, instance.ivFloatObjectField, 0);
        Assert.assertEquals(2.5f, (Float) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivFloatObjectField")).getInjectionObject(), 0);
        Assert.assertEquals(3.5f, instance.ivFloatMethod, 0);
        Assert.assertEquals(3.5f, (Float) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "float")).getInjectionObject(), 0);
        Assert.assertEquals(4.5f, instance.ivFloatObjectMethod, 0);
        Assert.assertEquals(4.5f, (Float) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "floatObject")).getInjectionObject(), 0);
        Assert.assertEquals(1L, instance.ivLongField);
        Assert.assertEquals(1L, (long) (Long) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivLongField")).getInjectionObject());
        Assert.assertEquals(2L, (long) instance.ivLongObjectField);
        Assert.assertEquals(2L, (long) (Long) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivLongObjectField")).getInjectionObject());
        Assert.assertEquals(3L, instance.ivLongMethod);
        Assert.assertEquals(3L, (long) (Long) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "long")).getInjectionObject());
        Assert.assertEquals(4L, (long) instance.ivLongObjectMethod);
        Assert.assertEquals(4L, (long) (Long) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "longObject")).getInjectionObject());
        Assert.assertEquals(1.5, instance.ivDoubleField, 0);
        Assert.assertEquals(1.5, (Double) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivDoubleField")).getInjectionObject(), 0);
        Assert.assertEquals(2.5, instance.ivDoubleObjectField, 0);
        Assert.assertEquals(2.5, (Double) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivDoubleObjectField")).getInjectionObject(), 0);
        Assert.assertEquals(3.5, instance.ivDoubleMethod, 0);
        Assert.assertEquals(3.5, (Double) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "double")).getInjectionObject(), 0);
        Assert.assertEquals(4.5, instance.ivDoubleObjectMethod, 0);
        Assert.assertEquals(4.5, (Double) bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "doubleObject")).getInjectionObject(), 0);
        Assert.assertEquals(TestSimpleAnnotation.class, instance.ivClassField);
        Assert.assertEquals(TestSimpleAnnotation.class, bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivClassField")).getInjectionObject());
        Assert.assertEquals(TestSimpleAnnotation.class, instance.ivClassMethod);
        Assert.assertEquals(TestSimpleAnnotation.class, bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "class")).getInjectionObject());
        Assert.assertEquals(TestSimpleEnum.AAA, instance.ivEnumField);
        Assert.assertEquals(TestSimpleEnum.AAA, bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "ivEnumField")).getInjectionObject());
        Assert.assertEquals(TestSimpleEnum.AAA, instance.ivEnumMethod);
        Assert.assertEquals(TestSimpleEnum.AAA, bindings.get(TestHelper.envName(TestSimpleAnnotation.class, "enum")).getInjectionObject());
    }

    public static class TestSimpleAnnotation
    {
        @Resource
        String ivStringField;

        String ivStringMethod;

        @Resource
        void setString(String value)
        {
            ivStringMethod = value;
        }

        @Resource
        boolean ivBooleanField;
        @Resource
        Boolean ivBooleanObjectField;

        boolean ivBooleanMethod;
        Boolean ivBooleanObjectMethod;

        @Resource
        void setBoolean(boolean value)
        {
            ivBooleanMethod = value;
        }

        @Resource
        void setBooleanObject(Boolean value)
        {
            ivBooleanObjectMethod = value;
        }

        @Resource
        byte ivByteField;
        @Resource
        Byte ivByteObjectField;

        byte ivByteMethod;
        Byte ivByteObjectMethod;

        @Resource
        void setByte(byte value)
        {
            ivByteMethod = value;
        }

        @Resource
        void setByteObject(Byte value)
        {
            ivByteObjectMethod = value;
        }

        @Resource
        short ivShortField;
        @Resource
        Short ivShortObjectField;

        short ivShortMethod;
        Short ivShortObjectMethod;

        @Resource
        void setShort(short value)
        {
            ivShortMethod = value;
        }

        @Resource
        void setShortObject(Short value)
        {
            ivShortObjectMethod = value;
        }

        @Resource
        char ivCharField;
        @Resource
        Character ivCharacterField;

        char ivCharMethod;
        Character ivCharacterMethod;

        @Resource
        void setChar(char value)
        {
            ivCharMethod = value;
        }

        @Resource
        void setCharacter(Character value)
        {
            ivCharacterMethod = value;
        }

        @Resource
        int ivIntField;
        @Resource
        Integer ivIntegerField;

        int ivIntMethod;
        Integer ivIntegerMethod;

        @Resource
        void setInt(int value)
        {
            ivIntMethod = value;
        }

        @Resource
        void setInteger(Integer value)
        {
            ivIntegerMethod = value;
        }

        @Resource
        float ivFloatField;
        @Resource
        Float ivFloatObjectField;

        float ivFloatMethod;
        Float ivFloatObjectMethod;

        @Resource
        void setFloat(float value)
        {
            ivFloatMethod = value;
        }

        @Resource
        void setFloatObject(Float value)
        {
            ivFloatObjectMethod = value;
        }

        @Resource
        long ivLongField;
        @Resource
        Long ivLongObjectField;

        long ivLongMethod;
        Long ivLongObjectMethod;

        @Resource
        void setLong(long value)
        {
            ivLongMethod = value;
        }

        @Resource
        void setLongObject(Long value)
        {
            ivLongObjectMethod = value;
        }

        @Resource
        double ivDoubleField;
        @Resource
        Double ivDoubleObjectField;

        double ivDoubleMethod;
        Double ivDoubleObjectMethod;

        @Resource
        void setDouble(double value)
        {
            ivDoubleMethod = value;
        }

        @Resource
        void setDoubleObject(Double value)
        {
            ivDoubleObjectMethod = value;
        }

        @Resource
        Class<?> ivClassField;

        Class<?> ivClassMethod;

        @Resource
        void setClass(Class<?> value)
        {
            ivClassMethod = value;
        }

        @Resource
        TestSimpleEnum ivEnumField;

        TestSimpleEnum ivEnumMethod;

        @Resource
        void setEnum(TestSimpleEnum value)
        {
            ivEnumMethod = value;
        }
    }

    public enum TestSimpleEnum
    {
        AAA
    }

    @Test
    public void testSimpleXML()
                    throws Exception
    {
        TestSimpleXML instance = new TestSimpleXML();
        TestHelper helper = new TestHelper()
                        .setClassLoader()
                        .setJavaColonCompEnvMap()
                        .addEnvEntry(new EnvEntryImpl("ivStringField", String.class, "StringField", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivStringField")))
                        .addEnvEntry(new EnvEntryImpl("string", String.class, "StringMethod", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "string")))
                        .addEnvEntry(new EnvEntryImpl("ivBooleanField", Boolean.class, "true", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivBooleanField")))
                        .addEnvEntry(new EnvEntryImpl("ivBooleanObjectField", Boolean.class, "true", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivBooleanObjectField")))
                        .addEnvEntry(new EnvEntryImpl("boolean", Boolean.class, "true", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "boolean")))
                        .addEnvEntry(new EnvEntryImpl("booleanObject", Boolean.class, "true", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "booleanObject")))
                        .addEnvEntry(new EnvEntryImpl("ivByteField", Byte.class, "1", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivByteField")))
                        .addEnvEntry(new EnvEntryImpl("ivByteObjectField", Byte.class, "2", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivByteObjectField")))
                        .addEnvEntry(new EnvEntryImpl("byte", Byte.class, "3", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "byte")))
                        .addEnvEntry(new EnvEntryImpl("byteObject", Byte.class, "4", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "byteObject")))
                        .addEnvEntry(new EnvEntryImpl("ivShortField", Short.class, "1", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivShortField")))
                        .addEnvEntry(new EnvEntryImpl("ivShortObjectField", Short.class, "2", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivShortObjectField")))
                        .addEnvEntry(new EnvEntryImpl("short", Short.class, "3", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "short")))
                        .addEnvEntry(new EnvEntryImpl("shortObject", Short.class, "4", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "shortObject")))
                        .addEnvEntry(new EnvEntryImpl("ivCharField", Character.class, "A", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivCharField")))
                        .addEnvEntry(new EnvEntryImpl("ivCharacterField", Character.class, "B", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivCharacterField")))
                        .addEnvEntry(new EnvEntryImpl("char", Character.class, "C", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "char")))
                        .addEnvEntry(new EnvEntryImpl("character", Character.class, "D", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "character")))
                        .addEnvEntry(new EnvEntryImpl("ivIntField", Integer.class, "1", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivIntField")))
                        .addEnvEntry(new EnvEntryImpl("ivIntegerField", Integer.class, "2", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivIntegerField")))
                        .addEnvEntry(new EnvEntryImpl("int", Integer.class, "3", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "int")))
                        .addEnvEntry(new EnvEntryImpl("integer", Integer.class, "4", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "integer")))
                        .addEnvEntry(new EnvEntryImpl("ivFloatField", Float.class, "1.5", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivFloatField")))
                        .addEnvEntry(new EnvEntryImpl("ivFloatObjectField", Float.class, "2.5", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivFloatObjectField")))
                        .addEnvEntry(new EnvEntryImpl("float", Float.class, "3.5", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "float")))
                        .addEnvEntry(new EnvEntryImpl("floatObject", Float.class, "4.5", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "floatObject")))
                        .addEnvEntry(new EnvEntryImpl("ivLongField", Long.class, "1", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivLongField")))
                        .addEnvEntry(new EnvEntryImpl("ivLongObjectField", Long.class, "2", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivLongObjectField")))
                        .addEnvEntry(new EnvEntryImpl("long", Long.class, "3", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "long")))
                        .addEnvEntry(new EnvEntryImpl("longObject", Long.class, "4", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "longObject")))
                        .addEnvEntry(new EnvEntryImpl("ivDoubleField", Double.class, "1.5", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivDoubleField")))
                        .addEnvEntry(new EnvEntryImpl("ivDoubleObjectField", Double.class, "2.5", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivDoubleObjectField")))
                        .addEnvEntry(new EnvEntryImpl("double", Double.class, "3.5", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "double")))
                        .addEnvEntry(new EnvEntryImpl("doubleObject", Double.class, "4.5", null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "doubleObject")))
                        .addEnvEntry(new EnvEntryImpl("ivClassField", Class.class, TestSimpleXML.class.getName(), null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivClassField")))
                        .addEnvEntry(new EnvEntryImpl("class", Class.class, TestSimpleXML.class.getName(), null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "class")))
                        .addEnvEntry(new EnvEntryImpl("ivEnumField", TestSimpleEnum.class, TestSimpleEnum.AAA.name(), null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "ivEnumField")))
                        .addEnvEntry(new EnvEntryImpl("enum", TestSimpleEnum.class, TestSimpleEnum.AAA.name(), null,
                                        TestHelper.createInjectionTarget(TestSimpleXML.class, "enum")))
                        .processAndInject(instance);
        Map<String, InjectionBinding<?>> bindings = helper.getJavaColonCompEnvMap();

        Assert.assertEquals("StringField", instance.ivStringField);
        Assert.assertEquals("StringField", bindings.get("ivStringField").getInjectionObject());
        Assert.assertEquals("StringMethod", instance.ivStringMethod);
        Assert.assertEquals("StringMethod", bindings.get("string").getInjectionObject());
        Assert.assertEquals(true, instance.ivBooleanField);
        Assert.assertEquals(true, bindings.get("ivBooleanField").getInjectionObject());
        Assert.assertEquals(true, instance.ivBooleanObjectField);
        Assert.assertEquals(true, bindings.get("ivBooleanObjectField").getInjectionObject());
        Assert.assertEquals(true, instance.ivBooleanMethod);
        Assert.assertEquals(true, bindings.get("boolean").getInjectionObject());
        Assert.assertEquals(true, instance.ivBooleanObjectMethod);
        Assert.assertEquals(true, bindings.get("booleanObject").getInjectionObject());
        Assert.assertEquals(1, instance.ivByteField);
        Assert.assertEquals(1, (byte) (Byte) bindings.get("ivByteField").getInjectionObject());
        Assert.assertEquals(2, (byte) instance.ivByteObjectField);
        Assert.assertEquals(2, (byte) (Byte) bindings.get("ivByteObjectField").getInjectionObject());
        Assert.assertEquals(3, instance.ivByteMethod);
        Assert.assertEquals(3, (byte) (Byte) bindings.get("byte").getInjectionObject());
        Assert.assertEquals(4, (byte) instance.ivByteObjectMethod);
        Assert.assertEquals(4, (byte) (Byte) bindings.get("byteObject").getInjectionObject());
        Assert.assertEquals(1, instance.ivShortField);
        Assert.assertEquals(1, (short) (Short) bindings.get("ivShortField").getInjectionObject());
        Assert.assertEquals(2, (short) instance.ivShortObjectField);
        Assert.assertEquals(2, (short) (Short) bindings.get("ivShortObjectField").getInjectionObject());
        Assert.assertEquals(3, instance.ivShortMethod);
        Assert.assertEquals(3, (short) (Short) bindings.get("short").getInjectionObject());
        Assert.assertEquals(4, (short) instance.ivShortObjectMethod);
        Assert.assertEquals(4, (short) (Short) bindings.get("shortObject").getInjectionObject());
        Assert.assertEquals('A', instance.ivCharField);
        Assert.assertEquals('A', (char) (Character) bindings.get("ivCharField").getInjectionObject());
        Assert.assertEquals('B', (char) instance.ivCharacterField);
        Assert.assertEquals('B', (char) (Character) bindings.get("ivCharacterField").getInjectionObject());
        Assert.assertEquals('C', instance.ivCharMethod);
        Assert.assertEquals('C', (char) (Character) bindings.get("char").getInjectionObject());
        Assert.assertEquals('D', (char) instance.ivCharacterMethod);
        Assert.assertEquals('D', (char) (Character) bindings.get("character").getInjectionObject());
        Assert.assertEquals(1, instance.ivIntField);
        Assert.assertEquals(1, (int) (Integer) bindings.get("ivIntField").getInjectionObject());
        Assert.assertEquals(2, (int) instance.ivIntegerField);
        Assert.assertEquals(2, (int) (Integer) bindings.get("ivIntegerField").getInjectionObject());
        Assert.assertEquals(3, instance.ivIntMethod);
        Assert.assertEquals(3, (int) (Integer) bindings.get("int").getInjectionObject());
        Assert.assertEquals(4, (int) instance.ivIntegerMethod);
        Assert.assertEquals(4, (int) (Integer) bindings.get("integer").getInjectionObject());
        Assert.assertEquals(1.5f, instance.ivFloatField, 0);
        Assert.assertEquals(1.5f, (Float) bindings.get("ivFloatField").getInjectionObject(), 0);
        Assert.assertEquals(2.5f, instance.ivFloatObjectField, 0);
        Assert.assertEquals(2.5f, (Float) bindings.get("ivFloatObjectField").getInjectionObject(), 0);
        Assert.assertEquals(3.5f, instance.ivFloatMethod, 0);
        Assert.assertEquals(3.5f, (Float) bindings.get("float").getInjectionObject(), 0);
        Assert.assertEquals(4.5f, instance.ivFloatObjectMethod, 0);
        Assert.assertEquals(4.5f, (Float) bindings.get("floatObject").getInjectionObject(), 0);
        Assert.assertEquals(1L, instance.ivLongField);
        Assert.assertEquals(1L, (long) (Long) bindings.get("ivLongField").getInjectionObject());
        Assert.assertEquals(2L, (long) instance.ivLongObjectField);
        Assert.assertEquals(2L, (long) (Long) bindings.get("ivLongObjectField").getInjectionObject());
        Assert.assertEquals(3L, instance.ivLongMethod);
        Assert.assertEquals(3L, (long) (Long) bindings.get("long").getInjectionObject());
        Assert.assertEquals(4L, (long) instance.ivLongObjectMethod);
        Assert.assertEquals(4L, (long) (Long) bindings.get("longObject").getInjectionObject());
        Assert.assertEquals(1.5, instance.ivDoubleField, 0);
        Assert.assertEquals(1.5, (Double) bindings.get("ivDoubleField").getInjectionObject(), 0);
        Assert.assertEquals(2.5, instance.ivDoubleObjectField, 0);
        Assert.assertEquals(2.5, (Double) bindings.get("ivDoubleObjectField").getInjectionObject(), 0);
        Assert.assertEquals(3.5, instance.ivDoubleMethod, 0);
        Assert.assertEquals(3.5, (Double) bindings.get("double").getInjectionObject(), 0);
        Assert.assertEquals(4.5, instance.ivDoubleObjectMethod, 0);
        Assert.assertEquals(4.5, (Double) bindings.get("doubleObject").getInjectionObject(), 0);
        Assert.assertEquals(TestSimpleXML.class, instance.ivClassField);
        Assert.assertEquals(TestSimpleXML.class, bindings.get("ivClassField").getInjectionObject());
        Assert.assertEquals(TestSimpleXML.class, instance.ivClassMethod);
        Assert.assertEquals(TestSimpleXML.class, bindings.get("class").getInjectionObject());
        Assert.assertEquals(TestSimpleEnum.AAA, instance.ivEnumField);
        Assert.assertEquals(TestSimpleEnum.AAA, bindings.get("ivEnumField").getInjectionObject());
        Assert.assertEquals(TestSimpleEnum.AAA, instance.ivEnumMethod);
        Assert.assertEquals(TestSimpleEnum.AAA, bindings.get("enum").getInjectionObject());
    }

    public static class TestSimpleXML
    {
        String ivStringField;
        String ivStringMethod;

        void setString(String value)
        {
            ivStringMethod = value;
        }

        boolean ivBooleanField;
        Boolean ivBooleanObjectField;
        boolean ivBooleanMethod;
        Boolean ivBooleanObjectMethod;

        void setBoolean(boolean value)
        {
            ivBooleanMethod = value;
        }

        void setBooleanObject(Boolean value)
        {
            ivBooleanObjectMethod = value;
        }

        byte ivByteField;
        Byte ivByteObjectField;
        byte ivByteMethod;
        Byte ivByteObjectMethod;

        void setByte(byte value)
        {
            ivByteMethod = value;
        }

        void setByteObject(Byte value)
        {
            ivByteObjectMethod = value;
        }

        short ivShortField;
        Short ivShortObjectField;
        short ivShortMethod;
        Short ivShortObjectMethod;

        void setShort(short value)
        {
            ivShortMethod = value;
        }

        void setShortObject(Short value)
        {
            ivShortObjectMethod = value;
        }

        char ivCharField;
        Character ivCharacterField;
        char ivCharMethod;
        Character ivCharacterMethod;

        void setChar(char value)
        {
            ivCharMethod = value;
        }

        void setCharacter(Character value)
        {
            ivCharacterMethod = value;
        }

        int ivIntField;
        Integer ivIntegerField;
        int ivIntMethod;
        Integer ivIntegerMethod;

        void setInt(int value)
        {
            ivIntMethod = value;
        }

        void setInteger(Integer value)
        {
            ivIntegerMethod = value;
        }

        float ivFloatField;
        Float ivFloatObjectField;
        float ivFloatMethod;
        Float ivFloatObjectMethod;

        void setFloat(float value)
        {
            ivFloatMethod = value;
        }

        void setFloatObject(Float value)
        {
            ivFloatObjectMethod = value;
        }

        long ivLongField;
        Long ivLongObjectField;
        long ivLongMethod;
        Long ivLongObjectMethod;

        void setLong(long value)
        {
            ivLongMethod = value;
        }

        void setLongObject(Long value)
        {
            ivLongObjectMethod = value;
        }

        double ivDoubleField;
        Double ivDoubleObjectField;
        double ivDoubleMethod;
        Double ivDoubleObjectMethod;

        void setDouble(double value)
        {
            ivDoubleMethod = value;
        }

        void setDoubleObject(Double value)
        {
            ivDoubleObjectMethod = value;
        }

        Class<?> ivClassField;
        Class<?> ivClassMethod;

        void setClass(Class<?> value)
        {
            ivClassMethod = value;
        }

        TestSimpleEnum ivEnumField;
        TestSimpleEnum ivEnumMethod;

        void setEnum(TestSimpleEnum value)
        {
            ivEnumMethod = value;
        }
    }

    @Test
    public void testLookup()
    {
        TestLookup instance = new TestLookup();
        new TestHelper()
                        .setClassLoader()
                        .addIndirectJndiLookupValue("lookup", 1)
                        .addEnvEntry(new EnvEntryImpl("xml", Integer.class, null, "lookup",
                                        TestHelper.createInjectionTarget(TestLookup.class, "ivXML")))
                        .processAndInject(instance);
        Assert.assertEquals(1, instance.ivAnnotation);
        Assert.assertEquals(1, instance.ivXML);
    }

    public static class TestLookup
    {
        @Resource(lookup = "lookup")
        int ivAnnotation;
        int ivXML;
    }

    @Test
    public void testBinding()
    {
        TestBinding instance = new TestBinding();
        new TestHelper()
                        .setClassLoader()
                        .addEnvEntryValue("value", "1")
                        .addEnvEntryBinding("binding", "lookup")
                        .addIndirectJndiLookupValue("lookup", 2)
                        .processAndInject(instance);

        Assert.assertEquals(1, instance.ivValue);
        Assert.assertEquals(2, instance.ivBinding);
    }

    public static class TestBinding
    {
        @Resource(name = "value")
        int ivValue;
        @Resource(name = "binding")
        int ivBinding;
    }

    @Test
    public void testNoValue()
    {
        TestNoValue instance = new TestNoValue();
        new TestHelper()
                        .setClassLoader()
                        .addEnvEntry(new EnvEntryImpl("", Integer.class, null, null,
                                        TestHelper.createInjectionTarget(TestNoValue.class, "xML")))
                        .processAndInject(instance);
    }

    public static class TestNoValue
    {
        @Resource
        public void setAnnotation(int value)
        {
            Assert.fail("unexpected: " + value);
        }

        public void setXML(int value)
        {
            Assert.fail("unexpected: " + value);
        }
    }

    @Test
    public void testEJB10Properties()
                    throws Exception
    {
        Properties envProps = new Properties();
        TestHelper helper = new TestHelper();
        helper.setInjectionClasses(Collections.<Class<?>> emptyList());
        helper.setEnvironmentProperties(envProps);

        helper
                        .addEnvEntry(new EnvEntryImpl("ejb10-properties/propname", String.class, "value", null))
                        .process();

        Assert.assertEquals(1, envProps.size());
        Assert.assertEquals("value", envProps.get("propname"));
    }

    @Test
    public void testCompatibleXMLMethodTarget()
    {
        TestCompatibleXMLMethodTarget instance = new TestCompatibleXMLMethodTarget();
        new TestHelper()
                        .setClassLoader()
                        .addEnvEntry(new EnvEntryImpl("int", Integer.class, "1", null,
                                        TestHelper.createInjectionTarget(TestCompatibleXMLMethodTarget.class, "int")))
                        .addEnvEntry(new EnvEntryImpl("integer", Integer.class, "2", null,
                                        TestHelper.createInjectionTarget(TestCompatibleXMLMethodTarget.class, "integer")))
                        .addEnvEntry(new EnvEntryImpl("object", Integer.class, "3", null,
                                        TestHelper.createInjectionTarget(TestCompatibleXMLMethodTarget.class, "object")))
                        .processAndInject(instance);

        Assert.assertEquals(1, instance.ivInt);
        Assert.assertEquals(2, (Object) instance.ivInteger);
        Assert.assertEquals(3, instance.ivObject);
    }

    public static class TestCompatibleXMLMethodTarget
    {
        int ivInt;
        Integer ivInteger;
        Object ivObject;

        public void setInt(int value)
        {
            ivInt = value;
        }

        public void setInteger(Integer value)
        {
            ivInteger = value;
        }

        public void setObject(Object value)
        {
            ivObject = value;
        }
    }

    @Test
    public void testXMLImplicitType()
    {
        TestXMLImplicitType instance = new TestXMLImplicitType();
        String envNameInt = "enventryInt";
        String envNameInteger = "enventryInteger";
        TestHelper helper = new TestHelper()
                        .setClassLoader()
                        .setJavaColonCompEnvMap()
                        .addEnvEntry(new EnvEntryImpl(envNameInt, null, null, null,
                                        TestHelper.createInjectionTarget(TestXMLImplicitType.class, "int")))
                        .addEnvEntry(new EnvEntryImpl(envNameInteger, null, null, null,
                                        TestHelper.createInjectionTarget(TestXMLImplicitType.class, "integer")))
                        .addEnvEntryValue(envNameInt, "1")
                        .addEnvEntryValue(envNameInteger, "2")
                        .processAndInject(instance);
        Assert.assertEquals(1, instance.ivInt);
        Assert.assertEquals(1, helper.getJavaColonCompEnvMap().get(envNameInt).getBindingObject());
        Assert.assertEquals(2, (Object) instance.ivInteger);
        Assert.assertEquals(2, helper.getJavaColonCompEnvMap().get(envNameInteger).getBindingObject());
    }

    public static class TestXMLImplicitType
    {
        int ivInt;
        Integer ivInteger;

        public void setInt(int value)
        {
            ivInt = value;
        }

        public void setInteger(Integer value)
        {
            ivInteger = value;
        }
    }

    @Resource(name = "single", type = int.class)
    @Resources(@Resource(name = "plural", type = Integer.class))
    public static class TestClassAnnotationTypeMerged
    {
        // Empty.
    }

    /**
     * Test that env-entry value specified by application.xml
     */
    @Test
    public void testInjectableNonComp()
                    throws Exception
    {
        for (J2EEName j2eeName : new J2EEName[] {
                                                 null,
                                                 new J2EENameImpl("testapp", "testmod.jar", "test"),
                                                 new J2EENameImpl("testapp", null, null),
        })
        {
            TestHelper helper = new TestHelper("test", j2eeName)
                            .addEnvEntry(new EnvEntryImpl("java:global/env/string", String.class, "value", null))
                            .addEnvEntry(new EnvEntryImpl("java:app/env/string", String.class, "value", null))
                            .process();
            TestInjectableNonComp instance = new TestInjectableNonComp();
            new TestHelper()
                            .setInjectionEngine(helper.createInjectionEngine())
                            .processAndInject(instance);

            Assert.assertNull(instance.global);
            String expected = j2eeName != null && j2eeName.getModule() == null ? "value" : null;
            Assert.assertEquals(expected, instance.app);
        }
    }

    public static class TestInjectableNonComp
    {
        @Resource(name = "java:global/env/string")
        public String global;
        @Resource(name = "java:app/env/string")
        public String app;
    }

    @Test
    public void testClassAnnotationTypeMerged()
    {
        TestClassAnnotationTypeMerged instance = new TestClassAnnotationTypeMerged();
        TestHelper helper = new TestHelper()
                        .setJavaColonCompEnvMap()
                        .addEnvEntry(new EnvEntryImpl("single", null, "1", null))
                        .addEnvEntry(new EnvEntryImpl("plural", null, "2", null))
                        .processAndInject(instance);
        Assert.assertEquals(1, helper.getJavaColonCompEnvMap().get("single").getBindingObject());
        Assert.assertEquals(2, helper.getJavaColonCompEnvMap().get("plural").getBindingObject());
    }

    private static InjectionBinding<?> getMergeSavedValueInjectionBinding(Class<?> klass, String value)
                    throws InjectionException
    {
        return new TestHelper()
                        .addEnvEntryValue("string", value)
                        .addInjectionClass(klass)
                        .processAndGetInjectionBinding();
    }

    private static InjectionBinding<?> getMergeSavedLookupInjectionBinding(Class<?> klass, String lookup)
                    throws InjectionException
    {
        return new TestHelper()
                        .setClassLoader()
                        .addIndirectJndiLookupValue(lookup, "value")
                        .addInjectionClass(klass)
                        .processAndGetInjectionBinding();
    }

    private static InjectionBinding<?> getMergeSavedValueInjectionBinding(EnvEntry envEntry, String value)
                    throws InjectionException
    {
        return new TestHelper()
                        .addEnvEntryValue("string", value)
                        .addEnvEntry(envEntry)
                        .processAndGetInjectionBinding();
    }

    private static InjectionBinding<?> getMergeSavedLookupInjectionBinding(EnvEntry envEntry, String lookup)
                    throws InjectionException
    {
        return new TestHelper()
                        .setClassLoader()
                        .addIndirectJndiLookupValue(lookup, "value")
                        .addEnvEntry(envEntry)
                        .processAndGetInjectionBinding();
    }

    @Test
    public void testMergeSavedDefault()
                    throws Exception
    {
        InjectionBinding<?> annBinding = getMergeSavedValueInjectionBinding(TestMergeSavedDefault.class, "value");
        TestHelper.mergeSaved(annBinding, annBinding);

        InjectionBinding<?> xmlBinding = getMergeSavedValueInjectionBinding(new EnvEntryImpl("string", String.class, null, null), "value");
        TestHelper.mergeSaved(xmlBinding, xmlBinding);

        TestHelper.mergeSaved(annBinding, xmlBinding);
    }

    @Resource(name = "string", type = String.class)
    public static class TestMergeSavedDefault { /* empty */}

    @Test
    public void testMergeSavedValue()
                    throws Exception
    {
        InjectionBinding<?> annBinding = getMergeSavedValueInjectionBinding(TestMergeSavedValue.class, "value");
        TestHelper.mergeSaved(annBinding, annBinding);

        InjectionBinding<?> xmlBinding = getMergeSavedValueInjectionBinding(new EnvEntryImpl("string", String.class, "value", null), "value");
        TestHelper.mergeSaved(xmlBinding, xmlBinding);

        TestHelper.mergeSaved(annBinding, xmlBinding);
    }

    @Resource(name = "string", type = String.class)
    public static class TestMergeSavedValue { /* empty */}

    @Test
    public void testMergeSavedLookup()
                    throws Exception
    {
        InjectionBinding<?> annBinding = getMergeSavedLookupInjectionBinding(TestMergeSavedLookup.class, "lookup");
        TestHelper.mergeSaved(annBinding, annBinding);

        InjectionBinding<?> xmlBinding = getMergeSavedLookupInjectionBinding(new EnvEntryImpl("string", String.class, null, "lookup"), "lookup");
        TestHelper.mergeSaved(xmlBinding, xmlBinding);

        TestHelper.mergeSaved(annBinding, xmlBinding);
    }

    @Resource(name = "string", type = String.class, lookup = "lookup")
    public static class TestMergeSavedLookup { /* empty */}

    @Test
    public void testMergeSavedConflict()
                    throws Exception
    {
        TestHelper.mergeSavedFail(getMergeSavedValueInjectionBinding(TestMergeSavedConflictType1.class, "0"),
                                  getMergeSavedValueInjectionBinding(TestMergeSavedConflictType2.class, "0"));
        TestHelper.mergeSavedFail(getMergeSavedValueInjectionBinding(new EnvEntryImpl("string", String.class, null, null), "0"),
                                  getMergeSavedValueInjectionBinding(new EnvEntryImpl("string", Integer.class, null, null), "0"));

        TestHelper.mergeSavedFail(getMergeSavedValueInjectionBinding(TestMergeSavedConflictType1.class, "value1"),
                                  getMergeSavedValueInjectionBinding(TestMergeSavedConflictType1.class, "value2"));
        TestHelper.mergeSavedFail(getMergeSavedValueInjectionBinding(new EnvEntryImpl("string", String.class, "value1", null), null),
                                  getMergeSavedValueInjectionBinding(new EnvEntryImpl("string", String.class, "value2", null), null));

        TestHelper.mergeSavedFail(getMergeSavedLookupInjectionBinding(TestMergeSavedConflictLookup1.class, "lookup1"),
                                  getMergeSavedLookupInjectionBinding(TestMergeSavedConflictLookup2.class, "lookup2"));
        TestHelper.mergeSavedFail(getMergeSavedLookupInjectionBinding(new EnvEntryImpl("string", String.class, null, "lookup1"), "lookup1"),
                                  getMergeSavedLookupInjectionBinding(new EnvEntryImpl("string", String.class, null, "lookup2"), "lookup2"));
    }

    @Resource(name = "string", type = String.class)
    public static class TestMergeSavedConflictType1 { /* empty */}

    @Resource(name = "string", type = Integer.class)
    public static class TestMergeSavedConflictType2 { /* empty */}

    @Resource(name = "string", type = String.class, lookup = "lookup1")
    public static class TestMergeSavedConflictLookup1 { /* empty */}

    @Resource(name = "string", type = String.class, lookup = "lookup2")
    public static class TestMergeSavedConflictLookup2 { /* empty */}
}
