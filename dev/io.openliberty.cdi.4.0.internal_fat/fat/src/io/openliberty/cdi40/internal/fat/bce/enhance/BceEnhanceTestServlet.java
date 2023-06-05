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
package io.openliberty.cdi40.internal.fat.bce.enhance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/augment")
@SuppressWarnings("serial")
public class BceEnhanceTestServlet extends FATServlet {

    @Inject
    @EnhanceTestQualifier
    Instance<EnhanceTestBean> testBeans;

    @Test
    @Mode(TestMode.FULL)
    public void testBeanAugmented() {
        // EnhanceTestBean should have the qualifier added by the extension
        assertEquals("test bean count", testBeans.stream().count(), 1);
        assertTrue("test bean result", testBeans.get().test());
    }
}
