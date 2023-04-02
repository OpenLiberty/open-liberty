/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.async.fat.mix.ejb;

/**
 * Local interface for Basic Container Managed Transaction Stateful
 * Session bean.
 **/
public interface InheritanceXMLLocal {
    public void test_beanMethodAsync(String param);

    public void test_AnnAndXMLMethodAsync(String param);

    public void test_inheritance();
}