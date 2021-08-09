/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SpnGSSCredentialTest {

    @Test
    public void testExtractHostName() throws Exception {
        SpnGssCredential spnGssCredential = new SpnGssCredential();
        assertEquals("host name should be host1", "host1", spnGssCredential.extractHostName("host1@AUSTIN.IBM.COM"));
        assertEquals("host name should be host2", "host2", spnGssCredential.extractHostName("host2"));
        assertEquals("host name should be host3", "host3", spnGssCredential.extractHostName("HTTP/host3@AUSTIN.IBM.COM"));
    }
}
