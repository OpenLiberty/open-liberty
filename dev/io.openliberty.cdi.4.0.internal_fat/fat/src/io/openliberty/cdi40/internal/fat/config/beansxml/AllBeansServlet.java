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
package io.openliberty.cdi40.internal.fat.config.beansxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

//AllBeansServlet expects to find a UnannotatedBean bean even though it is not annotated
@WebServlet("/all")
public class AllBeansServlet extends FATServlet {

    @Inject
    private RequestScopedBean requestScopedBean;

    @Inject
    private UnannotatedBean unannotatedBean;

    @Test
    public void testAllBeansInjected() {
        assertNotNull(requestScopedBean);
        assertEquals("RequestScopedBean", requestScopedBean.test());

        assertNotNull(unannotatedBean);
        assertEquals("UnannotatedBean", unannotatedBean.test());
    }
}
