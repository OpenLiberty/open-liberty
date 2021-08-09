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
package com.ibm.ws.cdi.beansxml.implicit.apps.beans;

import javax.enterprise.context.Dependent;

import com.ibm.ws.cdi.beansxml.implicit.utils.SimpleAbstract;

/**
 * This bean is in an archive with {@code bean-discovery-mode=all}. This is an <em>explicit</em> bean archive.
 */
@Dependent
public class InExplicitBeanArchive extends SimpleAbstract {}
