/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.monitor.meters;

/**
 *
 */
public interface Histogram {
    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    public void update(int value);

    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    public void update(long value);

    /**
     * Returns the number of values recorded.
     *
     * @return the number of values recorded
     */

    public long getCount();

    public Snapshot getSnapshot();
}
