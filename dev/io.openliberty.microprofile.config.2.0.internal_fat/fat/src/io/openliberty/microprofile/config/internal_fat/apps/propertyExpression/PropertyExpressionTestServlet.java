/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.propertyExpression;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/propertyExpressionTestServlet")
public class PropertyExpressionTestServlet extends FATServlet {

    @Inject
    PropertyExpressionBean bean;

    @Test
    public void variableInServerXMLProperyExpression() throws Exception {
        bean.checkVariable();
    }

    @Test
    public void appPropertyInServerXMLProperyExpression() throws Exception {
        bean.checkAppProperty();
    }
}