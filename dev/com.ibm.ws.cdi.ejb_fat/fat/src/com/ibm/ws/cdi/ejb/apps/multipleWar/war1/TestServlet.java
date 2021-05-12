/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.multipleWar.war1;

import static org.junit.Assert.assertEquals;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.ejb.apps.multipleWar.embeddedJar.MyEjb;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/")
public class TestServlet extends FATServlet {
    @EJB(name = "myEjbInWar1")
    MyEjb myEjb;
    @Inject
    MyBean myBean;

    /**  */
    private static final long serialVersionUID = 1L;

    @Test
    public void testDupEJBClassNames() throws Exception {
        assertEquals(MyEjb.NAME, myEjb.getMyEjbName());
        assertEquals(MyBean.NAME, myBean.getName());
    }
}
