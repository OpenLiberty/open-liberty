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
import testjms.web.JmsStreamMessageTests;
import testjms.web.util.OutputHelper;

import javax.jms.StreamMessage;

public class JmsStreamMessageTestsImpl extends JmsTestsBase implements JmsStreamMessageTests {
    public JmsStreamMessageTestsImpl(ConnectionFactoryType cfType) { super(cfType); }

    @Override
    public void testStreamMessage_writeObject_String() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final StreamMessage m = ts.session.createStreamMessage();
            m.writeObject(testString);
            final StreamMessage copy = ts.sendAndReceive(m, StreamMessage.class, ts.queue);
            final Object r = copy.readObject();
            if (!testString.equals(r)) {
                Assert.fail(OutputHelper.comparisonFailureDescription(testString, r));
            }
        }
    }

    @Override
    public void testStreamMessage_writeString() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final StreamMessage m = ts.session.createStreamMessage();
            m.writeString(testString);
            final StreamMessage copy = ts.sendAndReceive(m, StreamMessage.class, ts.queue);
            final Object r = copy.readString();
            if (!testString.equals(r)) {
                Assert.fail(OutputHelper.comparisonFailureDescription(testString, r));
            }
        }
    }
}
