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
package ejb;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.resource.cci.Connection;

/**
 *
 */
@Stateless
public class JCAEJBDDBean implements JCAEJBDDLocal {

    @Resource(name = "jca/cfejbDDRef", lookup = "java:comp/env/jca/cfejbDD")
    javax.resource.cci.ConnectionFactory cf3;

    //@Resource(name = "jca/aodejbdd", lookup = "java:comp/env/jca/aodejbdd" )
    @Resource(name = "jca/aodejbdd", lookup = "java:comp/env/jca/aodejb")

    com.ibm.adapter.message.FVTBaseMessageProvider aodejbddobj;

    @Override
    public void testLookupConnectionFactoryEJBDElement() throws Exception {

        Connection con = cf3.getConnection();
        if (con == null)
            throw new Exception("Null connection obtained");
        con.close();

    }

    @Override
    public void testLookupAdministeredObjectEJBDD() throws Exception {
        if (aodejbddobj == null)
            throw new NullPointerException("Failed to create/get basic Administered Object Definition");
    }
}
