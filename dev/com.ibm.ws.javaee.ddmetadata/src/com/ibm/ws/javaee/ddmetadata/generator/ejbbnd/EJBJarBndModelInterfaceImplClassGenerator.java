/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmetadata.generator.ejbbnd;

import java.io.File;
import java.io.PrintWriter;

import com.ibm.ws.javaee.ddmetadata.generator.util.AbstractEJBJarBndExtModelInterfaceImplClassGenerator;
import com.ibm.ws.javaee.ddmetadata.model.ModelInterfaceType;

public class EJBJarBndModelInterfaceImplClassGenerator extends AbstractEJBJarBndExtModelInterfaceImplClassGenerator {
    public EJBJarBndModelInterfaceImplClassGenerator(File destdir, ModelInterfaceType type) {
        super(destdir, type);
    }

    @Override
    protected void writeFinishExtra(PrintWriter out, String indent) {
        out.println();
        writeCheckUnique(out, indent,
                         true, "com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean", "getName", null, null,
                         "bean", "getEnterpriseBeans()", "beans");

        out.println();
        writeCheckUnique(out, indent,
                         false, "com.ibm.ws.javaee.dd.commonbnd.Interceptor", "getClassName", "interceptor", "class",
                         "interceptor", "getInterceptors()", "interceptors");

        out.println();
        writeCheckUnique(out, indent,
                         false, "com.ibm.ws.javaee.dd.commonbnd.MessageDestination", "getName", "message-destination", "name",
                         "messageDestination", "getMessageDestinations()", "messageDestinations");
    }
}
