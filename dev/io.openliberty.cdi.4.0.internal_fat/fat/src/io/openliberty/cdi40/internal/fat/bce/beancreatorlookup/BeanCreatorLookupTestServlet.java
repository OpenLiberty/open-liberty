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
package io.openliberty.cdi40.internal.fat.bce.beancreatorlookup;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

/**
 * Test that SyntheticBeanCreator can look up other beans
 */
@WebServlet("/beancreatorlookup")
@SuppressWarnings("serial")
public class BeanCreatorLookupTestServlet extends FATServlet {

    @Inject
    private Instance<SyntheticBean> syntheticBeanInstance;

    @Test
    public void testBeanCreatorLookup() {
        assertEquals(0, DependantBean.testCallCount.get());

        // Creating an instance of synthetic bean should cause SyntheticCreator to create and call DependentBean
        SyntheticBean syntheticBean = syntheticBeanInstance.get();
        // Need to actually call it for the bean instance to be created
        assertEquals("synthetic", syntheticBean.getName());

        assertEquals(1, DependantBean.testCallCount.get());
    }
}
