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

import com.ibm.ws.kernel.instrument.serialfilter.config.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import test.util.CartesianProduct;
import test.util.StreamCapture;
import test.util.TestLogger;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;

import static com.ibm.ws.kernel.instrument.serialfilter.config.PermissionMode.ALLOW;
import static com.ibm.ws.kernel.instrument.serialfilter.config.PermissionMode.DENY;
import static com.ibm.ws.kernel.instrument.serialfilter.config.ValidationMode.ENFORCE;
import static com.ibm.ws.kernel.instrument.serialfilter.config.ValidationMode.REJECT;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static test.util.Exceptions.rethrow;

@RunWith(Parameterized.class)
public class CompositeValidationTest {

    @Before
    public void reinitializeLogging() throws Exception {LogManager.getLogManager().readConfiguration();}

    public static final SimpleConfig CONFIG = ConfigFacade.getSystemConfigProxy();

    private static final Object[] THINGS_TO_BE_SERIALIZED = {
            "String",
            "array of Strings".split(" "),
            asList("List of Strings"),
            new BlacklistedObject(),
            new WhitelistedObject(),
            new BlacklistedChild(),
            new WhitelistedChild(),
            BlacklistedEnum.UNLISTED_ENUM,
            WhitelistedEnum.WHITELISTED_ENUM,
    };

    private enum ConfigurationRoute {
        PROG_EXACT {
            void setStream(ValidationMode m, Class<?> c) {CONFIG.setValidationMode(m, c.getName());}
            void setCaller(ValidationMode m, Class<?> c) {CONFIG.setValidationMode(m, c.getName() + "#copy");}
            void setPerm(PermissionMode m, String spec)  {CONFIG.setPermission(m, spec);}
        },
        PROG_WILDCARD {
            void setStream(ValidationMode m, Class<?> c) {CONFIG.setValidationMode(m, c.getName().replaceFirst(".$", "*"));}
            void setCaller(ValidationMode m, Class<?> c) {CONFIG.setValidationMode(m, c.getName() + "#cop*");}
            void setPerm(PermissionMode m, String spec)  {CONFIG.setPermission(m, spec);}
        },
        PROPS_EXACT {
            void setStream(ValidationMode m, Class<?> c) {sendProp(c.getName(), m.name());}
            void setCaller(ValidationMode m, Class<?> c) {sendProp(c.getName() + "#copy", m.name());}
            void setPerm(PermissionMode m, String spec)  {sendProp(spec, m.name());}
        },
        PROPS_WILDCARD {
            void setStream(ValidationMode m, Class<?> c) {sendProp(c.getName().replaceFirst(".$", "*"), m.name());}
            void setCaller(ValidationMode m, Class<?> c) {sendProp(c.getName() + "#cop*", m.name());}
            void setPerm(PermissionMode m, String spec)  {sendProp(spec, m.name());}
        },
        PROPSFILE_EXACT {
            PrintWriter out;
            void beforeConfig() throws Exception         {out = createTempConfigFile();}
            void setStream(ValidationMode m, Class<?> c) {writeProp(out, c.getName(), m.name());}
            void setCaller(ValidationMode m, Class<?> c) {writeProp(out, c.getName() + "#copy", m.name());}
            void setPerm(PermissionMode m, String spec)  {writeProp(out, spec, m.name());}
            void afterConfig()                           {consumeConfig(out);}
        },
        PROPSFILE_WILDCARD {
            PrintWriter out;
            void beforeConfig() throws Exception         {out = createTempConfigFile();}
            void setStream(ValidationMode m, Class<?> c) {writeProp(out, c.getName().replaceFirst(".$","*"), m.name());}
            void setCaller(ValidationMode m, Class<?> c) {writeProp(out, c.getName() + "#cop*", m.name());}
            void setPerm(PermissionMode m, String spec)  {writeProp(out, spec, m.name());}
            void afterConfig()                           {consumeConfig(out);}
        };

        void beforeConfig() throws Exception {CONFIG.reset();}
        abstract void setStream(ValidationMode m, Class<?> c);
        abstract void setCaller(ValidationMode m, Class<?> c);
        abstract void setPerm(PermissionMode m, String spec);
        void afterConfig() {}

        void setPerm(PermissionMode m, Class<?> c) {
            boolean wildcard = this.name().endsWith("WILDCARD");
            setPerm(m, wildcard ? c.getName() : c.getName().replaceFirst(".$","*"));
        }

        private static void sendProp(String name, String value) {
            Properties p = new Properties();
            p.setProperty(name, value);
            CONFIG.load(p);
        }

        private static PrintWriter createTempConfigFile() throws IOException {
            File dir = new File("build");
            Assert.assertTrue(dir.isDirectory());
            File configFile = File.createTempFile("serialval", ".properties", dir);
            configFile.deleteOnExit();
            System.setProperty(Config.FILE_PROPERTY, configFile.getPath());
            return new PrintWriter(new FileWriter(configFile));
        }

