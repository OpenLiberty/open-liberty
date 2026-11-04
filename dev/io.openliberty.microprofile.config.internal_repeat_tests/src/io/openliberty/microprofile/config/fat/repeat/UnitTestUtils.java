/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.microprofile.config.fat.repeat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;

import com.ibm.ws.kernel.service.util.ServiceCaller;

import io.openliberty.microprofile.config.internal.serverxml.OSGiConfigUtils;

/**
 * Assorted utility methods for the package io.openliberty.microprofile.config.internal.serverxml
 */
public class UnitTestUtils {

    public static void mockOSGiCallers() throws Exception {
        setOSGiCallers(true);
    }

    public static void restoreOSGiCallers() throws Exception {
        setOSGiCallers(false);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void setOSGiCallers(boolean setToMockClass) throws Exception {
        Class<OSGiConfigUtils> clazzToModify = OSGiConfigUtils.class;

        for (Field field : clazzToModify.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == ServiceCaller.class) {
                field.setAccessible(true);
                if (setToMockClass) {
                    field.set(null, new TestServiceCaller());
                } else {
                    ParameterizedType fType = (ParameterizedType) field.getGenericType();
                    Class<?> serviceType = (Class<?>) fType.getActualTypeArguments()[0];
                    field.set(null, new ServiceCaller(clazzToModify, serviceType));
                }
            }
        }
    }
}
