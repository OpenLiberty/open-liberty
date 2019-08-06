/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.test.classes.repeatable;

// Disabled: Requires java8
//
// import java.lang.annotation.Repeatable;
// @Repeatable(Authors.class)

public @interface Author {
     String name() default "DEFAULT!";
}