/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cdi.beansxml.fat.apps.aftertypediscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/")
public class AfterTypeServlet extends FATServlet {

    private static final long serialVersionUID = 8549700799591343964L;

    @Inject
    AfterTypeInterface b;

    @Inject
    InterceptedBean ib;

    @Inject
    AfterTypeAlternativeInterface altOne;

    @Inject
    @UseAlternative
    AfterTypeAlternativeInterface altTwo;

    private List<String> test() {

        List<String> results = new ArrayList<String>();

        ib.doNothing();

        results.add(b.getMsg());

        for (String s : GlobalState.getOutput()) {
            results.add(s);
        }

        return results;
    }

    @Test
    public void testAfterTypeDecoratorAddition() throws Exception {
        assertTrue(test().contains(AfterTypeBeanDecorator.DECORATED + AfterTypeBean.MESSAGE));
    }

    @Test
    public void testAfterTypeInterceptorAddition() throws Exception {
        assertTrue(test().contains(AfterTypeInterceptorImpl.INTERCEPTED));
    }

    @Test
    public void testAfterTypeAlternativeOne() throws Exception {
        assertEquals(AfterTypeAlternativeOne.MSG, altOne.getMsg());
    }

    @Test
    public void testAfterTypeAlternativeTwo() throws Exception {
        assertEquals(AfterTypeAlternativeTwo.MSG, altTwo.getMsg());
    }
}
