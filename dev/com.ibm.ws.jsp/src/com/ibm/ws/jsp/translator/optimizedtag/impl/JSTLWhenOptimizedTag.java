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

public class JSTLWhenOptimizedTag implements OptimizedTag {
    private String test = null;
    
    public boolean doOptimization(OptimizedTagContext context) {
        boolean optimize = false;
        if (context.getParent() != null && context.getParent() instanceof JSTLChooseOptimizedTag) {
            optimize = true;
        }
        return optimize;
    }

    public void generateImports(OptimizedTagContext context) {
    }

    public void generateDeclarations(OptimizedTagContext context) {
    }

    public void generateStart(OptimizedTagContext context) {
        JSTLChooseOptimizedTag chooseTag = (JSTLChooseOptimizedTag)context.getParent();
        if (chooseTag.isFirstWhenSpecified()) {
            context.writeSource("} else if(");
        }
        else {
            context.writeSource("if (");
            chooseTag.setFirstWhenSpecified(true);
        }
        context.writeSource(test);
        context.writeSource(") {");
    }

    public void generateEnd(OptimizedTagContext context) {
    }

    public void setAttribute(String attrName, Object attrValue) {
        if (attrName.equals("test")) {
            test = (String)attrValue;
        }
    }
    
    public boolean canGenTagInMethod(OptimizedTagContext context) {
        return false;
    }
}
