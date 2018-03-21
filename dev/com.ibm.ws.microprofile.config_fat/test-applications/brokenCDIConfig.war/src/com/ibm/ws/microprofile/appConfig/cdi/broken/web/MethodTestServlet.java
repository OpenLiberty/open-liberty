/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

import com.ibm.ws.microprofile.appConfig.cdi.broken.beans.ConfigUnnamedMethodInjectionBean;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/methodUnnamed")
public class MethodTestServlet extends FATServlet {

    @Inject
    ConfigUnnamedMethodInjectionBean configBean3;
}
