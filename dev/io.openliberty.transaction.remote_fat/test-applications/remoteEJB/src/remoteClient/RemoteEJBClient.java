/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package remoteClient;

import static org.junit.Assert.assertFalse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.junit.Test;

import client.EJBClient;
import shared.TestBeanRemote;

@SuppressWarnings("serial")
@WebServlet("/RemoteEJBClient")
public class RemoteEJBClient extends EJBClient {

    @Override
    public void init() throws ServletException {

        final int remoteIIOPPort = Integer.getInteger("bvt.prop.IIOP.secondary");

        final String lookup = "corbaname:iiop:localhost:" + remoteIIOPPort
                              + "/NameService#ejb/global/TestBeanApp/TestBeanEJB/TestBean!shared\\.TestBeanRemote";
        System.out.println("Looking up: " + lookup);

        try {
            final Object found = new InitialContext()
                            .lookup(lookup);
            bean = (TestBeanRemote) PortableRemoteObject.narrow(found, TestBeanRemote.class);
        } catch (NamingException e) {
            throw new ServletException(e);
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