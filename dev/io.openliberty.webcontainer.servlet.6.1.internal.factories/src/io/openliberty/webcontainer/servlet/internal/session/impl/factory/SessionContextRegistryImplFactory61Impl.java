/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet.internal.session.impl.factory;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.webcontainer.httpsession.SessionManager;
import com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImpl;
import com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImplFactory;

import io.openliberty.webcontainer60.session.impl.SessionContextRegistryImpl60;

/**
 *
 */
@Component(service = SessionContextRegistryImplFactory.class, property = { "service.vendor=IBM" })
public class SessionContextRegistryImplFactory61Impl implements SessionContextRegistryImplFactory {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImplFactory#createSessionContextRegistryImpl(com.ibm.ws.webcontainer.httpsession.SessionManager)
     */
    @Override
    public SessionContextRegistryImpl createSessionContextRegistryImpl(SessionManager smgr) {
        return new SessionContextRegistryImpl60(smgr);
    }

}
