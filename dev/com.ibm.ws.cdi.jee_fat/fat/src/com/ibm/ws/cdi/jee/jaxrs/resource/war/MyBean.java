/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.cdi.jee.jaxrs.resource.war;

import javax.enterprise.context.RequestScoped;

//This class does nothing and isn't injected into anywhere. It simply declares this
//whole archive to be an implicit bean archive by existing.
@RequestScoped
public class MyBean {

    public void doNothing() {}

}
