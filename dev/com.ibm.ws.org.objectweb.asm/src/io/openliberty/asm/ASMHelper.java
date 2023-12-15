/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

package io.openliberty.asm;

import org.objectweb.asm.Opcodes;

/**
 * Convenience utility for Liberty components which consume ASM.
 */
public class ASMHelper {
    private static final int CURRENT_ASM = Opcodes.ASM9; // Update this when an ASM update introduces a new constant
    private static final int CURRENT_MAX_JAVA_LEVEL = Opcodes.V22; // Update this to show the maximum version of Java we can run with

    /**
     * Returns the constant representing the current version of ASM.
     *
     * @return
     */
    public final static int getCurrentASM() {
        return CURRENT_ASM;
    }

    /**
     * Returns the constant representing the maximum Java level at which we can run
     *
     * @return
     */
    public final static int getMaximumJavaLevel() {
        return CURRENT_MAX_JAVA_LEVEL;
    }
}
