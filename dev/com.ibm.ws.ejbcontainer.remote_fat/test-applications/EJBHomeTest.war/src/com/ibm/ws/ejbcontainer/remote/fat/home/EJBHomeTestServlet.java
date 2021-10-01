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
package com.ibm.ws.ejbcontainer.remote.fat.home;

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
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.portable.ApplicationException;
import org.omg.CORBA.portable.RemarshalException;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;

import componenttest.app.FATServlet;

@WebServlet("/EJBHomeTestServlet")
@SuppressWarnings("serial")
public class EJBHomeTestServlet extends FATServlet {
    private static final Logger logger = Logger.getLogger(EJBHomeTestServlet.class.getName());
    private static final String rmicCompatibleProperty = System.getProperty("com.ibm.websphere.ejbcontainer.rmicCompatible");
    private static final boolean rmicCompatible = EJB.class.getName().startsWith("jakarta.")
                                                  || ("all".equalsIgnoreCase(rmicCompatibleProperty) || "values".equalsIgnoreCase(rmicCompatibleProperty));

    @EJB(name = "ejb/home")
    private TestEJBHome home;

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
    public void testEJBHomeInjection_EJBHomeTest() throws Exception {
        assertEquals("abcd", home.create().echo("abcd"));
    }

    @Test
    public void testEJBHomeLookup_EJBHomeTest() throws Exception {
        assertEquals("abcd", ((TestEJBHome) new InitialContext().lookup("java:module/TestEJBHomeBean")).create().echo("abcd"));
    }

    private Object cosNamingResolve(String... names) throws Exception {
        ORB orb = (ORB) new InitialContext().lookup("java:comp/ORB");

        NamingContext context = NamingContextHelper.narrow(orb.resolve_initial_references("NameService"));
        logger.info("resolved initial reference to NameService");
        for (int i = 0; i < names.length - 1; i++) {
            logger.info("resolving context " + names[i] + " relative to " + context);
            context = NamingContextHelper.narrow(context.resolve(new NameComponent[] { new NameComponent(names[i], "") }));
        }

        logger.info("resolving object " + names[names.length - 1] + " relative to " + context);
        return context.resolve(new NameComponent[] { new NameComponent(names[names.length - 1], "") });
    }

    @Test
    public void testEJBHomeCosNamingResolve() throws Exception {
        assertEquals("abcd", narrow(cosNamingResolve("ejb", "global", "EJBHomeTest", "TestEJBHomeBean!" + TestEJBHome.class.getName()), TestEJBHome.class).create().echo("abcd"));
    }

