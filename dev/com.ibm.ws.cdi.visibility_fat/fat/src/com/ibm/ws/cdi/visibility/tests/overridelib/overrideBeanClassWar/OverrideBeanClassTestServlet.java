/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.overridelib.overrideBeanClassWar;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.visibility.test.dups.TestBean1;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/OverrideBeanClassTestServlet")
public class OverrideBeanClassTestServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    private TestBean1 testBean1;

    @Test
    @Mode(TestMode.FULL)
    public void testOverrideBeanClass() {
        assertTrue("TestBean1 did not get target injection.", testBean1.isTargetSet());
    }
}
