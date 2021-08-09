/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.inmemory.compiler;

import com.ibm.ws.jsp.JspOptions;
import com.ibm.wsspi.jsp.compiler.JspCompiler;
import com.ibm.wsspi.jsp.compiler.JspCompilerFactory;

public class InMemoryJspCompilerFactory implements JspCompilerFactory {
    private ClassLoader loader = null;
    private JspOptions jspOptions = null;

    public InMemoryJspCompilerFactory(ClassLoader loader, JspOptions jspOptions) {
        this.loader = loader;
        this.jspOptions = jspOptions;
    }
    
    public JspCompiler createJspCompiler() {
        return new InMemoryJDTCompiler(loader, jspOptions);
    }
}
