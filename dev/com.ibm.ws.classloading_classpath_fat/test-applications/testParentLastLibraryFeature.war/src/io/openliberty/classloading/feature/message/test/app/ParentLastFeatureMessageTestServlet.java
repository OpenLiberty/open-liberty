/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.feature.message.test.app;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/ParentLastFeatureMessageTestServlet")
public class ParentLastFeatureMessageTestServlet extends FATServlet{

    private static final long serialVersionUID = 1L;

    @Test
    public void testNoFeatureSuggestionMessage() throws ClassNotFoundException {
        Class.forName("javax.transaction.Transaction");
    }

    @Test
    public void testYesFeatureSuggestionMessage() {
        // intentional load of a class that cannot be found (even on Java 8)
        try {
            Class.forName("javax.xml.bind.TriggerNotFound");
        } catch (ClassNotFoundException e) {
            // expected
        }
    }
}
