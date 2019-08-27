/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.Test;

/**
 * This is a dummy test that will always execute and pass. We need at least one test to always
 * execute in the automation framework.
 * 
 */
public class JSFDummyTest {
    private static final Logger LOG = Logger.getLogger(JSFDummyTest.class.getName());

    @Test
    public void testDummy() throws Exception {
        // Need to ensure that at least one test executes.
        LOG.info("\n /******************************************************************************/");
        LOG.info("\n [JSF | JSFDummyTest]: testDummy");
        LOG.info("\n /******************************************************************************/");

        assertTrue("Dummy test passes!", true);
    }

}
