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
package localClient;

import static org.junit.Assert.assertTrue;

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
@WebServlet("/LocalEJBClient")
public class LocalEJBClient extends EJBClient {

    @Override
    public void init() throws ServletException {
        try {
            final Object found = new InitialContext()
                            .lookup("shared.TestBeanRemote");
            bean = (TestBeanRemote) PortableRemoteObject.narrow(found, TestBeanRemote.class);
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    @Test
    public void testBeanIsLocal(HttpServletRequest request,
                                HttpServletResponse response) throws NotSupportedException, SystemException, NamingException {
        assertTrue("Bean is running remotely", System.getProperty(SERVER_NAME_PROPERTY).equals(bean.getProperty(SERVER_NAME_PROPERTY)));
    }
}