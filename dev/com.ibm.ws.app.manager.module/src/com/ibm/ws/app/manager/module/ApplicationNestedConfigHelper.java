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
package com.ibm.ws.app.manager.module;

import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.wsspi.application.handler.ApplicationInformation;

/**
 * Implementation of {@link NestedConfigHelper} that gets it's properties from a {@link ApplicationInformation} instance.
 */
public class ApplicationNestedConfigHelper implements NestedConfigHelper {

    private final ApplicationInformation<?> appInfo;

    /**
     * @param appInfo
     */
    public ApplicationNestedConfigHelper(ApplicationInformation<?> appInfo) {
        super();
        this.appInfo = appInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.app.deploy.NestedConfigHelper#get(java.lang.String)
     */
    @Override
    public Object get(String propName) {
        return appInfo.getConfigProperty(propName);
    }

}
