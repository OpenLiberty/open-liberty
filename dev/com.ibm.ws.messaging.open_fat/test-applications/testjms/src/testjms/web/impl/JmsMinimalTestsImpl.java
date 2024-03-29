/* ============================================================================
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ============================================================================
 */
package testjms.web.impl;

import javax.jms.Message;

import org.junit.Assert;
import testjms.web.JmsMinimalTests;

public class JmsMinimalTestsImpl extends JmsTestsBase implements JmsMinimalTests {
    public JmsMinimalTestsImpl(ConnectionFactoryType cfType) {
        super(cfType);
    }

    @Override
    public void basicTest() {
        System.out.println("Test is running in an HttpServlet");
        Assert.assertTrue("Can also use JUnit assertions", true);
    }

    @Override
    public void testBasicJmsLookup() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
        }
    }

    @Override
    public void testClearQueue() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final Message m = ts.session.createMessage();
            ts.send(m, ts.queue);
            Assert.assertEquals("Queue did not hold one message", 1, ts.clearQueue(ts.queue));
            Assert.assertEquals("Queue was not empty",0, ts.clearQueue(ts.queue));
        }
    }
}
