/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.introspect;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.ws.ras.instrument.internal.model.PackageInfo;
import com.ibm.ws.ras.instrument.internal.model.TraceOptionsData;

public class TraceConfigPackageVisitor extends ClassVisitor {

    protected final static Type TRACE_OPTIONS_TYPE = Type.getObjectType("com/ibm/websphere/ras/annotation/TraceOptions");
    protected final static Type TRIVIAL_TYPE = Type.getObjectType("com/ibm/websphere/ras/annotation/Trivial");

    protected String internalPackageName;
    protected boolean trivialPackage;
    protected TraceOptionsAnnotationVisitor traceOptionsAnnotationVisitor;

    public TraceConfigPackageVisitor() {
        super(Opcodes.ASM7);
    }

    public TraceConfigPackageVisitor(ClassVisitor visitor) {
        super(Opcodes.ASM7, visitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.internalPackageName = name.replaceAll("/[^/]+$", "");
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        if (TRIVIAL_TYPE.getDescriptor().equals(desc)) {
            trivialPackage = true;
        } else if (TRACE_OPTIONS_TYPE.getDescriptor().equals(desc)) {
            traceOptionsAnnotationVisitor = new TraceOptionsAnnotationVisitor(av);
            av = traceOptionsAnnotationVisitor;
        }
        return av;
    }

    public PackageInfo getPackageInfo() {
        TraceOptionsData traceOptionsData = null;
        if (traceOptionsAnnotationVisitor != null) {
            traceOptionsData = traceOptionsAnnotationVisitor.getTraceOptionsData();
        }
        return new PackageInfo(internalPackageName, trivialPackage, traceOptionsData);
    }
}
