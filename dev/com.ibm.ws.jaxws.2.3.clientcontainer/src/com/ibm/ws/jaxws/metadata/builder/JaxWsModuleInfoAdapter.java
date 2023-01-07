/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxws.metadata.builder;

import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class JaxWsModuleInfoAdapter implements ContainerAdapter<JaxWsModuleInfo> {

    /** {@inheritDoc} */
    @Override
    public JaxWsModuleInfo adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        NonPersistentCache overlayCache = containerToAdapt.adapt(NonPersistentCache.class);

        JaxWsModuleInfo jaxWsModuleInfo = (JaxWsModuleInfo) overlayCache.getFromCache(JaxWsModuleInfo.class);

        return jaxWsModuleInfo;
    }
}
