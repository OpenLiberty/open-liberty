/* ============================================================================
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ============================================================================
 */
package testjms.web.impl;

import org.junit.Assert;
import testjms.web.JmsMessageTests;
import testjms.web.util.OutputHelper;

import javax.jms.Message;

public class JmsMessageTestsImpl extends JmsTestsBase implements JmsMessageTests {
    private static final String KEY = "KEY";

    public JmsMessageTestsImpl(ConnectionFactoryType cfType) { super(cfType); }

    @Override
    public void testMessage_setObjectProperty_String() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final Message m = ts.session.createMessage();
            m.setObjectProperty(KEY, testString);
            final Message copy = ts.sendAndReceive(m, Message.class, ts.queue);
            final Object r = copy.getObjectProperty(KEY);
            if (!testString.equals(r)) {
                Assert.fail(OutputHelper.comparisonFailureDescription(testString, r));
            }
        }
    }

    @Override
    public void testMessage_setStringProperty() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final Message m = ts.session.createMessage();
            m.setStringProperty(KEY, testString);
            final Message copy = ts.sendAndReceive(m, Message.class, ts.queue);
            final Object r = copy.getStringProperty(KEY);
            if (!testString.equals(r)) {
                Assert.fail(OutputHelper.comparisonFailureDescription(testString, r));
            }
        }
    }
}
