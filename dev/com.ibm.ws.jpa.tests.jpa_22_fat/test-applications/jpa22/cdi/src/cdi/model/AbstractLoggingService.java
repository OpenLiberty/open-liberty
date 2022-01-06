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

package cdi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractLoggingService {
    private String clsName = null;
    private Logger svLogger = null;
    private List<String> _messages = new ArrayList<String>();

    protected AbstractLoggingService(Logger logger, String className) {
        clsName = className;
        svLogger = logger;
    }

    public synchronized void log(String s) {
        svLogger.logp(Level.INFO, clsName, "log", s);
        _messages.add(s);
    }

    public synchronized List<String> getAndClearMessages() {
        List<String> messages = _messages;
        _messages = new ArrayList<String>();
        return messages;
    }
}
