/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package testservlet40.jar.jandex_v3;

/*
 * Tests TypeVariableReference
 * 
 * https://smallrye.io/blog/jandex-3-0-0/
 * 
 * For example, one often defines type parameters like <T extends Comparable<T>>. This type parameter definition is recursive in T. Since Jandex types generally do not form cycles, the first occurence of T is represented as TypeVariable, but the second occurence as UnresolvedTypeVariable.
 * To become more faithful yet still avoid cycles in the object model, Jandex 3.0.0 includes an additional representation of type variables occuring in their own definition:
 *
 *   TypeVariableReference
 *
 * With Jandex 3.0.0, the second occurence of T in the example above is represented as TypeVariableReference. A reference may be follow()-ed to obtain the type variable it points to.
 */
public class TypeParamaterClass<T extends Comparable<T>> {
	
	public TypeParamaterClass() {
		super();
	}

	public int compareTo(T o) {
		return 0;
	}

}
