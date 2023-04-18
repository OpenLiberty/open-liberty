/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.noncontextual.apps.nonContextualWar;

import static org.junit.Assert.assertEquals;

import javax.enterprise.inject.spi.Unmanaged;
import javax.enterprise.inject.spi.Unmanaged.UnmanagedInstance;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class NonContextualTestServlet extends FATServlet {

    @Test
    public void testNonContextualBean() {
        Unmanaged<NonContextualBean> unmanagedBean = new Unmanaged<NonContextualBean>(NonContextualBean.class);
        UnmanagedInstance<NonContextualBean> beanInstance = unmanagedBean.newInstance();
        NonContextualBean bean = beanInstance.produce().inject().postConstruct().get();

        try {
            assertEquals("42!", bean.hello());
        } finally {
            beanInstance.preDestroy().dispose();
        }
    }

}
