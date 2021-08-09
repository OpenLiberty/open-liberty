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

import com.ibm.ws.jsp.JspOptions; //PK65013

public interface OptimizedTagContext {
    void writeSource(String source);
    void writeImport(String importId, String importSource);
    void writeDeclaration(String declarationId, String declarationSource);
    String createTemporaryVariable();
    boolean hasAttribute(String attrName);
    boolean isJspAttribute(String attrName);
    OptimizedTag getParent();
    boolean hasBody();
    boolean hasJspBody();
    JspOptions getJspOptions();  //PK65013
    boolean isTagFile(); //PK65013
}
