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
package servlets;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.servlet.annotation.WebServlet;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/JavaCompServlet")
public class JavaCompServlet extends FATServlet {

    @Test
    public void testListJavaComp() throws Exception {
        NamingEnumeration<NameClassPair> ne = ((Context) new InitialContext().lookup("java:comp")).list("");
        Map<String, String> found = Collections.list(ne)
                        .stream()
                        .filter((n) -> n.getName().contains("Transaction"))
                        .collect(Collectors.toMap((n) -> n.getName(), (n) -> n.getClassName()));
        assertEquals("Wrong number found: " + found, 3, found.size());
        assertEquals("No TransactionManager", TransactionManager.class.getName(), found.get("TransactionManager"));
        assertEquals("No TransactionSynchronizationRegistry", TransactionSynchronizationRegistry.class.getName(), found.get("TransactionSynchronizationRegistry"));
        assertEquals("No UserTransaction", UserTransaction.class.getName(), found.get("UserTransaction"));
    }

    @Test
    public void testListJavaCompWebSphere() throws Exception {
        NamingEnumeration<NameClassPair> ne = ((Context) new InitialContext().lookup("java:comp/websphere")).list("");
        Map<String, String> found = Collections.list(ne)
                        .stream()
                        .collect(Collectors.toMap((n) -> n.getName(), (n) -> n.getClassName()));
        assertEquals("Wrong number found: " + found, 2, found.size());
        assertEquals("No ExtendedJTATransaction", "com.ibm.websphere.jtaextensions.ExtendedJTATransaction", found.get("ExtendedJTATransaction"));
        assertEquals("No UOWManager", com.ibm.wsspi.uow.UOWManager.class.getName(), found.get("UOWManager"));
    }
}
