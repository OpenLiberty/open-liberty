/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config14.test.apps.badobserver;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/")
public class BadObserverServlet extends FATServlet {

    @Inject
    BadObserverBean bean;

    public void dummyTest() {
        bean.dummyTest();
    }
}
