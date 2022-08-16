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
package com.ibm.ws.transport.iiop.workcontext;

//import java.util.Map;

import org.apache.yoko.osgi.locator.LocalFactory;
import org.apache.yoko.osgi.locator.Register;
import org.apache.yoko.osgi.locator.ServiceProvider;
//import org.omg.CORBA.ORB;
//import org.omg.CORBA.Policy;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.transport.iiop.spi.SubsystemFactory;
import com.ibm.wsspi.threading.ExecutorServiceTaskInterceptor;

@Component(service = SubsystemFactory.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.ranking:Integer=2" })
public class IIOPWorkContextSubsystemFactory extends SubsystemFactory {
    private static enum MyLocalFactory implements LocalFactory {
        INSTANCE;
        public Class<?> forName(String name) throws ClassNotFoundException {
            return IIOPWorkContextInitializer.class;
        }
        @SuppressWarnings("rawtypes")
        public Object newInstance(Class cls) throws InstantiationException, IllegalAccessException {
            return new IIOPWorkContextInitializer();
        }
    }

    private Register providerRegistry;
    private ServiceProvider workcontextInitializerClass;

    @Reference
    protected void setRegister(Register providerRegistry) {
        this.providerRegistry = providerRegistry;
    }


    // Interceptor to Activate
    @Reference
    protected synchronized void setInterceptor(ExecutorServiceTaskInterceptor interceptor) {

    }

    protected synchronized void unsetInterceptor(ExecutorServiceTaskInterceptor interceptor) {

    }


    // ORB locates IIOPWorkContextInitializer
    @Activate
    protected void activate(BundleContext bundleContext) {
        workcontextInitializerClass = new ServiceProvider(MyLocalFactory.INSTANCE, IIOPWorkContextInitializer.class);
        providerRegistry.registerProvider(workcontextInitializerClass);
        System.out.println(" leon Factory Activate ") ;
    }

    @Deactivate
    protected void deactivate() {
        providerRegistry.unregisterProvider(workcontextInitializerClass);
    }

    /** {@inheritDoc} */
    @Override
    public String getInitializerClassName(boolean endpoint) {
        return IIOPWorkContextInitializer.class.getName();
    }

}
