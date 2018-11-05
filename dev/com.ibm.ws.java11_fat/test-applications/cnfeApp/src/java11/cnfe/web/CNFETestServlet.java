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
package java11.cnfe.web;

import static org.junit.Assert.assertTrue;

import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/CNFETestServlet")
public class CNFETestServlet extends FATServlet {

    private static final String CNFE_MSGID = "CWWKL0084W";

    public void testClassForNameTCCL() throws Exception {
        try {
            Class.forName("javax.xml.bind.annotation.DomHandler", false, Thread.currentThread().getContextClassLoader());
            Assert.fail("Should not be able to load JAX-B class in the server.");
        } catch (ClassNotFoundException expected) {
            expected.printStackTrace();
        }
    }

    public void testClassForName() throws Exception {
        try {
            Class.forName("javax.jws.WebService");
            Assert.fail("Should not be able to load JAX-WS annotation in the server.");
        } catch (ClassNotFoundException expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void testClassLoaderLoadJAXB() throws Exception {
        try {
            CNFETestServlet.class.getClassLoader().loadClass("javax.xml.bind.JAXBException");
        } catch (ClassNotFoundException expected) {
            expected.printStackTrace();
            assertContains(expected.getMessage(), "jaxb-2.2");
            assertContains(expected.getMessage(), CNFE_MSGID);
        }
    }

    @Test
    public void testUseClassThatUsesJAXB() throws Exception {
        try {
            new SomeJAXBClass().useJAXB();
        } catch (NoClassDefFoundError expected) {
            expected.printStackTrace();
            Throwable t1 = expected.getCause();
            assertTrue("First cause of NCDFE should have been a CNFE but was: " + t1.getClass(), t1 instanceof ClassNotFoundException);
            ClassNotFoundException cnfe = (ClassNotFoundException) t1;
            assertContains(cnfe.getMessage(), CNFE_MSGID);
            assertContains(cnfe.getMessage(), "javax.xml.bind.JAXBContext");
        }
    }

    @Test
    public void testNewClassThatUsesTransaction() throws Exception {
        try {
            Class.forName("java11.cnfe.web.SomeJDBCClass");
        } catch (NoClassDefFoundError expected) {
            expected.printStackTrace();
            Throwable t1 = expected.getCause();
            assertTrue("First cause of NCDFE should have been a CNFE but was: " + t1.getClass(), t1 instanceof ClassNotFoundException);
            ClassNotFoundException cnfe = (ClassNotFoundException) t1;
            assertContains(cnfe.getMessage(), CNFE_MSGID);
            assertContains(cnfe.getMessage(), "javax.transaction.InvalidTransactionException");
        }
    }

    private void assertContains(String str, String lookFor) {
        assertTrue("Did not find string '" + lookFor + "' in the message: " + str, str != null && str.contains(lookFor));
    }

}
