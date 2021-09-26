/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    String isThere2(String name);

    String hasProperties2(String name);
}
