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
package com.ibm.ws.transport.iiop.security.config.ssl.yoko;

import com.ibm.ws.transport.iiop.spi.SubsystemFactory;
import org.apache.yoko.orb.OB.ZERO_PORT_POLICY_ID;
import org.apache.yoko.osgi.locator.LocalFactory;
import org.apache.yoko.osgi.locator.Register;
import org.apache.yoko.osgi.locator.ServiceProvider;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;
import org.omg.CSIIOP.TransportAddress;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component(service = SubsystemFactory.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "service.ranking:Integer=1" })
public class SSLSubsystemFactory extends SubsystemFactory {
    private static enum MyLocalFactory implements LocalFactory {
        INSTANCE;
        public Class<?> forName(String name) throws ClassNotFoundException {
            return Class.forName(name);
        }
        public Object newInstance(Class cls) throws InstantiationException, IllegalAccessException {
            return null;
        }
    }

    private static final String ADDR_KEY = "com.ibm.ws.transport.iiop.server.security.CSIv2SubsystemFactory"; //CSIv2SubsystemFactory.class.getName();
    private Register providerRegistry;
    private ServiceProvider sslInitializerClass;

    @Reference
    protected void setRegister(Register providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        sslInitializerClass = new ServiceProvider(MyLocalFactory.INSTANCE, ORBInitializer.class);
        providerRegistry.registerProvider(sslInitializerClass);
    }

    @Deactivate
    protected void deactivate() {
        providerRegistry.unregisterProvider(sslInitializerClass);
    }

    /**
     * {@inheritDoc}
     *
     * @throws PolicyError
     */
    @Override
    public Policy getTargetPolicy(ORB orb, Map<String, Object> properties, Map<String, Object> extraConfig) throws Exception {
        Policy portPolicy = null;
        if (isAnySecureTransportAddressAvailable(extraConfig)) {
            Any any = orb.create_any();
            any.insert_boolean(true);
            portPolicy = orb.create_policy(ZERO_PORT_POLICY_ID.value, any);
        }
        return portPolicy;
    }

    /*
     * On a server, the address map will only contain a "null" entry if there are only unsecured transport addresses.
     * A client container will not have an address map, so check for its existence and assume there are secured transport
     * addresses when an address map is not found.
     */
    @SuppressWarnings("unchecked")
    private boolean isAnySecureTransportAddressAvailable(Map<String, Object> extraConfig) {
        Map<String, List<TransportAddress>> addrMap = (Map<String, List<TransportAddress>>) extraConfig.get(ADDR_KEY);
        if (addrMap != null) {
            Set<String> sslAliases = addrMap.keySet();
            return sslAliases.size() != 1;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getInitializerClassName(boolean endpoint) {
        return ORBInitializer.class.getName();
    }

}
