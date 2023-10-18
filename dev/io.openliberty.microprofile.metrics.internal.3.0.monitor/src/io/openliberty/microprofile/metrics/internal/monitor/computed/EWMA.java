/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.metrics.internal.monitor.computed;

public class EWMA {
    private Double currVal;
    private double alpha;

    public EWMA(double alpha) {
        this.alpha = alpha;
    }

    public void updateNewValue(double value) {
        if (currVal != null) {
            currVal = alpha * value + (1 - alpha) * currVal;
        } else {
            currVal = value; // during first initialization
        }
    }

    public double getAveragedValue() {
        return currVal; // in seconds
    }
}
