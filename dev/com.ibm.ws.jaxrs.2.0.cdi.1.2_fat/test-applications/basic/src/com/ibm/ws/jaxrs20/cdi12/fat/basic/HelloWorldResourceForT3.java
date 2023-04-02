/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.cdi12.fat.basic;

import javax.inject.Inject;
import javax.ws.rs.Path;

@Path("/helloworldt3")
public class HelloWorldResourceForT3 extends HelloWorldResource {

    @Inject
    public HelloWorldResourceForT3(Person person2) {
        super.setType("helloworldt3");
        this.setPerson(person2);
    }
}