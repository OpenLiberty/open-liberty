/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.xpath;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import junit.framework.Assert;

/**
 * Test for the {@link WIMXPathInterpreter} class.
 */
public class WIMXPathInterpreterTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void parse_principalName_literal() throws Exception {
        Assert.assertNotNull(new WIMXPathInterpreter(getInputStream("@xsi:type='PersonAccount' and principalName =  \"bob\"")).parse(null));
    }

    @Test
    public void parse_principalName_wildcard() throws Exception {
        Assert.assertNotNull(new WIMXPathInterpreter(getInputStream("@xsi:type='PersonAccount' and principalName =  \"*\"")).parse(null));
    }

    @Test
    public void parse_multiple_attributes() throws Exception {
        Assert.assertNotNull(new WIMXPathInterpreter(getInputStream("@xsi:type='PersonAccount' and cn =  \"bob\" and uid = \"bob\"")).parse(null));
    }

    /**
     * Quotation marks are escaped in XPath by doubling them up (""). Verify we parse them correctly.
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void parse_quotations() throws Exception {
        Assert.assertNotNull(new WIMXPathInterpreter(getInputStream("@xsi:type='PersonAccount' and cn = \"*\"\"*\"")).parse(null));
        Assert.assertNotNull(new WIMXPathInterpreter(getInputStream("@xsi:type='PersonAccount' and cn = \"*\"\"\"\"*\"")).parse(null));
        Assert.assertNotNull(new WIMXPathInterpreter(getInputStream("@xsi:type='PersonAccount' and cn = \"*\"\"\"\"\"\"*\"")).parse(null));
    }

    /**
     * Check for unmatched escaped quotation marks.
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void parse_quotations_unmatched() throws Exception {
        expectedException.expect(TokenMgrError.class);
        expectedException.expectMessage("Lexical error at line 1, column 39.  Encountered: \"*\" (42), after : \"\"");

        new WIMXPathInterpreter(getInputStream("@xsi:type='PersonAccount' and cn = \"*\"*\"")).parse(null);
    }

    /**
     * Apostrophes are escaped in XPath by doubling them up (''). Verify we parse them correctly.
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void parse_apostrophes() throws Exception {
        Assert.assertNotNull(new WIMXPathInterpreter(getInputStream("@xsi:type='PersonAccount' and cn = '*''*'")).parse(null));
        Assert.assertNotNull(new WIMXPathInterpreter(getInputStream("@xsi:type='PersonAccount' and cn = '*''''*'")).parse(null));
        Assert.assertNotNull(new WIMXPathInterpreter(getInputStream("@xsi:type='PersonAccount' and cn = '*''''''*'")).parse(null));
    }

    /**
     * Check for unmatched escaped apostrophes.
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void parse_apostrophes_unmatched() throws Exception {
        expectedException.expect(TokenMgrError.class);
        expectedException.expectMessage("Lexical error at line 1, column 39.  Encountered: \"*\" (42), after : \"\"");

        new WIMXPathInterpreter(getInputStream("@xsi:type='PersonAccount' and cn = '*'*'")).parse(null);
    }

    private static InputStream getInputStream(String value) {
        return new ByteArrayInputStream(value.getBytes());
    }
}
