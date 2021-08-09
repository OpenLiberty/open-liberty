/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.policy;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.PolicyException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.bus.LibertyApplicationBus;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusListener;

/**
 * PolicyAttachmentApplicationBusListener will create and save a DynamicAttachmentProvider as a bus extension, which will be used
 * to resolve the special policy attachment in the runtime
 */
public class PolicyAttachmentApplicationBusListener implements LibertyApplicationBusListener {

    private static final TraceComponent tc = Tr.register(PolicyAttachmentApplicationBusListener.class);
    private static final String clientFileName = "policy-attachments-client.xml";
    private static final String serviceFileName = "policy-attachments-server.xml";

    @Override
    public void preInit(Bus bus) {}

    @Override
    public void initComplete(Bus bus) {

        if (bus == null) {
            return;
        }

        LibertyApplicationBus.Type busType = bus.getExtension(LibertyApplicationBus.Type.class);
        if (busType == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to recognize the bus type from bus, DynamicAttachmentProvider will not be created");
            }
            return;
        }

        String location[] = null;
        String finalName = "";
        if (busType.equals(LibertyApplicationBus.Type.CLIENT)) {
            finalName = clientFileName;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "This is client side so check", finalName);
            }
        } else if (busType.equals(LibertyApplicationBus.Type.SERVER)) {
            finalName = serviceFileName;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "This is server side so check", finalName);
            }

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unknown bus type", busType);
            }
            throw new PolicyException(new Exception("Cannot init PolicyAttachmentApplicationBusListener because of unknown bus type " + busType));
        }

        location = new String[] { "WEB-INF/" + finalName, "META-INF/" + finalName };

        InputStream is = null;
        ClassLoader classLoader = getThreadContextClassLoader();
        for (int i = 0; i <= 1; i++) {
            if (classLoader.getResource(location[i]) != null) {
                is = classLoader.getResourceAsStream(location[i]);
            }
            if (is != null) {
                registerBuilder(bus, location[i]);
                //defect 216117, close the InputStream, otherwise the war file will be locked.
                try {
                    is.close();
                } catch (IOException e) {
                    //Do nothing
                }
                break;
            }
        }
    }

    @Override
    public void preShutdown(Bus bus) {}

    @Override
    public void postShutdown(Bus bus) {}

    private ClassLoader getThreadContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }

        });
    }

    private void registerBuilder(Bus bus, String location) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Special policy attachment file exists, register DynamicAttachmentProvider", location);
        }
        DynamicAttachmentProvider provider = new DynamicAttachmentProvider();
        provider.setBus(bus);
        provider.setLocation(location);
        bus.setExtension(new URIDomainExpressionBuilder(), URIDomainExpressionBuilder.class);
    }
}
