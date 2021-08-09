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
package com.ibm.ws.javaee.ddmetadata.generator;

import java.io.File;
import java.io.PrintWriter;

public class ModelPackageInfoClassGenerator extends ModelClassGenerator {
    public ModelPackageInfoClassGenerator(File destdir, String packageName) {
        super(destdir, packageName, "package-info");
    }

    public void generate() {
        PrintWriter out = open();
        out.println("import com.ibm.websphere.ras.annotation.TraceOptions;");
        out.println("import com.ibm.ws.javaee.ddmodel.DDModelConstants;");
        out.close();
    }

    @Override
    protected void writePackageAnnotations(PrintWriter out) {
        out.println("/**");
        out.println(" * @version 1.0.16");
        out.println(" */");
        out.println("@org.osgi.annotation.versioning.Version(\"1.0.16\")");
        out.println("@TraceOptions(traceGroup = DDModelConstants.TRACE_GROUP, messageBundle = DDModelConstants.TRACE_MESSAGES)");
    }
}
