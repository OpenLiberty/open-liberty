/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.metrics30.setup.config;

public abstract class PropertyBooleanConfiguration extends PropertyConfiguration {
    protected boolean isEnabled = false;

    @Override
    public String toString() {
        return String.format(this.getClass().getName() + "metric name: [%s]; isEnabled: [%s]", metricName,
                             isEnabled);
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
