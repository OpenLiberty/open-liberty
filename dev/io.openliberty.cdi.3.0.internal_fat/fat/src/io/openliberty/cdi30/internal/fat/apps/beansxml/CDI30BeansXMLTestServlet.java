/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi30.internal.fat.apps.beansxml;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/CDI30BeansXMLTestServlet")
public class CDI30BeansXMLTestServlet extends FATServlet {

    @Inject
    private SimpleBean bean;

    @Test
    public void testBeansXML() {
        assertEquals(SimpleBean.HELLO, bean.hello());
    }

}
