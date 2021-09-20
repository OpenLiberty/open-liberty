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
package com.ibm.ws.ejbcontainer.remote.fat.crossapp.client;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared.CrossAppBusinessRMI;
import com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared.CrossAppBusinessRemote;
import com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared.CrossAppEJBHome;
import com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared.CrossAppEJBHome2x;

import componenttest.app.FATServlet;

@WebServlet("/CrossAppTestServlet")
@SuppressWarnings("serial")
public class CrossAppTestServlet extends FATServlet {
    private static final String JNDI_HOME = "java:global/CrossAppRemoteEJB/CrossAppRemoteBean!com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared.CrossAppEJBHome";
    private static final String JNDI_RMI = "java:global/CrossAppRemoteEJB/CrossAppRemoteBean!com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared.CrossAppBusinessRMI";
    private static final String JNDI_REMOTE = "java:global/CrossAppRemoteEJB/CrossAppRemoteBean!com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared.CrossAppBusinessRemote";
    private static final String JNDI_2X_HOME = "java:global/CrossApp2xTest/CrossApp2xEJB/CrossAppEJBHome2xBean!com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared.CrossAppEJBHome2x";

    @EJB(name = "ejb/home", lookup = JNDI_HOME)
    private CrossAppEJBHome home;
    @EJB(name = "ejb2x/home", lookup = JNDI_2X_HOME)
    private CrossAppEJBHome2x home2X;
    @EJB(name = "ejb/rmi", lookup = JNDI_RMI)
    private CrossAppBusinessRMI businessRMI;
    @EJB(name = "ejb/remote", lookup = JNDI_REMOTE)
    private CrossAppBusinessRemote businessRemote;

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException("actual=" + actual + " != expected=" + expected);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T narrow(Object from, Class<T> to) {
        return (T) PortableRemoteObject.narrow(from, to);
    }

    @Test
    public void testHomeInjection() throws Exception {
        assertEquals("abcd", home.create().echo("abcd"));
    }

    @Test
    public void test2XHomeInjection() throws Exception {
        assertEquals("abcd", home2X.create().echo("abcd"));
    }

    @Test
    public void testHomeLookup() throws Exception {
        assertEquals("abcd", ((CrossAppEJBHome) new InitialContext().lookup(JNDI_HOME)).create().echo("abcd"));
    }

    //Narrow is not needed anymore but regression testing
    @Test
    public void testHomeLookupWithNarrow() throws Exception {

        assertEquals("abcd", narrow(new InitialContext().lookup(JNDI_HOME), CrossAppEJBHome.class).create().echo("abcd"));
    }

    @Test
    public void test2XHomeLookup() throws Exception {
        assertEquals("abcd", ((CrossAppEJBHome2x) new InitialContext().lookup(JNDI_2X_HOME)).create().echo("abcd"));
    }

    //Narrow is not needed anymore but regression testing
    @Test
    public void test2XHomeLookupWithNarrow() throws Exception {
        assertEquals("abcd", narrow(new InitialContext().lookup(JNDI_2X_HOME), CrossAppEJBHome2x.class).create().echo("abcd"));
    }

    @Test
    public void testRMIInjection() throws Exception {
        assertEquals("abcd", businessRMI.echo("abcd"));
    }

    @Test
    public void testRMILookup() throws Exception {
        assertEquals("abcd", ((CrossAppBusinessRMI) new InitialContext().lookup(JNDI_RMI)).echo("abcd"));
    }

    @Test
    public void testRemoteInjection() throws Exception {
        assertEquals("abcd", businessRMI.echo("abcd"));
    }

    @Test
    public void testRemoteLookup() throws Exception {
        assertEquals("abcd", ((CrossAppBusinessRemote) new InitialContext().lookup(JNDI_REMOTE)).echo("abcd"));
    }
}
