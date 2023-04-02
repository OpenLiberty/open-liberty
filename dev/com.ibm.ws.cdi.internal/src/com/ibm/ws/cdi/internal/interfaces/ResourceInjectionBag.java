/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.internal.interfaces;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefBindingHelper;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

public class ResourceInjectionBag {
    public final Map<JNDIEnvironmentRefType, Map<String, String>> allBindings = JNDIEnvironmentRefBindingHelper.createAllBindingsMap();
    public final Map<String, String> envEntryValues = new HashMap<String, String>();
    public final ResourceRefConfigList resourceRefConfigList;

    public ResourceInjectionBag(ResourceRefConfigList resourceRefConfigList) {
        this.resourceRefConfigList = resourceRefConfigList;
    }
}
