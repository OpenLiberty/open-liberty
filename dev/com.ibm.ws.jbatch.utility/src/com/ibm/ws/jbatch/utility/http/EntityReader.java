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

import java.io.IOException;
import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonReaderFactory;

public interface EntityReader<T> {

    static final JsonReaderFactory readerFactory = Json.createReaderFactory(null);

    /**
     * @return the entity read from the given entityStream.
     */
    public T readEntity(InputStream entityStream) throws IOException;

}
