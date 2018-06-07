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

import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface TimeWeightedMXBean {

    public String getDescription();

    public String getUnit();

    public long getCount();

    public double getMean();

    public void update(long duration, TimeUnit unit);

    public Snapshot getSnapshot();

    public double getCurrent();

}
