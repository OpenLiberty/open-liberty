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
package com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb;

import javax.ejb.RemoveException;

/**
 * Remote business interface for Stateful Session bean for testing Remove
 * methods.
 **/
public interface RemoveBMTRemote {
    /** Begins a 'sticky' global transaction. **/
    public String begin(String string);

    /** Commits a 'sticky' global transaction. **/
    public String commit(String string);

    /** Rolls back a 'sticky' global transaction. **/
    public String rollback(String string);

    /** Return the String value state of Stateful bean - no tx change. **/
    public String getString();

    public String remove(int x, int y);

    public String remove(String string) throws TestAppException;

    public String remove_begin(String string);

    public String remove_commit(String string);

    public String remove_rollback(String string);

    public String remove_Transaction(String string);

    public String remove_retain(String string) throws TestAppException;

    public String remove_RemoveEx() throws RemoveException;

}
