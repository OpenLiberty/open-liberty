/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import javax.naming.Reference;

import com.ibm.ws.injectionengine.osgi.util.Link;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.wsspi.injectionengine.InjectionEngineAccessor;
import com.ibm.wsspi.injectionengine.factory.ResAutoLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResourceInfo;

public class ResAutoLinkReferenceFactoryImpl implements ResAutoLinkReferenceFactory {
    private final ResRefReferenceFactoryImpl resRefReferenceFactory;

    ResAutoLinkReferenceFactoryImpl(ResRefReferenceFactoryImpl resRefReferenceFactory) {
        this.resRefReferenceFactory = resRefReferenceFactory;
    }

    @Override
    public Reference createResAutoLinkReference(ResourceInfo resourceInfo) {
        ResourceRefConfig resourceRefConfig = resourceInfo.getConfig();
        resourceRefConfig.setJNDIName(getBindingName(resourceInfo));
        return resRefReferenceFactory.createResRefLookupReference(resourceInfo.getName(), resourceRefConfig, true);
    }

    /**
     * When a message-destination-link is available, look up the binding from
     * message destination link map. If that binding is not available, default
     * to the link name.
     */
    private String getBindingName(ResourceInfo resourceInfo) {
        String destBindingName = null;

        String link = resourceInfo.getLink();
        if (link == null) {
            destBindingName = getBindingName(resourceInfo.getName());
        } else {
            destBindingName = InjectionEngineAccessor.getMessageDestinationLinkInstance().
                            findDestinationByName(resourceInfo.getApplication(),
                                                  resourceInfo.getModule(),
                                                  link);
            // if the link name is not found in the map, default the binding to
            // the link name, but strip off a module name if there is one.
            if (destBindingName == null) {
                Link mdLink = Link.parseMessageDestinationLink(resourceInfo.getModule(), link);
                destBindingName = mdLink.name;
            }
        }
        return destBindingName;
    }

    /**
     * Determine the binding name that will be used for a reference name. This
     * implements basically the same algorithm as "default bindings" in traditional WAS.
     */
    public static String getBindingName(String name) {
        // For "java:" names, come up with a reasonable binding name by removing
        // "java:" and if possible, the scope (e.g., "global") and "env".
        if (name.startsWith("java:")) {
            int begin = "java:".length();

            int index = name.indexOf('/', begin);
            if (index != -1) {
                begin = index + 1;
            }

            if (begin + "env/".length() <= name.length() && name.regionMatches(begin, "env/", 0, "env/".length())) {
                begin += "env/".length();
            }

            return name.substring(begin);
        }

        return name;
    }
}
