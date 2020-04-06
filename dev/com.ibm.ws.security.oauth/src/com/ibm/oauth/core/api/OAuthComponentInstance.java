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
package com.ibm.oauth.core.api;

import com.ibm.oauth.core.api.oauth20.OAuth20Component;

/**
 * Primary entry point for the OAuth service provider component. It is possible to create
 * multiple OAuth service provider component instances. Each OAuth service provider
 * component is represented by one unique instance of OAuthComponentInstance, and each
 * OAuthComponentInstance has one unique Id.
 * 
 * 
 */
public interface OAuthComponentInstance {

    /**
     * Returns a unique instanceId for this instance of the OAuth service provider 
     * component. This can be used to uniquely identify one component
     * in multiple OAuth service provider component environment.
     */
    public String getInstanceId();

    /**
     * Returns an OAuth20 service provider component interface
     */
    public OAuth20Component getOAuth20Component();
}
