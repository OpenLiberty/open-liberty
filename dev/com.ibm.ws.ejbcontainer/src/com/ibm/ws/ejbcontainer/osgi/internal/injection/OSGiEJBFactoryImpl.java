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
package com.ibm.ws.ejbcontainer.osgi.internal.injection;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ejs.container.EJBLinkResolver;
import com.ibm.ejs.container.EJBNotFoundException;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ejbcontainer.EJBFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.osgi.EJBHomeRuntime;
import com.ibm.ws.ejbcontainer.osgi.internal.EJBRuntimeImpl;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = EJBFactory.class)
public class OSGiEJBFactoryImpl extends EJBLinkResolver {
    private static final TraceComponent tc = Tr.register(OSGiEJBFactoryImpl.class);
    private static final String REFERENCE_RUNTIME = "runtime";

    private J2EENameFactory j2eeNameFactory;
    private volatile boolean homeRuntime;

    private final AtomicServiceReference<EJBRuntimeImpl> runtimeSR = new AtomicServiceReference<EJBRuntimeImpl>(REFERENCE_RUNTIME);

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=server)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    @Reference
    protected void setJ2EENameFactory(J2EENameFactory ref) {
        this.j2eeNameFactory = ref;
    }

    protected void unsetJ2EENameFactory(J2EENameFactory ref) {}

    @Reference(service = EJBHomeRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        homeRuntime = true;
    }

    protected void unsetEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        homeRuntime = false;
    }

    @Reference(name = REFERENCE_RUNTIME, service = EJBRuntimeImpl.class)
    protected void setRuntime(ServiceReference<EJBRuntimeImpl> reference) {
        runtimeSR.setReference(reference);
    }

    protected void unsetRuntime(ServiceReference<EJBRuntimeImpl> reference) {
        runtimeSR.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        runtimeSR.activate(cc);

        // Force the EJBRuntime to start if it hasn't already, and create the HomeOfHomes
        runtimeSR.getServiceWithException();

        initialize(EJSContainer.homeOfHomes, j2eeNameFactory);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        runtimeSR.deactivate(cc);
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
