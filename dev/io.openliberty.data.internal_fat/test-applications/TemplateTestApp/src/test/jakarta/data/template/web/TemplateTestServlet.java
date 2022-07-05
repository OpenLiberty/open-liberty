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
package test.jakarta.data.template.web;

import java.time.Year;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.data.Entities;
import io.openliberty.data.Template;

@Entities(House.class)
@WebServlet("/*")
public class TemplateTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private static final long TIMEOUT_MINUTES = 2L;

    @Inject
    Template template;

    @Resource
    private UserTransaction tran;

    /**
     */
    @Test
    public void testTemplate() {
        House h = new House();
        h.area = 1500;
        h.lotSize = 0.18f;
        h.numBedrooms = 3;
        h.parcel = "P0001.2000.3333.4040.1";
        h.purchasePrice = 125000.00f;
        h.sold = Year.of(2015);
        template.insert(h);
    }
}
