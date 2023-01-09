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
package io.openliberty.cdi.internal.core.lookup.app;

import static org.junit.Assert.assertTrue;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/lookup")
public class CDILookupTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Test
    public void testLookup() {
        LookupTestBean bean = CDI.current().select(LookupTestBean.class).get();
        assertTrue(bean.test());
    }

}