        private static void writeProp(PrintWriter out, String name, String value) {
            out.printf("%s=%s%n", name, value);
        }

        private static void consumeConfig(PrintWriter out) {
            // write the file
            out.flush();
            out.close();
            // tell config to read it
            CONFIG.reset();
            // ensure it is not read again
            System.getProperties().remove(Config.FILE_PROPERTY);
        }
    }

    @Parameterized.Parameters()
    public static List<Object[]> parameters() {
        return CartesianProduct.of(ConfigurationRoute.class).with(THINGS_TO_BE_SERIALIZED).with(PermissionMode.class);
    }

    @Rule
    public final TestLogger logger = new TestLogger();

    @Rule
    public final StreamCapture capture = new StreamCapture();

    private final ConfigurationRoute cfg;
    private final Object o;
    private final PermissionMode defaultPermission;

    public CompositeValidationTest(ConfigurationRoute configRoute, Object thingToBeDeserialized, PermissionMode defaultPermission) {
        System.out.println("configRoute : " + configRoute + ", thingTobeDeserialized : " + thingToBeDeserialized + ", defaultPermission : " + defaultPermission);
        this.cfg = configRoute;
        this.o = thingToBeDeserialized;
        this.defaultPermission = defaultPermission;
    }

    @Before
    public void configureValidationModesAndWhitelist() throws Exception {
        cfg.beforeConfig();
        try {
            cfg.setStream(ValidationMode.INACTIVE, InactiveStream.class);
            cfg.setStream(ValidationMode.DISCOVER, DiscoveryStream.class);
            cfg.setStream(ValidationMode.ENFORCE, EnforcedStream.class);
            cfg.setCaller(ValidationMode.INACTIVE, InactiveCaller.class);
            cfg.setCaller(ValidationMode.DISCOVER, DiscoveryCaller.class);
            cfg.setCaller(ValidationMode.ENFORCE, EnforcedCaller.class);
            switch(defaultPermission) {
                case DENY:
                    cfg.setPerm(DENY, "*");
                    cfg.setPerm(ALLOW, "java.*");
                    cfg.setPerm(ALLOW, UniqueObject.class);
                    cfg.setPerm(ALLOW, WhitelistedObject.class);
                    cfg.setPerm(ALLOW, WhitelistedChild.class);
                    cfg.setPerm(ALLOW, WhitelistedEnum.class);
                    break;
                case ALLOW:
                    // this is the internal default, so no need to set it explicitly
                    cfg.setPerm(DENY, BlacklistedObject.class);
                    cfg.setPerm(DENY, BlacklistedChild.class);
                    cfg.setPerm(DENY, BlacklistedEnum.class);
                    break;
            }
            cfg.setPerm(DENY, "*");
            cfg.setPerm(ALLOW, "java.*");
            cfg.setPerm(ALLOW, UniqueObject.class);
            cfg.setPerm(ALLOW, WhitelistedObject.class);
            cfg.setPerm(ALLOW, WhitelistedChild.class);
            cfg.setPerm(ALLOW, WhitelistedEnum.class);
        } finally {
            cfg.afterConfig();
        }
    }

    @AfterClass
    public static void resetConfig() {
        CONFIG.reset();
    }


    @Test public void test() {defaultCaller().copyDefault();}
    @Test public void testInactiveStream() {defaultCaller().copyInactive();}
    @Test public void testInactiveCaller() {inactiveCaller().copyDefault();}
    @Test public void testDiscoveryStream() {defaultCaller().copyDiscovery();}
    @Test public void testDiscoveryCaller() {discoveryCaller().copyDefault();}
    @Test public void testEnforcedStream() {defaultCaller().copyEnforced();}
    @Test public void testEnforcedCaller() {enforcedCaller().copyDefault();}
    @Test public void testInactiveCallerEnforcedStream() {inactiveCaller().copyEnforced();}
    @Test public void testInactiveCallerDiscoveryStream() {inactiveCaller().copyDiscovery();}
    @Test public void testDiscoveryCallerInactiveStream() {discoveryCaller().copyInactive();}
    @Test public void testDiscoveryCallerEnforcedStream() {discoveryCaller().copyEnforced();}
    @Test public void testEnforcedCallerInactiveStream() {enforcedCaller().copyInactive();}
    @Test public void testEnforcedCallerDiscoveryStream() {enforcedCaller().copyDiscovery();}
    @Test public void testInactiveCallerAfterEnforcedCaller() {enforcedCaller().inactiveCaller().copyDefault();}
    @Test public void testInactiveCallerAfterDiscoveryCaller() {discoveryCaller().inactiveCaller().copyDefault();}
    @Test public void testDiscoveryCallerAfterInactiveCaller() {inactiveCaller().discoveryCaller().copyDefault();}
    @Test public void testDiscoveryCallerAfterEnforcedCaller() {enforcedCaller().discoveryCaller().copyDefault();}
    @Test public void testEnforcedCallerAfterInactiveCaller() {inactiveCaller().enforcedCaller().copyDefault();}
    @Test public void testEnforcedCallerAfterDiscoveryCaller() {discoveryCaller().enforcedCaller().copyDefault();}

