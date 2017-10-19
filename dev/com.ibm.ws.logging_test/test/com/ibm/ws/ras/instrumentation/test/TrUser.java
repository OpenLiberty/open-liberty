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
package com.ibm.ws.ras.instrumentation.test;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.TraceOptions;

@TraceOptions(messageBundle = "org.org.org.org.org.org")
public class TrUser {

    // private final static TraceComponent tc = Tr.register(TrUser.class);
    // private final static TraceComponent tc1 = Tr.register(TrUser.class, "Group");
    private final static Logger logger = Logger.getLogger("foo");

    // private final static Logger logger1 = Logger.getLogger("foo", "bar.resource");

    // public TrUser() {
    // if (TraceComponent.isAnyTracingEnabled() && tc != null && tc.isEntryEnabled()) {
    // Tr.entry(tc, "<init>");
    // }
    // }
    //
    // public void doTrEntryExpoitation() {
    // if (tc.isEventEnabled()) Tr.event(tc, "This is an odd event.");
    // }
    //
    // public void justAnotherSimpleMethod(String var) {
    // System.out.println(var);
    // }

    public void doLoggerExploitation() {
        if (logger.isLoggable(Level.FINE))
            logger.entering(getClass().getName(), "doLoggerExploitation");
    }

    @Sensitive
    public String getValue() {
        return "value";
    }

    // public String toString() {
    // StringBuilder sb = new StringBuilder(super.toString());
    // //sb.append(";tc1=").append(tc1);
    // //sb.append(",logger1=").append(logger1);
    // sb.append(",value=").append(getValue());
    // return sb.toString();
    // }
}
