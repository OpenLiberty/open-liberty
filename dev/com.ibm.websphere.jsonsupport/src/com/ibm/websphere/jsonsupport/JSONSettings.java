/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.jsonsupport;

/**
 *
 */
public class JSONSettings {
    public enum Include {
        ALWAYS,
        NON_NULL,
    }

    private Include inclusion;

    public JSONSettings() {
        inclusion = Include.ALWAYS;
    }

    public JSONSettings(Include inclusion) {
        this.inclusion = inclusion;
    }

    public Include getInclusion() {
        return this.inclusion;
    }

    public void setInclusion(Include inclusion) {
        this.inclusion = inclusion;
    }
}
