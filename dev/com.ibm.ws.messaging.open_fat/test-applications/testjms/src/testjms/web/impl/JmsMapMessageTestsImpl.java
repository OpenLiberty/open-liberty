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
import testjms.web.JmsMapMessageTests;
import testjms.web.util.OutputHelper;

import javax.jms.MapMessage;

public class JmsMapMessageTestsImpl extends JmsTestsBase implements JmsMapMessageTests {
    private static final String KEY = "KEY";

    public JmsMapMessageTestsImpl(ConnectionFactoryType cfType) { super(cfType); }

    @Override
    public void testMapMessage_setObject_String() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final MapMessage m = ts.session.createMapMessage();
            m.setObject(KEY, testString);
            final MapMessage copy = ts.sendAndReceive(m, MapMessage.class, ts.queue);
            final Object r = copy.getObject(KEY);
            if (!testString.equals(r)) {
                Assert.fail(OutputHelper.comparisonFailureDescription(testString, r));
            }
        }
    }

    @Override
    public void testMapMessage_setString() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final MapMessage m = ts.session.createMapMessage();
            m.setString(KEY, testString);
            final MapMessage copy = ts.sendAndReceive(m, MapMessage.class, ts.queue);
            final String r = copy.getString(KEY);
            if (!testString.equals(r)) {
                Assert.fail(OutputHelper.comparisonFailureDescription(testString, r));
            }
        }
    }
}
