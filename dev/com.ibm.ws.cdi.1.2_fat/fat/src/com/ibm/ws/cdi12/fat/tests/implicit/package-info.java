/**
 * Tests for loading beans from <strong>implicit bean archives</strong>. This includes implicit beans (<a
 * href="http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#bean_defining_annotations">CDI 1.2 Bean defining annotations</a>) and EJBs.
 * <p>
 * "An <strong>implicit bean archive</strong> is <em>any other archive</em> which contains one or more bean classes with a bean defining annotation as defined in Bean defining annotations, or one or more session beans."
 * - <a href="http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#bean_archive">CDI 1.2 Bean archives</a>
 * <p>
 * Where <em>any other archive</em> can be (for example) an archive with:
 * <ul>
 * <li>no beans.xml (and does not contain an extension), or,</li>
 * <li>a bean-discovery-mode of annotated</li>
 * </ul>
 * <p>
 * This is in contrast to an <strong>explicit bean archive</strong> which has:
 * <ul>
 * <li>a version number of 1.1 (or later), with the bean-discovery-mode of all, or,</li>
 * <li>no version number, or,</li>
 * <li>an empty file</li>
 * </ul>
 */
/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests.implicit;
