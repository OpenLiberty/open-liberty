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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.persistence.asm.internal.Util;

public abstract class ClassReader {

    //This block must be first - begin
    private final static String ASM_CLASSREADER_ECLIPSELINK = "org.eclipse.persistence.internal.libraries.asm.ClassReader";
    private final static String ASM_CLASSREADER_OW2 = "org.objectweb.asm.ClassReader";

    private final static Map<String, String> ASM_CLASSREADER_MAP = new HashMap<>();

    static {
        ASM_CLASSREADER_MAP.put(ASMFactory.ASM_SERVICE_OW2, ASM_CLASSREADER_OW2);
        ASM_CLASSREADER_MAP.put(ASMFactory.ASM_SERVICE_ECLIPSELINK, ASM_CLASSREADER_ECLIPSELINK);
    }
    //This block must be first - end

    public static final int SKIP_CODE = valueInt("SKIP_CODE");
    public static final int SKIP_DEBUG = valueInt("SKIP_DEBUG");
    public static final int SKIP_FRAMES = valueInt("SKIP_FRAMES");
    
    private static int valueInt(String fieldName) {
        return ((int) Util.getFieldValue(ASM_CLASSREADER_MAP, fieldName, Integer.TYPE));
    }

    public abstract void accept(final ClassVisitor classVisitor, final int parsingOptions);

    public abstract void accept(final ClassVisitor classVisitor, final Attribute[] attributePrototypes, final int parsingOptions);

    public abstract int getAccess();

    public abstract String getSuperName();

    public  abstract String[] getInterfaces();

    public abstract <T> T unwrap();
}
