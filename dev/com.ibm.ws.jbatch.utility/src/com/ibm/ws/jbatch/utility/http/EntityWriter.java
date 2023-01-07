/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
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
package com.ibm.ws.jbatch.utility.http;

import java.io.OutputStream;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonWriterFactory;

/**
 * 
 */
public interface EntityWriter {

    static final JsonWriterFactory writerFactory = Json.createWriterFactory(null);

    static final JsonBuilderFactory builderFactory = Json.createBuilderFactory(null);

    /**
     * Write the entity to the given entityStream.
     */
    public void writeEntity(OutputStream entityStream);

}
