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
package io.openliberty.cdi.internal.core.producers.app;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/producer")
public class CDIProducerTestServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    @ProducedBy("field")
    private TestBean producedByField;

    @Inject
    @ProducedBy("method")
    private TestBean producedByMethod;

    @Test
    public void testProducerField() {
        assertEquals("field", producedByField.getValue());
    }

    @Test
    public void testProducerMethod() {
        assertEquals("method", producedByMethod.getValue());
    }
}
