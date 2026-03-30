/*
 * Copyright 2014, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop.security.config.css;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.omg.CSI.ITTAnonymous;

public class CSSSASITTAnonymousTest {

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSSASITTAnonymous#getType()}.
     */
    @Test
    public void testGetType() {
        CSSSASITTAnonymous cssSASITTAnonymous = new CSSSASITTAnonymous();
        assertEquals("The token type must be set.", ITTAnonymous.value, cssSASITTAnonymous.getType());
    }

}
