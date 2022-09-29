/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.remoteEJB.web;

import static org.junit.Assert.assertFalse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.junit.Test;

import com.ibm.ws.remoteEJB.shared.TestBeanRemote;

@SuppressWarnings("serial")
@WebServlet("/RemoteEJBClient")
public class RemoteEJBClient extends EJBClient {

    @Override
    public void init() {
        System.getProperties().entrySet().stream().forEach(e -> System.out.println("Prop: " + e.getKey() + " -> " + e.getValue()));
        System.getenv().entrySet().stream().forEach(e -> System.out.println("Env: " + e.getKey() + " -> " + e.getValue()));

        final int remoteIIOPPort = Integer.getInteger("bvt.prop.IIOP.secondary");

        try {
            Object found = new InitialContext()
                            .lookup("corbaname:iiop:localhost:" + remoteIIOPPort
                                    + "/NameService#ejb/global/TestBeanApp/TestBeanEJB/TestBean!com.ibm.ws.remoteEJB.shared.TestBeanRemote");
            bean = (TestBeanRemote) PortableRemoteObject.narrow(found, TestBeanRemote.class);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBeanIsRemote(HttpServletRequest request,
                                 HttpServletResponse response) throws NotSupportedException, SystemException, NamingException {
        assertFalse("Bean is running locally", System.getProperty(SERVER_NAME_PROPERTY).equals(bean.getProperty(SERVER_NAME_PROPERTY)));
    }

    /* The methods below override the tests that will fail until propagation is implemented */
    @Override
    public void testRequiredWith(HttpServletRequest request,
                                 HttpServletResponse response) throws NotSupportedException, SystemException, NamingException {
    }

    @Override
    public void testMandatoryWith(HttpServletRequest request,
                                  HttpServletResponse response) throws NotSupportedException, SystemException, NamingException {
    }

    @Override
    public void testSupportsWith(HttpServletRequest request,
                                 HttpServletResponse response) throws NotSupportedException, SystemException, NamingException {
    }
}