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
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Pattern;

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

    private static final class PoisonedTestObject implements Serializable {
        private static final long serialVersionUID = 0L;
        private void readObject(ObjectInputStream ois) throws IOException {
            throw new NotSerializableException("Invalid attempt to deserialize object");
        }
    }

    private static final Serializable[] testObjects = {
            null, "wibble", new PoisonedTestObject(), new LinkedList(Arrays.asList(new PoisonedTestObject())) };

    @Override
    public void testObjectMessage_toString() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            for (Serializable s: testObjects) {
                final String oscDesc = (s == null) ? "null" : ObjectStreamClass.lookup(s.getClass()).toString();
                final Pattern descPattern = Pattern.compile(String.format("%s\\Z", Pattern.quote(oscDesc)));
                final ObjectMessage m = ts.session.createObjectMessage();
                m.setObject(s);

                checkStringForPattern(m.toString(), descPattern);
                final ObjectMessage copy = ts.sendAndReceive(m, ObjectMessage.class, ts.queue);
                checkStringForPattern(copy.toString(), descPattern);
            }
        }
    }

    private static void checkStringForPattern(String s, Pattern p) {
        if (p.matcher(s).find() == false) {
            Assert.fail(String.format("Pattern \"%s\" not found in string:%n%s%n", p, s));
        }
    }
}
