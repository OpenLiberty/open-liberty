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
package com.ibm.ws.cdi12.test.implicitWar;

import javax.enterprise.context.RequestScoped;

import com.ibm.ws.cdi12.test.utils.SimpleAbstract;

/**
 * This bean has a bean-defining annotation, so this container should be considered to be a bean archive.
 */
@RequestScoped
public class AnnotatedBean extends SimpleAbstract {

}