    /**
     * Call a remote method with parameter/return values that should use
     * write_value/read_value. The "WAS EJB 3" marshalling actually uses
     * writeAbstractObject/read_abstract_interface, so a stub/tie mismatch will
     * result in an OutOfMemoryError.
     */
    @Test
    public void testEJBHomeWriteValue_EJBHomeTest() throws Exception {
        List<?> expected = new ArrayList<Object>(Arrays.asList("a", "b", "c"));
        List<?> actual = home.create().testWriteValue(expected);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("actual=" + actual + " != expected=" + expected);
        }
    }

    /**
     * Call a remote method directly (without a stub) with parameter/return
     * values that should use write_value/read_value. The "WAS EJB 3"
     * marshalling actually uses writeAbstractObject/read_abstract_interface,
     * so a tie mismatch will result in an OutOfMemoryError.
     * The Enterprise Beans 4.0 marshalling is compatible with RMIC and uses
     * write_value/read_value so a tie mismatch will result in
     * CORBA.MARSHAL: Illegal valuetype.
     */
    @Test
    public void testEJBHomeWriteValueDirect_EJBHomeTest() throws Exception {
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
                    if (rmicCompatible) {
                        // Enterprise Beans 4.0 (or rmicCompatible) marshalling using write_value..
                        out.write_value((Serializable) value, List.class);
                    } else {
                        // "WAS EJB 3" marshalling using writeAbstractObject...
                        Util.writeAbstractObject(out, value);
                    }
                    in = (InputStream) stub._invoke(out);
                    if (rmicCompatible) {
                        // ...and read_value.
                        return (List<?>) in.read_value(List.class);
                    } else {
                        // ...and read_abstract_interface.
                        return (List<?>) in.read_abstract_interface(List.class);
                    }
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

    @Test
    public void testEJBHomeContextLookup_EJBHomeTest() throws Exception {
        // narrow not required per spec.
        assertEquals("abcd", home.create().lookupTestEJBHome("ejb/home").create().echo("abcd"));
    }

    @Test
    public void testSessionContextGetEJBObject_EJBHomeTest() throws Exception {
        assertEquals("abc", home.create().getSessionContextEJBObject().echo("abc"));
    }

    @Test
    public void testSessionContextGetEJBHome_EJBHomeTest() throws Exception {
        assertEquals("abc", home.create().getSessionContextEJBHome().create().echo("abc"));
    }

    @Test
    public void testEJBMetaDataGetEJBHome_EJBHomeTest() throws Exception {
        assertEquals("abcd", ((TestEJBHome) home.getEJBMetaData().getEJBHome()).create().echo("abcd"));
    }

    @Test
    public void testEJBMetaDataGetHomeInterfaceClass_EJBHomeTest() throws Exception {
        assertEquals(TestEJBHome.class, home.getEJBMetaData().getHomeInterfaceClass());
    }

    @Test
    public void testEJBMetaDataGetPrimaryKeyClass_EJBHomeTest() throws Exception {
        try {
            assertEquals(null, home.getEJBMetaData().getPrimaryKeyClass());
            throw new IllegalStateException("expected EJBException from getPrimaryKeyClass");
        } catch (EJBException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    @Test
    public void testEJBMetaDataGetRemoteInterfaceClass_EJBHomeTest() throws Exception {
        assertEquals(TestEJBObject.class, home.getEJBMetaData().getRemoteInterfaceClass());
    }

    @Test
    public void testEJBMetaDataIsSession_EJBHomeTest() throws Exception {
        assertEquals(true, home.getEJBMetaData().isSession());
    }

    @Test
    public void testEJBMetaDataIsStatelessSession_EJBHomeTest() throws Exception {
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
    public void testHomeHandle_EJBHomeTest() throws Exception {
        assertEquals("abcd", narrow(home.getHomeHandle().getEJBHome(), TestEJBHome.class).create().echo("abcd"));
    }

    @Test
    public void testHomeHandleCopy_EJBHomeTest() throws Exception {
        assertEquals("abcd", narrow(copy(home.getHomeHandle()).getEJBHome(), TestEJBHome.class).create().echo("abcd"));
    }

    @Test
    public void testEJBHomeRemoveByHandle_EJBHomeTest() throws Exception {
        home.remove(home.create().getHandle());
    }

    @Test
    public void testEJBHomeRemoveByPrimaryKey_EJBHomeTest() throws Exception {
        try {
            home.remove(home.create());
            throw new IllegalStateException("expected RemoveException from remove");
        } catch (RemoveException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    @Test
    public void testEJBObjectGetEJBHome_EJBHomeTest() throws Exception {
        assertEquals("abcd", narrow(home.create().getEJBHome(), TestEJBHome.class).create().echo("abcd"));
    }

    @Test
    public void testHandle_EJBHomeTest() throws Exception {
        assertEquals("abcd", narrow(home.create().getHandle().getEJBObject(), TestEJBObject.class).echo("abcd"));
    }

    @Test
    public void testHandleCopy_EJBHomeTest() throws Exception {
        assertEquals("abcd", narrow(copy(home.create().getHandle()).getEJBObject(), TestEJBObject.class).echo("abcd"));
    }

    @Test
    public void testEJBObjectGetPrimaryKey_EJBHomeTest() throws Exception {
        try {
            home.create().getPrimaryKey();
        } catch (RemoteException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    @Test
    public void testEJBObjectRemove_EJBHomeTest() throws Exception {
        home.create().remove();
    }

    @Test
    public void testEJBObjectIsIdentical_EJBHomeTest() throws Exception {
        assertEquals(true, home.create().isIdentical(home.create()));
    }

    @Test
    public void testHandleDelegateLookup_EJBHomeTest() throws Exception {
        if (new InitialContext().lookup("java:comp/HandleDelegate") == null) {
            throw new IllegalStateException("expected java:comp/HandleDelegate");
        }
    }
}
