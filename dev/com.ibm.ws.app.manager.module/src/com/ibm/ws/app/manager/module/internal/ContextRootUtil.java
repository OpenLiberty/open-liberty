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
package com.ibm.ws.app.manager.module.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class ContextRootUtil {
    private static final TraceComponent tc = Tr.register(ContextRootUtil.class);

    public static String getContextRoot(String root) {
        if (root != null && !root.isEmpty()) {
            if (!root.startsWith("/")) {
                root = "/" + root;
            }
            return root;
        }
        return null;
    }

    public static String getContextRoot(Container webContainer) {
        if (webContainer != null) {
            try {
                WebExt webExt = webContainer.adapt(WebExt.class);
                if (webExt != null) {
                    String contextRoot = webExt.getContextRoot();
                    return getContextRoot(contextRoot);
                }
            } catch (UnableToAdaptException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                	Tr.debug(tc, "getContextRoot: Unable to parse the WebExt file", e);
                }
            }
        }
        return null;
    }
}
