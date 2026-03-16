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
package web.cdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.resource.AdministeredObjectDefinition;
import jakarta.servlet.annotation.WebServlet;
import lib.cdi.DummyService;
import ra.ao.DummyInterface;

@AdministeredObjectDefinition(name = "java:comp/env/jca/dummyaod",
                              description = "Test Administered Object",
                              resourceAdapter = "#adminobject",
                              className = "ra.ao.DummyImpl",
                              interfaceName = "ra.ao.DummyInterface",
                              properties = { "message=DUMMY_MESSAGE" })
@WebServlet("/*")
public class AODServlet extends FATServlet {

    @Resource(name = "jca/dummyaodRef", lookup = "java:comp/env/jca/dummyaod")
    DummyInterface dummy;

    @Inject
    DummyService service;

    public void testAODResourceWithCDI() throws Exception {
        assertNotNull(dummy);
        assertEquals("DUMMY_MESSAGE", dummy.message());

        assertNotNull(service);
        assertEquals("DUMMY_MESSAGE", service.message());
    }
}
