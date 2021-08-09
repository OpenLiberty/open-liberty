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
package testjms.web;

import componenttest.app.FATServlet;
import org.junit.Test;
import testjms.web.impl.ConnectionFactoryType;
import testjms.web.impl.JmsBytesMessageTestsImpl;
import testjms.web.impl.JmsMapMessageTestsImpl;
import testjms.web.impl.JmsMessageTestsImpl;
import testjms.web.impl.JmsMinimalTestsImpl;
import testjms.web.impl.JmsObjectMessageTestsImpl;
import testjms.web.impl.JmsStreamMessageTestsImpl;
import testjms.web.impl.JmsTextMessageTestsImpl;

import javax.servlet.annotation.WebServlet;

/**
 * Servlet for running tests locally.
 * The implementation should be identical to that in {@link TestRemoteJmsServlet}, except for the value of CF_TYPE.
 */
@WebServlet("/TestLocalJmsServlet")
public class TestLocalJmsServlet extends FATServlet implements JmsMinimalTests, JmsBytesMessageTests, JmsMapMessageTests, JmsMessageTests, JmsObjectMessageTests, JmsStreamMessageTests, JmsTextMessageTests {
    private static final ConnectionFactoryType CF_TYPE = ConnectionFactoryType.LocalCF;

    // ======== BASIC TESTS ========
    private final JmsMinimalTests jmsMinimalTests = new JmsMinimalTestsImpl(CF_TYPE);

    @Test
    @Override
    public void basicTest() throws Exception {
        jmsMinimalTests.basicTest();
    }

    @Test
    @Override
    public void testBasicJmsLookup() throws Exception {
        jmsMinimalTests.testBasicJmsLookup();
    }

    @Test
    @Override
    public void testClearQueue() throws Exception {
        jmsMinimalTests.testClearQueue();
    }

    // ======== BYTESMESSAGE TESTS ========
    private final JmsBytesMessageTests jmsBytesMessageTests = new JmsBytesMessageTestsImpl(CF_TYPE);

    @Test
    @Override
    public void testBytesMessage_writeByte() throws Exception {
        jmsBytesMessageTests.testBytesMessage_writeByte();
    }

    @Test
    @Override
    public void testBytesMessage_writeBytes() throws Exception {
        jmsBytesMessageTests.testBytesMessage_writeBytes();
    }

    @Test
    @Override
    public void testBytesMessage_writeChar() throws Exception {
        jmsBytesMessageTests.testBytesMessage_writeChar();
    }

    @Test
    @Override
    public void testBytesMessage_writeDouble() throws Exception {
        jmsBytesMessageTests.testBytesMessage_writeDouble();
    }

    @Test
    @Override
    public void testBytesMessage_writeFloat() throws Exception {
        jmsBytesMessageTests.testBytesMessage_writeFloat();
    }

    @Test
    @Override
    public void testBytesMessage_writeInt() throws Exception {
        jmsBytesMessageTests.testBytesMessage_writeInt();
    }

    @Test
    @Override
    public void testBytesMessage_writeLong() throws Exception {
        jmsBytesMessageTests.testBytesMessage_writeLong();
    }

    @Test
    @Override
    public void testBytesMessage_writeShort() throws Exception {
        jmsBytesMessageTests.testBytesMessage_writeShort();
    }

    @Test
    @Override
    public void testBytesMessage_writeUTF() throws Exception {
        jmsBytesMessageTests.testBytesMessage_writeUTF();
    }

    // ======== MAPMESSAGE TESTS ========
    private final JmsMapMessageTests jmsMapMessageTests = new JmsMapMessageTestsImpl(CF_TYPE);

    @Test
    @Override
    public void testMapMessage_setObject_String() throws Exception {
        jmsMapMessageTests.testMapMessage_setObject_String();
    }

    @Test
    @Override
    public void testMapMessage_setString() throws Exception {
        jmsMapMessageTests.testMapMessage_setString();
    }

    // ======== MESSAGE TESTS ========
    private final JmsMessageTests jmsMessageTests = new JmsMessageTestsImpl(CF_TYPE);

    @Test
    @Override
    public void testMessage_setObjectProperty_String() throws Exception {
        jmsMessageTests.testMessage_setObjectProperty_String();
    }

    @Test
    @Override
    public void testMessage_setStringProperty() throws Exception {
        jmsMessageTests.testMessage_setStringProperty();
    }

    // ======== OBJECTMESSAGE TESTS ========
    private final JmsObjectMessageTests jmsObjectMessageTests = new JmsObjectMessageTestsImpl(CF_TYPE);

    @Test
    @Override
    public void testObjectMessage_setObject_String() throws Exception {
        jmsObjectMessageTests.testObjectMessage_setObject_String();
    }

    @Test
    @Override
    public void testObjectMessage_toString() throws Exception {
        jmsObjectMessageTests.testObjectMessage_toString();
    }

    // ======== STREAMMESSAGE TESTS ========
    private final JmsStreamMessageTests jmsStreamMessageTests = new JmsStreamMessageTestsImpl(CF_TYPE);

    @Test
    @Override
    public void testStreamMessage_writeObject_String() throws Exception {
        jmsStreamMessageTests.testStreamMessage_writeObject_String();
    }

    @Test
    @Override
    public void testStreamMessage_writeString() throws Exception {
        jmsStreamMessageTests.testStreamMessage_writeString();
    }

    // ======== TEXTMESSAGE TESTS ========
    private final JmsTextMessageTests jmsTextMessageTests = new JmsTextMessageTestsImpl(CF_TYPE);

    @Test
    @Override
    public void testTextMessage_setText() throws Exception {
        jmsTextMessageTests.testTextMessage_setText();
    }
}
