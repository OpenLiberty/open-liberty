/*******************************************************************************
 * Copyright (c) 2021,2023 IBM Corporation and others.
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
package com.ibm.ws.logging.internal.osgi.stackjoiner.bci;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.openliberty.asm.ASMHelper;

/**
 * Applies a transformation to the java.lang.Throwable class
 *
 */
public class ThrowableClassAdapter extends ClassVisitor implements Opcodes {

    public ThrowableClassAdapter(ClassVisitor cv) {
        super(ASMHelper.getCurrentASM(), cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        MethodVisitor tmv = new ThrowableMethodAdapter(mv, name, desc, signature);
        return tmv != null ? tmv : mv;
    };

}
