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
package com.ibm.ws.jaxws.cdi.component;

import javax.enterprise.inject.spi.BeanManager;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;

/**
 *
 */
@Component(
           name = "com.ibm.ws.jaxws.cdi.component.JAXWSCDIServiceImplByJndi",
           service = { ApplicationStateListener.class },
           property = { "service.vendor=IBM" })
public class JAXWSCDIServiceImplByJndi implements ApplicationStateListener {
    private static final TraceComponent tc = Tr.register(JAXWSCDIServiceImplByJndi.class);

    private static BeanManager beanManager = null;

    public static BeanManager getBeanManager() {
        return beanManager;
    }

    public static void setBeanManager(BeanManager manager) {
        beanManager = manager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStarting(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStarted(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStopping(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStopped(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        beanManager = null;

    }
}
