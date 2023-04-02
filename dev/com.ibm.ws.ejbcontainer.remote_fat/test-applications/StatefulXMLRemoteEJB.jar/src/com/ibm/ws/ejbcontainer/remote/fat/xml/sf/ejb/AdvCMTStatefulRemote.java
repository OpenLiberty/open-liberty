/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb;

/**
 * Remote interface for advanced Container Managed Transaction Stateful Session
 * bean.
 *
 * Intentionally has different methods than BasicCMTStatefulRemote to insure
 * transaction attributes are mapped correctly for all Local interface methods.
 **/
public interface AdvCMTStatefulRemote {
    public void adv_Tx_Mandatory();

    public void adv_Tx_NotSupported();

    public void tx_Default();

    public void tx_NotSupported();

    public void tx_RequiresNew();

    public void tx_Supports();

    public void tx_Never();

    public void tx_Mandatory();

    public void test_getBusinessObject(boolean businessInterface);
}
