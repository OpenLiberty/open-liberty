/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

/**
 * A bind-able scaling policy. See /com.ibm.ws.scaling.controller/resources/OSGI-INF/metatype/metatype.xml
 * 
 */
public class ScalingPolicy extends ConfigElement {

    // To be implemented

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(this.getClass().getSimpleName());
        buf.append("{");

        // To be implemented

        buf.append("}");
        return buf.toString();
    }

    @Override
    public ScalingPolicy clone() throws CloneNotSupportedException {
        ScalingPolicy clone = (ScalingPolicy) super.clone();

        // To be implemented

        return clone;
    }

}
