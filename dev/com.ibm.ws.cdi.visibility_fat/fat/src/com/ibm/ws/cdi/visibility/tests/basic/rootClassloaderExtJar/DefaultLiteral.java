/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.basic.rootClassloaderExtJar;

import javax.enterprise.inject.Default;
import javax.enterprise.util.AnnotationLiteral;

@SuppressWarnings("all")
public class DefaultLiteral extends AnnotationLiteral<Default> implements Default {

    public static final Default INSTANCE = new DefaultLiteral();

    private DefaultLiteral() {}

}
