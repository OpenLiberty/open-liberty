/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConnectionFactoryDefinition;
import jakarta.servlet.annotation.WebServlet;
import ra.jms.cf.DummyJMSConnectionFactoryImpl;

@JMSConnectionFactoryDefinition(name = "java:comp/env/jms/dummycfd",
                                description = "Test JMS Connection Factory",
                                resourceAdapter = "testJMSCFDResource.jmsconnectionfactory",
                                interfaceName = "jakarta.jms.ConnectionFactory")
@WebServlet("/*")
public class JMSCFDServlet extends FATServlet {

    @Resource(name = "jms/dummycfdRef", lookup = "java:comp/env/jms/dummycfd")
    ConnectionFactory dummy;

    public void testJMSCFDResource() throws Exception {
        assertNotNull(dummy);
        assertTrue(dummy instanceof DummyJMSConnectionFactoryImpl);
    }
}
