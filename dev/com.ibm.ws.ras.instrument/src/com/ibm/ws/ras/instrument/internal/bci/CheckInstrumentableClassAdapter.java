/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ras.instrument.internal.bci;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Simple adapter that exposes {@code isInstrumentableClass()} and {@code isInstrumentableMethod()}.
 */
public class CheckInstrumentableClassAdapter extends ClassVisitor {

    /**
     * The internal name of the class we're processing.
     */
    private String classInternalName;

    /**
     * Indication of whether or not this class represents an interface.
     */
    private boolean isInterface;

    /**
     * Indication of whether or not this class was generated and not
     * present in source.
     */
    private boolean isSynthetic;

    /**
     * Default chained constructor.
     * 
     * @param visitor
     *            the visitor to delegate to
     */
    public CheckInstrumentableClassAdapter(ClassVisitor visitor) {
        super(Opcodes.ASM7, visitor);
    }

    /**
     * Begin processing a class. We save some of the header information that
     * we only get from the header to assist with processing.
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.classInternalName = name;
        this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        this.isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0;

        super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * Indicate whether or not the class is instrumentable. A class is
     * considered instrumentable if it's:
     * <ul>
     * <li>not an interface
     * <li>not synthetic
     * <li>not a dynamic proxy
     * <li>not <code>java.lang.Object</code>
     * </ul>
     * 
     * @return true if the class can be instrumented for trace.
     */
    public boolean isInstrumentableClass() {
        // Don't instrument interfaces
        if (isInterface) {
            return false;
        }
        // Don't instrument methods that are not in the source
        if (isSynthetic) {
            return false;
        }
        // Stay away from java.lang.Object
        if (classInternalName.equals("java/lang/Object")) {
            return false;
        }
        // Don't instrument dynamic proxies
        if (classInternalName.startsWith("$Proxy")) {
            return false;
        }
        // Don't instrument package-info
        if (classInternalName.endsWith("/package-info")) {
            return false;
        }
        return true;
    }

    /**
     * Indicate whether or not the target method is instrumentable.
     * 
     * @param access
     *            the method property flags
     * @param methodName
     *            the name of the method
     * 
     * @return true if the method is not synthetic, is not native, and is
     *         not named toString or hashCode.
     */
    public boolean isInstrumentableMethod(int access, String methodName, String descriptor) {
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            return false;
        }
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            return false;
        }
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            return false;
        }
        if (methodName.equals("toString") && descriptor.equals("()Ljava/lang/String;")) {
            return false;
        }
        if (methodName.equals("hashCode") && descriptor.equals("()I")) {
            return false;
        }
        return true;
    }
}
