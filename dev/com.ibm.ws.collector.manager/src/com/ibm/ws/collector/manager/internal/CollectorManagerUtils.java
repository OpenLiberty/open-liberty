/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.collector.manager.internal;

import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.collector.manager.Source;

/**
 * Utility methods for collector manager framework
 */
public class CollectorManagerUtils {

    public static final String NAME = "name";
    public static final String LOCATION = "location";

    public static String getSourceId(Source source) {
        return source.getSourceName() + "|" + source.getLocation();
    }

    public static String getHandlerId(Handler handler) {
        return handler.getHandlerName();
    }
}
