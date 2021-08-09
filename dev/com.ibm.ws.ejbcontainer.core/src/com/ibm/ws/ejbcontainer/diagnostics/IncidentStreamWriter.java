/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.diagnostics;

import com.ibm.ws.ffdc.IncidentStream;

/**
 * IntrospectionWriter implementation that prints all introspection data using
 * IncidentStream. <p>
 */
public class IncidentStreamWriter extends TextIntrospectionWriter {
    private final IncidentStream is;

    public IncidentStreamWriter(IncidentStream is) {
        this.is = is;
    }

    /** {@inheritDoc} */
    @Override
    public void writeln(String line) {
        is.writeLine("", line);
    }
}
