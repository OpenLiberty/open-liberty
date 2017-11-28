/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.server.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.kernel.server.ServerEndpointControlMBean;
import com.ibm.ws.kernel.launch.service.PauseableComponent;
import com.ibm.ws.kernel.launch.service.PauseableComponentController;
import com.ibm.ws.kernel.launch.service.PauseableComponentControllerRequestFailedException;

/*
 * Register this mbean as an osgi service
 */
@Component(service = { ServerEndpointControlMBean.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM",
                        "jmx.objectname=" + ServerEndpointControlMBean.OBJECT_NAME })

public class ServerEndpointControlMBeanImpl extends StandardMBean implements ServerEndpointControlMBean {

    @Reference(target = "(betaProperty=true)")
    java.lang.Object betaDependency;

    /*
     * inject the service that will allow pause and resume control of endpoints
     *
     */
    @Reference(service = PauseableComponentController.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.STATIC,
               policyOption = ReferencePolicyOption.GREEDY)
    private PauseableComponentController pauseableComponentController;

    public ServerEndpointControlMBeanImpl() throws NotCompliantMBeanException {
        super(ServerEndpointControlMBean.class);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.kernel.server.ServerEndpointControlMBean#pause(java.lang.String)
     */
    @Override
    public void pause(String targets) throws MBeanException {
        try {
            if (targets == null | "".equals(targets))
                pauseableComponentController.pause();
            else
                pauseableComponentController.pause(targets);
        } catch (PauseableComponentControllerRequestFailedException ex) {
            throw new MBeanException(ex);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.kernel.server.ServerEndpointControlMBean#resume(java.lang.String)
     */
    @Override
    public void resume(String targets) throws MBeanException {
        try {
            if (targets == null | "".equals(targets))
                pauseableComponentController.resume();
            else
                pauseableComponentController.resume(targets);
        } catch (PauseableComponentControllerRequestFailedException ex) {
            throw new MBeanException(ex);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.kernel.server.ServerEndpointControlMBean#isPaused(java.lang.String)
     */
    @Override
    public boolean isPaused(String targets) throws MBeanException {
        try {
            if (targets == null | "".equals(targets))
                return pauseableComponentController.isPaused();
            else
                return pauseableComponentController.isPaused(targets);
        } catch (PauseableComponentControllerRequestFailedException ex) {
            throw new MBeanException(ex);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.kernel.server.ServerEndpointControlMBean#listEndpoints()
     */
    @Override
    public List<String> listEndpoints() throws MBeanException {
        Collection<PauseableComponent> pauseableComponents = pauseableComponentController.getPauseableComponents();
        System.out.println("pauseableComponents length" + pauseableComponents.size() + " " + pauseableComponents.toString());
        ArrayList<String> endpoints = new ArrayList<String>();
        // list of endpoints may change while iterating
        synchronized (this) {
            while (true) {
                try {
                    for (PauseableComponent pauseableComponent : pauseableComponents) {
                        endpoints.add(pauseableComponent.getName());
                    }
                    break;
                } catch (Throwable t) {
                    // Someone modified our list of endpoints. Retry.
                    endpoints.clear();
                }
            }
        }
        return endpoints;
    }

    /** {@inheritDoc} */
    @Override
    protected final String getDescription(MBeanInfo info) {
        return "MBean to pause or resume a server endpoint.";
    }

    /** {@inheritDoc} */
    @Override
    protected final String getDescription(MBeanOperationInfo info) {
        final String opName = info.getName();
        String opDescription = new String();
        switch (opName) {
            case "listEndpoints":
                opDescription = "Returns the name of all endpoints that can be paused/resumed";
                break;
            case "isPaused":
                opDescription = "Returns true if the server endpoint(s) specified by target(s) is paused, otherwise false.";
                break;
            case "resume":
                opDescription = "Resumes the server endpoint(s) specified by target(s)";
                break;
            case "pause":
                opDescription = "Pauses the server endpoint(s) specified by target(s)";
                break;
            default:
                opDescription = "Operation exposed for management";
                break;
        }
        return opDescription;
    }

}
