/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.resource.ConnectionFactoryDefinition;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.servlet.annotation.WebServlet;

@ConnectionFactoryDefinition(name = "java:comp/env/jca/dummycfd",
                             description = "Test Connection Factory",
                             resourceAdapter = "#connectionfactory",
                             interfaceName = "jakarta.resource.cci.ConnectionFactory")
@WebServlet("/*")
public class CFDServlet extends FATServlet {

    @Resource(name = "jca/dummycfdRef", lookup = "java:comp/env/jca/dummycfd")
    ConnectionFactory dummy;

    public void testCFDResource() throws Exception {
        assertNotNull(dummy);
        assertEquals("DUMMY_NAME", dummy.getMetaData().getAdapterName());
    }
}
