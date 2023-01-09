/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.fat.util;

import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Provides a java.util.logging.StreamHandler extension that outputs to sys out instead of sys err.
 * The following properties can be set in logging.properties to configure this Handler:<br>
 * <br>
 * <ul>
 * <li><code>com.ibm.ws.fat.util.GenericHandler.level</code> - specifies the default level for the Handler (defaults to Level.INFO).</li>
 * <li><code>com.ibm.ws.fat.util.GenericHandler.formatter</code> - specifies the name of a Formatter class to use (defaults to com.ibm.ws.fvt.logging.GenericFormatter).</li>
 * <li><code>com.ibm.ws.fat.util.GenericHandler.stream</code> - specifies the output stream to use (valid values are: System.err and System.out. defaults to System.out).</li>
 * <li><code>com.ibm.ws.fat.util.GenericHandler.flush</code> - controls whether to flush the wrapped OutputStream whenever a LogRecord is published (defaults to false).</li>
 * </ul>
 *
 * @author Tim Burns
 *
 */
public class GenericHandler extends StreamHandler {

    protected static final String PROP_FORMATTER = GenericHandler.class.getName() + ".formatter";
    protected static final String PROP_STREAM = GenericHandler.class.getName() + ".stream";
    protected static final String PROP_FLUSH = GenericHandler.class.getName() + ".flush";
    protected static final String SYSTEM_ERR = "system.err";

    protected boolean flush;

    /**
     * The primary constructor sets state based on JVM system properties.
     */
    public GenericHandler() {
        super();
        LogManager manager = LogManager.getLogManager();
        this.flush = new Boolean(manager.getProperty(PROP_FLUSH)).booleanValue();
        String formatter = manager.getProperty(PROP_FORMATTER);
        Formatter formatterInstance;
        try {
            formatterInstance = (Formatter) Class.forName(formatter).newInstance();
        } catch (Exception e) {
            formatterInstance = new GenericFormatter();
        }
        this.setFormatter(formatterInstance);
        String stream = manager.getProperty(PROP_STREAM);
        if (stream != null && SYSTEM_ERR.equals(stream.toLowerCase())) {
            this.setOutputStream(System.err);
        } else {
            this.setOutputStream(System.out);
        }
    }

    @Override
    public void publish(LogRecord record) {
        super.publish(record);
        if (this.flush) {
            this.flush();
        }
    }

}
