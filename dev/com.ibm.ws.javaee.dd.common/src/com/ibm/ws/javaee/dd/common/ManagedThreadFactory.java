/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common;

import java.util.List;

/**
 * Represents &lt;managed-thread-factory>.
 */
public interface ManagedThreadFactory extends JNDIEnvironmentRef, Describable {
    /**
     * @return &lt;context-service-ref>, or null if unspecified
     */
    String getContextServiceRef();

    /**
     * @return &lt;priority> if specified
     * @see #isSetPriority
     */
    int getPriority();

    /**
     * @return &lt;property> elements as a read-only list
     */
    List<Property> getProperties();

    /**
     * @return true if &lt;priority> is specified
     * @see #getPriority
     */
    boolean isSetPriority();
}
