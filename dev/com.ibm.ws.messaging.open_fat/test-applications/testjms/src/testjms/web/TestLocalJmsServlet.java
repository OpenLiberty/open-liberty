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
import testjms.web.impl.JmsMinimalTestsImpl;

import javax.servlet.annotation.WebServlet;

/**
 * Servlet for running tests locally.
 * The implementation should be identical to that in {@link TestRemoteJmsServlet}, except for the value of CF_TYPE.
 */
@WebServlet("/TestLocalJmsServlet")
public class TestLocalJmsServlet extends FATServlet implements JmsMinimalTests, JmsBytesMessageTests {
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
    public void test_writeByte() throws Exception {
        jmsBytesMessageTests.test_writeByte();
    }

    @Test
    @Override
    public void test_writeBytes() throws Exception {
        jmsBytesMessageTests.test_writeBytes();
    }

    @Test
    @Override
    public void test_writeChar() throws Exception {
        jmsBytesMessageTests.test_writeChar();
    }

    @Test
    @Override
    public void test_writeDouble() throws Exception {
        jmsBytesMessageTests.test_writeDouble();
    }

    @Test
    @Override
    public void test_writeFloat() throws Exception {
        jmsBytesMessageTests.test_writeFloat();
    }

    @Test
    @Override
    public void test_writeInt() throws Exception {
        jmsBytesMessageTests.test_writeInt();
    }

    @Test
    @Override
    public void test_writeLong() throws Exception {
        jmsBytesMessageTests.test_writeLong();
    }

    @Test
    @Override
    public void test_writeShort() throws Exception {
        jmsBytesMessageTests.test_writeShort();
    }

    @Test
    @Override
    public void test_writeUTF() throws Exception {
        jmsBytesMessageTests.test_writeUTF();
    }
}
