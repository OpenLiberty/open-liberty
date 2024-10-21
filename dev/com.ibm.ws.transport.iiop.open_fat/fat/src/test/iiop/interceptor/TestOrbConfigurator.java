/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.iiop.interceptor;

import static test.iiop.interceptor.TestOrbConfigurator.MyLocalFactory.SERVICE_PROVIDER;

import java.util.Map;

import org.apache.yoko.osgi.locator.LocalFactory;
import org.apache.yoko.osgi.locator.Register;
import org.apache.yoko.osgi.locator.ServiceProvider;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.transport.iiop.spi.OrbConfigurator;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TestOrbConfigurator implements OrbConfigurator {
    static enum MyLocalFactory implements LocalFactory {
		INSTANCE;
		static final ServiceProvider SERVICE_PROVIDER = new ServiceProvider(INSTANCE, ORBInitializerImpl.class);
		public Class<?> forName(String ignored) throws ClassNotFoundException { return ORBInitializerImpl.class; }
		public Object newInstance(@SuppressWarnings("rawtypes") Class ignored) { return new ORBInitializerImpl(); }
	}

    @Activate
    public TestOrbConfigurator(@Reference Register providerRegistry) { providerRegistry.registerProvider(SERVICE_PROVIDER); }

    @Override
    public Policy getTargetPolicy(ORB orb, Map<String, Object> properties, Map<String, Object> extraConfig) { return null; }

    @Override
    public Policy getClientPolicy(ORB orb, Map<String, Object> properties) { return null; }

    @Override
    public String getInitializerClassName(boolean endpoint) { return ORBInitializerImpl.class.getName(); }
}
