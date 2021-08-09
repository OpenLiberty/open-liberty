/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.security.internal;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.container.service.security.SecurityRoles;
import com.ibm.ws.javaee.dd.appbnd.ApplicationBnd;
import com.ibm.ws.javaee.dd.appbnd.SecurityRole;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 *
 */
public class SecurityRolesAdapter implements ContainerAdapter<SecurityRoles> {

    @Override
    public SecurityRoles adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {

        ApplicationBnd appBnd = containerToAdapt.adapt(ApplicationBnd.class);

        List<SecurityRole> roles;
        if (appBnd == null)
            roles = Collections.emptyList();
        else
            roles = appBnd.getSecurityRoles();

        return new SecurityRolesImpl(roles);
    }

}
