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
package com.ibm.ws.jaxws.metadata.builder;

import com.ibm.ws.javaee.ddmodel.wsbnd.HttpPublishing;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebservicesBnd;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleType;
import com.ibm.ws.jaxws.utils.URLUtils;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * get the context root from custom binding file ibm-ws-bnd.xml for EJB based Web services.
 */
public class EJBContextRootJaxWsModuleInfoBuilderExtension extends AbstractJaxWsModuleInfoBuilderExtension {

    public EJBContextRootJaxWsModuleInfoBuilderExtension() {
        super(JaxWsModuleType.EJB);
    }

    @Override
    public void preBuild(JaxWsModuleInfoBuilderContext jaxWsModuleInfoBuilderContext, JaxWsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException {}

    @Override
    public void postBuild(JaxWsModuleInfoBuilderContext jaxWsModuleInfoBuilderContext, JaxWsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException {
        WebservicesBnd webservicesBnd = jaxWsModuleInfoBuilderContext.getContainer().adapt(WebservicesBnd.class);
        if (webservicesBnd == null) {
            return;
        }

        // get context root for EJB services
        HttpPublishing httpPublishing = webservicesBnd.getHttpPublishing();
        if (httpPublishing != null) {
            String contextRoot = httpPublishing.getContextRoot();
            contextRoot = URLUtils.normalizePath(contextRoot);
            if (contextRoot != null && !contextRoot.isEmpty()) {
                jaxWsModuleInfo.setContextRoot(contextRoot);
            }
        }

    }

}
