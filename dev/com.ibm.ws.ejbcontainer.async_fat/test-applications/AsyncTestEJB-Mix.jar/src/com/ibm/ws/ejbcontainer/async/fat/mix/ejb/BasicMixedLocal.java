/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.mix.ejb;

/**
 * Local interface for Basic Container Managed Transaction Stateful
 * Session bean.
 **/
public interface BasicMixedLocal {
    public void test_asyncMethAnnWithStyle2XML(String param);

    public void test_asyncMethAnnOnly(String param);

    public void test_asyncMethStyle2XMLOnly(String param);

    public void test_syncMethod(String param);
}