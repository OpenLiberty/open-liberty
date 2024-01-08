/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation
package org.eclipse.persistence.asm;

import org.eclipse.persistence.asm.internal.Util;
import org.eclipse.persistence.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;

public class Opcodes {

    //This block must be first - begin
    private final static String ASM_OPCCODES_ECLIPSELINK = "org.eclipse.persistence.internal.libraries.asm.Opcodes";
    private final static String ASM_OPCCODES_OW2 = "org.objectweb.asm.Opcodes";

    private final static Map<String, String> ASM_OPCCODES_MAP = new HashMap<>();

    static {
        ASM_OPCCODES_MAP.put(ASMFactory.ASM_SERVICE_OW2, ASM_OPCCODES_OW2);
        ASM_OPCCODES_MAP.put(ASMFactory.ASM_SERVICE_ECLIPSELINK, ASM_OPCCODES_ECLIPSELINK);
    }
    //This block must be first - end

    public static final int AASTORE = valueInt("AASTORE");
    public static final int ACC_ENUM = valueInt("ACC_ENUM");
    public static final int ACC_FINAL = valueInt("ACC_FINAL");
    public static final int ACC_INTERFACE = valueInt("ACC_INTERFACE");
    public static final int ACC_PRIVATE = valueInt("ACC_PRIVATE");
    public static final int ACC_PROTECTED = valueInt("ACC_PROTECTED");
    public static final int ACC_PUBLIC = valueInt("ACC_PUBLIC");
    public static final int ACC_STATIC = valueInt("ACC_STATIC");
    public static final int ACC_SUPER = valueInt("ACC_SUPER");
    public static final int ACC_SYNTHETIC = valueInt("ACC_SYNTHETIC");
    public static final int ACC_TRANSIENT = valueInt("ACC_TRANSIENT");
    public static final int ACONST_NULL = valueInt("ACONST_NULL");
    public static final int ALOAD = valueInt("ALOAD");
    public static final int ANEWARRAY = valueInt("ANEWARRAY");
    public static final int ARETURN = valueInt("ARETURN");
    public static final int ASM9 = Opcodes.valueInt("ASM9");
    public static final int ASTORE = valueInt("ASTORE");
    public static final int BIPUSH = valueInt("BIPUSH");
    public static final int CHECKCAST = valueInt("CHECKCAST");
    public static final int DUP = valueInt("DUP");
    public static final int F_SAME = valueInt("F_SAME");
    public static final int GETFIELD = valueInt("GETFIELD");
    public static final int GETSTATIC = valueInt("GETSTATIC");
    public static final int GOTO = valueInt("GOTO");
    public static final int ICONST_0 = valueInt("ICONST_0");
    public static final int ICONST_1 = valueInt("ICONST_1");
    public static final int ICONST_2 = valueInt("ICONST_2");
    public static final int ICONST_3 = valueInt("ICONST_3");
    public static final int ICONST_4 = valueInt("ICONST_4");
    public static final int ICONST_5 = valueInt("ICONST_5");
    public static final int IF_ACMPEQ = valueInt("IF_ACMPEQ");
    public static final int IF_ACMPNE = valueInt("IF_ACMPNE");
    public static final int IFEQ = valueInt("IFEQ");
    public static final int IFNE = valueInt("IFNE");
    public static final int IFNONNULL = valueInt("IFNONNULL");
    public static final int IFNULL = valueInt("IFNULL");
    public static final int ILOAD = valueInt("ILOAD");
    public static final int INVOKEINTERFACE = valueInt("INVOKEINTERFACE");
    public static final int INVOKESPECIAL = valueInt("INVOKESPECIAL");
    public static final int INVOKESTATIC = valueInt("INVOKESTATIC");
    public static final int INVOKEVIRTUAL = valueInt("INVOKEVIRTUAL");
    public static final int IRETURN = valueInt("IRETURN");
    public static final int NEW = valueInt("NEW");
    public static final int POP = valueInt("POP");
    public static final int PUTFIELD = valueInt("PUTFIELD");
    public static final int PUTSTATIC = valueInt("PUTSTATIC");
    public static final int RETURN = valueInt("RETURN");
    public static final int SIPUSH = valueInt("SIPUSH");
    public static final int V1_8 = Opcodes.valueInt("V1_8");
    
    private static int valueInt(String fieldName) {
        return ((int) Util.getFieldValue(ASM_OPCCODES_MAP, fieldName, Integer.TYPE));
    }

    public static Class getOpcodesClass() {
        String asmService = ASMFactory.getAsmService();
        Class<?> clazz;
        try {
            String className = ASM_OPCCODES_MAP.get(asmService);
            if (className == null) {
                throw ValidationException.incorrectASMServiceProvided();
            }
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw ValidationException.notAvailableASMService();
        }
        return clazz;
    }

}
