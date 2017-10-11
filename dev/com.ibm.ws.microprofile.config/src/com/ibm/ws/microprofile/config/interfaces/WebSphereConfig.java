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
package com.ibm.ws.microprofile.config.interfaces;

import java.io.Closeable;

import org.eclipse.microprofile.config.Config;

/**
 *
 */
public interface WebSphereConfig extends Config, Closeable {

    public <T> T convertValue(String rawValue, Class<T> type);

    public String dump();

    public <T> SourcedValue<T> getSourcedValue(String key, Class<T> type);

}
