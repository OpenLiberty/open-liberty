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
package com.ibm.ws.jsp.translator.optimizedtag;

public class OptimizedTagConfig {
    protected String tlibUri = null;
    protected String tlibversion = null;
    protected String shortName = null;
    protected Class optClass = null;
    
    public OptimizedTagConfig() {}
      
    public String getShortName() {
        return shortName;
    }

    public String getTlibUri() {
        return tlibUri;
    }

    public String getTlibversion() {
        return tlibversion;
    }

    public void setShortName(String string) {
        shortName = string;
    }

    public void setTlibUri(String string) {
        tlibUri = string;
    }

    public void setTlibversion(String string) {
        tlibversion = string;
    }
    
    public Class getOptClass() {
        return optClass;
    }

    public void setOptClass(Class class1) {
        optClass = class1;
    }
}
