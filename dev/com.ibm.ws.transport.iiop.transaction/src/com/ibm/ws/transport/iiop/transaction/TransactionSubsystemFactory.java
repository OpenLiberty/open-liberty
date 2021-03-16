/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.transaction;

import com.ibm.ws.transport.iiop.spi.SubsystemFactory;
import com.ibm.ws.transport.iiop.transaction.nodistributedtransactions.NoDTxClientTransactionPolicyConfig;
import com.ibm.ws.transport.iiop.transaction.nodistributedtransactions.NoDtxServerTransactionPolicyConfig;
import org.apache.yoko.osgi.locator.LocalFactory;
import org.apache.yoko.osgi.locator.Register;
import org.apache.yoko.osgi.locator.ServiceProvider;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import javax.transaction.TransactionManager;
import java.util.Map;

@Component(service = SubsystemFactory.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.ranking:Integer=2" })
public class TransactionSubsystemFactory extends SubsystemFactory {
    private static enum MyLocalFactory implements LocalFactory {
        INSTANCE;
        public Class<?> forName(String name) throws ClassNotFoundException {
            return TransactionInitializer.class;
        }
        public Object newInstance(Class cls) throws InstantiationException, IllegalAccessException {
            return new TransactionInitializer();
        }
    }

    private Register providerRegistry;
    private ServiceProvider transactionInitializerClass;

    private TransactionManager transactionManager;

    @Reference
    protected void setRegister(Register providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Reference
    protected void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        transactionInitializerClass = new ServiceProvider(MyLocalFactory.INSTANCE, TransactionInitializer.class);
        providerRegistry.registerProvider(transactionInitializerClass);
    }

    @Deactivate
    protected void deactivate() {
        providerRegistry.unregisterProvider(transactionInitializerClass);
    }

    /** {@inheritDoc} */
    @Override
    public Policy getTargetPolicy(ORB orb, Map<String, Object> properties, Map<String, Object> extraConfig) throws Exception {
        return new ServerTransactionPolicy(new NoDtxServerTransactionPolicyConfig(transactionManager));
    }

    /** {@inheritDoc} */
    @Override
    public Policy getClientPolicy(ORB orb, Map<String, Object> properties) throws Exception {
        return new ClientTransactionPolicy(new NoDTxClientTransactionPolicyConfig(transactionManager));
    }

    /** {@inheritDoc} */
    @Override
    public String getInitializerClassName(boolean endpoint) {
        return TransactionInitializer.class.getName();
    }

}
