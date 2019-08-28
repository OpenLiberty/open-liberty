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
package com.ibm.ws.jbatch.utility.http;

import java.io.IOException;
import java.io.InputStream;

public interface EntityReader<T> {

    /**
     * @return the entity read from the given entityStream.
     */
    public T readEntity(InputStream entityStream) throws IOException;

}
