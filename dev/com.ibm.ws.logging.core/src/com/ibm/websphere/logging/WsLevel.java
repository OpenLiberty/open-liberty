/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.logging;

import java.util.logging.Level;

/**
 * The WsLevel class extends the java.util.logging.Level class, which defines a
 * set of standard logging levels that can be used to control logging output. We
 * extend that class to add WebSphere-specific levels, since the base set of
 * levels are inadequate.
 * 
 * @ibm-api
 */

public class WsLevel extends java.util.logging.Level {
    private static final long serialVersionUID = -8795434113718441359L;

    /**
     * FATAL is higher than SEVERE.
     */
    public static final Level FATAL = new WsLevel("FATAL", 1100);

    /**
     * ERROR is the same as SEVERE.
     */
    public static final Level ERROR = new WsLevel("ERROR", Level.SEVERE.intValue());

    /**
     * AUDIT is in between INFO and WARNING
     */
    public static final Level AUDIT = new WsLevel("AUDIT", 850);

    /**
     * EVENT is the same as FINE
     */
    public static final Level EVENT = new WsLevel("EVENT", Level.FINE.intValue());

    /**
     * DETAIL is after CONFIG.
     */
    public static final Level DETAIL = new WsLevel("DETAIL", 625);

    /**
     * Constructor.
     * 
     * @param name
     * @param value
     */
    WsLevel(String name, int value) {
        super(name, value, "com.ibm.ws.logging.internal.resources.LoggingMessages");
    }

    /**
     * This parse method is based on the Level.parse(String) method with the addition of
     * support for WebSphere type levels such as FATAL, AUDIT, and DETAIL.
     * 
     * @param name name of a WebSphere Level or java.util.logging Level to be parsed
     * @return Level Level object that matches the name parameter passed to this method
     */
    public static Level parse(String name) throws IllegalArgumentException, NullPointerException {
        if (name == null)
            throw new NullPointerException();

        name = name.toUpperCase();
        if (name.equals(FATAL.getName())) {
            return FATAL;
        }
        else if (name.equals(ERROR.getName())) {
            return ERROR;
        }
        else if (name.equals(AUDIT.getName())) {
            return AUDIT;
        }
        else if (name.equals(EVENT.getName())) {
            return EVENT;
        }
        else if (name.equals(DETAIL.getName())) {
            return DETAIL;
        }

        return Level.parse(name);
    }

}
