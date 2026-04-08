/*
 * Copyright 2015, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.csiv2.config;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.transport.iiop.security.config.css.CSSCompoundSecMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechConfig;

public class CompatibleMechanismsTest {

    private CompatibleMechanisms compatibleMechanisms;
    private CSSCompoundSecMechConfig clientMech;
    private TSSCompoundSecMechConfig serverMech;

    @Before
    public void setUp() {
        clientMech = new CSSCompoundSecMechConfig();
        serverMech = new TSSCompoundSecMechConfig();
        compatibleMechanisms = new CompatibleMechanisms(clientMech, serverMech);
    }

    /**
     * Test method for {@link com.ibm.ws.security.csiv2.config.CompatibleMechanisms#getCSSCompoundSecMechConfig()}.
     */
    @Test
    public void testGetCSSCompoundSecMechConfig() {
        assertEquals("The client compound sec mech must be found in the compatible policy.", clientMech, compatibleMechanisms.getCSSCompoundSecMechConfig());
    }

    /**
     * Test method for {@link com.ibm.ws.security.csiv2.config.CompatibleMechanisms#getTSSCompoundSecMechConfig()}.
     */
    @Test
    public void testGetTSSCompoundSecMechConfig() {
        assertEquals("The server compound sec mech must be found in the compatible policy.", serverMech, compatibleMechanisms.getTSSCompoundSecMechConfig());
    }

}
