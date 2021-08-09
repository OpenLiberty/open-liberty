/*******************************************************************************
 * Copyright (c) 2016,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmetadata.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

@Target({ TYPE, METHOD, FIELD })
/**
 * Indicates that a type or method is not used by the Liberty runtime.
 *
 * Note that in some cases code will reference these types or methods, but the values
 * make no difference to the runtime so they should not be configurable.
 */
public @interface LibertyNotInUse {
    // EMPTY
}
