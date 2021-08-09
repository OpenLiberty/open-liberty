/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.log;

import javax.enterprise.context.ApplicationScoped;

/**
 * Log for the CDI servlets.
 */
@ApplicationScoped
public class ApplicationLog extends Log {
    //
    private static final long serialVersionUID = 1L;

    //
    public ApplicationLog() {
        super();
        // (new Throwable("Dummy")).printStackTrace(System.out);
    }

    @Override
    public void addLine(String line) {
        super.addLine(line);
        // (new Throwable("Dummy")).printStackTrace(System.out);
    }

}