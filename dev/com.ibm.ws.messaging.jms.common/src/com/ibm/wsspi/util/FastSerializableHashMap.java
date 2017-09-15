/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
//477704 mcasile imports for FFDC facade


/**
 * <p>
 * An implementation of the map interface that only deserializes the objects
 * when they are requested.
 * </p>
 * 
 * <p>
 * build component: utils
 * </p>
 * 
 * @author nottinga
 * @version 1.4
 * @since 1.0
 */
public class FastSerializableHashMap implements Map, Externalizable
{
    /** The map to delegate onto. */
    private Map delegate = new java.util.HashMap();

    /** The source info for this class */
   // private static final String $ssccid = "

    /** The serialization uid */
    private static final long serialVersionUID = 8450249571767958267L;

    /**
     * <p>
     * An implementation of Map.Entry that delegates to another. It also
     * converts a ValueHolder to the value in the getValue method.
     * </p>
     */
    private static class EntryDelegator implements Map.Entry
    {
        /** The delegate Map.Entry */
        private Map.Entry _delegate;

        /* ---------------------------------------------------------------------- */
        /*
         * EntryDelegator method /*
         * ----------------------------------------------------------------------
         */
        /**
         * @param entry
         *            The entry.
         */
        public EntryDelegator(Map.Entry entry)
        {
            _delegate = entry;
        }

        /* ---------------------------------------------------------------------- */
        /*
         * getKey method /*
         * ----------------------------------------------------------------------
         */
        /**
         * @see java.util.Map.Entry#getKey()
         * @return the key
         */
        public Object getKey()
        {
            return _delegate.getKey();
        }

        /* ---------------------------------------------------------------------- */
        /*
         * getValue method /*
         * ----------------------------------------------------------------------
         */
        /**
         * @see java.util.Map.Entry#getValue()
         * @return the value
         */
        public Object getValue()
        {
            ValueHolder holder = (ValueHolder) _delegate.getValue();
            if (holder != null)
            {
                return holder.getValue();
            }
            else
            {
                return null;
            }
        }

        /* ---------------------------------------------------------------------- */
        /*
         * setValue method /*
         * ----------------------------------------------------------------------
         */
        /**
         * @see java.util.Map.Entry#setValue(java.lang.Object)
         * @param value
         * @return the previous value.
         */
        public Object setValue(Object value)
        {
            ValueHolder holder = (ValueHolder) _delegate.getValue();
            Object result = null;

            if (holder == null)
            {
                _delegate.setValue(new ValueHolder(value));
            }
            else
            {
                result = holder.getValue();
                holder.setValue(value);
            }

            return result;
        }
    }

    /**
     * <p>
     * This class holds the value in either object or serialized form. If it
     * contains a byte array it is converted on demand into the object form.
     * </p>
     */
    private static class ValueHolder implements Externalizable
    {
        /** The value as an object */
        private Object _value;

        /** The Serialized form of the object */
        private byte[] _valueBytes;

        /** The serialization uid */
        private static final long serialVersionUID = 6780206225209223097L;

        /* ---------------------------------------------------------------------- */
        /*
         * ValueHolder method /*
         * ----------------------------------------------------------------------
         */
        /**
         * Default Constructor.
         */
        public ValueHolder()
        {
        }

        /* ---------------------------------------------------------------------- */
        /*
         * ValueHolder method /*
         * ----------------------------------------------------------------------
         */
        /**
         * @param value
         *            The value to be held.
         */
        public ValueHolder(Object value)
        {
            _value = value;
        }

        /* ---------------------------------------------------------------------- */
        /*
         * setValue method /*
         * ----------------------------------------------------------------------
         */
        /**
         * @param obj
         *            The object to replace the current one.
         */
        public void setValue(Object obj)
        {
            _value = obj;
        }

        /* ---------------------------------------------------------------------- */
        /*
         * getValue method /*
         * ----------------------------------------------------------------------
         */
        /**
         * This method returns the value, deserializing if necessary.
         * 
         * @return the value
         */
        public Object getValue()
        {
            if (_value != null)
            {
                return _value;
            }
            else if (_value == null && _valueBytes != null)
            {
                // deserialize
                try
                {
                    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(_valueBytes));
                    _value = in.readObject();
                    return _value;
                }
                catch (Exception e)
                {
                  
                    return null;
                }
            }
            else
            {
                return null;
            }
        }

