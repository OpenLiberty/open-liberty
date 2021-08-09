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
import javax.ejb.EJB;
import javax.resource.cci.Connection;

import componenttest.app.FATServlet;
import ejb.JCAEJBDDLocal;

public class JCADDServlet extends FATServlet {

    private static final long serialVersionUID = 7909282314904580334L;

    @Resource(name = "jca/cfAppDDRef", lookup = "java:app/env/jca/cfAppDD")
    javax.resource.cci.ConnectionFactory cf2;

    @Resource(name = "jca/cfWebDDRef", lookup = "java:comp/env/jca/cfWebDD")
    javax.resource.cci.ConnectionFactory cf3;

    @Resource(name = "jca/aodd", lookup = "java:comp/env/jca/aodwebdd")
    com.ibm.adapter.message.FVTBaseMessageProvider aodddobj;

    @Resource(name = "jca/aodappdd", lookup = "java:app/env/jca/aodappdd")
    javax.jms.Queue aodappddobj;

    @EJB
    JCAEJBDDLocal ddbean;

    public void testLookupConnectionFactoryAppDD() throws Throwable {
        Connection con = cf2.getConnection();
        con.close();
    }

    public void testLookupConnectionFactoryWebDD() throws Throwable {
        Connection con = cf3.getConnection();
        con.close();
    }

    public void testLookupConnectionFactoryEJBDD() throws Throwable {
        ddbean.testLookupConnectionFactoryEJBDElement();
    }

    public void testLookupAdministeredObjectWebDD() throws Throwable {
        if (aodddobj == null)
            throw new NullPointerException("Failed to create/get basic Administered Object Definition");
    }

    public void testLookupAdministeredObjectAppDD() throws Throwable {
        if (aodappddobj == null)
            throw new NullPointerException("Failed to create/get basic Administered Object Definition");
    }

    public void testLookupAdministeredObjectEJBDD() throws Throwable {
        ddbean.testLookupAdministeredObjectEJBDD();
    }
}
