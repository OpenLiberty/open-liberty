/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.sources;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

import io.openliberty.microprofile.config.internal.common.InternalConfigSource;

/**
 *
 */
public class EnvConfig14Source extends InternalConfigSource {

    private static final TraceComponent tc = Tr.register(EnvConfig14Source.class);

    private static Pattern p = null;

    private final String name;

    /**
     * The environment. This is unmodifiable and can be returned to the user.
     */
    private static final Map<String, String> env;

    static {
        // Retrieve a reference to the environment once at startup in a privileged action
        env = AccessController.doPrivileged(new PrivilegedAction<Map<String, String>>() {
            @Override
            public Map<String, String> run() {
                return System.getenv();
            }
        });

        p = Pattern.compile(ConfigConstants.CONFIG13_ALLOWABLE_CHARS_IN_ENV_VAR_SOURCE);
    }

    @Trivial
    public EnvConfig14Source() {
        name = Tr.formatMessage(tc, "environment.variables.config.source");
    }

    @Override
    @Trivial
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    protected int getDefaultOrdinal() {
        return ConfigConstants.ORDINAL_ENVIRONMENT_VARIABLES;
    }

    @Override
    @Trivial
    public Map<String, String> getProperties() {
        return env;
    }

    @Override
    public String getValue(String key) {
        String nameToTry = key;
        String result = env.get(nameToTry);

        if (result == null) {
            nameToTry = replaceNonAlpha(nameToTry);
            result = env.get(nameToTry);
        }

        if (result == null) {
            nameToTry = nameToTry.toUpperCase();
            result = env.get(nameToTry);
        }

        return result;
    }

    /**
     * Replace non-alphanumeric characters in a string with underscores.
     *
     * @param name
     * @return modified name
     */
    private String replaceNonAlpha(String name) {
        String modifiedName = null;
        if (p != null)
            modifiedName = p.matcher(name).replaceAll("_");
        return modifiedName;
    }

    @Override
    public String toString() {
        return "Environment Variables Config Source";
    }

}
