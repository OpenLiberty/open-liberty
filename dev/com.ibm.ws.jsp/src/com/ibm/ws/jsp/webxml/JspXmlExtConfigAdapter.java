/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.webxml;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.jsp.configuration.JspXmlExtConfig;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class JspXmlExtConfigAdapter implements ContainerAdapter<JspXmlExtConfig> {

    @Override
    public JspXmlExtConfig adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        JspXmlExtConfig extConfig = (JspXmlExtConfig) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), JspXmlExtConfig.class);
        return extConfig;
    }

    protected void activate(ComponentContext context) {
        
    }
    protected void deactivate(ComponentContext context) {
        
    }
}
