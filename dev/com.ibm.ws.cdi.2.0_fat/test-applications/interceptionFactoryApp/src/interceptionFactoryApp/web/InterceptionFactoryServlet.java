/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package interceptionFactoryApp.web;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/interceptionFactoryTest")
public class InterceptionFactoryServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 671383796561361465L;

    @Inject
    Thing t;

    @Test
    public void testInterceptionFactory() throws Exception {
        t.hello();
        assertTrue("Interceptor was not invoked", Intercepted.get());
    }
}
