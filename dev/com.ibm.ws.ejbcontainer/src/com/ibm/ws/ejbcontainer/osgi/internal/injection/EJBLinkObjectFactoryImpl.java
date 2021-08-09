/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.injection;

import javax.naming.spi.ObjectFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ejs.container.EJBNotFoundException;
import com.ibm.ejs.container.EJSHome;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.injection.factory.EJBLinkObjectFactory;
import com.ibm.ws.ejbcontainer.osgi.EJBHomeRuntime;
import com.ibm.ws.kernel.LibertyProcess;

@Component(service = { ObjectFactory.class, EJBLinkObjectFactoryImpl.class })
public class EJBLinkObjectFactoryImpl extends EJBLinkObjectFactory {
    private static final TraceComponent tc = Tr.register(EJBLinkObjectFactoryImpl.class);

    private volatile boolean homeRuntime;

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=server)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    @Reference(service = EJBHomeRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        homeRuntime = true;
    }

    protected void unsetEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        homeRuntime = false;
    }

    @Override
    public void checkHomeSupported(EJSHome home, String homeInterface) throws EJBNotFoundException {
        if (!homeRuntime) {
            J2EEName j2eeName = home.getJ2EEName();
            String appName = j2eeName.getApplication();
            String moduleName = j2eeName.getModule();
            String ejbName = j2eeName.getComponent();

            String msgTxt = Tr.formatMessage(tc, "INJECTION_CANNOT_INSTANTIATE_HOME_CNTR4011E",
                                             homeInterface,
                                             ejbName,
                                             moduleName,
                                             appName);

            throw new EJBNotFoundException(msgTxt);
        }
    }
}
