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
package com.ibm.ws.cdi.ejb.apps.aroundconstruct;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

/**
 * These tests use {@link AroundConstructLogger} to record what happens while intercepting constructors.
 * <p>{@link AroundConstructLogger} is <code>@RequestScoped</code> so a new instance is created for every test.
 */
@WebServlet("/beanTestServlet")
public class BeanServlet extends AroundConstructTestServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    Bean bean;

    @Override
    protected void before() {
        bean.doSomething(); // need to actually use the injected bean, otherwise things go a bit funny
    }
}
