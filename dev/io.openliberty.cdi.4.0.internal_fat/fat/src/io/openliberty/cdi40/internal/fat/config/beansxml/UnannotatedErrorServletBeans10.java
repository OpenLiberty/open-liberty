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
package io.openliberty.cdi40.internal.fat.config.beansxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/UnannotatedErrorBeans10")
public class UnannotatedErrorServletBeans10 extends FATServlet {

    @Inject
    private RequestScopedBean requestScopedBean;

    @Inject
    private UnannotatedBean unannotatedBean; //this will actually cause a deployment error, even if the legacy config option is set

    @Test
    public void testAllBeansInjected() {
        //this should never be called because app won't deploy
        assertNotNull(requestScopedBean);
        assertEquals("RequestScopedBean", requestScopedBean.test());

        assertNotNull(unannotatedBean);
        assertEquals("UnannotatedBean", unannotatedBean.test());
    }

}
