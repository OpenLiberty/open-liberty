/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import javax.annotation.Resource;
import javax.resource.AdministeredObjectDefinition;
import javax.resource.AdministeredObjectDefinitions;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@AdministeredObjectDefinitions({
                                 @AdministeredObjectDefinition(name = "java:comp/env/jca/aod2",
                                                               description = "It is Test Administrator Object",
                                                               resourceAdapter = "fvtapp.adapter",
                                                               className = "com.ibm.adapter.message.FVTMessageProviderImpl",
                                                               interfaceName = "com.ibm.adapter.message.FVTBaseMessageProvider",
                                                               properties = {}),

                                 @AdministeredObjectDefinition(name = "java:comp/env/jca/aod3",
                                                               description = "It is Test Administrator Object",
                                                               resourceAdapter = "fvtapp.adapter",
                                                               className = "com.ibm.adapter.message.FVTMessageProviderImpl",
                                                               interfaceName = "com.ibm.adapter.message.FVTMessageProvider",
                                                               properties = {}),
                                 @AdministeredObjectDefinition(name = "java:comp/env/jca/aod4",
                                                               description = "It is Test Administrator Object",
                                                               resourceAdapter = "fvtapp.adapter",
                                                               className = "com.ibm.adapter.message.FVTMessageProviderImpl",
                                                               properties = {}),
                                 @AdministeredObjectDefinition(name = "java:comp/env/jca/aod5",
                                                               description = "It is Test Administrator Object",
                                                               resourceAdapter = "fvtapp.adapter",
                                                               className = "com.ibm.adapter.message.FVTMessageProviderImpl",
                                                               interfaceName = "com.ibm.adapter.message.FVTMessageProvider",
                                                               properties = { "property_a=property_a", "property_m=property_m" })
})
public class JCAAODSFVTServlet extends FATServlet {
    @Resource(name = "jca/aod2ref", lookup = "java:comp/env/jca/aod2")
    com.ibm.adapter.message.FVTBaseMessageProvider aod2obj;
    @Resource(name = "jca/aod3ref", lookup = "java:comp/env/jca/aod3")
    com.ibm.adapter.message.FVTMessageProvider aod3obj;
    @Resource(name = "jca/aod4ref", lookup = "java:comp/env/jca/aod4")
    com.ibm.adapter.message.FVTBaseMessageProvider aod4obj;
    @Resource(name = "jca/aod5ref", lookup = "java:comp/env/jca/aod5")
    com.ibm.adapter.message.FVTMessageProvider aod5obj;

    public void testLookupAdministeredObjectDefinitions() throws Throwable {
        if (aod2obj == null)
            throw new NullPointerException("Failed to create/get basic Administered Object Definitions");
        if (aod3obj == null)
            throw new NullPointerException("Failed to create/get basic Administered Object Definitions");

    }

    public void testLookupAdministeredObjectNoInterfaceName() throws Throwable {
        if (aod4obj == null)
            throw new NullPointerException("Failed to create/get basic Administered Object Definitions");

    }

    public void testLookupAdministeredObjectWithProperties() throws Throwable {
        if (aod5obj == null)
            throw new NullPointerException("Failed to create/get basic Administered Object Definitions");
        if (!aod5obj.getProperty_a().equals("property_a") &&
            !aod5obj.getProperty_m().equals("property_m"))
            throw new IllegalStateException("Failed to set Bean specific Properties ");
    }
}
