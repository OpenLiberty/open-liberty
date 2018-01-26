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

package cdi12.helloworld.jeeResources.test;

import javax.ejb.Stateless;
import javax.inject.Inject;

import cdi12.helloworld.jeeResources.ejb.SessionBeanInterface;

@Stateless
public class MySessionBean implements SessionBeanInterface {

    @Inject
    HelloWorldExtensionBean2 bean;

}
