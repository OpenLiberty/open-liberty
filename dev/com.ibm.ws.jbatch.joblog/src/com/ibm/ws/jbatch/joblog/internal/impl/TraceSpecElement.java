/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.joblog.internal.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a single trace spec element, e.g "com.ibm.ws.jbatch.*=all=enabled".
 */
public class TraceSpecElement {

    private final Logger logger;
    private final Level level;
    private final boolean isEnabled;

    /**
     * CTOR.
     */
    public TraceSpecElement(String spec) {

        if (StringUtils.isEmpty(spec)) {
            throw new IllegalArgumentException("spec cannot be empty");
        }

        // There may or may not be an '=' in the spec
        String[] sp = spec.split("=");

        logger = Logger.getLogger(StringUtils.trimSuffixes(sp[0], ".*", "*"));
        level = (sp.length >= 2) ? Level.parse(sp[1].toUpperCase()) : Level.ALL;
        isEnabled = (sp.length < 3 || StringUtils.isEmpty(sp[2])) ? true : sp[2].equalsIgnoreCase("enabled");
    }

    public Logger getLogger() {
        return logger;
    }

    public Level getLevel() {
        return level;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
