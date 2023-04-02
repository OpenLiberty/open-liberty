/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.cdi.internal.core.injection.app;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/injection")
public class CDIInjectionTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private InjectedBean injectedBean;

    @Test
    public void testInjectedField() {
        injectedBean.testInjectedField();
    }

    @Test
    public void testFromConstructor() {
        injectedBean.testFromConstructor();
    }

    @Test
    public void testFromMethod() {
        injectedBean.testFromMethod();
    }

}
