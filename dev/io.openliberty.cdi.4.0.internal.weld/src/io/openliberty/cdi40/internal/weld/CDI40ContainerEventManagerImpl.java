/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi40.internal.weld;

import java.util.HashSet;
import java.util.Set;

import org.jboss.weld.bootstrap.BeanDeploymentModule;
import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.injection.spi.InjectionServices;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.internal.interfaces.CDIContainerEventManager;

import jakarta.enterprise.event.Shutdown;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.inject.Any;

@Trivial
@Component(name = "io.openliberty.cdi40.internal.weld.CDI40ContainerEventManagerImpl",
                service = { CDIContainerEventManager.class },
                property = { "service.vendor=IBM" },
                configurationPolicy = ConfigurationPolicy.IGNORE)
public class CDI40ContainerEventManagerImpl implements CDIContainerEventManager {

    private static Environment LIBERTY_EE_ENVIRONMENT = new LibertyEEEnvironment();

    @Override
    public void fireStartupEvent(BeanDeploymentModule module) {
        module.fireEvent(Startup.class, new Startup(), Any.Literal.INSTANCE);
    }

    @Override
    public void fireShutdownEvent(BeanDeploymentModule module) {
        module.fireEvent(Shutdown.class, new Shutdown(), Any.Literal.INSTANCE);
    }

    @Override
    public Environment getEnvironment() {
        return LIBERTY_EE_ENVIRONMENT;
    }

    private static class LibertyEEEnvironment implements Environment {

        private final Set<Class<? extends Service>> requiredBeanDeploymentArchiveServices = new HashSet<Class<? extends Service>>();
        private final Set<Class<? extends Service>> requiredDeploymentServices = new HashSet<Class<? extends Service>>();

        public LibertyEEEnvironment() {
            requiredDeploymentServices.add(TransactionServices.class);
            requiredDeploymentServices.add(SecurityServices.class);

            requiredBeanDeploymentArchiveServices.add(ResourceLoader.class);
            requiredBeanDeploymentArchiveServices.add(InjectionServices.class);
        }

        @Override
        public boolean automaticallyHandleStartupShutdownEvents() {
            return false;
        }

        @Override
        public Set<Class<? extends Service>> getRequiredBeanDeploymentArchiveServices() {
            return requiredBeanDeploymentArchiveServices;
        }

        @Override
        public Set<Class<? extends Service>> getRequiredDeploymentServices() {
            return requiredDeploymentServices;
        }

        @Override
        public boolean isEEModulesAware() {
            return true;
        }

    }
}
