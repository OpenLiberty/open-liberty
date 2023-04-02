/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.lifecycle.apps.transientReferenceBasicWar;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import javax.enterprise.inject.TransientReference;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class TransientReferenceTestServlet extends FATServlet {

    @Inject
    BeanHolder bh;

    @Test
    public void testTransientReference() {
        bh.doNothing();
        assertThat(GlobalState.getOutput(), containsInAnyOrder("destroyed-one", "destroyed-two", "doNothing2"));
    }

    @Inject
    public void transientVisit(@TransientReference TransiantDependentScopedBeanTwo bean) {
        bean.doNothing();
    }

}
