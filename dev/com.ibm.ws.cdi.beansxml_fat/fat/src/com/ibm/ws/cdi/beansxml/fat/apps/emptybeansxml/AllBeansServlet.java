/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.beansxml.fat.apps.emptybeansxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

//AllBeansServlet expects to find a UnannotatedBean bean even though it is not annotated
@WebServlet("/all")
public class AllBeansServlet extends FATServlet {

    @Inject
    private RequestScopedBean requestScopedBean;

    @Inject
    private Instance<UnannotatedBean> unannotatedBean;

    @Test
    public void testAllBeansInjected() {
        assertNotNull(requestScopedBean);
        assertEquals("RequestScopedBean", requestScopedBean.test());

        assertFalse(unannotatedBean.isUnsatisfied());
        assertEquals("UnannotatedBean", unannotatedBean.get().test());
    }
}
