/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.derbyra.resourceadapter;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.work.WorkException;
import javax.sql.DataSource;

/**
 * java.util.Map administered object
 */
public class DerbyMap<K, V> implements Map<K, V>, ResourceAdapterAssociation, Serializable {
    private static final long serialVersionUID = 1557430607598372401L;

    private transient DerbyResourceAdapter adapter;
    private String keyType;
    private String tableName;
    private String valueType;

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
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                return stmt.executeQuery("select value from " + tableName + " where id='" + key + "'").next();
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                return stmt.executeQuery("select id from " + tableName + " where value='" + value + "'").next();
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    private void createTable() {
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                stmt.executeUpdate("create table " + tableName + "(id " + keyType + " not null primary key, value " + valueType + ")");
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

    @SuppressWarnings("unchecked")
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        Map<K, V> map = new TreeMap<K, V>();
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                ResultSet result = stmt.executeQuery("select id, value from " + tableName);
                while (result.next())
                    map.put((K) result.getObject(1), (V) result.getObject(2));
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }

        return Collections.unmodifiableSet(map.entrySet());
    }

    @Override
    public V get(Object key) {
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                ResultSet result = stmt.executeQuery("select value from " + tableName + " where id='" + key + "'");
                @SuppressWarnings("unchecked")
                V value = result.next() ? (V) result.getObject(1) : null;
                return value;
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public String getKeyType() {
        return keyType;
    }

    public String getTableName() {
        return tableName;
    }

    public String getValueType() {
        return valueType;
    }

    @Override
    public boolean isEmpty() {
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                return !stmt.executeQuery("select id from " + tableName).next();
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<K> keySet() {
        Set<K> set = new TreeSet<K>();
        try {
            DataSource ds1 = adapter.lookup_ds1ref.call();
            Connection con = ds1.getConnection();
            try {
                Statement stmt = con.createStatement();
                ResultSet result = stmt.executeQuery("select id from " + tableName);
                while (result.next())
                    set.add((K) result.getObject(1));
            } finally {
                con.close();
            }
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }

        return Collections.unmodifiableSet(set);
    }

    @Override
    public V put(K key, V value) {
        if (value == null)
            throw new NullPointerException("value");
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                while (true) {
                    ResultSet result = stmt.executeQuery("select value from " + tableName + " where id='" + key + "'");
                    if (result.next()) {
                        @SuppressWarnings("unchecked")
                        V previous = (V) result.getObject(1);
                        int updateCount = stmt.executeUpdate("update " + tableName + " set value='" + value + "' where id='" + key + "' and value='" + previous + "'");
                        if (updateCount == 1) {
                            // Replacing a value can be a trigger for sending a message
                            for (DerbyActivationSpec as : adapter.activationSpecs)
                                if (key instanceof String && ((String) key).startsWith(as.keyPrefix))
                                    try {
                                        adapter.bootstrapContext.getWorkManager().scheduleWork(new DerbyMessageWork(as, key, value, previous));
                                    } catch (WorkException x) {
                                        x.printStackTrace();
                                    }
                            return previous;
                        }
                    } else
                        try {
                            int updateCount = stmt.executeUpdate("insert into " + tableName + " values ('" + key + "', '" + value + "')");
                            if (updateCount == 1)
                                return null;
                        } catch (SQLIntegrityConstraintViolationException x) {
                        }
                }
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                ResultSet result = stmt.executeQuery("select value from " + tableName + " where id='" + key + "'");
                if (result.next()) {
                    @SuppressWarnings("unchecked")
                    V value = (V) result.getObject(1);
                    int updateCount = stmt.executeUpdate("delete from " + tableName + " where id='" + key + "'");
                    return updateCount == 1 ? value : null;
                } else
                    return null;
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (DerbyResourceAdapter) adapter;
        if (adapter != null && tableName != null && keyType != null && valueType != null)
            createTable();
    }

    public void setKeyType(String keyType) throws SQLException {
        this.keyType = keyType;
        if (adapter != null && tableName != null && keyType != null && valueType != null)
            createTable();
    }

    public void setTableName(String tableName) throws SQLException {
        this.tableName = tableName;
        if (adapter != null && tableName != null && keyType != null && valueType != null)
            createTable();
    }

    public void setValueType(String valueType) throws SQLException {
        this.valueType = valueType;
        if (adapter != null && tableName != null && keyType != null && valueType != null)
            createTable();
    }

    @Override
    public int size() {
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                ResultSet result = stmt.executeQuery("select count(*) from " + tableName);
                return result.next() ? result.getInt(1) : 0;
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<V> values() {
        Collection<V> list = new LinkedList<V>();
        try {
            Statement stmt = adapter.connection.createStatement();
            try {
                ResultSet result = stmt.executeQuery("select value from " + tableName);
                while (result.next())
                    list.add((V) result.getObject(1));
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }

        return Collections.unmodifiableCollection(list);
    }
}
