/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.ejb.apps.scope;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * Singleton bean which checks whether request scope is active during postConstruct
 */
@Singleton
@Startup
public class PostConstructingStartupBean {

    @Inject
    RequestScopedBean bean;

    private boolean wasRequestScopeActive;

    /**
     * @return whether the request scope was active during postConstruct
     */
    public boolean getWasRequestScopeActive() {
        return wasRequestScopeActive;
    }

    @PostConstruct
    private void init() {
        try {
            bean.doNothing();
            wasRequestScopeActive = true;
        } catch (Throwable ex) {
            wasRequestScopeActive = false;
        }

    }

}
