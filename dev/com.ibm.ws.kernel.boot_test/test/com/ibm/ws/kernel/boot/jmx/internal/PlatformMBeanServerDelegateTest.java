/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.jmx.internal;

import static org.junit.Assert.assertTrue;

import javax.management.MBeanServerDelegate;

import org.junit.Test;

/**
 *
 */
public class PlatformMBeanServerDelegateTest {

    @Test
    public void testPlatformMBeanServerDelegateAttributes() throws Exception {
        MBeanServerDelegate mBeanServerDelegate = new PlatformMBeanServerDelegate();
        assertTrue("Expected that server ID starts with WebSphere",
                   mBeanServerDelegate.getMBeanServerId().startsWith("WebSphere"));
    }
}
