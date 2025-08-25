/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.resource.AdministeredObjectDefinition;

@AdministeredObjectDefinition(name = "java:comp/env/jca/aod1",
                              description = "It is Test Administrator Object",
                              //resourceAdapter = "fvtapp.adapter", //TODO add a seperate testcase for embedded RA
                              resourceAdapter = "#adapter",
                              className = "com.ibm.adapter.message.FVTMessageProviderImpl",
                              interfaceName = "com.ibm.adapter.message.FVTBaseMessageProvider",
                              properties = {})
public class JCAAODFVTServlet extends FATServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    @Resource(name = "jca/aod1ref", lookup = "java:comp/env/jca/aod1")
    com.ibm.adapter.message.FVTBaseMessageProvider aod1obj;

    public void testLookupAdministeredObjectDefinition() throws Throwable {
        if (aod1obj == null)
            throw new NullPointerException("Failed to create/get basic Administered Object Definition");
    }
}
