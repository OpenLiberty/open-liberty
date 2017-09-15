/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class LoggerHandlerManager {
    private static final Logger rootLogger = Logger.getLogger("");
    private static Handler console;
    private static Handler singleton;

    /**
     * Called by LogManager.readConfiguration instead of reading
     * logging.properties.
     */
    public static synchronized void initialize() {
        if (console == null) {
            console = new ConsoleHandler();
            updateSingleton(console);
        }
    }

    private static synchronized void updateSingleton(Handler newHandler) {
        Handler oldHandler = singleton;
        if (newHandler != oldHandler) {
            // There is no way to atomically replace a handler on a logger.  We add
            // the new handler before removing the old handler so that the worst
            // case is a duplicate message rather than a lost message.

            if (newHandler != null) {
                rootLogger.addHandler(newHandler);
            }

            singleton = newHandler;

            if (oldHandler != null) {
                rootLogger.removeHandler(oldHandler);
            }
        }
    }

    public static synchronized Handler getSingleton() {
        return singleton != console ? singleton : null;
    }

    public static synchronized void setSingleton(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException();
        }
        updateSingleton(handler);
    }

    public static synchronized void unsetSingleton() {
        updateSingleton(console);
    }
}