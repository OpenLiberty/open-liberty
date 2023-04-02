/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.apps.enablementWar;

import static org.junit.Assert.fail;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Test that CDI is not enabled when there are no beans visible except from extensions
 * <p>
 * The app must be deployed without the shared library for this test to pass
 */
@SuppressWarnings("serial")
@WebServlet("/test")
public class EnablementTestServlet extends FATServlet {

    @Test
    public void testCdiNotEnabledForApp() {
        // If there are no beans visible, CDI gets disabled for the whole app
        try {
            CDI.current();
            fail("CDI.current() did not throw exception");
        } catch (IllegalStateException e) {
            // expected
        }
    }

}
