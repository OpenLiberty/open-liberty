/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.internal;

import javax.ejb.EJBException;

import com.ibm.ejs.container.EJBNotFoundException;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.EJSWrapperCommon;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBReference;
import com.ibm.ws.ejbcontainer.EJBReferenceFactory;
import com.ibm.ws.managedobject.ManagedObjectContext;

public class EJBReferenceFactoryImpl implements EJBReferenceFactory {
    private static final TraceComponent tc = Tr.register(EJBReferenceFactoryImpl.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");
    private final J2EEName j2eeName;
    private EJSHome home;

    public EJBReferenceFactoryImpl(J2EEName j2eeName) {
        this.j2eeName = j2eeName;
    }

    private synchronized EJSHome getEJSHome() {
        if (home == null) {
            try {
                home = EJSContainer.getDefaultContainer().getInstalledHome(j2eeName);
            } catch (EJBNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return home;
    }

    @Override
    public EJBReference create(ManagedObjectContext context) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "create : " + j2eeName + " : " + context);

        EJSWrapperCommon wc;
        try {
            wc = getEJSHome().createBusinessObjectWrappers(context);
        } catch (EJBException e) {
            throw e;
        } catch (Exception e) {
            throw new EJBException(e);
        }

        EJBReference ejbReference = new EJBReferenceImpl(wc);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "create : " + ejbReference);
        return ejbReference;
    }
}
