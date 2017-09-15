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

public class CodeGenerationPhase {
    public static final int IMPORT_SECTION = 1;
    public static final int CLASS_SECTION = 2;
    public static final int STATIC_SECTION = 3;
    public static final int INIT_SECTION = 4;
    public static final int METHOD_INIT_SECTION = 5;
    public static final int METHOD_SECTION = 6;
    public static final int FINALLY_SECTION = 7;
}
