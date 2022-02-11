/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common;

/**
 * Represents &lt;managed-executor&gt;.
 */
public interface ManagedExecutor extends JNDIContextServiceRef {
    /**
     * @return &lt;hung-task-threshold&gt; if specified
     * @see #isSetHungTaskThreshold
     */
    int getHungTaskThreshold();

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
}
