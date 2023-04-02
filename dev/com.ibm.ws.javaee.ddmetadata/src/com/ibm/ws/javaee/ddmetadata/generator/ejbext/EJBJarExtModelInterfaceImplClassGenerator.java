/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmetadata.generator.ejbext;

import java.io.File;
import java.io.PrintWriter;

import com.ibm.ws.javaee.ddmetadata.generator.util.AbstractEJBJarBndExtModelInterfaceImplClassGenerator;
import com.ibm.ws.javaee.ddmetadata.model.ModelInterfaceType;

public class EJBJarExtModelInterfaceImplClassGenerator extends AbstractEJBJarBndExtModelInterfaceImplClassGenerator {
    public EJBJarExtModelInterfaceImplClassGenerator(File destdir, ModelInterfaceType type) {
        super(destdir, type);
    }

    @Override
    protected void writeFinishExtra(PrintWriter out, String indent) {
        out.println();
        writeCheckUnique(out, indent,
                         true, "com.ibm.ws.javaee.dd.ejbext.EnterpriseBean", "getName", null, null,
                         "bean", "getEnterpriseBeans()", "beans");
    }
}
