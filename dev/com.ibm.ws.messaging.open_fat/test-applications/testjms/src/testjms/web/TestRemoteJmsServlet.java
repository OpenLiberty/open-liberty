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
package testjms.web;

import componenttest.app.FATServlet;
import org.junit.Test;
import testjms.web.impl.ConnectionFactoryType;
import testjms.web.impl.JmsTestsImpl;

import javax.servlet.annotation.WebServlet;

@WebServlet("/TestRemoteJmsServlet")
public class TestRemoteJmsServlet extends FATServlet implements JmsTests {
    private final JmsTests jmsTests = new JmsTestsImpl(ConnectionFactoryType.RemoteCF);

    @Test
    @Override
    public void basicTest() throws Exception {
        jmsTests.basicTest();
    }

    @Test
    @Override
    public void testBasicJmsLookup() throws Exception {
        jmsTests.testBasicJmsLookup();
    }

    @Test
    @Override
    public void testClearQueue() throws Exception {
        jmsTests.testClearQueue();
    }
}
