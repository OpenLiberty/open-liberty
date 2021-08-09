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

public interface OptimizedTag {
    void generateImports(OptimizedTagContext context);
    void generateDeclarations(OptimizedTagContext context);
    void setAttribute(String attrName, Object attrValue);
    void generateStart(OptimizedTagContext context);
    void generateEnd(OptimizedTagContext context);
    boolean doOptimization(OptimizedTagContext context);
    boolean canGenTagInMethod(OptimizedTagContext context);
}
