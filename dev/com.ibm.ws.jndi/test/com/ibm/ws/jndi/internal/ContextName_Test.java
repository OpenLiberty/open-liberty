/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import static com.ibm.ws.jndi.internal.Assert.assertEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.InvalidNameException;

import org.junit.Test;

import com.ibm.ws.jndi.WSName;

@SuppressWarnings("serial")
public class ContextName_Test {
    private static Properties PROPS = new Properties() {
        {
            put("jndi.syntax.direction", "right_to_left");
            put("jndi.syntax.separator", ":");
        }
    };

    private CompoundName newName(String s) throws InvalidNameException {
        return new CompoundName(s, PROPS);
    }

    @Test
    public void testCreateEmptyName() {
        WSName empty = new WSName();
        assertTrue("New name with no args should be empty", empty.isEmpty());
        assertEquals("String representation should be empty", "", empty);
        assertEquals("Two new empty names should compare equal", new WSName(), empty);
    }

    @Test
    public void testCreateNameWithOneSeparator() throws Exception {
        WSName name1 = new WSName("/");
        assertEquals("Name should have length 1", 1, name1.size());
        assertEquals("Component should be empty", "", name1.get(0));
        WSName name2 = new WSName(newName(":"));
        assertEquals("Name should have length 1", 1, name2.size());
        assertEquals("Component should be empty", "", name2.get(0));
        assertEquals("Two separator-only names should compare equal", name1, name2);
    }

    @Test
    public void testCreateLongName() throws Exception {
        WSName name1 = new WSName("a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z");
        WSName name2 = new WSName(newName("z:y:x:w:v:u:t:s:r:q:p:o:n:m:l:k:j:i:h:g:f:e:d:c:b:a"));
        assertEquals("Name should have 26 components", 26, name1.size());
        assertEquals("Name should have 26 components", 26, name2.size());
        assertEquals("Two names should compare equal", name1, name2);
        assertEquals("a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z", name1.toString());
        assertEquals("a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z", name2.toString());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testPrefixWithNegativeIndex() throws Exception {
        new WSName("a/b/c").getPrefix(-1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testPrefixWithTooHighIndex() throws Exception {
        new WSName("a/b/c").getPrefix(4);
    }

    @Test
    public void testPrefix() throws Exception {
        WSName name = new WSName("a/b/c");
        assertEquals("Zero prefix should result in empty name", "", name.getPrefix(0));
        assertEquals("Max prefix should result in full name", "a/b/c", name.getPrefix(3));
        assertEquals("Should be able to take prefisuffixElementx from empty name", "", new WSName().getPrefix(0));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testSuffixWithNegativeIndex() throws Exception {
        new WSName("a/b/c").getSuffix(-1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testSuffixWithTooHighIndex() throws Exception {
        new WSName("a/b/c").getSuffix(4);
    }

    @Test
    public void testSuffix() throws Exception {
        WSName name = new WSName("a/b/c");
        assertEquals("Max suffix should result in empty name", "", name.getSuffix(3));
        assertEquals("Zero suffix should result in full name", "a/b/c", name.getSuffix(0));
        assertEquals("Should be able to take Suffix from empty name", "", new WSName().getSuffix(0));
    }

    @Test(expected = InvalidNameException.class)
    public void testGetParentFromEmptyName() throws Exception {
        new WSName().getParent();
    }

    public void testGetParent() throws Exception {
        assertEquals("Parent of single name should be empty", "", new WSName("a").getParent());
        assertEquals("Parent of a/b/c should be a/b", "a/b", new WSName("a/b/c").getParent());
    }

    @Test(expected = InvalidNameException.class)
    public void testGetLastFromEmptyName() throws Exception {
        new WSName().getLast();
    }

    public void testGetLast() throws Exception {
        assertEquals("Last part of single name should be empty", "", new WSName("a").getLast());
        assertEquals("Last part of a/b/c should be c", "c", new WSName("a/b/c").getLast());
    }

    @Test
    public void testPlusName() throws Exception {
        WSName empty = new WSName();
        WSName abc = new WSName("a/b/c");
        WSName def = new WSName("d/e/f");
        assertEquals("a/b/c + d/e/f = a/b/c/d/e/f", "a/b/c/d/e/f", abc.plus(def));
        // Subtle test alert! 
        // The following *also* check that plus leaves the addends unchanged.
        assertEquals("empty + a/b/c = a/b/c", "a/b/c", empty.plus(abc));
        assertEquals("d/e/f + empty = d/e/f", "d/e/f", def.plus(empty));
    }

    @Test
    public void testPlusString() throws Exception {
        assertEquals("empty + a = a", "a", new WSName().plus("a"));
        assertEquals("a + b = a/b", "a/b", new WSName("a").plus("b"));
        assertEquals("a/b/c + d = a/b/c/d", "a/b/c/d", new WSName("a/b/c").plus("d"));
    }
}
