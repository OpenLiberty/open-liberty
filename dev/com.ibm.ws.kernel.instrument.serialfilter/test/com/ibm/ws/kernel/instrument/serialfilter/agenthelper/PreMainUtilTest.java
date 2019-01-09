/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.agenthelper;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class PreMainUtilTest {

    @Test
    public void testConfigurationValues() {
        assertTrue("The property value FACTORY_INIT_PROPERTY should not be changed.", PreMainUtil.FACTORY_INIT_PROPERTY.equals("com.ibm.serialization.validators.factory.instance"));
        assertTrue("The property value DEBUG_PROPERTY should not be changed.", PreMainUtil.DEBUG_PROPERTY.equals("com.ibm.websphere.kernel.instrument.serialfilter.debug"));;
    }

    @Test
    public void testIsDebugEnabled() {
        System.setProperty(PreMainUtil.DEBUG_PROPERTY, "true");
        assertTrue("isDebugEnabled method should return true.", PreMainUtil.isDebugEnabled());
        System.setProperty(PreMainUtil.DEBUG_PROPERTY, "false");
        assertFalse("isDebugEnabled method should return false.", PreMainUtil.isDebugEnabled());
        System.clearProperty(PreMainUtil.DEBUG_PROPERTY);
        assertFalse("isDebugEnabled method should return false if no value is set.", PreMainUtil.isDebugEnabled());
    }
}
