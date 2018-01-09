/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.transport.model.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.FilterVersion;

/**
 * Tests for {@link AppliesToFilterInfo}
 */
public class AppliesToFilterInfoTest {

    /**
     * Test that hasMaxVersion defaults to false
     */
    @Test
    public void testHasMaxVersionDefault() {
        AppliesToFilterInfo testObject = new AppliesToFilterInfo();
        assertFalse("The default should be false", Boolean.valueOf(testObject.getHasMaxVersion()));
    }

    /**
     * Test that setting max version also sets hasMaxVersion
     */
    @Test
    public void testSetMaxVersion() {
        AppliesToFilterInfo testObject = new AppliesToFilterInfo();
        testObject.setMaxVersion(new FilterVersion());
        testObject.setHasMaxVersion("true");
        assertTrue("The has max version should now be set", Boolean.valueOf(testObject.getHasMaxVersion()));
    }

    /**
     * Test that resetting max version also resets hasMaxVersion
     */
    @Test
    public void testReSetMaxVersion() {
        AppliesToFilterInfo testObject = new AppliesToFilterInfo();
        testObject.setMaxVersion(new FilterVersion());
        testObject.setHasMaxVersion("true");
        assertTrue("The has max version should now be set", Boolean.valueOf(testObject.getHasMaxVersion()));
        testObject.setMaxVersion(null);
        testObject.setHasMaxVersion("false");
        assertFalse("The has max version should now be reset", Boolean.valueOf(testObject.getHasMaxVersion()));
    }
}
