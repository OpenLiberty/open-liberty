/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.annocache.test.classes.v3;

public sealed abstract class SealedClassExample permits SealedSubclassA, SealedSubclassB, SealedSubclassC {
    
    public abstract String testMethod();

}

final class SealedSubclassA extends SealedClassExample {
    public String testMethod() {
        return "SealedSubclassA";
    }
}

sealed class SealedSubclassB extends SealedClassExample permits SealedSubclassB1 {
    public String testMethod() {
        return "SealedSubclassB";
    }
}

final class SealedSubclassB1 extends SealedSubclassB {
    public String testMethod() {
        return "SealedSubclassB1";
    }
}

non-sealed class SealedSubclassC extends SealedClassExample {
    public String testMethod() {
        return "SealedSubclassC";
    }
}

