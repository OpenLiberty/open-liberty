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

import javax.enterprise.inject.Default;
import javax.inject.Singleton;

@Default
@Singleton
public class LoggingServiceImpl implements LoggingService {
    public final static String CLASS_NAME = WidgetEntityListener.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private List<String> _messages = new ArrayList<String>();

    @Override
    public synchronized void log(String s) {
        svLogger.logp(Level.INFO, CLASS_NAME, "log", s);
        _messages.add(s);
    }

    @Override
    public synchronized List<String> getAndClearMessages() {
        List<String> messages = _messages;
        _messages = new ArrayList<String>();
        return messages;
    }
}
