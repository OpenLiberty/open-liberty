/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.diagnostics;

import java.io.PrintWriter;

import com.ibm.ws.ejbcontainer.diagnostics.TextIntrospectionWriter;

/**
 * IntrospectionWriter implementation that prints all introspection data using
 * an OutputStream. <p>
 */
public class IntrospectionWriterImpl extends TextIntrospectionWriter {
    private final PrintWriter writer;

    public IntrospectionWriterImpl(PrintWriter writer) {
        this.writer = writer;
    }

    @Override
    protected void writeln(String line) {
        writer.println(line);
    }
}
