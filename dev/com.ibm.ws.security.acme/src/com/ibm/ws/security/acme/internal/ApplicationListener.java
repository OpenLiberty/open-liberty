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
package com.ibm.ws.security.acme.internal;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.security.KeyPair;
import java.security.Security;
import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.URI;
import java.io.IOException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.security.acme.internal.ApplicationProcessor;


@Component(service = { ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ApplicationListener implements ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(ApplicationListener.class);
    
    private ApplicationProcessor appProcessor;
    /**
    * Service reference to this instance.
    */
    private final AtomicReference<ComponentContext> cctx = new AtomicReference<ComponentContext>();

    /**
     * The properties class that contain the attributes defined
     * by inside of server.xml.
     */
    private final Properties sessionProperties = new Properties();
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
    	Tr.info(tc, "******* JTM ******* ApplicationListener: inside activate() method.");
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context, Map<String, Object> properties) {
    	Tr.info(tc, " ******* JTM ******* ApplicationListener: inside deactivate() method.");
    }
 
    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
       	Tr.info(tc, " ******* JTM ******* ApplicationListener: inside applicationStarting() method for application: "+appInfo.getName());
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
    	Tr.info(tc, " ******* JTM ******* ApplicationListener: inside applicationStarted() method for application: "+appInfo.getName());
    }
    
    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
       	Tr.info(tc, " ******* JTM ******* ApplicationListener: inside applicationStopping() method for application: "+appInfo.getName());
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
    	Tr.info(tc, " ******* JTM ******* ApplicationListener: inside applicationStopped() method for application: "+appInfo.getName());
    }
    
    @Reference(service = ApplicationProcessor.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    protected void setApplicationProcessor(ApplicationProcessor appProcessor) {
        this.appProcessor = appProcessor;
    }

    protected void unsetApplicationProcessor(ApplicationProcessor appProcessor) {
        this.appProcessor = null;
    }
    
}