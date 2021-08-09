/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.pmi.wire;

import java.util.*;

public interface WpdCollection extends java.io.Serializable {
    public static final long serialVersionUID = -5657823364948277461L;

    // return the name of the collection
    public String getName();

    // set the name of the collection - for server collection
    public void setName(String name);

    // return the type of the collection
    public int getType();

    // get the instrumentation level
    public int getLevel();

    // set the instrumentation level
    public void setLevel(int level);

    // return the collection in XML format
    public String toXML();

    // set data members
    public void setDataMembers(ArrayList dataMembers);

    // set subcollections
    public void setSubcollections(ArrayList subCollections);

    // return data members only
    public ArrayList dataMembers();

    // return subcollections only
    public ArrayList subCollections();

    // find a data member with given id
    public WpdData getData(int dataId);

    // find a subcollection with given name
    public WpdCollection getSubcollection(String name);

    // add a data member
    public boolean add(WpdData newMember);

    // add a subcollection
    public boolean add(WpdCollection newMember);

    // add a data member with given id
    public boolean remove(int dataId);

    // add a subcollection with given name
    public boolean remove(String name);

    public String toString();

    public String toString(String indent);
}
