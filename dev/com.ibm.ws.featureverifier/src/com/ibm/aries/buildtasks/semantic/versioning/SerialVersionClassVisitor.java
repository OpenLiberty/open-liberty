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

import java.io.IOException;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

public class SerialVersionClassVisitor extends SerialVersionUIDAdder {

    public SerialVersionClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM7, cv);

    }

    public long getComputeSerialVersionUID() {

        try {
            return computeSVUID();
        } catch (IOException ioe) {
            // not a issue
        }
        //If you start seeing these.. you're probably invoking getComputeSerialVersionUID on an interface or an enum (I was).

        //catch (NullPointerException e){
//			[err] java.lang.NullPointerException
//			[err] 	at org.objectweb.asm.commons.SerialVersionUIDAdder.computeSVUID(Unknown Source)
//			[err] 	at com.ibm.aries.buildtasks.semantic.versioning.SerialVersionClassVisitor.getComputeSerialVersionUID(SerialVersionClassVisitor.java:36)
//			[err] 	at com.ibm.aries.buildtasks.semantic.versioning.decls.LiveClassDeclaration.getSerialVersionUID(LiveClassDeclaration.java:176)
//			[err] 	at com.ibm.aries.buildtasks.semantic.versioning.decls.ClassDeclaration.toXML(ClassDeclaration.java:59)
        //}

        return 0;
    }
}
