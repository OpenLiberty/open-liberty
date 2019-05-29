/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cxf.core;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.staxutils.StaxUtils;


import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.ConfigurationPolicy;
/**
 *
 */
@Component(property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class StaxActivator {



    private static final Logger LOG = LogUtils.getL7dLogger(JAXBUtils.class);
    /*
     * Called by Declarative Services to activate service
     */
    @Activate
    protected void activate(ComponentContext cc) throws Exception {

        //This is a workaroud to avoid invoking createWoodstoxFactory in org.apache.cxf.staxutils.StaxUtils.createXMLInputFactor
        AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
            @Override
            public Boolean run() throws Exception {
                System.setProperty(StaxUtils.ALLOW_INSECURE_PARSER, "1"); // sets the bla property without throwing an exception -> ok
                return Boolean.TRUE;
            }
        });

        LOG.log(Level.INFO, "Set BLA Property: " + StaxUtils.ALLOW_INSECURE_PARSER + " to: " + System.getProperty(StaxUtils.ALLOW_INSECURE_PARSER));
        
    }
}
