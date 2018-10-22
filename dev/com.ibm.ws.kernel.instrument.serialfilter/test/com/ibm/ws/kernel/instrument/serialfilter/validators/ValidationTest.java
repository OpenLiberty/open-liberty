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
package com.ibm.ws.kernel.instrument.serialfilter.validators;

import com.ibm.ws.kernel.instrument.serialfilter.config.ConfigFacade;
import com.ibm.ws.kernel.instrument.serialfilter.config.SimpleConfig;
import com.ibm.ws.kernel.instrument.serialfilter.config.ValidationMode;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import test.util.StreamCapture;
import test.util.TestLogger;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;

import static com.ibm.ws.kernel.instrument.serialfilter.config.PermissionMode.ALLOW;
import static com.ibm.ws.kernel.instrument.serialfilter.config.PermissionMode.DENY;
import static com.ibm.ws.kernel.instrument.serialfilter.config.ValidationMode.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ValidationTest {
    @Before
    public void reinitializeLogging() throws Exception {LogManager.getLogManager().readConfiguration();}

    private static final Class<ValidationTest> MY_CLASS = ValidationTest.class;
    private static final SimpleConfig CONFIG = ConfigFacade.getSystemConfigProxy();

    @Rule
    public final StreamCapture capture = new StreamCapture();

    @Rule
    public final TestRule log = new TestLogger();

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setValidationModesForMethods() {
        for (Method m : MY_CLASS.getDeclaredMethods()) {
            // match method names containing something like "_INACTIVE_"
            // and try to treat it as a validator mode
            String modeName = m.getName().replaceFirst("^\\p{Alpha}*_(\\p{Upper}+)_\\p{Alpha}*$", "$1");
            System.err.printf("method %s -> mode string %s%n", m.getName(), modeName);
            try {
                ValidationMode mode = ValidationMode.valueOf(modeName);
                CONFIG.setValidationMode(mode, MY_CLASS.getName() + "#" + m.getName());
            } catch (IllegalArgumentException suppressed) {
            }
        }
    }

    @BeforeClass
    public static void setValidationModesForStreams() {
        CONFIG.setValidationMode(INACTIVE, ObjectInputStream_INACTIVE.class.getName());
        CONFIG.setValidationMode(DISCOVER, ObjectInputStream_DISCOVER.class.getName());
        CONFIG.setValidationMode(ENFORCE, ObjectInputStream_ENFORCE.class.getName());
        CONFIG.setValidationMode(REJECT, ObjectInputStream_REJECT.class.getName());
    }

    @BeforeClass
    public static void whiteListOnlySelectSerializableClasses() {
        CONFIG.setPermission(DENY, "*");
        CONFIG.setPermission(ALLOW, "java.*");
        CONFIG.setPermission(ALLOW, UniqueObject.class.getName());
        CONFIG.setPermission(ALLOW, WhitelistedObject.class.getName());
        CONFIG.setPermission(ALLOW, WhitelistedChild.class.getName());
    }

    @AfterClass
    public static void resetConfig() {
        CONFIG.reset();
    }

    @Test
    public void testDeserializeAString() throws Exception {
        copyBySerializing(new String("Hello, world."), ObjectInputStream.class);
    }

    @Test
    public void testDeserializeAnArrayList() throws Exception {
        assertNull(getModeForCurrentTestMethod());
        copyBySerializing(new ArrayList<Void>(), ObjectInputStream.class);
    }

    @Test
    public void testDefaultValidationOfWhitelistedObject() throws Exception {
        copyBySerializing(new WhitelistedObject(), ObjectInputStream.class);
    }

    @Test(expected=InvalidClassException.class)
    public void testDefaultValidationOfUnlistedObject() throws Exception {
        copyBySerializing(new UnlistedObject(), ObjectInputStream.class);
    }

    @Test
    public void testDefaultValidationOfWhitelistedChild() throws Exception {
        copyBySerializing(new WhitelistedChild(), ObjectInputStream.class);
    }

    @Test(expected=InvalidClassException.class)
    public void testDefaultValidationOfUnlistedChild() throws Exception {
        copyBySerializing(new UnlistedChild(), ObjectInputStream.class);
    }

    @Test
    public void test_INACTIVE_MethodValidationOfWhitelistedObject() throws Exception {
        copyBySerializing(new WhitelistedObject(), ObjectInputStream.class);
    }

    @Test
    public void test_INACTIVE_MethodValidationOfUnlistedObject() throws Exception {
        copyBySerializing(new UnlistedObject(), ObjectInputStream.class);
    }

    @Test
    public void test_INACTIVE_MethodValidationOfWhitelistedChild() throws Exception {
        copyBySerializing(new WhitelistedChild(), ObjectInputStream.class);
    }

    @Test
    public void test_INACTIVE_MethodValidationOfUnlistedChild() throws Exception {
        copyBySerializing(new UnlistedObject(), ObjectInputStream.class);
    }

    @Test
    public void test_DISCOVER_MethodValidationOfWhitelistedObject() throws Exception {
        copyBySerializing(new WhitelistedObject(), ObjectInputStream.class);
    }

    @Test
    public void test_DISCOVER_MethodValidationOfUnlistedObject() throws Exception {
        copyBySerializing(new UnlistedObject(), ObjectInputStream.class);
    }

    @Test(expected=InvalidClassException.class)
    public void test_ENFORCE_MethodValidationOfUnlistedObject() throws Exception {
        copyBySerializing(new UnlistedObject(), ObjectInputStream.class);
    }

    @Test
    public void test_ENFORCE_MethodValidationOfWhitelistedObject() throws Exception {
        copyBySerializing(new WhitelistedObject(), ObjectInputStream.class);
    }

    private ValidationMode getModeForCurrentTestMethod() {
        return CONFIG.getValidationMode(testName.getMethodName());
    }

    private static<S extends ObjectInputStream> Object copyBySerializing(Object original, Class<S> streamClass) throws Exception {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(bytesOut);
        objectOut.writeObject(original);
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
        ObjectInputStream objectIn = streamClass
                .getConstructor(InputStream.class)
                .newInstance(bytesIn);
        Object copy = objectIn.readObject();
        assertThat(copy, is(equalTo(original)));
        return copy;
    }

    public static class ObjectInputStream_INACTIVE extends ObjectInputStream {
        public ObjectInputStream_INACTIVE(InputStream in) throws IOException {
            super(in);
        }
    }

    public static class ObjectInputStream_ENFORCE extends ObjectInputStream {
        public ObjectInputStream_ENFORCE(InputStream in) throws IOException {
            super(in);
        }
    }

    public static class ObjectInputStream_DISCOVER extends ObjectInputStream_ENFORCE {
        public ObjectInputStream_DISCOVER(InputStream in) throws IOException {
            super(in);
        }
    }

    public static class ObjectInputStream_REJECT extends ObjectInputStream_DISCOVER {
        public ObjectInputStream_REJECT(InputStream in) throws IOException {
            super(in);
        }
    }

    public static class UniqueObject implements Serializable {
        private static AtomicInteger nextId = new AtomicInteger(1);
        protected final int id = nextId.getAndIncrement();

        @Override
        public String toString() {
            return String.format("%s@%03d", this.getClass().getSimpleName(), id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return id == ((UniqueObject) o).id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    public static class UnlistedObject extends UniqueObject implements Serializable {}

    public static class WhitelistedObject extends UniqueObject implements Serializable {}

//    public static class UnlistedChild extends WhitelistedObject implements Serializable {}
    public static class UnlistedChild extends UnlistedObject implements Serializable {}

    public static class WhitelistedChild extends UnlistedObject implements Externalizable {
        public void writeExternal(ObjectOutput out) throws IOException {out.writeInt(id);}
        public void readExternal(ObjectInput in) throws IOException {
            try {
                Field idField = UniqueObject.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(this, in.readInt());
            } catch (NoSuchFieldException e) {
                throw new Error(e);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
        }
    }

    public enum UnlistedEnum{INSTANCE}
}
