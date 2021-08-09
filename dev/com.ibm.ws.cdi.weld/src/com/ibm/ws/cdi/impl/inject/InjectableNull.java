/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.inject;

/**
 * According to the CDI spec a @Dependent scoped producer method may return null.
 * However that is the only time CDI can inject a null.
 * 
 * Therefore InjectInjectionObjectFactory will return this pusdo class to distinguish
 * between a null that came from @Dependent scoped producer method and any other null.
 *
 * After we have gone through the checks to ensure CDI is not trying to inject a null,
 * InjectableNull objects will be converted back into regular nulls.
 * 
 */
public class InjectableNull {
//intentionally empty.
}

