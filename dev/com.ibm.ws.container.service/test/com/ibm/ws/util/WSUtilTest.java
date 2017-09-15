/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Tests expected behavior of WSUtil - which currently has one method, resolveURI
 */
public class WSUtilTest {

    /**
     * Negative test - ensures that we receive the proper exception and message when specifying
     * an invalid URL containing more parent directories than actual directories.
     */
    @Test
    public void resolveURI_invalidArgument_tooManyParents() {
        String badURI = "/dir1/dir2/../../../blah"; // note three parents, but only two directories deep
        try {
            WSUtil.resolveURI(badURI);
            fail("Did not throw IllegalArgumentException for invalid URI parameter");
        } catch (IllegalArgumentException ex) {
            assertTrue("Did not contain expected exception text indicating more parents than directory depth",
                       ex.getMessage().contains("is invalid because it contains more references to parent directories (\"..\") than is possible."));
        } catch (Throwable t) {
            fail("Wrong exception thrown.  Expected IllegalArgumentException, caught " + t);
        }
    }

    /**
     * Ensure that parent directories ("..") are correctly resolved.
     */
    @Test
    public void resolveURI_resolveParents() {
        String unresolvedURI = "/usr/servers/../../etc";
        assertEquals("Did not correctly resolve parent directories", "/etc", WSUtil.resolveURI(unresolvedURI));
    }
}
