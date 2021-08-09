/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.taglib.config;

public class AvailabilityCondition {
    private AvailabilityConditionType type = null;
    private String value = null;
    
    public AvailabilityCondition(AvailabilityConditionType type, String value) {
        this.type = type;
        this.value = value;
    }
    
    public AvailabilityConditionType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

}
