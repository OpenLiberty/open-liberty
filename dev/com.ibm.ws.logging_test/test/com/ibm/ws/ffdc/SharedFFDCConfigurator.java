/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
/**
 *
 */
package com.ibm.ws.ffdc;

import java.lang.reflect.Field;
import java.util.Map;

import com.ibm.ws.logging.internal.impl.BaseFFDCService;
import com.ibm.wsspi.logprovider.FFDCFilterService;

public class SharedFFDCConfigurator extends FFDCConfigurator {

    static Field incidents = null;

    public static void clearDelegates() {
        FFDCConfigurator.delegate = null;

        FFDCConfigurator.loggingConfig.set(null);
    }

    public static void setDelegate(FFDCFilterService mockservice) {
        FFDCConfigurator.delegate = mockservice;
    }

    public static FFDCFilterService getDelegate() {
        return FFDCConfigurator.getDelegate();
    }

    public static void clearFFDCIncidents() throws Exception {
        if (incidents == null) {
            incidents = BaseFFDCService.class.getDeclaredField("incidents");
            incidents.setAccessible(true);
        }

        @SuppressWarnings("rawtypes")
        Map map = (Map) incidents.get(getDelegate());
        map.clear();
    }
}