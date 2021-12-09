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

package com.ibm.ws.classloading.exporting.test.unavailable;

/**
 * This interface is implemented by TestImpl of testInterfaceClassNotFound.jar.
 * Since this package is a private-package, it can't be seen outside the bundle.
 *
 * @author Tamir Faibish
 */

public interface TestInterface3 {

   String isThere3(String name);

}
