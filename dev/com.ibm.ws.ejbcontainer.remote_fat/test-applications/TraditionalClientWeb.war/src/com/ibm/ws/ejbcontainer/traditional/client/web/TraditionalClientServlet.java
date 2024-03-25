/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.traditional.client.web;

import static org.junit.Assert.assertTrue;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Tests variations of looking up remote enterprise beans located on a traditional server.
 */
@WebServlet("/TraditionalClientServlet")
public class TraditionalClientServlet extends FATServlet {
    private static final long serialVersionUID = -5671511025293075382L;

    /**
     * Tests variations of looking up remote enterprise beans located on a traditional server.
     */
    @Test
    public void testTraditionalLookups() throws Exception {

        assertTrue("Failures occurred during lookup : " + StartupSingletonToTraditional.Failures, StartupSingletonToTraditional.Failures.isEmpty());

        assertTrue("Lookups have not completed", StartupSingletonToTraditional.AllLookupsPassed);
    }
}