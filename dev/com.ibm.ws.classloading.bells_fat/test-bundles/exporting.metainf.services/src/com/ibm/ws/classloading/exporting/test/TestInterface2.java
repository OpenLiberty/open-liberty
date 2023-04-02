/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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

package com.ibm.ws.classloading.exporting.test;

/**
 * An interface to be implemented in one of the jar files in files/ExportingMetaInfServices.
 *
 * @author Tamir Faibish
 */

public interface TestInterface2 {

    default String isThere2(String name) {
        return name + " is there";
    }

    default String hasProperties2(String name) {
        return name + " has properties " + null;
    }

    default String hasUpdatedProperties2(String name) {
        return name + " has updated properties " + null;
    }
}
