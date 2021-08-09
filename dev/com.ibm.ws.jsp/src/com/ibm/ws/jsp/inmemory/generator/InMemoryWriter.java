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
package com.ibm.ws.jsp.inmemory.generator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

import com.ibm.ws.jsp.translator.visitor.generator.JavaFileWriter;

public class InMemoryWriter extends JavaFileWriter{
    
    public InMemoryWriter(Writer writer, Map jspElementMap, Map cdataJspIdMap, Map customTagMethodJspIdMap) throws IOException  {
    	super(new PrintWriter(new BufferedWriter(writer)),jspElementMap, cdataJspIdMap, customTagMethodJspIdMap );
    }
}
