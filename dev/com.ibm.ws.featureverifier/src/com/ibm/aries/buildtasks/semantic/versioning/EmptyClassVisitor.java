/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.semantic.versioning;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class EmptyClassVisitor extends ClassVisitor {

    public EmptyClassVisitor() {
        super(Opcodes.ASM7);
    }
}
