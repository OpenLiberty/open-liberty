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

/**
 * Remote business interface for Stateful Session bean for testing Remove
 * methods.
 **/
public interface RemoveRemote {
    /** Return the String value state of Stateful bean. **/
    public String getString();

    public String remove(String string) throws TestAppException;

    public String removeUnique(String string);
}
