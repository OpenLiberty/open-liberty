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

import java.util.List;

import com.ibm.wsspi.jsp.compiler.JspCompilerResult;

public class InMemoryJspCompilerResult implements JspCompilerResult {
    private int rc = 0;
    private String compilerMessage = null;
    private List resourcesList = null;
    private List compilerFailureFileNames=null; //PK72039
    
    public InMemoryJspCompilerResult(int rc, String compilerMessage, List resourcesList) {
        this.rc = rc;
        this.compilerMessage = compilerMessage;
        this.resourcesList = resourcesList;
    }

    public String getCompilerMessage() {
        return compilerMessage;
    }

    public int getCompilerReturnValue() {
        return rc;
    }

    public List getResourcesList() {
        return resourcesList;
    }
    
    //PK72039 start
    public List getCompilerFailureFileNames() {
        return compilerFailureFileNames;
    }
    //PK72039 end

}
