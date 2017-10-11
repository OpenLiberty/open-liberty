/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 *
 */
package test;

import java.util.ArrayList;

import com.ibm.ws.ffdc.IncidentStream;

/**
 *
 */
public class Component_DM extends com.ibm.ws.ffdc.DiagnosticModule {
    @SuppressWarnings("unused")
    private String id;

    public ArrayList<String> calledMethods = new ArrayList<String>();

    public Component_DM() {
        id = null;
    }

    public void ffdcDumpDefaultObjectX(Throwable ex, IncidentStream is, Object callerThis, Object[] o, String sourceId) {
        calledMethods.add("ffdcDumpDefaultObjectX");
        is.writeLine("ffdcDumpDefaultObjectX", true);
    }

    public void ffdcdumpMethod1(Throwable ex, IncidentStream is, Object callerThis, Object[] o, String sourceId) {
        calledMethods.add("ffdcdumpMethod1");
        is.writeLine("ffdcdumpMethod1", ex);
    }

    @SuppressWarnings("unused")
    public void ffdcdumpMethod2(Throwable ex, IncidentStream is, Object callerThis, Object[] o, String sourceId) {
        calledMethods.add("ffdcdumpMethod2");
        if (true)
            throw new RuntimeException("Intentional exception in diagnostic method");

        is.writeLine("ffdcdumpMethod2", callerThis);
    }

    public void ffdcdumpMethod3(Throwable ex, IncidentStream is, Object callerThis, Object[] o, String sourceId) {
        calledMethods.add("ffdcdumpMethod3");
        is.writeLine("ffdcdumpMethod3", o);

        if (true)
            this.stopProcessingException();
    }

    public void ffdcdumpMethod4(Throwable ex, IncidentStream is, Object callerThis, Object[] o, String sourceId) {
        calledMethods.add("ffdcdumpMethod4");
        is.writeLine("ffdcdumpMethod4", sourceId);
    }

    public void bindString(String inString) {
        id = inString;
    }
}
