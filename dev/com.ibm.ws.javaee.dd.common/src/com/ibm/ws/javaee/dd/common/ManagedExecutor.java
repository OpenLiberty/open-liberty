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
 * Represents &lt;managed-executor&gt;.
 */
public interface ManagedExecutor extends JNDIEnvironmentRef, Describable {
    /**
     * @return &lt;context-service-ref&gt;, or null if unspecified
     */
    String getContextServiceRef();

    /**
     * @return &lt;hung-task-threshold&gt; if specified
     * @see #isSetHungTaskThreshold
     */
    long getHungTaskThreshold();

    /**
     * @return true if &lt;hung-task-threshold&gt; is specified
     * @see #getHungTaskThreshold
     */
    boolean isSetHungTaskThreshold();

    /**
     * @return &lt;max-async&gt; if specified
     * @see #isSetMaxAsync
     */
    int getMaxAsync();

    /**
     * @return true if &lt;max-async&gt; is specified
     * @see #getMaxAsync
     */
    boolean isSetMaxAsync();

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