        /* ---------------------------------------------------------------------- */
        /*
         * writeExternal method /*
         * ----------------------------------------------------------------------
         */
        /**
         * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
         * @param out
         * @throws IOException
         */
        public void writeExternal(ObjectOutput out) throws IOException
        {
            // if the _value string write the U char, followed by the _value as
            // UTF
            if (_value instanceof String)
            {
                out.writeByte('U');
                out.writeUTF((String) _value);
            }
            else if (_value instanceof Byte)
            {
                out.writeByte('B');
                out.writeByte(((Byte) _value).byteValue());
            }
            else if (_value instanceof Short)
            {
                out.writeByte('S');
                out.writeShort(((Short) _value).shortValue());
            }
            else if (_value instanceof Character)
            {
                out.writeByte('C');
                out.writeChar(((Character) _value).charValue());
            }
            else if (_value instanceof Integer)
            {
                out.writeByte('I');
                out.writeInt(((Integer) _value).intValue());
            }
            else if (_value instanceof Long)
            {
                out.writeByte('J');
                out.writeLong(((Long) _value).longValue());
            }
            else if (_value instanceof Float)
            {
                out.writeByte('F');
                out.writeFloat(((Float) _value).floatValue());
            }
            else if (_value instanceof Double)
            {
                out.writeByte('D');
                out.writeDouble(((Double) _value).doubleValue());
            }
            else if (_value instanceof Boolean)
            {
                out.writeByte('Z');
                out.writeBoolean(((Boolean) _value).booleanValue());
            }
            // otherwise write o and then the object.
            else
            {
                if (_valueBytes == null)
                {
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                    ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
                    objOut.writeObject(_value);
                    _valueBytes = byteOut.toByteArray();
                }

                out.writeByte('L');
                out.writeInt(_valueBytes.length);
                out.write(_valueBytes);
            }
        }

