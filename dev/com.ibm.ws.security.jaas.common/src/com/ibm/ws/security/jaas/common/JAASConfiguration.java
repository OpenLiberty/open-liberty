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
package com.ibm.ws.security.jaas.common;

import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 *
 */
public interface JAASConfiguration {

    /**
     * Get all jaasLoginContextEntry in the server.xml and create any missing default entries.
     * If there is no jaas configuration, then create all the default entries:
     * On the server: system.DEFAULT,
     * system.WEB_INBOUND, system.DESERIALIZE_CONTEXT, system.UNAUTHENTICATED and WSLogin.
     * On the client: ClientContainer
     * 
     * @return list of the JAAS login context entries mapped to their names
     */
    public Map<String, List<AppConfigurationEntry>> getEntries();

    /**
     * 
     * @param jaasLoginContextEntries
     */
    public void setJaasLoginContextEntries(ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries);

}
