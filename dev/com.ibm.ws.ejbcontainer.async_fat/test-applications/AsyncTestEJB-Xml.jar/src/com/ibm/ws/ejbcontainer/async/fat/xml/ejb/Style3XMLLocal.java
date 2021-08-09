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
package com.ibm.ws.ejbcontainer.async.fat.xml.ejb;

import java.util.concurrent.CountDownLatch;

/**
 * Local interface for Basic Container Managed Transaction Stateless
 * Session bean.
 **/
public interface Style3XMLLocal {
    public CountDownLatch getBeanLatch();

    public void test_xmlStyle3();

    public void test_xmlStyle3(String param);

    public void test_diffNameSameParam(String param);
}