        /* ---------------------------------------------------------------------- */
        /*
         * readExternal method /*
         * ----------------------------------------------------------------------
         */
        /**
         * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
         * @param in
         * @throws IOException
         * @throws ClassNotFoundException
         */
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            byte valueByte = in.readByte();
            // if the key byte is s then readUTF
            switch (valueByte)
            {
            // the key is a byte
            case 'B':
                _value = new Byte(in.readByte());
                break;
            // the _value is a char
            case 'C':
                _value = new Character(in.readChar());
                break;
            // the _value is a short
            case 'S':
                _value = new Short(in.readShort());
                break;
            // the _value is a int
            case 'I':
                _value = new Integer(in.readInt());
                break;
            // the _value is a long
            case 'J':
                _value = new Long(in.readLong());
                break;
            // the _value is a float
            case 'F':
                _value = new Float(in.readFloat());
                break;
            // the _value is a double
            case 'D':
                _value = new Double(in.readDouble());
                break;
            // the _value is a boolean
            case 'Z':
                _value = (in.readBoolean()) ? Boolean.TRUE : Boolean.FALSE;
                break;
            // the _value is a string
            case 'U':
                _value = in.readUTF();
                break;
            // the _value is a object
            case 'L':
                _valueBytes = new byte[in.readInt()];
                in.readFully(_valueBytes);
                break;
            default:
                throw new IOException("Unable to parse input stream");
            }
        }

        /* ---------------------------------------------------------------------- */
        /*
         * equals method /*
         * ----------------------------------------------------------------------
         */
        /**
         * @see java.lang.Object#equals(java.lang.Object)
         * @param obj
         * @return if this value equals the other.
         */
        public boolean equals(Object obj)
        {
            if (obj == null) return false;
            return obj.equals(getValue());
        }

        /* ---------------------------------------------------------------------- */
        /*
         * hashCode method /*
         * ----------------------------------------------------------------------
         */
        /**
         * @see java.lang.Object#hashCode()
         * @return the objects hashCode.
         */
        public int hashCode()
        {
            final Object value = getValue();
            final int hashCode;

            if (value == null)
            {
                hashCode = 0;
            }
            else
            {
                hashCode = value.hashCode();
            }

            return hashCode;
        }
    }

    /* ------------------------------------------------------------------------ */
    /*
     * size method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#size()
     * @return The size of the map.
     */
    public int size()
    {
        return delegate.size();
    }

    /* ------------------------------------------------------------------------ */
    /*
     * isEmpty method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#isEmpty()
     * @return true if and only if the map is empty.
     */
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    /* ------------------------------------------------------------------------ */
    /*
     * containsKey method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     * @param key
     *            The key to be checked
     * @return true if and only if their is a value in the map under the
     *         specified key.
     */
    public boolean containsKey(Object key)
    {
        return delegate.containsKey(key);
    }

    /* ------------------------------------------------------------------------ */
    /*
     * containsValue method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     * @param value
     *            The value to be checked.
     * @return true if and only if their is a value in the map matching the one
     *         passed in.
     */
    public boolean containsValue(Object value)
    {
        Iterator it = delegate.values().iterator();

        while (it.hasNext())
        {
            ValueHolder valueHolder = (ValueHolder) it.next();
            if (valueHolder.equals(value))
            {
                return true;
            }
        }

        return false;
    }

    /* ------------------------------------------------------------------------ */
    /*
     * get method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#get(java.lang.Object)
     * @param key
     *            The key.
     * @return the value associated with the specified key.
     */
    public Object get(Object key)
    {
        ValueHolder holder = (ValueHolder) delegate.get(key);
        if (holder == null)
        {
            return null;
        }
        else
        {
            return holder.getValue();
        }
    }

    /* ------------------------------------------------------------------------ */
    /*
     * put method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     * @param key
     *            The key.
     * @param value
     *            The value.
     * @return The previous object mapped in under the key.
     */
    public Object put(Object key, Object value)
    {
        ValueHolder valueHolder = (ValueHolder) delegate.put(key, new ValueHolder(value));
        if (valueHolder == null)
        {
            return null;
        }
        else
        {
            return valueHolder.getValue();
        }
    }

    /* ------------------------------------------------------------------------ */
    /*
     * remove method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#remove(java.lang.Object)
     * @param key
     *            The key for which the value should be removed.
     * @return The value removed.
     */
    public Object remove(Object key)
    {
        ValueHolder holder = (ValueHolder) delegate.remove(key);
        if (holder == null)
        {
            return null;
        }
        else
        {
            return holder.getValue();
        }
    }

    /* ------------------------------------------------------------------------ */
    /*
     * putAll method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#putAll(java.util.Map)
     * @param t
     *            another map.
     */
    public void putAll(Map t)
    {
        Iterator entries = t.entrySet().iterator();

        while (entries.hasNext())
        {
            Map.Entry entry = (Map.Entry) entries.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    /* ------------------------------------------------------------------------ */
    /*
     * clear method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#clear()
     */
    public void clear()
    {
        delegate.clear();
    }

    /* ------------------------------------------------------------------------ */
    /*
     * keySet method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#keySet()
     * @return the set of keys.
     */
    public Set keySet()
    {
        return delegate.keySet();
    }

    /* ------------------------------------------------------------------------ */
    /*
     * values method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#values()
     * @return the values in the map.
     */
    public Collection values()
    {
        Collection c = delegate.values();
        Collection newC = new ArrayList(c.size());
        Iterator it = c.iterator();
        while (it.hasNext())
        {
            ValueHolder holder = (ValueHolder) it.next();
            newC.add(holder.getValue());
        }

        return newC;
    }

    /* ------------------------------------------------------------------------ */
    /*
     * entrySet method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#entrySet()
     * @return The entries in the map.
     */
    public Set entrySet()
    {
        Iterator entries = delegate.entrySet().iterator();
        Set newEntries = new HashSet();

        while (entries.hasNext())
        {
            Map.Entry entry = (Map.Entry) entries.next();
            newEntries.add(new EntryDelegator(entry));
        }

        return newEntries;
    }

    /* ------------------------------------------------------------------------ */
    /*
     * toString method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.lang.Object#toString()
     * @return a printable string.
     */
    public String toString()
    {
        return delegate.toString();
    }

    /* ------------------------------------------------------------------------ */
    /*
     * equals method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#equals(java.lang.Object)
     * @param other
     * @return whether the two maps are equal.
     */
    public boolean equals(Object other)
    {
        return delegate.equals(other);
    }

    /* ------------------------------------------------------------------------ */
    /*
     * hashCode method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @see java.util.Map#hashCode()
     * @return the hashCode.
     */
    public int hashCode()
    {
        return delegate.hashCode();
    }

    /* ------------------------------------------------------------------------ */
    /*
     * writeExternal method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @param out
     *            the stream to write the state out to.
     * @throws IOException
     *             If the stream cannot be written to.
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeLong(serialVersionUID);
        Iterator it = delegate.entrySet().iterator();
        while (it.hasNext())
        {
            // Get the next entry
            Map.Entry entry = (Map.Entry) it.next();
            // get the key
            Object key = entry.getKey();
            // if the key string write the U char, followed by the key as UTF
            if (key instanceof String)
            {
                out.writeByte('U');
                out.writeUTF((String) key);
            }
            else if (key instanceof Byte)
            {
                out.writeByte('B');
                out.writeByte(((Byte) key).byteValue());
            }
            else if (key instanceof Short)
            {
                out.writeByte('S');
                out.writeShort(((Short) key).shortValue());
            }
            else if (key instanceof Character)
            {
                out.writeByte('C');
                out.writeChar(((Character) key).charValue());
            }
            else if (key instanceof Integer)
            {
                out.writeByte('I');
                out.writeInt(((Integer) key).intValue());
            }
            else if (key instanceof Long)
            {
                out.writeByte('J');
                out.writeLong(((Long) key).longValue());
            }
            else if (key instanceof Float)
            {
                out.writeByte('F');
                out.writeFloat(((Float) key).floatValue());
            }
            else if (key instanceof Double)
            {
                out.writeByte('D');
                out.writeDouble(((Double) key).doubleValue());
            }
            else if (key instanceof Boolean)
            {
                out.writeByte('Z');
                out.writeBoolean(((Boolean) key).booleanValue());
            }
            // otherwise write o and then the object.
            else
            {
                out.writeByte('L');
                out.writeObject(key);
            }
            ValueHolder holder = (ValueHolder) entry.getValue();
            holder.writeExternal(out);
        }
        out.writeByte('e');
    }

    /* ------------------------------------------------------------------------ */
    /*
     * readExternal method /*
     * ------------------------------------------------------------------------
     */
    /**
     * @param in
     *            the stream to read the object state from.
     * @throws IOException
     *             If the stream cannot be read
     * @throws ClassNotFoundException
     *             if a Class cannot be found.
     * @see Externalizable#readExternal(ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        in.readLong();

        byte keyByte = in.readByte();
        while (keyByte != 'e')
        {
            Object key;
            // if the key byte is s then readUTF
            switch (keyByte)
            {
            // the key is a byte
            case 'B':
                key = new Byte(in.readByte());
                break;
            // the key is a char
            case 'C':
                key = new Character(in.readChar());
                break;
            // the key is a short
            case 'S':
                key = new Short(in.readShort());
                break;
            // the key is a int
            case 'I':
                key = new Integer(in.readInt());
                break;
            // the key is a long
            case 'J':
                key = new Long(in.readLong());
                break;
            // the key is a float
            case 'F':
                key = new Float(in.readFloat());
                break;
            // the key is a double
            case 'D':
                key = new Double(in.readDouble());
                break;
            // the key is a boolean
            case 'Z':
                key = (in.readBoolean()) ? Boolean.TRUE : Boolean.FALSE;
                break;
            // the key is a string
            case 'U':
                key = in.readUTF();
                break;
            // the key is a object
            case 'L':
                key = in.readObject();
                break;
            default:
                throw new IOException("Unable to parse input stream");
            }

            ValueHolder holder = new ValueHolder();
            holder.readExternal(in);

            delegate.put(key, holder);

            keyByte = in.readByte();
        }
    }
}
