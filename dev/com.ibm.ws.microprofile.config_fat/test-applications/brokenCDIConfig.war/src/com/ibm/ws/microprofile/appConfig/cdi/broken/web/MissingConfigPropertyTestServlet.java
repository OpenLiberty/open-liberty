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
package com.ibm.ws.microprofile.appConfig.cdi.broken.web;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.microprofile.appConfig.cdi.broken.beans.MissingConfigPropertyBean;
import com.ibm.ws.microprofile.appConfig.cdi.web.AbstractBeanServlet;

@SuppressWarnings("serial")
@WebServlet("/missingConfigProperty")
public class MissingConfigPropertyTestServlet extends AbstractBeanServlet {

    @Inject
    MissingConfigPropertyBean configBean;

    /** {@inheritDoc} */
    @Override
    public Object getBean() {
        return configBean;
    }
}
