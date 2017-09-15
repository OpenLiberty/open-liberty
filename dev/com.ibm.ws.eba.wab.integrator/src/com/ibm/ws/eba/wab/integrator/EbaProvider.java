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
package com.ibm.ws.eba.wab.integrator;

import org.osgi.framework.Bundle;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;

/**
 * This interface will provide application info for a bundle.
 */
public interface EbaProvider {

    /**
     * Returns the application info for the application that contains the supplied bundle.
     * 
     * @param bundle The bundle to find the application info for
     * @return The application info for the bundle or <code>null</code> if there isn't an application for this bundle
     */
    public ApplicationInfo getApplicationInfo(Bundle bundle);

}
