/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.class_scanner.ano;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ModifierType;

/**
 * Asm utility class
 *
 */
public final class AsmHelper {
	public enum RoleFilter {
		CLASS,
		FIELD,
		METHOD
	}
	
	/**
	 * Converts a class from an internal representation (i.e., "java/lang/Object") to a normal
	 * representation (i.e., "java.lang.Object")
	 * 
	 */
	public static final String normalizeClassName(String className) {
		return className.replace('/', '.');
	}
	
	/**
	 * Extract the package name from a fully qualified  (normalized) class name, i.e.: converts
	 * "java.lang.Object" to "java.lang".
	 * 
	 * @param className
	 * @return
	 */
	public static final String extractPackageName(String className) {
		if (className == null || className.trim().isEmpty()) {
			return "";
		}
		
		final int idx = className.lastIndexOf('.');
		if (idx == -1) {
			return "";
		} else {
			return className.substring(0, idx);
		}
	}
	
	/**
	 * Extract the simple class name from a fully qualified  (normalized) class name, i.e.: converts
	 * "java.lang.Object" to "Object".
	 * 
	 * @param className
	 * @return
	 */
	public static final String extractSimpleClassName(String className) {
		if (className == null) {
			throw new NullPointerException();
		}
		
		final int idx = className.lastIndexOf('.');
		if (idx == -1) {
			return className;
		} else {
			return className.substring(idx + 1);
		}
	}
	
	public static final List<ModifierType> resolveAsmOpcode(RoleFilter filter, int opcode) {
		final List<ModifierType> retList = new ArrayList<ModifierType>();
		
		// Modifiers associated with Classes, Fields, and Methods
		
		if (0 != (Opcodes.ACC_ABSTRACT & opcode)) {
			retList.add(ModifierType.ABSTRACT);
		}
		
		if (0 != (Opcodes.ACC_FINAL & opcode)) {
			retList.add(ModifierType.FINAL);
		}		
		
		if (0 != (Opcodes.ACC_PRIVATE & opcode)) {
			retList.add(ModifierType.PRIVATE);
		}
		
		if (0 != (Opcodes.ACC_PROTECTED & opcode)) {
			retList.add(ModifierType.PROTECTED);
		}
		
		if (0 != (Opcodes.ACC_PUBLIC & opcode)) {
			retList.add(ModifierType.PUBLIC);
		}
		
		if (RoleFilter.CLASS == filter) {
			// The above are the OpCodes suitable for ModifierType items associated with Classes
			return retList;
		}
		
		// Modifiers associated with Fields and Methods
		
		if (0 != (Opcodes.ACC_STATIC & opcode)) {
            retList.add(ModifierType.STATIC);
        }
		
		return retList;
	}
	
	public static final boolean isPrimitiveType(Type type) {
		if (type == null) {
			throw new NullPointerException(); // Nope
		}
		
		if (Type.VOID_TYPE.equals(type)
				|| Type.BOOLEAN_TYPE.equals(type) 
				|| Type.CHAR_TYPE.equals(type)
				|| Type.BYTE_TYPE.equals(type)
				|| Type.SHORT_TYPE.equals(type)
				|| Type.INT_TYPE.equals(type)
				|| Type.FLOAT_TYPE.equals(type) 
				|| Type.LONG_TYPE.equals(type)
				|| Type.DOUBLE_TYPE.equals(type)) {
			return true;
		}
		
		return false;
	}
}
