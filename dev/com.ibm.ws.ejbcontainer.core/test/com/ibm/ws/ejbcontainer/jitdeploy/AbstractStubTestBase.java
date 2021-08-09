/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import java.io.Externalizable;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Map;

import javax.rmi.CORBA.Stub;

import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.portable.ObjectImpl;

import com.ibm.ws.ejbcontainer.jitdeploy._underscore.TestUnderScoreException;
import com.ibm.ws.ejbcontainer.jitdeploy.lowercase.exception.TestLowerCaseException;
import com.ibm.ws.ejbcontainer.jitdeploy.uppercase.EXCEPTION.TestUpperCaseException;
import com.ibm.wsspi.ejbcontainer.JITDeploy;

public abstract class AbstractStubTestBase
                extends AbstractRMITestBase
{
    protected abstract byte[] getStubBytes(Class<?> remoteInterface, String stubClassName, int rmicCompatible);

    private <T> T createStub(Class<T> remoteInterface, int rmicCompatible, String methodName, boolean appEx, TestMethodCall... calls)
    {
        String stubClassName = JITDeploy.getStubClassName(remoteInterface);
        byte[] stubClassBytes = getStubBytes(remoteInterface, stubClassName, rmicCompatible);
        Class<? extends Stub> stubClass = new TestClassLoader().defineClass(stubClassName, stubClassBytes).asSubclass(Stub.class);

        Stub stub;
        try
        {
            stub = stubClass.newInstance();
        } catch (IllegalAccessException ex)
        {
            throw new IllegalStateException(ex);
        } catch (InstantiationException ex)
        {
            throw new IllegalStateException(ex);
        }

        stub._set_delegate(new TestDelegateImpl(new TestMethodCalls(methodName, appEx, calls)));

        @SuppressWarnings("unchecked")
        T uncheckedStub = (T) stub;
        return uncheckedStub;
    }

    private <T> T createStub(Class<T> remoteInterface, int rmicCompatible, TestMethodCall... calls)
    {
        return createStub(remoteInterface, rmicCompatible, null, false, calls);
    }

    @Test
    public void testMutableIds()
    {
        for (int rmicCompatible : getRMICCompatible())
        {
            TestMutableIds stub = createStub(TestMutableIds.class, rmicCompatible);
            ObjectImpl objectImpl = (ObjectImpl) stub;

            String[] ids = objectImpl._ids();
            String[] idsClone = ids.clone();
            ids[0] = "mutable";
            Assert.assertEquals(Arrays.asList(idsClone), Arrays.asList(objectImpl._ids()));
        }
    }

    public static interface TestMutableIds
                    extends Remote
    {
        // Nothing.
    }

    @Test
    public void testPrimitive()
                    throws Exception
    {
        for (int rmicCompatible : getRMICCompatible())
        {
            Assert.assertEquals(true,
                                createStub(TestPrimitive.class,
                                           rmicCompatible,
                                           write("boolean", false),
                                           read("boolean", true))
                                                .testBoolean(false));
            Assert.assertEquals(11,
                                createStub(TestPrimitive.class,
                                           rmicCompatible,
                                           write("octet", (byte) 10),
                                           read("octet", (byte) 11))
                                                .testByte((byte) 10));
            Assert.assertEquals(21,
                                createStub(TestPrimitive.class,
                                           rmicCompatible,
                                           write("short", (short) 20),
                                           read("short", (short) 21))
                                                .testShort((short) 20));
            Assert.assertEquals(31,
                                createStub(TestPrimitive.class,
                                           rmicCompatible,
                                           write("wchar", (char) 30),
                                           read("wchar", (char) 31))
                                                .testChar((char) 30));
            Assert.assertEquals(31,
                                createStub(TestPrimitive.class,
                                           rmicCompatible,
                                           write("long", 30),
                                           read("long", 31))
                                                .testInt(30));
            Assert.assertEquals((float) 1.5,
                                createStub(TestPrimitive.class,
                                           rmicCompatible,
                                           write("float", (float) 1.1),
                                           read("float", (float) 1.5))
                                                .testFloat((float) 1.1), 0);
            Assert.assertEquals(41,
                                createStub(TestPrimitive.class,
                                           rmicCompatible,
                                           write("longlong", (long) 40),
                                           read("longlong", (long) 41))
                                                .testLong(40), 0);
            Assert.assertEquals(2.5,
                                createStub(TestPrimitive.class,
                                           rmicCompatible,
                                           write("double", 2.1),
                                           read("double", 2.5))
                                                .testDouble(2.1), 0);
        }
    }

    public interface TestPrimitive
                    extends Remote
    {
        boolean testBoolean(boolean value)
                        throws RemoteException;

        byte testByte(byte value)
                        throws RemoteException;

        short testShort(short value)
                        throws RemoteException;

        char testChar(char value)
                        throws RemoteException;

        int testInt(int value)
                        throws RemoteException;

        float testFloat(float value)
                        throws RemoteException;

        long testLong(long value)
                        throws RemoteException;

        double testDouble(double value)
                        throws RemoteException;
    }

    @Test
    public void testClass()
                    throws Exception
    {
        for (int rmicCompatible : getRMICCompatible())
        {
            createStub(TestClass.class,
                       rmicCompatible,
                       utilWrite("Any", "any", null),
                       utilRead("Any", "any", null))
                            .testObject(null);
            createStub(TestClass.class,
                       rmicCompatible,
                       write("value@2", null),
                       read("value@1", null))
                            .testString(null);
        }
    }

    public interface TestClass
                    extends Remote
    {
        Object testObject(Object value)
                        throws RemoteException;

        String testString(String value)
                        throws RemoteException;
    }

    @Test
    public void testArray()
                    throws Exception
    {
        for (int rmicCompatible : getRMICCompatible())
        {
            createStub(TestArray.class,
                       rmicCompatible,
                       write("value@2", null),
                       read("value@1", null))
                            .testByteArray(null);
            createStub(TestArray.class,
                       rmicCompatible,
                       write("value@2", null),
                       read("value@1", null))
                            .testByteArrayArray(null);

            createStub(TestArray.class,
                       rmicCompatible,
                       write("value@2", null),
                       read("value@1", null))
                            .testObjectArray(null);
            createStub(TestArray.class,
                       rmicCompatible,
                       write("value@2", null),
                       read("value@1", null))
                            .testObjectArrayArray(null);
        }
    }

    public interface TestArray
                    extends Remote
    {
        byte[] testByteArray(byte[] value)
                        throws RemoteException;

        byte[][] testByteArrayArray(byte[][] value)
                        throws RemoteException;

        Object[] testObjectArray(Object[] value)
                        throws RemoteException;

        Object[][] testObjectArrayArray(Object[][] value)
                        throws RemoteException;
    }

    @Test
    public void testInterface()
                    throws Exception
    {
        for (int rmicCompatible : getRMICCompatible())
        {
            boolean rmicCompatibleValues = com.ibm.wsspi.ejbcontainer.JITDeploy.isRMICCompatibleValues(rmicCompatible);
            boolean isRMICNonIBM = isRMIC() && !JAVA_VENDOR_IBM;
            String abstractInterfaceMethodName = isRMICNonIBM ? "abstract_interface" : "abstract_interface@1";
            // HotSpot rmic uses read_Object()+PortableRemoteObject.narrow
            String objectMethodName = isRMICNonIBM ? "Object" : "Object@1";

            createStub(TestInterface.class,
                       rmicCompatible,
                       utilWrite("Any", "any", null),
                       utilRead("Any", "any", null))
                            .testSerializable(null);
            createStub(TestInterface.class,
                       rmicCompatible,
                       utilWrite("AbstractObject", "abstract_interface", null),
                       read(abstractInterfaceMethodName, null))
                            .testExtendsSerializable(null);

            createStub(TestInterface.class,
                       rmicCompatible,
                       utilWrite("Any", "any", null),
                       utilRead("Any", "any", null))
                            .testExternalizable(null);
            createStub(TestInterface.class,
                       rmicCompatible,
                       utilWrite("AbstractObject", "abstract_interface", null),
                       read(abstractInterfaceMethodName, null))
                            .testExtendsExternalizable(null);

            createStub(TestInterface.class,
                       rmicCompatible,
                       rmicCompatibleValues ? write("Object", null) :
                                       utilWrite("AbstractObject", "abstract_interface", null),
                       rmicCompatibleValues ? read("Object", null) :
                                       read("abstract_interface@1", null))
                            .testCORBAObject(null);
            if (isExtendsCORBAObjectSupported())
            {
                createStub(TestInterface2.class,
                           rmicCompatible,
                           rmicCompatibleValues ? write("Object", null) :
                                           utilWrite("AbstractObject", "abstract_interface", null),
                           rmicCompatibleValues ? read("Object@1", null) :
                                           read("abstract_interface@1", null))
                                .testExtendsCORBAObject(null);
            }
            else
            {
                // TODO: rmic generates uncompilable source for ExtendsCORBAObject.
                System.out.println("Skipping testExtendsCORBAObject");
            }

            createStub(TestInterface.class,
                       rmicCompatible,
                       utilWrite("RemoteObject", "Object", null),
                       read(objectMethodName, null))
                            .testRemote(null);
            createStub(TestInterface.class,
                       rmicCompatible,
                       utilWrite("RemoteObject", "Object", null),
                       read(objectMethodName, null))
                            .testExtendsRemote(null);

            createStub(TestInterface.class,
                       rmicCompatible,
                       utilWrite("AbstractObject", "abstract_interface", null),
                       read(abstractInterfaceMethodName, null))
                            .testEmptyInterface(null);
            createStub(TestInterface.class,
                       rmicCompatible,
                       utilWrite("AbstractObject", "abstract_interface", null),
                       read(abstractInterfaceMethodName, null))
                            .testThrowsRemoteExceptionInterface(null);
            createStub(TestInterface.class,
                       rmicCompatible,
                       utilWrite("AbstractObject", "abstract_interface", null),
                       read(abstractInterfaceMethodName, null))
                            .testThrowsExceptionInterface(null);
            createStub(TestInterface.class,
                       rmicCompatible,
                       utilWrite("AbstractObject", "abstract_interface", null),
                       read(abstractInterfaceMethodName, null))
                            .testThrowsThrowableInterface(null);
            createStub(TestInterface.class,
                       rmicCompatible,
                       rmicCompatibleValues ? write("value@2", null) :
                                       utilWrite("AbstractObject", "abstract_interface", null),
                       rmicCompatibleValues ? read("value@1", null) :
                                       read(abstractInterfaceMethodName, null))
                            .testNonAbstractInterface(null);
        }
    }

    protected boolean isExtendsCORBAObjectSupported()
    {
        return true;
    }

    public interface TestInterface
                    extends Remote
    {
        Serializable testSerializable(Serializable value)
                        throws RemoteException;

        ExtendsSerializable testExtendsSerializable(ExtendsSerializable value)
                        throws RemoteException;

        Externalizable testExternalizable(Externalizable value)
                        throws RemoteException;

        ExtendsExternalizable testExtendsExternalizable(ExtendsExternalizable value)
                        throws RemoteException;

        org.omg.CORBA.Object testCORBAObject(org.omg.CORBA.Object value)
                        throws RemoteException;

        // NOTE: rmic generates uncompilable source for testExtendsCORBAObject,
        // so that method is moved to TestInterface2.

        Remote testRemote(Remote remote)
                        throws RemoteException;

        ExtendsRemote testExtendsRemote(Remote remote)
                        throws RemoteException;

        EmptyInterface testEmptyInterface(EmptyInterface value)
                        throws RemoteException;

        ThrowsRemoteExceptionInterface testThrowsRemoteExceptionInterface(ThrowsRemoteExceptionInterface value)
                        throws RemoteException;

        ThrowsExceptionInterface testThrowsExceptionInterface(ThrowsExceptionInterface value)
                        throws RemoteException;

        ThrowsThrowableInterface testThrowsThrowableInterface(ThrowsThrowableInterface value)
                        throws RemoteException;

        Map<?, ?> testNonAbstractInterface(Map<?, ?> value)
                        throws RemoteException;
    }

    public interface TestInterface2
                    extends Remote
    {
        ExtendsCORBAObject testExtendsCORBAObject(org.omg.CORBA.Object value)
                        throws RemoteException;
    }

    public interface ExtendsSerializable
                    extends Serializable
    {
        // Empty.
    }

    public interface ExtendsExternalizable
                    extends Externalizable
    {
        // Empty.
    }

    public interface ExtendsRemote
                    extends Remote
    {
        // Empty.
    }

    public interface ExtendsCORBAObject
                    extends org.omg.CORBA.Object
    {
        // Empty.
    }

    public interface EmptyInterface
    {
        // Empty.
    }

    public interface ThrowsRemoteExceptionInterface
    {
        void method()
                        throws RemoteException;
    }

    public interface ThrowsExceptionInterface
    {
        void method()
                        throws Exception;
    }

    public interface ThrowsThrowableInterface
    {
        void method()
                        throws Throwable;
    }

    @Test
    public void testExceptionMangling()
                    throws Exception
    {
        for (int rmicCompatible : getRMICCompatible())
        {
            boolean rmicCompatibleExceptions = com.ibm.wsspi.ejbcontainer.JITDeploy.isRMICCompatibleExceptions(rmicCompatible);

            createStub(TestExceptionManglingIntf.class,
                       rmicCompatible,
                       isKeywordOperationMangled() ? "_exception" : "exception",
                       false)
                            .exception();

            Exception exThrown = new Exception();
            try
            {
                createStub(TestExceptionManglingIntf.class,
                           rmicCompatible,
                           null,
                           true,
                           read("string", "IDL:java/lang/Ex:1.0"),
                           read("value@1", exThrown))
                                .throwException();
                Assert.fail();
            } catch (Exception ex)
            {
                // Avoid passing if unrelated exceptions are thrown.
                if (ex != exThrown)
                {
                    throw ex;
                }
            }

            for (String exName : getTestExceptionManglingNames(rmicCompatibleExceptions,
                                                               "IDL:com/ibm/ws/ejbcontainer/jitdeploy/lowercase/exception/TestLowerCaseEx:1.0",
                                                               "IDL:com/ibm/ws/ejbcontainer/jitdeploy/lowercase/_exception/TestLowerCaseEx:1.0"))
            {
                try
                {
                    createStub(TestExceptionManglingIntf.class,
                               rmicCompatible,
                               null,
                               true,
                               read("string", exName),
                               read("value@1", new TestLowerCaseException()))
                                    .throwTestLowerCaseException();
                    Assert.fail();
                } catch (TestLowerCaseException ex)
                {
                    // Pass.
                }
            }

            for (String exName : getTestExceptionManglingNames(rmicCompatibleExceptions,
                                                               "IDL:com/ibm/ws/ejbcontainer/jitdeploy/uppercase/EXCEPTION/TestUpperCaseEx:1.0",
                                                               "IDL:com/ibm/ws/ejbcontainer/jitdeploy/uppercase/_EXCEPTION/TestUpperCaseEx:1.0"))
            {
                try
                {
                    createStub(TestExceptionManglingIntf.class,
                               rmicCompatible,
                               null,
                               true,
                               read("string", exName),
                               read("value@1", new TestUpperCaseException()))
                                    .throwTestUpperCaseException();
                    Assert.fail();
                } catch (TestUpperCaseException ex)
                {
                    // Pass.
                }
            }

            for (String exName : getTestExceptionManglingNames(rmicCompatibleExceptions,
                                                               "IDL:com/ibm/ws/ejbcontainer/jitdeploy/_underscore/TestUnderScoreEx:1.0",
                                                               "IDL:com/ibm/ws/ejbcontainer/jitdeploy/J_underscore/TestUnderScoreEx:1.0"))
            {
                try
                {
                    createStub(TestExceptionManglingIntf.class,
                               rmicCompatible,
                               null,
                               true,
                               read("string", exName),
                               read("value@1", new TestUnderScoreException()))
                                    .throwTestUnderScoreException();
                    Assert.fail();
                } catch (TestUnderScoreException ex)
                {
                    // Pass.
                }
            }
        }
    }

    protected static String[] getTestExceptionManglingNames(boolean rmicCompatibleExceptions, String unmangled, String mangled)
    {
        return rmicCompatibleExceptions ? new String[] { mangled } :
                        new String[] { mangled, unmangled };
    }

    public interface TestExceptionManglingIntf
                    extends Remote
    {
        void exception()
                        throws RemoteException;

        void throwException()
                        throws RemoteException, Exception;

        void throwTestLowerCaseException()
                        throws RemoteException, TestLowerCaseException;

        void throwTestUpperCaseException()
                        throws RemoteException, TestUpperCaseException;

        void throwTestUnderScoreException()
                        throws RemoteException, TestUnderScoreException;
    }
}
