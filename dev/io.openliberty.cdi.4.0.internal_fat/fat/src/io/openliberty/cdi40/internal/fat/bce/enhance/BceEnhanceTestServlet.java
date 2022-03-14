/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat.bce.enhance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.app.FATServlet;
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
    public void testBeanAugmented() {
        // EnhanceTestBean should have the qualifier added by the extension
        assertEquals("test bean count", testBeans.stream().count(), 1);
        assertTrue("test bean result", testBeans.get().test());
    }
}
