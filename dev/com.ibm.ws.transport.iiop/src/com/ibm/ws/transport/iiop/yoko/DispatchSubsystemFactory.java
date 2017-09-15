/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.yoko;

import static com.ibm.wsspi.kernel.service.location.WsLocationConstants.LOC_PROCESS_TYPE;
import static com.ibm.wsspi.kernel.service.location.WsLocationConstants.LOC_PROCESS_TYPE_SERVER;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

@Component(service = SubsystemFactory.class,
                configurationPolicy = ConfigurationPolicy.IGNORE,
                property = { "service.vendor=IBM", "service.ranking:Integer=1" })
public class DispatchSubsystemFactory extends SubsystemFactory {
    private ExecutorDispatchPolicy dispatcherPolicy;

    @Reference
    protected void setExcutorService(ExecutorService executor) {
        dispatcherPolicy = new ExecutorDispatchPolicy(new ExecutorDispatchStrategy(executor));
    }

    @Override
    public Policy getTargetPolicy(ORB orb, Map<String, Object> properties, Map<String, Object> extraConfig) throws Exception {
        return dispatcherPolicy;
    }

    //Make this component only start on the server.
    @Reference(
                    service = LibertyProcess.class,
                    target = "(" + LOC_PROCESS_TYPE + "=" + LOC_PROCESS_TYPE_SERVER + ")",
                    unbind = "ensureProcessIsAServer")
    protected void ensureProcessIsAServer(ServiceReference<?> unused) {}
}
