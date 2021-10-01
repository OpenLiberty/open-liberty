/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.home2x.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.RemoveException;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.rmi.CORBA.Stub;
import javax.rmi.CORBA.Util;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.portable.ApplicationException;
import org.omg.CORBA.portable.RemarshalException;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

import com.ibm.ws.ejbcontainer.remote.fat.home2x.ejb.RMICCompatImplements;
import com.ibm.ws.ejbcontainer.remote.fat.home2x.ejb.RMICCompatImplements2;
import com.ibm.ws.ejbcontainer.remote.fat.home2x.ejb.RMICCompatParam;
import com.ibm.ws.ejbcontainer.remote.fat.home2x.ejb.RMICCompatReturn;
import com.ibm.ws.ejbcontainer.remote.fat.home2x.ejb.RMICCompatTestDelegate;
import com.ibm.ws.ejbcontainer.remote.fat.home2x.ejb.TestEJBHome2x;
import com.ibm.ws.ejbcontainer.remote.fat.home2x.ejb.TestEJBObject2x;

import componenttest.app.FATServlet;

@WebServlet("/EJBHome2xTestServlet")
@SuppressWarnings("serial")
public class EJBHome2xTestServlet extends FATServlet {
    private static final Logger logger = Logger.getLogger(EJBHome2xTestServlet.class.getName());

    @EJB(name = "ejb/home")
    private TestEJBHome2x home;

    private static void assertEquals(Object expected, Object actual) {
        if (!(expected == null ? actual == null : expected.equals(actual))) {
            throw new IllegalStateException("actual=" + actual + " != expected=" + expected);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T narrow(Object from, Class<T> to) {
        return (T) PortableRemoteObject.narrow(from, to);
    }

    @Test
    public void testEJBHomeInjection() throws Exception {
        assertEquals("abcd", home.create().echo("abcd"));
    }

    @Test
    public void testEJBHomeLookup() throws Exception {
        assertEquals("abcd", ((TestEJBHome2x) new InitialContext().lookup("java:app/EJBHome2xTestEJB/TestEJBHome2xBean")).create().echo("abcd"));
    }

    /**
     * Call a remote method with parameter/return values that should use
     * write_value/read_value. The "WAS EJB 3" marshalling would use
     * writeAbstractObject/read_abstract_interface, so a stub/tie mismatch will
     * result in an OutOfMemoryError.
     */
    @Test
    public void testEJBHomeWriteValue() throws Exception {
        List<?> expected = new ArrayList<Object>(Arrays.asList("a", "b", "c"));
        List<?> actual = home.create().testWriteValue(expected);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("actual=" + actual + " != expected=" + expected);
        }
    }

    /**
     * Call a remote method directly (without a stub) with parameter/return
     * values that should use write_value/read_value. The "WAS EJB 3"
     * marshalling would use writeAbstractObject/read_abstract_interface,
     * so a tie mismatch will result in an OutOfMemoryError.
     */
    @Test
    public void testEJBHomeWriteValueDirect() throws Exception {
        List<?> expected = new ArrayList<Object>(Arrays.asList("a", "b", "c"));
        List<?> actual = stubTestWriteValue((Stub) home.create(), expected);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("actual=" + actual + " != expected=" + expected);
        }
    }

    private List<?> stubTestWriteValue(Stub stub, List<?> value) throws RemoteException {
        while (true) {
            InputStream in = null;
            try {
                try {
                    OutputStream out = (OutputStream) stub._request("testWriteValue", true);
                    // RMIC compatible marshalling using write_value...
                    out.write_value((Serializable) value, List.class);
                    in = (InputStream) stub._invoke(out);
                    // ...and read_value.
                    return (List<?>) in.read_value(List.class);
                } catch (ApplicationException ex) {
                    in = (InputStream) ex.getInputStream();
                    String id = in.read_string();
                    throw new UnexpectedException(id);
                } catch (RemarshalException ex) {
                    continue;
                }
            } catch (SystemException ex) {
                throw Util.mapSystemException(ex);
            } finally {
                stub._releaseReply(in);
            }
        }
    }

    private static String getStubClassName(String className) {
        int index = className.lastIndexOf('.');
        return className.substring(0, index + 1) + '_' + className.substring(index + 1) + "_Stub";
    }

