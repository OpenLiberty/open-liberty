/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package testservlet40.jar.jandex_v3;

public abstract sealed class SealedClass permits SealedClass_SubclassA, SealedClass_SubclassB, SealedClass_SubclassC {
	
	public abstract String getMsg();
}

final class SealedClass_SubclassA extends SealedClass {
	public String getMsg() {
		return "A";
	}
}

sealed class SealedClass_SubclassB extends SealedClass permits SealedClass_SubclassB2 {
	public String getMsg() {
		return "B";
	}
}

final class SealedClass_SubclassB2 extends SealedClass_SubclassB {
	public String getMsg() {
		return "B2";
	}
}

non-sealed class SealedClass_SubclassC extends SealedClass {
	public String getMsg() {
		return "C";
	}
}
