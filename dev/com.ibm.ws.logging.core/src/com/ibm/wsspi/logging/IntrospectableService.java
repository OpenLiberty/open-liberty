/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.logging;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for introspect the framework.
 * Services implement this interface can provide the information to dump to a file.
 * 
 * @deprecated Use {@link Introspector} instead.
 */
@Deprecated
public interface IntrospectableService {

    /**
     * used as the file name of the service's dump file
     */
    public String getName();

    public String getDescription();

    /**
     * 
     * @param out, the dump file
     */
    public void introspect(OutputStream out) throws IOException;
}