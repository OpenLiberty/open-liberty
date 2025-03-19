/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.annotations.fat.apps.abstractDecorator;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class AbstractDecoratorServlet extends FATServlet {

    @Inject
    private AbstractDecoratorTestInterface bean;

    @Test
    public void testDecoratedMethod() {
        assertEquals("test1-decorated", bean.test1());
    }

    @Test
    public void testAbstractDecoratedMethod() {
        assertEquals("test2", bean.test2());
    }

    @Test
    public void testUnimplementedDecoratedMethod() {
        assertEquals("test3", bean.test3());
    }
}
