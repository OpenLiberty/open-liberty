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
package com.ibm.ws.microprofile.appConfig.cdi.web;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.microprofile.appConfig.cdi.beans.BuiltInConverterInjectionBean;

@SuppressWarnings("serial")
@WebServlet("/builtin")
public class BuiltInConverterTestServlet extends AbstractBeanServlet {

    @Inject
    BuiltInConverterInjectionBean configBean;

    /** {@inheritDoc} */
    @Override
    public Object getBean() {
        return configBean;
    }
}
