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
package com.ibm.tx.jta.cdi.interceptors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.Stereotype;

// Just uses a different ordering of stereotypes to HighestLevelStereotype to try to exercise the code more
@Stereotype
@NonTransactionalStereotype
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@TransactionalStereotypeSimple1
public @interface HighestLevelStereotype2 {

}
