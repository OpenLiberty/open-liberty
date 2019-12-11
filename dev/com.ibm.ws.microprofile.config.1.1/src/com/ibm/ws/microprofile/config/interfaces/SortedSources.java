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
package com.ibm.ws.microprofile.config.interfaces;

import java.util.Collection;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Contains a set of ConfigSources, sorted by ordinal, highest value first
 */
public interface SortedSources extends Collection<ConfigSource> {

    /**
     * @return
     */
    SortedSources unmodifiable();
}
