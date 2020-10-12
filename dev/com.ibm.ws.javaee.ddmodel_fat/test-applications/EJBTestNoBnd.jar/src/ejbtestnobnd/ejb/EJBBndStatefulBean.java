/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejbtestnobnd.ejb;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.interceptor.Interceptors;
import javax.sql.DataSource;

import org.junit.Assert;

@Stateful
@DataSourceDefinition(name = "jdbc/dsdOverrideBinding", className = "invalid")
public class EJBBndStatefulBean implements EJBBndLocal {
    @Resource
    SessionContext context;

    @EJB(name = "ejb/stateless/ambiguous1")
    EJBBndAmbiguousLocal ambiguousStateless1;
  
    // Checking interceptor's method
    protected static boolean isPostConstructCalled;
    protected static boolean isAroundInvokeCalled;
    protected static boolean isPreDestroyCalled;

    @Override
    public Class<?> getEJBClass() {
        return getClass();
    }

    @Resource(name = "boolAnnotationInjectionBinding")
    private boolean boolAnnotationInjectionBinding;

    @Override
    public void verifyEnvEntryBinding() {
        Assert.assertTrue("Interceptor should have set flag.", isAroundInvokeCalled);
        Assert.assertTrue("EnvEntry not retrieved from bindings.", boolAnnotationInjectionBinding);
    }

    @Resource(name = "jdbc/ejbResRefDefaultIsoDS")
    private DataSource _bindEJBDefaultDS;  
    
    @Resource(name = "jdbc/ejbResRefEjbIsoDS")
    private DataSource _bindEjbEjbTransactionReadUncommitDS;  
           
    @Resource(name = "jdbc/dupBindingDS")
    private DataSource _bindDupTranactionReadUncommitDS; 
    
    @Override
    public void verifyResourceBinding() throws Exception {
        Assert.assertTrue("Interceptor should have set flag.", isAroundInvokeCalled);
        verifyDataSource(_bindEJBDefaultDS, Connection.TRANSACTION_REPEATABLE_READ);  
    }
    
    @Override
    public void verifyResourceIsolationBindingMerge() throws Exception {
        Assert.assertTrue("Interceptor should have set flag.", isAroundInvokeCalled);
        verifyDataSource(_bindEjbEjbTransactionReadUncommitDS, Connection.TRANSACTION_READ_UNCOMMITTED);
        verifyDataSource(_bindDupTranactionReadUncommitDS, Connection.TRANSACTION_READ_UNCOMMITTED); 
    }
    
    @Override 
    public void verifyEJBRef() {
        Assert.assertTrue("ambiguousStateless1 should have been bound", ambiguousStateless1!=null);
        Assert.assertEquals("Name should have been bean1.", "bean1", ambiguousStateless1.getName());
    }
    
    public static void verifyDataSource(DataSource ds, int expectedIsolationLevel) throws SQLException {
        if (ds == null) {
            throw new IllegalStateException("DataSource is null");
        }

        // verify that a connection can be obtained from the data source
        Connection conn = ds.getConnection();
        if (conn == null) {
            throw new IllegalStateException("Failed to get connection");
        }

        try {
            // Verify that the isolation level is read from the extension file
            // the Derby default isolation level is TRANSACTION_REPEATABLE_READ=4.
            int isolationLevel = conn.getTransactionIsolation();
            Assert.assertEquals("Isolation level should have been set.", expectedIsolationLevel, isolationLevel);
            conn.close();
        } catch (Exception e) {
            // swallow this exception
        }
    }

    @Override
    public void verifyInterceptor() {
        // nothing to do here. See EJBBndInterceptor.java
    }

    @Override
    public void verifyDataSourceBinding() throws Exception {
        // EMPTY
    }
}
