/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.org.apache.felix.scr;

import org.osgi.service.component.ComponentContext;

public interface StaticComponentManager {

	ReturnValue activate(Object instance, ComponentContext componentContext);

    ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason);
    
    ReturnValue modified(Object instance, ComponentContext componentContext);

    ReturnValue bind(Object componentInstance, String name, Parameters parameters);

    ReturnValue unbind(Object componentInstance, String name, Parameters parameters);

    ReturnValue updated(Object componentInstance, String name, Parameters parameters);

    boolean init(Object instance, String name);
}