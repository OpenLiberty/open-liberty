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

    private final static String ASM_OPCCODES_ECLIPSELINK = "org.eclipse.persistence.internal.libraries.asm.Opcodes";
    private final static String ASM_OPCCODES_OW2 = "org.objectweb.asm.Opcodes";

    private final static Map<String, String> ASM_OPCCODES_MAP = new HashMap<>();

    static {
        ASM_OPCCODES_MAP.put(ASMFactory.ASM_SERVICE_OW2, ASM_OPCCODES_OW2);
        ASM_OPCCODES_MAP.put(ASMFactory.ASM_SERVICE_ECLIPSELINK, ASM_OPCCODES_ECLIPSELINK);
    }

    public static int valueInt(String fieldName) {
        return ((int) Util.getFieldValue(ASM_OPCCODES_MAP, fieldName, Integer.TYPE));
    }

    public static Integer valueInteger(String fieldName) {
        return ((Integer) Util.getFieldValue(ASM_OPCCODES_MAP, fieldName, Integer.class));
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
