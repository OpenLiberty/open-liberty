/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tck;

import java.util.logging.Logger;

import org.testng.reporters.VerboseReporter;

/**
 * TestNG reporter that logs using java.util.logging
 */
public class LoggingReporter extends VerboseReporter {

    private static final Logger LOGGER = Logger.getLogger(LoggingReporter.class.getName());

    public LoggingReporter() {
        super(""); // No message prefix
    }

    @Override
    protected void log(String message) {
        LOGGER.info(message);
    }

}
