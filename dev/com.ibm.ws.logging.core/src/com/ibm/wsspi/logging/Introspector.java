/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.logging;

import java.io.PrintWriter;

/**
 * A service that can be notified when the {@code server dump} command is used.
 */
public interface Introspector {
    /**
     * The name of the introspector, which is used for the introspection file
     * name. Names should follow the naming convention for a Java class and
     * typically end with {@code Introspector}; for example, {@code TestComponentIntrospector}.
     */
    String getIntrospectorName();

    /**
     * A description of the introspector, which is added to the introspection file.
     */
    String getIntrospectorDescription();

    /**
     * Performs the introspection. Implementations should be robust, but for
     * convenience, this method allows any exception to be thrown.
     */
    void introspect(PrintWriter out) throws Exception;
}
