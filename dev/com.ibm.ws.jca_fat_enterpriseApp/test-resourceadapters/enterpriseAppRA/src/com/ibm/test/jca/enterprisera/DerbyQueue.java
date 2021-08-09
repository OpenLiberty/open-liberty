/*******************************************************************************
 * Copyright (c) 2014,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.jca.enterprisera;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;

import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;

public class DerbyQueue<E> implements java.util.Queue<E>, ResourceAdapterAssociation, Serializable {
    private static final long serialVersionUID = 2095531544389008212L;

    private transient DerbyResourceAdapter adapter;
    private String tableName;
    private String queueType;
    private int queueStart = 0;
    private int queueEnd = 0;

    public String getTableName() {
        return tableName;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (DerbyResourceAdapter) adapter;
        createTable();
    }

    public void setTableName(String tableName) throws SQLException {
        this.tableName = tableName;
        createTable();
    }

    public void setQueueType(String queueType) {
        this.queueType = queueType;
        createTable();
    }

    private void createTable() {
        // If not all config props are set yet, then no-op
        if (adapter == null || tableName == null || queueType == null)
            return;

        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                stmt.executeUpdate("create table " + tableName + "(index int not null primary key, value " + queueType + ")");
            } catch (SQLException x) { // ignore if table already exists
                try {
                    stmt.executeQuery("select id from " + tableName);
                } catch (SQLException x2) {
                    throw x;
                }
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public int size() {
        return queueEnd - queueStart;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        try {
            Statement stmt = adapter.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " WHERE value='" + o + "'");
            boolean contains = rs.next();
            rs.close();
            stmt.close();
            return contains;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                stmt.executeUpdate("delete from " + tableName);
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
        createTable();
        queueEnd = 0;
        queueStart = 0;
    }

    @Override
    public boolean add(E e) {
        try {
            Statement stmt = adapter.connection.createStatement();
            int updateCount = stmt.executeUpdate("INSERT INTO " + tableName + " values(" + queueEnd++ + ", '" + e + "')");
            stmt.close();
            if (updateCount != 1)
                return false;
            else
                return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean offer(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove() {
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                ResultSet result = stmt.executeQuery("select value from " + tableName + " where index=" + queueStart);
                @SuppressWarnings("unchecked")
                E value = result.next() ? (E) result.getObject(1) : null;
                result.close();
                stmt.executeUpdate("delete from " + tableName + " where index=" + queueStart++);
                return value;
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public E poll() {
        if (isEmpty())
            return null;
        return remove();
    }

    @Override
    public E element() {
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                ResultSet result = stmt.executeQuery("select value from " + tableName + " where index='" + queueStart + "'");
                @SuppressWarnings("unchecked")
                E value = result.next() ? (E) result.getObject(1) : null;
                return value;
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public E peek() {
        if (isEmpty())
            return null;
        return element();
    }
}
