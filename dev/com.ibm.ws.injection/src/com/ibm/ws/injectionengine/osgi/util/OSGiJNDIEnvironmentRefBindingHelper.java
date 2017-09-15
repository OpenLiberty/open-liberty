/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.util;

import java.util.List;
import java.util.Map;

import com.ibm.ws.container.service.config.extended.RefBndAndExtHelper;
import com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

public class OSGiJNDIEnvironmentRefBindingHelper {

    /**
     * Process bindings and extensions for all JNDIEnvironmentRef. This method
     * can be called multiple times with different source objects.
     *
     * @param allBindings the map of all bindings to update
     * @param envEntryValues the env-entry value bindings map to update
     * @param resRefList the resource-ref binding and extension list to update
     * @param refBindingsGroup the source binding data
     * @param resRefExts the source extension data
     */
    public static void processBndAndExt(Map<JNDIEnvironmentRefType, Map<String, String>> allBindings,
                                        Map<String, String> envEntryValues,
                                        ResourceRefConfigList resRefList,
                                        RefBindingsGroup refBindingsGroup,
                                        List<com.ibm.ws.javaee.dd.commonext.ResourceRef> resRefExts) {
        RefBndAndExtHelper.configureEJBRefBindings(refBindingsGroup, allBindings.get(JNDIEnvironmentRefType.EJBRef));
        RefBndAndExtHelper.configureMessageDestinationRefBindings(refBindingsGroup, allBindings.get(JNDIEnvironmentRefType.MessageDestinationRef));
        RefBndAndExtHelper.configureResourceRefBindings(refBindingsGroup, allBindings.get(JNDIEnvironmentRefType.ResourceRef), resRefList);
        if (resRefExts != null) {
            RefBndAndExtHelper.configureResourceRefExtensions(resRefExts, resRefList);
        }
        RefBndAndExtHelper.configureResourceEnvRefBindings(refBindingsGroup, allBindings.get(JNDIEnvironmentRefType.ResourceEnvRef));
        RefBndAndExtHelper.configureEnvEntryBindings(refBindingsGroup, envEntryValues, allBindings.get(JNDIEnvironmentRefType.EnvEntry));
        RefBndAndExtHelper.configureDataSourceBindings(refBindingsGroup, allBindings.get(JNDIEnvironmentRefType.DataSource));
    }
}
