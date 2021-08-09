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

//PK72039      Add ability to continue to compile the rest of the JSPs during a batch compile failure  2008/10/21  Jay Sartoris

package com.ibm.ws.jsp.translator.compiler;

import com.ibm.wsspi.jsp.compiler.JspCompilerResult;
import java.util.List; //PK72039


/**
 */
public class JspCompilerResultImpl implements JspCompilerResult {
    private int compilerReturnValue=-1;
    private String compilerMessage=null;
    private List compilerFailureFileNames=null; //PK72039

    public JspCompilerResultImpl(int compilerRetVal, String compilerMsg){
        this.compilerReturnValue=compilerRetVal;
        this.compilerMessage=compilerMsg;
    }

    //PK72039 start
    public JspCompilerResultImpl(int compilerRetVal, String compilerMsg, List compilerFailureFN){
        this.compilerReturnValue=compilerRetVal;
        this.compilerMessage=compilerMsg;
        this.compilerFailureFileNames=compilerFailureFN;
    }
    //PK72039 start

    public int getCompilerReturnValue() {
        return compilerReturnValue;
    }

    public String getCompilerMessage() {
        return compilerMessage;
    }

    //PK72039 start
    public List getCompilerFailureFileNames() {
        return compilerFailureFileNames;
    }
    //PK72039 end
}
