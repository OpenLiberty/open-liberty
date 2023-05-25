/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package transactional.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.Stereotype;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

@Stereotype
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Transactional(value = TxType.MANDATORY,
               rollbackOn = { IllegalAccessException.class, InterruptedException.class },
               dontRollbackOn = { ArithmeticException.class, NullPointerException.class })
public @interface TransactionalStereotype {

}
