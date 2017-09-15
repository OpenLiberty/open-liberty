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
package web.jsonbtest;

import static web.jsonbtest.JSONBTestServlet.PROVIDER_JOHNZON;
import static web.jsonbtest.JSONBTestServlet.PROVIDER_YASSON;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JohnzonTestServlet")
public class JohnzonTestServlet extends FATServlet {

    @Test
    public void testApplicationClasses() throws Exception {
        JSONBTestServlet.testApplicationClasses(PROVIDER_JOHNZON);
    }

    @Test
    public void testJsonbProviderAvailable() throws Exception {
        JSONBTestServlet.testJsonbProviderAvailable(PROVIDER_JOHNZON);
    }

    @Test
    public void testJsonbProviderNotAvailable() throws Exception {
        JSONBTestServlet.testJsonbProviderNotAvailable(PROVIDER_YASSON);
    }

    @Test
    public void testThreadContextClassLoader() throws Exception {
        JSONBTestServlet.testThreadContextClassLoader(PROVIDER_JOHNZON);
    }
}
