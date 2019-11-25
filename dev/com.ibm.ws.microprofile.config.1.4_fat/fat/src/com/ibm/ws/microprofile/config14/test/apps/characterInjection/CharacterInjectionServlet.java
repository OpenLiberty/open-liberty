/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.test.apps.characterInjection;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/")
public class CharacterInjectionServlet extends FATServlet {

    @Inject
    CharacterInjectionBean bean;

    @Test
    public void characterInjectionTest() {
        bean.characterInjectionTest();
    }

    @Test
    public void charInjectionTest() {
        bean.charInjectionTest();
    }
}
