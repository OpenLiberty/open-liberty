/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.injection;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public interface InjectionProvider12 {

    public <T> T getManagedEndpointInstance(Class<T> endpointClass, ConcurrentHashMap map) throws InstantiationException;

    public void releaseCC(Object key, ConcurrentHashMap map);

}
