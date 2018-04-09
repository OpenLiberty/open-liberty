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
import testjms.web.JmsTextMessageTests;
import testjms.web.util.OutputHelper;

import javax.jms.TextMessage;

public class JmsTextMessageTestsImpl extends JmsTestsBase implements JmsTextMessageTests {
    public JmsTextMessageTestsImpl(ConnectionFactoryType cfType) { super(cfType); }

    @Override
    public void testTextMessage_setText() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final TextMessage m = ts.session.createTextMessage();
            m.setText(testString);
            final TextMessage copy = ts.sendAndReceive(m, TextMessage.class, ts.queue);
            final Object r = copy.getText();
            if (!testString.equals(r)) {
                Assert.fail(OutputHelper.comparisonFailureDescription(testString, r));
            }
        }
    }
}
