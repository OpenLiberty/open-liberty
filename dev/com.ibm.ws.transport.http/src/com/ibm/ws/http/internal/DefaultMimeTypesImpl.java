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
package com.ibm.ws.http.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.http.DefaultMimeTypes;

/**
 * This stores the default (server-wide) mime types. Set/use/configure this
 * only once, rather than once per virtual host. Virtual host supports a nested
 * element for defining custom mime types that can override these general defaults.
 * 
 * See metatype-mimetype.xml and metatype-mimetype.properties in
 * resources/OSGI-INF for the default types and extensions
 */
@Component(configurationPid = "com.ibm.ws.http.mimetype", immediate = true, property = { "service.vendor=IBM" })
public class DefaultMimeTypesImpl implements DefaultMimeTypes {
    private static final TraceComponent tc = Tr.register(DefaultMimeTypesImpl.class);
    private static final String CFG_KEY_DEFAULT_TYPES = "defaultType";
    private static final String CFG_KEY_MIME_TYPES = "type";

    private volatile Map<String, String> extToType = Collections.emptyMap();

    @Trivial
    @Activate
    protected void activate(Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating");
        }
        modified(properties);
    }

    @Trivial
    @Deactivate
    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating, reason=" + reason);
        }
    }

    @Trivial
    @Modified
    protected void modified(Map<String, Object> properties) {
        Map<String, String> extensionMap = new HashMap<String, String>();

        // Defaults
        String[] mimeTypeList = (String[]) properties.get(CFG_KEY_DEFAULT_TYPES);
        processList(mimeTypeList, extensionMap);

        // Customizations
        mimeTypeList = (String[]) properties.get(CFG_KEY_MIME_TYPES);
        processList(mimeTypeList, extensionMap);

        // Volatile map... the whole map is replaced at once.
        extToType = extensionMap;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Default mime configured", properties, extToType);
        }

    }

    @Trivial
    private void processList(final String[] mimeTypeList, final Map<String, String> extensionMap) {
        if (mimeTypeList != null) {
            for (String mimeType : mimeTypeList) {
                int equalIndex = mimeType.indexOf('=');
                if (equalIndex != -1 && mimeType.length() > equalIndex + 1) {
                    String ext = mimeType.substring(0, equalIndex);
                    String type = mimeType.substring(equalIndex + 1);
                    extensionMap.put(ext, type);
                }
            }
        }
    }

    @Override
    public String getType(String extension) {
        return extToType.get(extension);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[" + extToType + "]";
    }
}
