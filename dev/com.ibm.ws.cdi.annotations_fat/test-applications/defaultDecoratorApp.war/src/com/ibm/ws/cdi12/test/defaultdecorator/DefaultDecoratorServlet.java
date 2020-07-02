/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.defaultdecorator;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.enterprise.context.Conversation;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/")
public class DefaultDecoratorServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    Conversation c;

    private static String output = "FAIL";

    public static void setOutput(String s) {
        output = s;
    }

    @Test
    public void testDecorator() throws IOException {

        c.isTransient();

        assertEquals("decorating", output);

    }

}
