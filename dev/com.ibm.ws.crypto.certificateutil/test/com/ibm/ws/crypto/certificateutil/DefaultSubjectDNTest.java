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
package com.ibm.ws.crypto.certificateutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 *
 */
public class DefaultSubjectDNTest {

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.DefaultSubjectDN#DefaultSubjectDN()}.
     */
    @Test
    public void DefaultSubjectDN() {
        DefaultSubjectDN dn = new DefaultSubjectDN();
        assertTrue("Default DN should not have expected pattern, was " + dn.getSubjectDN(),
                   dn.getSubjectDN().matches("CN=.*,O=ibm,C=us"));
        assertFalse("Default DN should not have the server name",
                    dn.getSubjectDN().contains("OU="));
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.DefaultSubjectDN#DefaultSubjectDN(java.lang.String, java.lang.String)}.
     */
    @Test
    public void DefaultSubjectDNStringString() {
        DefaultSubjectDN dn = new DefaultSubjectDN("myhost", "myserver");
        assertEquals("Subject DN should contain host and server",
                     "CN=myhost,OU=myserver,O=ibm,C=us", dn.getSubjectDN());
    }

}
