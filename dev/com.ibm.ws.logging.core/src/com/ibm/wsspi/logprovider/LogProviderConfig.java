/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.logprovider;

import java.io.File;
import java.util.Map;

import com.ibm.wsspi.logging.TextFileOutputStreamFactory;

/**
 * This provides a representation of the configured LogProvider to the
 * stable/statically-accessed elements of the logging system.
 */
public interface LogProviderConfig {

    /** @return the configured log directory */
    File getLogDirectory();

    /** @return the configured/active TrService delegate instance */
    TrService getTrDelegate();

    /** @return the configured/active FFDCService delegate instance */
    FFDCFilterService getFfdcDelegate();

    /** @return the Factory that should be used to create text file output streams */
    TextFileOutputStreamFactory getTextFileOutputStreamFactory();

    /** @return the configured/active trace string */
    String getTraceString();

    /** @return the configured maximum number of log files */
    int getMaxFiles();

    /**
     * This is how the logging system will push dynamically received
     * updates down to the configured/active log provider.
     * 
     * @param newConfig A Map of String keys to object values: values might
     *            not be strings, as interaction with config admin will pre-convert
     *            the value into an int or a boolean or..
     */
    void update(Map<String, Object> newConfig);
}
