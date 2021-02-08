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
package com.ibm.ws.cdi.ejb.apps.scope;

import static org.junit.Assert.assertTrue;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/PostConstructScope")
public class PostConstructScopeServlet extends FATServlet {

    @EJB
    private PostConstructingStartupBean ejb;

    /**
     * Test that the request scope is active during postConstruct for an eager singleton bean.
     *
     * @throws Exception
     */
    @Test
    public void testPostConstructRequestScope() throws Exception {
        assertTrue("RequestScope not active during EJB postConstruct", ejb.getWasRequestScopeActive());
    }
}
