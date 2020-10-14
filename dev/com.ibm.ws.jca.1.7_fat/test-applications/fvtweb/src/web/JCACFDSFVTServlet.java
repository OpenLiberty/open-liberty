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
import javax.resource.ConnectionFactoryDefinitions;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.spi.TransactionSupport;

import componenttest.app.FATServlet;

@ConnectionFactoryDefinitions({ @ConnectionFactoryDefinition(name = "java:comp/env/jca/connfactory2",
                                                             interfaceName = "javax.resource.cci.ConnectionFactory",
                                                             resourceAdapter = "IMS1",
                                                             transactionSupport = TransactionSupport.TransactionSupportLevel.XATransaction,
                                                             maxPoolSize = 2,
                                                             minPoolSize = 1),
                                @ConnectionFactoryDefinition(name = "java:comp/env/jca/connfactory3",
                                                             interfaceName = "javax.resource.cci.ConnectionFactory",
                                                             resourceAdapter = "IMS1",
                                                             transactionSupport = TransactionSupport.TransactionSupportLevel.LocalTransaction,
                                                             maxPoolSize = 1,
                                                             minPoolSize = 1),
                                @ConnectionFactoryDefinition(name = "java:comp/env/jca/connfactory5",
                                                             interfaceName = "javax.resource.cci.ConnectionFactory",
                                                             resourceAdapter = "HELLOWORLD1",
                                                             transactionSupport = TransactionSupport.TransactionSupportLevel.NoTransaction,
                                                             maxPoolSize = 1,
                                                             minPoolSize = 1)
})
public class JCACFDSFVTServlet extends FATServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    @Resource(name = "jca/connfactoryref2", lookup = "java:comp/env/jca/connfactory2")
    javax.resource.cci.ConnectionFactory cf2;

    @Resource(name = "jca/connfactoryref3", lookup = "java:comp/env/jca/connfactory3")
    javax.resource.cci.ConnectionFactory cf3;

    @Resource(name = "jca/connfactoryref5", lookup = "java:comp/env/jca/connfactory5")
    javax.resource.cci.ConnectionFactory cf5;

    public void testLookupConnectionFactoryDefinitionsAnnotation() throws Throwable {
        Connection con1 = cf2.getConnection();
        Connection con2 = cf3.getConnection();
        Connection con3 = cf5.getConnection();
        con1.close();
        con2.close();
        con3.close();
    }

    public void testConnectionFactoryDefinitionsAnnotationMaxPoolSize() throws Throwable {
        Connection con = cf2.getConnection();
        Connection con1 = cf2.getConnection();
        try {
            cf2.getConnection();
            fail("Should not be able to get a third connection when maxPoolSize=2");
        } catch (ResourceException expected) {
            System.out.println("Got expected ResourceException");
        } finally {
            con.close();
            con1.close();
        }
    }

}
