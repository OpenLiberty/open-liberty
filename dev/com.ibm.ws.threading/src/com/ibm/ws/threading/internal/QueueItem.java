/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

/**
 * Interface that can be optionally implemented by queue items to supply additional information,
 * such as whether or not to expedite the item (insert it near the front instead of at the tail).
 */
public interface QueueItem {
    /**
     * Indicates whether the item should be expedited as a priority item vs enqueued to the tail.
     *
     * @return true if the item should be expedited, otherwise false.
     */
    boolean isExpedited();
}