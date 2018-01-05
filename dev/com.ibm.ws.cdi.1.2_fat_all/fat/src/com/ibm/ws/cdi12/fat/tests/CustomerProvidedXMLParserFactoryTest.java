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
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

/**
 * Tests the case where a customer provides their own SAX parser factory.
 * It is possible that an application that contains a beans.xml might also
 * package their own implementation of SAXParserFactory. In that case Liberty
 * needs to ensure that it uses a Liberty-supplied parser factory, and not the
 * customer's. If we use the customer's then we run into classloading problems
 * because we have already loaded and use the JDK's version of <code>
 * javax.xml.parsers.SAXParserFactory</code> - if the application provides this
 * same class, we will have a ClassCastException. This test verifies that we
 * can parse the beans.xml file without loading the customer's SAXParserFactory
 * when one is supplied.
 */
public class CustomerProvidedXMLParserFactoryTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12UserSAXParserFactory");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    /**
     * Test bean manager can be looked up via java:comp/BeanManager
     *
     * @throws Exception
     */
    @Test
    public void testBeansXMLIsParsedWithoutUsingCustomerSAXParserFactory() throws Exception {
        assertTrue("App with custom SAXParserFactory did not start successfully",
                   SHARED_SERVER.getLibertyServer().findStringsInLogs("CWWKZ0001I.*userSAXParserFactory").size() > 0);
        assertEquals("User's SAXParserFactory impl was used instead of Liberty's", 0,
                     SHARED_SERVER.getLibertyServer().findStringsInLogs("FAILED").size());

    }

}
