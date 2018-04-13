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
import testjms.web.JmsObjectMessageTests;
import testjms.web.util.OutputHelper;

import javax.jms.ObjectMessage;

public class JmsObjectMessageTestsImpl extends JmsTestsBase implements JmsObjectMessageTests {
    public JmsObjectMessageTestsImpl(ConnectionFactoryType cfType) { super(cfType); }

    @Override
    public void testObjectMessage_setObject_String() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final ObjectMessage m = ts.session.createObjectMessage();
            m.setObject(testString);
            final ObjectMessage copy = ts.sendAndReceive(m, ObjectMessage.class, ts.queue);
            final Object r = copy.getObject();
            if (!testString.equals(r)) {
                Assert.fail(OutputHelper.comparisonFailureDescription(testString, r));
            }
        }
    }
}
