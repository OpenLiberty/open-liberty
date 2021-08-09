/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.basic.test;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.ws.microprofile.config.AbstractConfigSource;

public class BasicConfigSource extends AbstractConfigSource implements ConfigSource {

    public BasicConfigSource(String id) {
        this(100, id);
    }

    public BasicConfigSource(int ordinal, String id) {
        super(ordinal, id);
    }
}
