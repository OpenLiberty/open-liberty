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
package io.openliberty.cdi40.internal.fat.bce.basicear.war2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.cdi40.internal.fat.bce.basicear.lib.LibTestBean;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/test")
@SuppressWarnings("serial")
public class EarBCETestServlet2 extends FATServlet {

    @Inject
    private LibTestBean testBean;

    @Test
    public void testBean() {
        assertEquals("test bean", testBean.getName());
    }
}
