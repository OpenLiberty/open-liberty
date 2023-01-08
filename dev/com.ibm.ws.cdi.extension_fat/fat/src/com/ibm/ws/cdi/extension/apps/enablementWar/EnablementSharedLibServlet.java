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
package com.ibm.ws.cdi.extension.apps.enablementWar;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.extension.impl.RTExtensionReqScopedBean;

import componenttest.app.FATServlet;

/**
 * Test that CDI is enabled if there's a shared lib containing a bean
 * <p>
 * The app must be deployed with a shared library for this test to pass
 */
@SuppressWarnings("serial")
@WebServlet("/testWithSharedLib")
public class EnablementSharedLibServlet extends FATServlet {

    @Inject
    private Instance<RTExtensionReqScopedBean> rtExtensionReqScopedBean;

    @Test
    public void testCdiEnabledViaSharedLib() {
        // If there's a bean visible via a shared library, CDI gets enabled and injection works
        assertThat(rtExtensionReqScopedBean, not(nullValue()));
        assertThat(rtExtensionReqScopedBean.get().getName(), equalTo("RTExtensionReqScopedBean"));
    }

}
