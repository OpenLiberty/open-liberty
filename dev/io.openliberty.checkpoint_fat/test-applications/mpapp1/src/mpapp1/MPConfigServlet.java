/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpapp1;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/MPConfigServlet")
public class MPConfigServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    MPConfigBean bean;

    @Test
    public void defaultValueTest() {
        bean.defaultValueTest();
    }

    @Test
    public void envValueTest() {
        bean.envValueTest();
    }

    @Test
    public void envValueChangeTest() {
        bean.envValueChangeTest();
    }

    @Test
    public void serverValueTest() {
        bean.serverValueTest();
    }

    @Test
    public void annoValueTest() {
        bean.annoValueTest();
    }
}