    final Caller defaultCaller() {return new DefaultCaller();}
    final Caller inactiveCaller() {return new InactiveCaller();}
    final Caller discoveryCaller() {return new DiscoveryCaller();}
    final Caller enforcedCaller() { return new EnforcedCaller();}

    class Caller {
        private Caller next;

        Caller inactiveCaller() {next = new InactiveCaller();return this;}
        Caller discoveryCaller() {next = new DiscoveryCaller();return this;}
        Caller enforcedCaller() {next = new EnforcedCaller();return this;}

        final void copyDefault() {copy(o, ObjectInputStream.class, CONFIG.getDefaultMode());}
        final void copyInactive() {copy(o, InactiveStream.class, ValidationMode.INACTIVE);}
        final void copyDiscovery() {copy(o, DiscoveryStream.class, ValidationMode.DISCOVER);}
        final void copyEnforced() {copy(o, EnforcedStream.class, ValidationMode.ENFORCE);}

        void copy(Object o, Class<? extends ObjectInputStream> c, ValidationMode m) {
            if (next == null) realCopy(o, c, m);
            else next.copy(o, c, m);
        }

        final void realCopy(Object o, Class<? extends ObjectInputStream> c, ValidationMode m) {
            boolean expectSuccess = isWhitelisted(o) || (m != ValidationMode.ENFORCE);
            try {
                copyBySerializing(o, c);
                switch (m) {
                    case ENFORCE:
                        // check this should not have thrown an exception
                        String className = o.getClass().getName();
                        assertTrue(className + " should starts with  java or [, or contains Whitelisted.", className.startsWith("java.") || className.startsWith("[") || className.contains("Whitelisted"));
                        break;
                    case REJECT:
                        throw new AssertionError("REJECT mode should prevent any deserialization");
                }
            } catch (Exception e) {
                if (!!!(e instanceof InvalidClassException)) rethrow(e);
                // check this exception was expected
                assertTrue(m.equals(ENFORCE) || m.equals(REJECT));
                String className = o.getClass().getName();
                assertFalse(className + " should neither start with either java nor contains Whitelisted.", className.startsWith("java.") ||  className.contains("Whitelisted"));
            }
            String logs = capture.getOut();
        }
    }

    class DefaultCaller extends Caller {
        void copy(Object o, Class<? extends ObjectInputStream> c, ValidationMode m) {
            super.copy(o, c, m == null ? CONFIG.getDefaultMode() : m);
        }
    }

    class InactiveCaller extends Caller {
        void copy(Object o, Class<? extends ObjectInputStream> c, ValidationMode m) {super.copy(o, c, ValidationMode.INACTIVE);}
    }

    class DiscoveryCaller extends Caller {
        void copy(Object o, Class<? extends ObjectInputStream> c, ValidationMode m) {super.copy(o, c, ValidationMode.DISCOVER);}
    }

    class EnforcedCaller extends Caller {
        void copy(Object o, Class<? extends ObjectInputStream> c, ValidationMode m) {super.copy(o, c, ValidationMode.ENFORCE);}
    }

    private static boolean isWhitelisted(Object obj) {
        return obj.getClass().getName().startsWith("java.")
                || obj.getClass().getSimpleName().contains("Whitelisted");
    }

    private <S extends ObjectInputStream> Object copyBySerializing(Object original, Class<S> streamClass) {
        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(bytesOut);
            objectOut.writeObject(original);
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
            ObjectInputStream objectIn = streamClass
                    .getConstructor(InputStream.class)
                    .newInstance(bytesIn);
            assertNotNull(objectIn);
            Object copy = objectIn.readObject();
            assertThat(copy, is(equalTo(original)));
            return copy;
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    public static class InactiveStream extends ObjectInputStream {
        public InactiveStream(InputStream in) throws IOException {
            super(in);
        }
    }

    public static class EnforcedStream extends ObjectInputStream {
        public EnforcedStream(InputStream in) throws IOException {
            super(in);
        }
    }

    public static class DiscoveryStream extends EnforcedStream {
        public DiscoveryStream(InputStream in) throws IOException {
            super(in);
        }
    }

    public static abstract class UniqueObject implements Serializable {
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

    public static class BlacklistedObject extends UniqueObject implements Serializable {}

    public static class WhitelistedObject extends UniqueObject implements Serializable {}

    public static class BlacklistedChild extends WhitelistedObject implements Serializable {}

    public static class WhitelistedChild extends BlacklistedObject implements Externalizable {
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

    public enum BlacklistedEnum {UNLISTED_ENUM}
    public enum WhitelistedEnum{WHITELISTED_ENUM}
}
