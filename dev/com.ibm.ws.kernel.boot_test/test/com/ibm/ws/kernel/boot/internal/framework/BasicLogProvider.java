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
package com.ibm.ws.kernel.boot.internal.framework;

import java.io.File;
import java.util.Map;

import com.ibm.wsspi.logprovider.LogProvider;

/**
 *
 */
public class BasicLogProvider implements LogProvider {
    @Override
    public void configure(Map<String, String> config,
                          File logLocation,
                          com.ibm.wsspi.logging.TextFileOutputStreamFactory factory) {}

    @Override
    public void stop() {}
}
