/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.monitor.jmx;

/**
 * Abstract base class that serves as the foundation for all meter
 * implementations.
 */
public abstract class Meter {

    /**
     * The unit of measurement for this meter.
     */
    String unit = "UNKNOWN";

    /**
     * A description of the recorded metric.
     */
    String description = null;

    public Meter() {}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

}