    private void testEJBHomeRecursiveStub(Class<?> c) throws Exception {
        // Manually create a stub, and set a test delegate that only allows
        // write_value and read_value operations for a List value.  This ensures
        // the stub was generated with RMIC compatibility or else the stub would
        // use Util.writeAbstractObject and read_abstract_interface.
        Class<? extends Stub> stubClass = Thread.currentThread().getContextClassLoader().loadClass(getStubClassName(c.getName())).asSubclass(Stub.class);
        Stub stub = stubClass.newInstance();
        stub._set_delegate(new RMICCompatTestDelegate());

        List<?> expected = new ArrayList<Object>(Arrays.asList("a", "b", "c"));
        List<?> actual = (List<?>) stubClass.getMethod("testWriteValue", List.class).invoke(stub, expected);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("actual=" + actual + " != expected=" + expected);
        }
    }

    /**
     * Ensure that RMIC compatible stubs are generated for all "remoteable"
     * interfaces recursively referenced by the EJB interfaces.
     */
    @Test
    public void testEJBHomeRecursiveStubs() throws Exception {
        // TestEJBObject2x extends RMICCompatImplements
        testEJBHomeRecursiveStub(RMICCompatImplements.class);
        // RMICCompatImplements extends RMICCompatImplements2
        testEJBHomeRecursiveStub(RMICCompatImplements2.class);
        // RMICCompatImplements2.testRecursive has an RMICCompatParam parameter
        testEJBHomeRecursiveStub(RMICCompatParam.class);
        // RMICCompatImplements2.testRecursiveRMIC has an RMICCompatReturn return value
        testEJBHomeRecursiveStub(RMICCompatReturn.class);
    }

    @Test
    public void testEJBHomeContextLookup() throws Exception {
        // narrow not required per spec.
        assertEquals("abcd", home.create().lookupTestEJBHome("ejb/home").create().echo("abcd"));
    }

    @Test
    public void testSessionContextGetEJBObject() throws Exception {
        assertEquals("abc", home.create().getSessionContextEJBObject().echo("abc"));
    }

    @Test
    public void testSessionContextGetEJBHome() throws Exception {
        assertEquals("abc", home.create().getSessionContextEJBHome().create().echo("abc"));
    }

    @Test
    public void testEJBMetaDataGetEJBHome() throws Exception {
        assertEquals("abcd", ((TestEJBHome2x) home.getEJBMetaData().getEJBHome()).create().echo("abcd"));
    }

    @Test
    public void testEJBMetaDataGetHomeInterfaceClass() throws Exception {
        assertEquals(TestEJBHome2x.class, home.getEJBMetaData().getHomeInterfaceClass());
    }

    @Test
    public void testEJBMetaDataGetPrimaryKeyClass() throws Exception {
        try {
            assertEquals(null, home.getEJBMetaData().getPrimaryKeyClass());
            throw new IllegalStateException("expected EJBException from getPrimaryKeyClass");
        } catch (EJBException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    @Test
    public void testEJBMetaDataGetRemoteInterfaceClass() throws Exception {
        assertEquals(TestEJBObject2x.class, home.getEJBMetaData().getRemoteInterfaceClass());
    }

    @Test
    public void testEJBMetaDataIsSession() throws Exception {
        assertEquals(true, home.getEJBMetaData().isSession());
    }

    @Test
    public void testEJBMetaDataIsStatelessSession() throws Exception {
        assertEquals(true, home.getEJBMetaData().isStatelessSession());
    }

    private static <T extends Serializable> T copy(T object) throws IOException, ClassNotFoundException {
        System.out.println("Copying " + object);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        @SuppressWarnings("unchecked")
        T result = (T) ois.readObject();
        return result;
    }

    @Test
    public void testHomeHandle() throws Exception {
        assertEquals("abcd", narrow(home.getHomeHandle().getEJBHome(), TestEJBHome2x.class).create().echo("abcd"));
    }

    @Test
    public void testHomeHandleCopy() throws Exception {
        assertEquals("abcd", narrow(copy(home.getHomeHandle()).getEJBHome(), TestEJBHome2x.class).create().echo("abcd"));
    }

    @Test
    public void testEJBHomeRemoveByHandle() throws Exception {
        home.remove(home.create().getHandle());
    }

    @Test
    public void testEJBHomeRemoveByPrimaryKey() throws Exception {
        try {
            home.remove(home.create());
            throw new IllegalStateException("expected RemoveException from remove");
        } catch (RemoveException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    @Test
    public void testEJBObjectGetEJBHome() throws Exception {
        assertEquals("abcd", narrow(home.create().getEJBHome(), TestEJBHome2x.class).create().echo("abcd"));
    }

    @Test
    public void testHandle() throws Exception {
        assertEquals("abcd", narrow(home.create().getHandle().getEJBObject(), TestEJBObject2x.class).echo("abcd"));
    }

    @Test
    public void testHandleCopy() throws Exception {
        assertEquals("abcd", narrow(copy(home.create().getHandle()).getEJBObject(), TestEJBObject2x.class).echo("abcd"));
    }

    @Test
    public void testEJBObjectGetPrimaryKey() throws Exception {
        try {
            home.create().getPrimaryKey();
        } catch (RemoteException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    @Test
    public void testEJBObjectRemove() throws Exception {
        home.create().remove();
    }

    @Test
    public void testEJBObjectIsIdentical() throws Exception {
        assertEquals(true, home.create().isIdentical(home.create()));
    }

    @Test
    public void testHandleDelegateLookup() throws Exception {
        if (new InitialContext().lookup("java:comp/HandleDelegate") == null) {
            throw new IllegalStateException("expected java:comp/HandleDelegate");
        }
    }
}
