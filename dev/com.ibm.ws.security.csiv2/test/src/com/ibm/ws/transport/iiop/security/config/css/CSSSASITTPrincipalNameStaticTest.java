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
import org.omg.CSI.ITTPrincipalName;

public class CSSSASITTPrincipalNameStaticTest {

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSSASITTPrincipalNameStatic#getType()}.
     */
    @Test
    public void testGetType() {
        CSSSASITTPrincipalNameStatic cssSASITTPrincipalNameStatic = new CSSSASITTPrincipalNameStatic("user");
        assertEquals("The token type must be set.", ITTPrincipalName.value, cssSASITTPrincipalNameStatic.getType());
    }

}
