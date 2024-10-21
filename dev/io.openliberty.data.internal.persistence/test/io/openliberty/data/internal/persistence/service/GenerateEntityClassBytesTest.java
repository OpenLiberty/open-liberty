/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Set;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

import io.openliberty.data.internal.persistence.models.Ambiguous;
import io.openliberty.data.internal.persistence.models.Boxed;
import io.openliberty.data.internal.persistence.models.Empty;
import io.openliberty.data.internal.persistence.models.Generic;
import io.openliberty.data.internal.persistence.models.GenericInfer;
import io.openliberty.data.internal.persistence.models.Naming;
import io.openliberty.data.internal.persistence.models.Primitive;
import io.openliberty.data.internal.persistence.models.Special;
import jakarta.data.exceptions.DataException;

/**
 * Unit tests for Record Transformer
 */
public class GenerateEntityClassBytesTest {

    @Test
    public void testPrimitive() throws Exception {
        byte[] classBytes = RecordTransformer //
                        .generateEntityClassBytes(Primitive.class,
                                                  Primitive.class.getName() + "Entity",
                                                  null,
                                                  Set.of());
        assertClassBytes(classBytes);
        assertClassSignature(Primitive.class, classBytes);
    }

    @Test
    public void testBoxed() throws Exception {
        byte[] classBytes = RecordTransformer //
                        .generateEntityClassBytes(Boxed.class,
                                                  Boxed.class.getName() + "Entity",
                                                  null,
                                                  Set.of());
        assertClassBytes(classBytes);
        assertClassSignature(Boxed.class, classBytes);
    }

    @Test
    public void testSpecial() throws Exception {
        byte[] classBytes = RecordTransformer //
                        .generateEntityClassBytes(Special.class,
                                                  Special.class.getName() + "Entity",
                                                  null,
                                                  Set.of());
        assertClassBytes(classBytes);
        assertClassSignature(Special.class, classBytes);
    }

    @Test
    public void testEmpty() throws Exception {
        byte[] classBytes = RecordTransformer //
                        .generateEntityClassBytes(Empty.class,
                                                  Empty.class.getName() + "Entity",
                                                  null,
                                                  Set.of());
        assertClassBytes(classBytes);
        assertClassSignature(Empty.class, classBytes);
    }

    @Test
    public void testNaming() throws Exception {
        byte[] classBytes = RecordTransformer //
                        .generateEntityClassBytes(Naming.class,
                                                  Naming.class.getName() + "Entity",
                                                  null,
                                                  Set.of());
        assertClassBytes(classBytes);
        assertClassSignature(Naming.class, classBytes);
    }

    @Test
    public void testGeneric() throws Exception {
        byte[] classBytes = RecordTransformer //
                        .generateEntityClassBytes(Generic.class,
                                                  Generic.class.getName() + "Entity",
                                                  null,
                                                  Set.of());
        assertClassBytes(classBytes);
        assertClassSignature(Generic.class, classBytes);
    }

    @Test
    public void testGenericInfer() throws Exception {
        try {
            RecordTransformer.generateEntityClassBytes(GenericInfer.class,
                                                       GenericInfer.class.getName() + "Entity",
                                                       null,
                                                       Set.of());
            fail("Should not have created a entity class for: " + GenericInfer.class.getName());
        } catch (DataException e) {
            //expected
        }
    }

    @Test
    public void testAmbiguous() throws Exception {
        try {
            RecordTransformer.generateEntityClassBytes(Ambiguous.class,
                                                       Ambiguous.class.getName() + "Entity",
                                                       null,
                                                       Set.of());
            fail("Should not have been able to create a well-formed entity class for: " + Ambiguous.class.getCanonicalName());
        } catch (DataException e) {
            //expected
        }
    }

    public static void assertClassSignature(Class<?> record, byte[] entityBytes) throws Exception {
        ClassLoader cl = record.getClassLoader();
        String primitiveClassName = record.getName() + "Entity";
        Class<?> primitiveEntity = new ClassDefiner().findLoadedOrDefineClass(cl, primitiveClassName, entityBytes);

        RecordComponent[] components = record.getRecordComponents();
        int componentCount = components.length;

        Class<?>[] typeArray = new Class<?>[componentCount];
        for (int i = 0; i < componentCount; i++)
            typeArray[i] = components[i].getType();

        //Ensure fields
        assertEquals("Found public field", 0, primitiveEntity.getFields().length);
        assertEquals("Incorrect number of private fields", componentCount, primitiveEntity.getDeclaredFields().length);

        //Ensure constructors
        assertEquals("Incorrect number of constructors", 2, primitiveEntity.getConstructors().length);
        primitiveEntity.getConstructor(); //no-arg
        primitiveEntity.getConstructor(record); //record

        int objectMethodCount = Object.class.getMethods().length;

        //Ensure methods
        int expectedDeclaredMethodCount = (componentCount * 2) + 1;
        int expectedMethodCount = objectMethodCount + expectedDeclaredMethodCount;
        assertEquals("Incorrect number of public methods", expectedMethodCount, primitiveEntity.getMethods().length);
        assertEquals("Found private method(s)", expectedDeclaredMethodCount, primitiveEntity.getDeclaredMethods().length);

        //Ensure toRecord method
        Method toRecord = primitiveEntity.getMethod("toRecord");
        assertEquals("toRecord method's returnType was incorrect", record, toRecord.getReturnType());

        //Ensure getters/setters
        for (RecordComponent component : components) {
            PropertyDescriptor desc = new PropertyDescriptor(component.getName(), primitiveEntity);
            Method getter = desc.getReadMethod();
            assertNotNull("Could not find getter for property: " + desc.getName(), getter);
            assertEquals("getter method took the wrong number of parameters", 0, getter.getParameterCount());
            assertEquals("getter method returnType was incorrect", component.getType(), getter.getReturnType());

            Method setter = desc.getWriteMethod();
            assertNotNull("Could not find setter for property: " + desc.getName(), setter);
            assertEquals("setter method took the wrong number of parameters", 1, setter.getParameterCount());
            assertEquals("setter method returnType was incorrect", component.getType(), setter.getParameterTypes()[0]);
        }

    }

    private static void assertClassBytes(byte[] classBytes) throws AssertionError {
        StringWriter sw = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(classBytes), false, new PrintWriter(sw));

        String result = sw.toString();
        if (result.isBlank()) {
            return;
        }

        throw new AssertionError(result);
    }

}
