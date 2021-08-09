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
import javax.resource.ConnectionFactoryDefinition;
import javax.resource.cci.Connection;
import javax.resource.spi.TransactionSupport;

import componenttest.app.FATServlet;

/* Helloworld rar has Transaction support level as NoTrandsaction,
 * XATransaction in @ConnectionFactoryDefinition should fail with CWNEN1006E error
 *
 */
@ConnectionFactoryDefinition(name = "java:comp/env/jca/connfactory4",
                             interfaceName = "javax.resource.cci.ConnectionFactory",
                             resourceAdapter = "HELLOWORLD1",
                             transactionSupport = TransactionSupport.TransactionSupportLevel.XATransaction,
                             maxPoolSize = 2,
                             minPoolSize = 1)
public class JCATransactionFVTServlet extends FATServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    @Resource(name = "jca/connfactoryref4", lookup = "java:comp/env/jca/connfactory4")
    javax.resource.cci.ConnectionFactory cf4;

    public void testConnectionFactoryAnnotationTransactionSupport() throws Throwable {
        Connection con1 = cf4.getConnection();
        con1.close();
    }
}
