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
import org.omg.CSI.ITTAbsent;

public class CSSSASITTAbsentTest {

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSSASITTAbsent#getType()}.
     */
    @Test
    public void testGetType() {
        CSSSASITTAbsent cssSASITTAbsent = new CSSSASITTAbsent();
        assertEquals("The token type must be set.", ITTAbsent.value, cssSASITTAbsent.getType());
    }

}
