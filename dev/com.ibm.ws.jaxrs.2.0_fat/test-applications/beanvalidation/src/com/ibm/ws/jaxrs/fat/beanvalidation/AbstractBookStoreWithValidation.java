/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.beanvalidation;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

public abstract class AbstractBookStoreWithValidation {
    @Valid
    public abstract BookWithValidation getBook(@Pattern(regexp = "\\d+") String id);
}
