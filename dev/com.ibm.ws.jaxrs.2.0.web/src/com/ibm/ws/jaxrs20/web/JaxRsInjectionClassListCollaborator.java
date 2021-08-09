/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
//import com.ibm.ws.jaxrs.metadata.HandlerInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleInfo;
import com.ibm.ws.jaxrs20.utils.JaxRsUtils;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInjectionClassListCollaborator;

/**
 * REVIEW: Grant - what this class works for? Shall we just remove that?
 */
@Component(name = "com.ibm.ws.jaxrs20.web.JaxRsInjectionClassListCollaborator",
           service = WebAppInjectionClassListCollaborator.class,
           immediate = true,
           property = { "service.vendor=IBM" })
public class JaxRsInjectionClassListCollaborator implements WebAppInjectionClassListCollaborator {
    private final static TraceComponent tc = Tr.register(JaxRsInjectionClassListCollaborator.class);

    /** {@inheritDoc} */
    @Override
    public List<String> getInjectionClasses(Container moduleContainer) {

        try {
            if (!JaxRsUtils.isWebModule(moduleContainer)) {
                return Collections.<String> emptyList();
            }
            return InjectionUtils.getJaxRsInjectionClasses(moduleContainer);
        } catch (UnableToAdaptException e) {
            return Collections.<String> emptyList();
        }

    }

    private List<String> getAllHandlerClassNames(JaxRsModuleInfo jaxRsModuleInfo) {
        List<String> handlerClassNames = new ArrayList<String>();

        for (EndpointInfo edpInfo : jaxRsModuleInfo.getEndpointInfos()) {
//            for (HandlerInfo hInfo : edpInfo.getHandlerChainsInfo().getAllHandlerInfos()) {
//                handlerClassNames.add(hInfo.getHandlerClass());
//            }
        }
        return handlerClassNames;
    }

}
