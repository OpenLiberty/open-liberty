/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.jndi.strings;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("")
public class LookupServlet extends FATServlet {

    @Inject
    JNDIStrings jndiStrings;

    @Inject
    ObserverBean observerBean;

    @Test
    public void testLookupFromConfig() throws Exception {
        assertEquals("Value from Config", jndiStrings.getFromConfig());
    }

    @Test
    public void testLookupFromBind() throws Exception {
        assertEquals("Value from Bind", jndiStrings.getFromBind());
    }

    @Test
    public void testJNDILookupInObserverJar() throws Exception {
        assertEquals("test/passed", observerBean.getResult());
    }
}
