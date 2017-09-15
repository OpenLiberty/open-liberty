/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

public enum JavaDumpAction {
    HEAP("heap"),
    SYSTEM("system"),
    THREAD("thread");
    // WARNING:  z/OS has special operator console support for all valid java dumps, so if a new java dump
    //           is added to this list, please add associated support from the z/OS console (see the
    //           com.ibm.ws.diagnostics.zos project for details)
    //
    //   Existing handlers for java dumps:
    //       heap   = com.ibm.ws.diagnostics.zos.javadump.HeapdumpCommandHandler.java
    //       system = com.ibm.ws.diagnostics.zos.tdump.TdumpCommandHandler.java
    //       thread = com.ibm.ws.diagnostics.zos.javadump.JavacoreCommandHandler.java

    public static JavaDumpAction forDisplayName(String displayName) {
        for (JavaDumpAction action : values()) {
            if (displayName.equals(action.displayName)) {
                return action;
            }
        }
        return null;
    }

    private final String displayName;

    JavaDumpAction(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
