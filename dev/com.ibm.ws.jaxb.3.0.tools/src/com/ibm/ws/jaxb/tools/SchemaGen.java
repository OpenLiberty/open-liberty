/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxb.tools;

/**
 * IBM Wrapper for SchemaGen tool.
 */
public class SchemaGen {

    public static void main(String args[]) throws java.lang.Throwable {
        com.sun.tools.jxc.SchemaGenerator.main(args);
    }

}
