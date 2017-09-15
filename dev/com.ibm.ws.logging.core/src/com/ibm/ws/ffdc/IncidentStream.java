/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ffdc;

public interface IncidentStream {

    public void write(String text, boolean value);

    public void write(String text, byte value);

    public void write(String text, char value);

    public void write(String text, short value);

    public void write(String text, int value);

    public void write(String text, long value);

    public void write(String text, float value);

    public void write(String text, double value);

    public void write(String text, String value);

    public void write(String text, Object value);

    public void introspectAndWrite(String text, Object value);

    public void introspectAndWrite(String text, Object value, int depth);

    public void introspectAndWrite(String text, Object value, int depth, int maxBytes);

    public void writeLine(String text, boolean value);

    public void writeLine(String text, byte value);

    public void writeLine(String text, char value);

    public void writeLine(String text, short value);

    public void writeLine(String text, int value);

    public void writeLine(String text, long value);

    public void writeLine(String text, float value);

    public void writeLine(String text, double value);

    public void writeLine(String text, String value);

    public void writeLine(String text, Object value);

    public void introspectAndWriteLine(String text, Object value);

    public void introspectAndWriteLine(String text, Object value, int depth);

    public void introspectAndWriteLine(String text, Object value, int depth, int maxBytes);
}

// End of file
