/*
 * =============================================================================
 * Copyright (c) 2014, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.jndi.global.fat.web;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@WebServlet("/JNDIRefTestServlet")
@SuppressWarnings("serial")
public class JNDIRefTestServlet extends FATServlet {
    @Test
    public void testRefEntry(Properties actual) throws Exception {
        Properties expected = new Properties();
        Assert.assertEquals("retrieved value should be empty properties for ref/entry", expected, actual);
    }

    @Test
    public void testRefEntry() throws Exception {
        testRefEntry((Properties) new InitialContext().lookup("ref/entry"));
    }

    @Resource(name = "ref/entry")
    public Properties refEntry;

    @Test
    public void testRefEntryInjection() throws Exception {
        testRefEntry(refEntry);
    }

    @Test
    public void testRefEntryProps(Properties actual) throws Exception {
        Properties expected = new Properties();
        expected.put("key1", "value1");
        expected.put("key2", "value2");
        expected.put("pwd", "{xor}LDo8Ki02KyY=");
        Assert.assertEquals("retrieved value should be configured properties for ref/entryProps", expected, actual);
    }

    @Test
    public void testRefEntryPropsDecode(Properties actual) throws Exception {
        Properties expected = new Properties();
        expected.put("key1", "value1");
        expected.put("key2", "value2");
        expected.put("pwd", "security"); // since decode=true is set, the encrypted value should be decoded.
        Assert.assertEquals("retrieved value should be configured and decrypted properties for ref/entryPropsDecode", expected, actual);
    }

    @Test
    public void testRefEntryProps() throws Exception {
        testRefEntryProps((Properties) new InitialContext().lookup("ref/entryProps"));
    }

    @Test
    public void testRefEntryPropsDecode() throws Exception {
        testRefEntryPropsDecode((Properties) new InitialContext().lookup("ref/entryPropsDecode"));
    }

    private void testRefEntryPropsDecodeError(Properties actual) throws Exception {
        Properties expected = new Properties();
        expected.put("key1", "value1");
        expected.put("key2", "value2");
        expected.put("pwd", "{xor}abcdefg"); // if there is a decrypting error, fall back to the original string.
        Assert.assertEquals("retrieved value should be configured and decrypted properties for ref/entryPropsDecode", expected, actual);
    }

    @Test
    @ExpectedFFDC("com.ibm.websphere.crypto.InvalidPasswordDecodingException")
    public void testRefEntryPropsDecodeError() throws Exception {
        testRefEntryPropsDecodeError((Properties) new InitialContext().lookup("ref/entryPropsDecodeError"));
    }

    @Resource(name = "ref/entryProps")
    private Properties refEntryProps;

    @Test
    public void testRefEntryPropsInjection() throws Exception {
        testRefEntryProps(refEntryProps);
    }

    @Test
    public void testRefEntryClassName() throws Exception {
        Map<String, NameClassPair> pairs = new HashMap<String, NameClassPair>();
        for (Enumeration<NameClassPair> en = new InitialContext().list("ref"); en.hasMoreElements();) {
            NameClassPair pair = en.nextElement();
            pairs.put(pair.getName(), pair);
        }

        Assert.assertEquals("expected unknown type for ref/entry", Object.class.getName(), pairs.get("entry").getClassName());
        Assert.assertEquals("expected configured type for ref/entryTyped", Properties.class.getName(), pairs.get("entryTyped").getClassName());
    }
}
