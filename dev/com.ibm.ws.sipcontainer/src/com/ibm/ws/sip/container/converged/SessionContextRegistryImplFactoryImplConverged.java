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
package com.ibm.ws.sip.container.converged;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.webcontainer.httpsession.SessionManager;
import com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImpl;
import com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImplFactory;
import com.ibm.wsspi.sip.converge.SessionContextRegistryConvergedImpl;

/**
 * This component overrides SessionContextRegistryImplFactory and will be loaded when
 * SipContainer added as a feature. In this case we will want to support Converged (SIP and HTTP)
 * application.
 */
@Component(service=SessionContextRegistryImplFactory.class, property = { "service.vendor=IBM","service.ranking:Integer=1" })
public class SessionContextRegistryImplFactoryImplConverged implements SessionContextRegistryImplFactory {

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImplFactory#createSessionContextRegistryImpl(com.ibm.ws.webcontainer.httpsession.SessionManager)
     */
    @Override
    public SessionContextRegistryImpl createSessionContextRegistryImpl(SessionManager smgr) {
        return new SessionContextRegistryConvergedImpl(smgr);
    }

}
