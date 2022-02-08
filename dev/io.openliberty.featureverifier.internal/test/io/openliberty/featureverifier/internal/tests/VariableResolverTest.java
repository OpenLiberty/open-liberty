/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.featureverifier.internal.tests;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;

import io.openliberty.featureverifier.internal.AbstractVariableResolver;

/**
 *
 */
public class VariableResolverTest {

    @Test
    public void testSimpleVariable() {
        TestResolver resolver = new TestResolver();
        String original = "Prefix ${key1} Suffix";
        String value = resolver.resolve(original);
        assertEquals("Prefix value1 Suffix", value);
    }

    @Test
    public void testMultiVariable() {
        TestResolver resolver = new TestResolver();
        String original = "Prefix ${key1} Middle ${key2} Suffix";
        String value = resolver.resolve(original);
        assertEquals("Prefix value1 Middle value2 Suffix", value);
    }

    @Test
    public void testExtraEndToken() {
        TestResolver resolver = new TestResolver();
        String original = "Prefix ${key1}} Middle ${key2} Suffix";
        String value = resolver.resolve(original);
        assertEquals("Prefix value1} Middle value2 Suffix", value);
    }

    @Test
    public void testExtraStartToken() {
        TestResolver resolver = new TestResolver();
        String original = "Prefix ${key1} Middle ${key2 Suffix";
        String value = resolver.resolve(original);
        assertEquals("Prefix value1 Middle ${key2 Suffix", value);
    }

    @Test
    public void testNestedVariable() {
        TestResolver resolver = new TestResolver();
        String original = "Prefix ${${key3}} Suffix";
        String value = resolver.resolve(original);
        assertEquals("Prefix value4 Suffix", value);
    }

    @Test
    public void testCompositeNestedVariable() {
        TestResolver resolver = new TestResolver();
        String original = "Prefix ${${key5}${key6}} Suffix";
        String value = resolver.resolve(original);
        assertEquals("Prefix value4 Suffix", value);
    }

    @Test
    public void testNoVariables() {
        TestResolver resolver = new TestResolver();
        String original = "Prefix Middle Suffix";
        String value = resolver.resolve(original);
        assertEquals("Prefix Middle Suffix", value);
    }

    @Test
    public void testConditionalTrue() {
        TestResolver resolver = new TestResolver();
        String original = "Prefix ${if;${key7};${key2};${key4}} Suffix";
        String value = resolver.resolve(original);
        assertEquals("Prefix value2 Suffix", value);
    }

    @Test
    public void testConditionalNotTrue() {
        TestResolver resolver = new TestResolver();
        String original = "Prefix ${if;${key8};${key2};${key4}} Suffix";
        String value = resolver.resolve(original);
        assertEquals("Prefix value4 Suffix", value);
    }

    @Test
    public void testConditionalEmptyThen() {
        TestResolver resolver = new TestResolver();
        String original = "Prefix ${if;${key8};;${key4}} Suffix";
        String value = resolver.resolve(original);
        assertEquals("Prefix value4 Suffix", value);
    }

    @Test
    public void testConditionalEmptyElse() {
        TestResolver resolver = new TestResolver();
        String original = "Prefix ${if;${key7};${key2};} Suffix";
        String value = resolver.resolve(original);
        assertEquals("Prefix value2 Suffix", value);
    }

    @Test
    public void testVariableStartIndex() {
        String original = "Prefix ${key}";
        int start = AbstractVariableResolver.indexOfVariableStart(original);
        assertEquals(7, start);
    }

    @Test
    public void testVariableStartIndexWithIF() {
        String original = "Prefix ${if;${key7};${key2};${key4}}";
        int start = AbstractVariableResolver.indexOfVariableStart(original);
        assertEquals(12, start);
    }

    public static class TestResolver extends AbstractVariableResolver {

        private final HashMap<String, String> map = new HashMap<String, String>();

        public TestResolver() {
            this.map.put("key1", "value1");
            this.map.put("key2", "value2");
            this.map.put("key3", "key4");
            this.map.put("key4", "value4");
            this.map.put("key5", "key");
            this.map.put("key6", "4");
            this.map.put("key7", "true");
            this.map.put("key8", "nottrue");
        }

        @Override
        public String getValue(String name) {
            return this.map.get(name);
        }

    }

}
