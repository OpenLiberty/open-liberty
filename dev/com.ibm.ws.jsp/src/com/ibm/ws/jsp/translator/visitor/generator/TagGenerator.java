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
package com.ibm.ws.jsp.translator.visitor.generator;

import java.util.List;

import org.w3c.dom.Node;

import com.ibm.ws.jsp.JspCoreException;

public interface TagGenerator {
    public MethodWriter generateTagStart() throws JspCoreException;
    public MethodWriter generateTagMiddle() throws JspCoreException;
    public MethodWriter generateTagEnd() throws JspCoreException;
    public MethodWriter getBodyWriter();
    public JavaCodeWriter getWriterForChild(int section, Node childElement) throws JspCoreException;
    public void generateImports(JavaCodeWriter writer);
    public void generateDeclarations(JavaCodeWriter writer);
    public void generateInitialization(JavaCodeWriter writer);
    public void generateFinally(JavaCodeWriter writer);
    public List generateSetters() throws JspCoreException;
    public void setParentTagInstanceInfo(CustomTagGenerator.TagInstanceInfo parentTagInstanceInfo);
    public void setIsInFragment(boolean isFragment);
    public boolean fragmentWriterUsed();
}
