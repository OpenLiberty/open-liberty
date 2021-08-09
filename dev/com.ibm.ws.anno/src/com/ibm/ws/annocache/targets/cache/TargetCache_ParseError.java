/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.cache;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TargetCache_ParseError {
    private static final String CLASS_NAME = "TargetCache_ParseError";

    //

    public static final String NULL_MSG_ID = null;

    public final String msgId;
    public final String msgText;

    public final String path;
    public final int lineNo;
    public final String line;

    public final String name;
    public final String value;

    public TargetCache_ParseError(String msgId, String msgText,
                                  String path, int lineNo, String line) {

        this(msgId, msgText,
             path, lineNo, line,
             null, null);
    }

    public TargetCache_ParseError(String msgId, String msgText,
                                  String path, int lineNo, String line,
                                  String name, String value) {

        this.msgId = msgId;
        this.msgText = msgText;

        this.path = path;
        this.lineNo = lineNo;
        this.line = line;

        this.name = name;
        this.value = value;
    }

    public void emit(Logger logger) {
        String methodName = "emit";

        if ( !logger.isLoggable(Level.FINER) ) {
            return;
        }

        logger.logp(Level.WARNING, CLASS_NAME, methodName,
                "Parse Error: [ {0} ] at [ {1} ]: {2}",
                new Object[] { path, Integer.valueOf(lineNo), msgText });

        logger.logp(Level.WARNING, CLASS_NAME, methodName,
                "Line [ {0} ] Name [ {1} ] Value [ {2} ]",
                new Object[] { line, name, value });
    }
}
