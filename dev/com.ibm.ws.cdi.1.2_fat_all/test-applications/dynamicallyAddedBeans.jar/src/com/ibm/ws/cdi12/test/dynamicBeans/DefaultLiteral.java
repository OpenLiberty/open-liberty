/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.dynamicBeans;

import javax.enterprise.inject.Default;
import javax.enterprise.util.AnnotationLiteral;

@SuppressWarnings("all")
public class DefaultLiteral extends AnnotationLiteral<Default> implements Default {

    public static final Default INSTANCE = new DefaultLiteral();

    private DefaultLiteral() {}

}
