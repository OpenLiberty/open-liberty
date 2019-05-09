/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb;

/**
 * Remote interface for basic Container Managed Transaction Stateful Session
 * bean.
 **/
public interface BasicCMTStatefulRemote {
    public void tx_Default();

    public void tx_Required();

    public void tx_NotSupported();

    public void tx_RequiresNew();

    public void tx_Supports();

    public void tx_Never();

    public void tx_Mandatory();

    public void test_getBusinessObject(boolean businessInterface);
}
