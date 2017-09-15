/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class CachedNamingEnumeration implements NamingEnumeration<SearchResult>, Cloneable {

    private List<SearchResult> iList = null;
    transient private Iterator<SearchResult> m_Enum = null;

    @Override
    public void close() throws NamingException {}

    CachedNamingEnumeration() {}

    CachedNamingEnumeration(List<SearchResult> list) {
        iList = list;
    }

    @Override
    @FFDCIgnore(CloneNotSupportedException.class)
    public Object clone() {
        try {
            super.clone();
        } catch (CloneNotSupportedException e) {
        }

		List<SearchResult> newList = null;
		if (iList != null) {
			newList = new Vector<SearchResult>(iList.size());
			for (SearchResult element : iList) {
				SearchResult result = (SearchResult) element;
				Attributes attrs = (Attributes) result.getAttributes().clone();
				SearchResult clonedResult = new SearchResult(result.getName(), null, null, attrs);
				newList.add(clonedResult);
			}
		}
		return new CachedNamingEnumeration(newList);
	}

    @Override
    public boolean hasMore() throws NamingException {
        if (iList == null) {
            iList = new Vector<SearchResult>(0);
        }
        if (m_Enum == null) {
            m_Enum = iList.iterator();
        }
        return m_Enum.hasNext();
    }

    @Override
    public SearchResult next() throws NamingException {
        if (iList == null) {
            iList = new Vector<SearchResult>(0);
        }
        if (m_Enum == null) {
            m_Enum = iList.iterator();
        }
        return m_Enum.next();
    }

    @Override
    @FFDCIgnore(NamingException.class)
    public boolean hasMoreElements() {
        try {
            return hasMore();
        } catch (NamingException e) {
            e.getMessage();
            return false;
        }
    }

    @Override
    @FFDCIgnore(NamingException.class)
    public SearchResult nextElement() {
        try {
            return next();
        } catch (NamingException e) {
            e.getMessage();
            return null;
        }
    }

    public void add(SearchResult elem) {
        if (iList == null) {
            iList = new Vector<SearchResult>(0);
        }
        iList.add(elem);
    }

    @Override
    public String toString() {
        if (iList != null) {
            return iList.toString();
        } else {
            return "";
        }

    }
}
