/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.session.impl.factory;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.webcontainer.httpsession.SessionManager;
import com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImpl;
import com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImplFactory;
import com.ibm.ws.webcontainer31.session.impl.SessionContextRegistry31Impl;

/**
 *
 */
@Component(service = SessionContextRegistryImplFactory.class)
public class SessionContextRegistryImplFactoryImpl31 implements SessionContextRegistryImplFactory {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImplFactory#createSessionContextRegistryImpl(com.ibm.ws.webcontainer.httpsession.SessionManager)
     */
    @Override
    public SessionContextRegistryImpl createSessionContextRegistryImpl(SessionManager smgr) {
        return new SessionContextRegistry31Impl(smgr);
    }

}
