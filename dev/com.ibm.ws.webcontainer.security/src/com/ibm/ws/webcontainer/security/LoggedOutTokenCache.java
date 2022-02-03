/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

/**
 * Interface methods to create, add, and get logged out tokens from the LoggedOutTokenMap DistributedMap
 */
public interface LoggedOutTokenCache {

    public Object getDistributedObjectLoggedOutToken(Object key);

    public Object putDistributedObjectLoggedOutToken(Object key, Object value, int timeToLive);

    public Object addTokenToDistributedMap(Object key, Object value);

}
