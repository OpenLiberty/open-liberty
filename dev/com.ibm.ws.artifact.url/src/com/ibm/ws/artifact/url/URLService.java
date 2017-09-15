/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.url;

import java.net.URL;

import org.osgi.framework.Bundle;

/**
 * Service for converting urls from a Bundle into a form that can survive round tripping via String
 * across frameworks.
 */
public interface URLService {
    /**
     * Convert a URL from the owningBundle into a form that can be round tripped via String across frameworks.
     * 
     * @param urlToConvert the url that should be converted
     * @param owningBundle the bundle the url came from (used to manage the lifecycle of the returned url)
     * @return new URL that can be safely used across frameworks.
     * @throws IllegalStateException if owningBundle is uninstalled.
     */
    public URL convertURL(URL urlToConvert, Bundle owningBundle) throws IllegalStateException;
}
