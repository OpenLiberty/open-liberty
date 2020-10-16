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

import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.resource.ConnectionFactoryDefinition;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.spi.TransactionSupport;

import componenttest.app.FATServlet;

@ConnectionFactoryDefinition(name = "java:comp/env/jca/connfactory1",
                             description = "It is Test ConnectionFactory",
                             interfaceName = "javax.resource.cci.ConnectionFactory",
                             resourceAdapter = "IMS1",
                             transactionSupport = TransactionSupport.TransactionSupportLevel.NoTransaction,
                             maxPoolSize = 2,
                             minPoolSize = 1,
                             properties = { "CM0Dedicated=true", "config.displayId=TestdisplayId",
                                            "dataStoreName=myDStrNm",
                                            "hostName=localhost",
                                            "portNumber=8888", "traceLevel=2",
                                            "InvalidProperty=Invalid" })
public class JCAFVTServlet extends FATServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    @Resource(name = "jca/connfactoryref1", lookup = "java:comp/env/jca/connfactory1")
    javax.resource.cci.ConnectionFactory cf1;

    public void testLookupConnectionFactoryAnnotation() throws Throwable {
        Connection con = cf1.getConnection();
        con.close();
    }

    public void testConnectionFactoryAnnotationMaxPoolSize() throws Throwable {
        Connection con = cf1.getConnection();
        Connection con1 = cf1.getConnection();
        try {
            cf1.getConnection();
            fail("Should not be able to get a third connection when maxPoolSize=2");
        } catch (ResourceException expected) {
        } finally {
            con.close();
            con1.close();
        }
    }
}
