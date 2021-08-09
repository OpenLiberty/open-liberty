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
package com.ibm.ws.jsp.translator.optimizedtag.impl;

import com.ibm.ws.jsp.translator.optimizedtag.OptimizedTag;
import com.ibm.ws.jsp.translator.optimizedtag.OptimizedTagContext;

public class JSTLChooseOptimizedTag implements OptimizedTag {
    private boolean firstWhenSpecified = false;
    
    public boolean doOptimization(OptimizedTagContext context) {
        return true;
    }

    public void generateImports(OptimizedTagContext context) {
    }

    public void generateDeclarations(OptimizedTagContext context) {
    }

    public void generateStart(OptimizedTagContext context) {
    }

    public void generateEnd(OptimizedTagContext context) {
        context.writeSource("}");
    }

    public void setAttribute(String attrName, Object attrValue) {
    }
    
    public boolean isFirstWhenSpecified() {
        return firstWhenSpecified;
    }
    
    public void setFirstWhenSpecified(boolean flag) {
        firstWhenSpecified = flag;    
    }
    
    public boolean canGenTagInMethod(OptimizedTagContext context) {
        return false;
    }
}
