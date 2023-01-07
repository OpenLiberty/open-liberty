/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi12.fat.apps.vetoAndExtendWar;

import static org.junit.Assert.assertEquals;

import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@Vetoed
@WebServlet("/")
public class VetoAndExtendTestServlet extends FATServlet {

    @Inject
    MyBean bean;

    @Test
    public void testVetoAndExtend() {
        assertEquals("test passed", bean.getMsg());
    }
}
