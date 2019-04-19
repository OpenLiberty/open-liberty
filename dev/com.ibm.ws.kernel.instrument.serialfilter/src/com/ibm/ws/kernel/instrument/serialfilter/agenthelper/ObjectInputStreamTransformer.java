/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.agenthelper;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

public class ObjectInputStreamTransformer implements ClassFileTransformer {
    private final boolean useHolder;

    public ObjectInputStreamTransformer(boolean useHolder) {
        this.useHolder = useHolder;
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className.equals("java/io/ObjectInputStream")) {
            boolean debugEnabled = PreMainUtil.isDebugEnabled();
            if (debugEnabled) {
                System.out.println("Invoking trnasformObjectInputStreamClass. useHolder : " + useHolder);
            }
            byte[] output = transformObjectInputStreamClass(classfileBuffer, useHolder);
            if (debugEnabled) {
                System.out.println("Exiting trnasformObjectInputStreamClass");
            }
            return output;
        } else {
            return classfileBuffer;
        }
    }

    private static byte[] transformObjectInputStreamClass(byte[] classfileBuffer, boolean useHolder) {
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, COMPUTE_MAXS);
        ClassVisitor cv;
        if (useHolder) {
            cv = new ObjectInputStreamClassWithHolderInjector(cw);
        } else {
            cv = new ObjectInputStreamClassInjector(cw);
        }
        cr.accept(cv, 0);
        return cw.toByteArray();
    }
}
