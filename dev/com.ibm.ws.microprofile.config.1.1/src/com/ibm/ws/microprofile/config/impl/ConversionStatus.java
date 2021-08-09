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
package com.ibm.ws.microprofile.config.impl;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A holder to hold whether a converter is found and the converted value.
 */
@Trivial
public class ConversionStatus {
    private boolean converterFound = false;
    private Object converted = null;

    public ConversionStatus() {
        //no-op
    }

    public ConversionStatus(boolean foundConverter, Object converted) {
        this.converted = converted;
        this.converterFound = foundConverter;
    }

    public boolean isConverterFound() {
        return this.converterFound;
    }

    public Object getConverted() {
        return this.converted;
    }

    public void setConverted(Object converted) {
        this.converterFound = true;
        this.converted = converted;
    }

    @Override
    public String toString() {
        return "Converver Found=" + this.converterFound;
    }
}
