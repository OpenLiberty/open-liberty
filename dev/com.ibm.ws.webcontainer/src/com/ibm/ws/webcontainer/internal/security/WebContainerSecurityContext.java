/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.internal.security;

import com.ibm.ws.security.core.SecurityContext;
import com.ibm.ws.webcontainer.osgi.collaborator.CollaboratorHelperImpl;

public class WebContainerSecurityContext extends SecurityContext {
    
    // Determined by the presence of a registered ('real') security collaborator for the currently active web app
    public static boolean isSecurityEnabled() {
        return CollaboratorHelperImpl.getCurrentSecurityEnabled();
    }
}
