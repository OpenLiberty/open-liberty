/*******************************************************************************
 * Copyright (c) 2030 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.jerseywithinjection;

import javax.enterprise.context.Dependent;

@Dependent
public class InjectableObject {

    public String getSomething() {
        return "Hello World!";
    }
}
