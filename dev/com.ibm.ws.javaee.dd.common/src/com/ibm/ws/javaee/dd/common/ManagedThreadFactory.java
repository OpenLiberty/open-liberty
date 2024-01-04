/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package com.ibm.ws.javaee.dd.common;

import java.util.List;

/**
 * Represents &lt;managed-thread-factory&gt;.
 */
public interface ManagedThreadFactory extends JNDIEnvironmentRef, Describable {
    /**
     * @return &lt;context-service-ref&gt;, or null if unspecified
     */
    String getContextServiceRef();

    /**
     * @return &lt;priority&gt; if specified
     * @see #isSetPriority
     */
    int getPriority();

    /**
     * @return true if &lt;priority&gt; is specified
     * @see #getPriority
     */
    boolean isSetPriority();

    /**
     * @return &lt;virtual&gt; if specified
     * @see #isSetVirutal
     */
    boolean isVirtual();

    /**
     * @return true if &lt;virtual&gt; is specified
     * @see #isVirtual
     */
    boolean isSetVirtual();

    /**
     * @return &lt;qualifier&gt; elements as a read-only list
     */
    String[] getQualifiers();

    /**
     * @return &lt;property&gt; elements as a read-only list
     */
    List<Property> getProperties();
}
