/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import static org.junit.Assert.assertEquals;

import javax.naming.Name;
import javax.naming.NamingException;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.jndi.url.contexts.javacolon.internal.JavaURLNameParser;

public class JavaURLNameParserTest {

    private JavaURLNameParser parser = null;

    @Before
    public void setupParser() {
        parser = new JavaURLNameParser();
    }

    @Test
    public void testParse() throws Exception {
        Name jndiName = parser.parse("java:comp/env/test");
        assertEquals("The first element of the name was not java:comp", "java:comp", jndiName.get(0));
        assertEquals("The second element of the name was not env", "env", jndiName.get(1));
        assertEquals("The third element of the name was not test", "test", jndiName.get(2));
    }

    @Test(expected = NamingException.class)
    public void testNullParse() throws Exception {
        parser.parse((String) null);
    }

    @Test(expected = NamingException.class)
    public void testEmptyParse() throws Exception {
        parser.parse("");
    }

    @Test(expected = NamingException.class)
    public void testParseException() throws Exception {
        parser.parse("java:com/env/test");
    }

    @Test
    public void testGetStringNameWithoutPrefix() throws Exception {
        Name jndiName = parser.parse("java:comp/env/test");
        String nameNoPrefix = parser.getStringNameWithoutPrefix(jndiName);
        assertEquals("The getStringNameWithoutPrefix() method did not return the correct name", "test", nameNoPrefix);
    }

}
