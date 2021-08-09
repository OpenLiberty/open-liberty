/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.misc.ejb;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.TransactionSynchronizationRegistry;

import org.junit.Assert;

@Stateless
@DataSourceDefinition(name = "java:app/env/dataSourceBinding", className = "binding.should.override")
public class JavaAppBean {
    // Value from another bean (JavaAppResourceBean)
    @Resource(name = "java:app/env/envEntryInjection")
    private int ivEnvEntryInjection;

    // value from binding file
    @Resource(name = "java:app/env/envEntryBindingValue")
    private int ivEnvEntryBindingValue;

    // binding-name from binding file
    @Resource(name = "java:app/env/envEntryBindingName")
    private int ivEnvEntryBindingName;
    @Resource(name = "java:app/env/envEntryBindingNameIndirect")
    private int ivEnvEntryBindingNameIndirect;

    @Resource(name = "java:app/env/sessionContext")
    private SessionContext ivSessionContext;

    @Resource(name = "java:app/env/transactionSynchronizationRegistry")
    private TransactionSynchronizationRegistry ivTSR;

    public void testEnvEntryInjection() throws Exception {
        System.out.println("JavaAppBean: java:app/env/envEntryInjection = " + new InitialContext().lookup("java:app/env/envEntryInjection"));
        Assert.assertEquals(1, ivEnvEntryInjection);
        Assert.assertEquals(1, new InitialContext().lookup("java:app/env/envEntryInjection"));
    }

    public void testEnvEntryBindingValue() throws Exception {
        Assert.assertEquals(2, ivEnvEntryBindingValue);
        Assert.assertEquals(2, new InitialContext().lookup("java:app/env/envEntryBindingValue"));
    }

    public void testEnvEntryBindingName() throws Exception {
        Assert.assertEquals(3, ivEnvEntryBindingName);
        Assert.assertEquals(3, new InitialContext().lookup("java:app/env/envEntryBindingName"));
    }

    public void testDataSourceBinding() throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup("java:app/env/dataSourceBinding");
        Assert.assertNotNull(ds.getConnection());
    }

    public void testSessionContext() throws Exception {
        for (SessionContext context : new SessionContext[] {
                                                             ivSessionContext,
                                                             (SessionContext) new InitialContext().lookup("java:app/env/sessionContext"),
        }) {
            Assert.assertEquals(JavaAppBean.class, context.getInvokedBusinessInterface());
        }
    }

    public void testTransactionSynchronizationRegistry() throws Exception {
        for (TransactionSynchronizationRegistry tsr : new TransactionSynchronizationRegistry[] {
                                                                                                 ivTSR,
                                                                                                 (TransactionSynchronizationRegistry) new InitialContext().lookup("java:app/env/transactionSynchronizationRegistry"),
        }) {
            Assert.assertFalse(tsr.getRollbackOnly());
        }
    }
}
