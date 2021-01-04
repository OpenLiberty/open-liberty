/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.helloworldra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.resource.cci.Record;

public class HelloWorldIndexedRecordImpl implements HelloWorldIndexedRecord {

    private ArrayList list = new ArrayList();
    private String name;
    private String description;

    /**
     * Constructor for HelloWorldIndexedRecordImpl
     */
    public HelloWorldIndexedRecordImpl() {

        super();
    }

    /**
     * @see Record#getRecordName()
     */
    @Override
    public String getRecordName() {

        return name;
    }

    /**
     * @see Record#setRecordName(String)
     */
    @Override
    public void setRecordName(String name) {

        this.name = name;
    }

    /**
     * @see Record#setRecordShortDescription(String)
     */
    @Override
    public void setRecordShortDescription(String description) {

        this.description = description;
    }

    /**
     * @see Record#getRecordShortDescription()
     */
    @Override
    public String getRecordShortDescription() {

        return description;
    }

    /**
     * @see List#size()
     */
    @Override
    public int size() {

        return list.size();
    }

    /**
     * @see List#isEmpty()
     */
    @Override
    public boolean isEmpty() {

        return list.isEmpty();
    }

    /**
     * @see List#contains(Object)
     */
    @Override
    public boolean contains(Object o) {

        return list.contains(o);
    }

    /**
     * @see List#iterator()
     */
    @Override
    public Iterator iterator() {

        return list.iterator();
    }

    /**
     * @see List#toArray()
     */
    @Override
    public Object[] toArray() {

        return list.toArray();
    }

    /**
     * @see List#toArray(Object[])
     */
    @Override
    public Object[] toArray(Object[] a) {

        return list.toArray(a);
    }

    /**
     * @see List#add(Object)
     */
    @Override
    public boolean add(Object o) {

        return list.add(o);
    }

    /**
     * @see List#remove(Object)
     */
    @Override
    public boolean remove(Object o) {

        return list.remove(o);
    }

    /**
     * @see List#containsAll(Collection)
     */
    @Override
    public boolean containsAll(Collection c) {

        return list.containsAll(c);
    }

    /**
     * @see List#addAll(Collection)
     */
    @Override
    public boolean addAll(Collection c) {

        return list.addAll(c);
    }

    /**
     * @see List#addAll(int, Collection)
     */
    @Override
    public boolean addAll(int index, Collection c) {

        return list.addAll(index, c);
    }

    /**
     * @see List#removeAll(Collection)
     */
    @Override
    public boolean removeAll(Collection c) {

        return list.removeAll(c);
    }

    /**
     * @see List#retainAll(Collection)
     */
    @Override
    public boolean retainAll(Collection c) {

        return list.retainAll(c);
    }

    /**
     * @see List#clear()
     */
    @Override
    public void clear() {

        list.clear();
    }

    /**
     * @see List#get(int)
     */
    @Override
    public Object get(int index) {

        return list.get(index);
    }

    /**
     * @see List#set(int, Object)
     */
    @Override
    public Object set(int index, Object o) {

        return list.set(index, o);
    }

    /**
     * @see List#add(int, Object)
     */
    @Override
    public void add(int index, Object o) {

        list.add(index, o);
    }

    /**
     * @see List#remove(int)
     */
    @Override
    public Object remove(int index) {

        return list.remove(index);
    }

    /**
     * @see List#indexOf(Object)
     */
    @Override
    public int indexOf(Object o) {

        return list.indexOf(o);
    }

    /**
     * @see List#lastIndexOf(Object)
     */
    @Override
    public int lastIndexOf(Object o) {

        return list.lastIndexOf(o);
    }

    /**
     * @see List#listIterator()
     */
    @Override
    public ListIterator listIterator() {

        return list.listIterator();
    }

    /**
     * @see List#listIterator(int)
     */
    @Override
    public ListIterator listIterator(int index) {

        return list.listIterator(index);
    }

    /**
     * @see List#subList(int, int)
     */
    @Override
    public List subList(int fromIndex, int toIndex) {

        return list.subList(fromIndex, toIndex);
    }

    /**
     * @see Record#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {

        throw new CloneNotSupportedException();
    }

}
