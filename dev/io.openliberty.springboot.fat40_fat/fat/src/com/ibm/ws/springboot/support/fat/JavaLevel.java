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
package com.ibm.ws.springboot.support.fat;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;

//All the tests extend this class. Therefore adding the annotations to set the min and max java level here.
@MinimumJavaLevel(javaLevel = 17)
@MaximumJavaLevel(javaLevel = 26) //https://docs.spring.io/spring-boot/4.0/system-requirements.html
public class JavaLevel {

}
