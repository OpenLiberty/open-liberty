/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb;

import javax.ejb.RemoveException;

/**
 * Remote business interface for Stateful Session bean for testing Remove
 * methods.
 **/
public interface RemoveCMTRemote {
    /** Return the String value state of Stateful bean. **/
    public String getString();

    public String remove(String string) throws TestAppException;

    public String remove_Required(String string) throws TestAppException;

    public String remove_RequiresNew(String string);

    public String remove_NotSupported(String string) throws TestAppException;

    public String remove_Supports(String string);

    public String remove_Mandatory(String string);

    public String remove_Never(String string);

    public String remove_retain_Required(String string) throws TestAppException;

    public String remove_retain_NotSupported(String string) throws TestAppException;

    public String remove_Required_RemoveEx() throws RemoveException;
}
