/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.ResourceBundle;

import test.LoggingTestUtils;

public class SharedTraceNLSResolver extends TraceNLSResolver {
    static {
        LoggingTestUtils.ensureLogManager();
    }

    private static Field instanceField = null;

    protected static Field getInstanceField() throws Exception {
        if (instanceField == null) {
            Class<?> inner[] = TraceNLSResolver.class.getDeclaredClasses();
            for (Class<?> c : inner) {
                if (c.getSimpleName().equals("ResolverSingleton")) {
                    instanceField = c.getDeclaredField("instance");
                    instanceField.setAccessible(true);
                    break;
                }
            }

            if (instanceField == null)
                throw new IllegalStateException("Could not find TraceNLSResolver.ResolverSingleton.instance field");
        }

        return instanceField;
    }

    public static void setInstance(boolean shared) throws Exception {
        Field f = getInstanceField();

        if (shared)
            f.set(null, new SharedTraceNLSResolver());
        else
            f.set(null, new TraceNLSResolver());
    }

    public static void beLoud() throws Exception {
        TraceNLSResolver instance = (TraceNLSResolver) getInstanceField().get(null);
        instance.makeNoise = true;
    }

    public static void beQuiet() throws Exception {
        TraceNLSResolver instance = (TraceNLSResolver) getInstanceField().get(null);
        instance.makeNoise = false;
    }

    @Override
    public String getMessage(Class<?> aClass, ResourceBundle bundle, String bundleName, String key, Object[] args, String defaultString, boolean format, Locale locale,
                             boolean quiet) {
        StringBuffer str = new StringBuffer();

        // Construct a return message based on the parms filled in
        str.append(aClass == null ? "null" : "class").append(", ");
        str.append(bundle == null ? "null" : "bundle").append(", ");
        str.append(bundleName == null ? "null" : "bundleName").append(", ");
        str.append(key == null ? "null" : "key").append(", ");
        str.append(args == null ? "null" : "args").append(", ");
        str.append(defaultString == null ? "null" : "defaultString").append(", ");
        str.append(format ? "format" : "false").append(", ");
        str.append(locale == null ? "null" : "locale").append(", ");
        str.append(quiet ? "quiet" : "false");

        return str.toString();
    }

    @Override
    public String getFormattedMessage(String message, Object[] args) {
        StringBuffer str = new StringBuffer();

        str.append(message == null ? "null" : "message").append(", ");
        str.append(args == null ? "null" : "args");

        return str.toString();
    }
}
