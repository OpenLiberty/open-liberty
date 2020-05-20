/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.utils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class ModuleUtils {
    private static final TraceComponent tc = Tr.register(ModuleUtils.class);

    @FFDCIgnore(UnableToAdaptException.class)
    public static WebModuleInfo getWebModuleInfo(Container container) {
        WebModuleInfo moduleInfo = null;
        NonPersistentCache overlayCache;
        try {
            overlayCache = container.adapt(NonPersistentCache.class);
        } catch (UnableToAdaptException e) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to get web module info: " + e.getMessage());
            }
            return null;
        }
        if (overlayCache != null) {
            moduleInfo = (WebModuleInfo) overlayCache.getFromCache(WebModuleInfo.class);
        }
        return moduleInfo;
    }
}
