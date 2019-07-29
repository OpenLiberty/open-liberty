/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EndpointUtilsTest {

    @Test
    public void testEscapeQuotesForJson() {
        String result = EndpointUtils.escapeQuotesForJson("badquote:\" goodquote:\\\"");
        String expected = "badquote:\\\" goodquote:\\\"";
        assertTrue("expected backslash to be inserted before unescaped quotation mark but got: " + result,
                expected.equals(result));
    }

}
