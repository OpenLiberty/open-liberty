/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.admin.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ConfigurationDictionary;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * A helper class for storing and retrieving the persisted config information.
 *
 * The format is:
 *
 * byte - version, set to 0
 * utf - bundleLocation
 * int - number of unique variables
 * utf - unique variables, write the number of entries dictated by prior
 * int - number of config references
 * configIDs - the config ids written parent to child in order.
 * int - optionaly. Written if this a parent to indicate how many entries in this tree
 * utf - the id
 * utf - the pid
 * utf - the child attribute
 * int - an index to which config id is the parent. If an entry is -1 it means it has no parent.
 * int - the number of entries in the config dictionary
 * entries - the entries in the config dictionary
 * utf - the name of the entry
 * int - the type of the entry
 * <value> - the value. How this is written depends on whether it is primitive, collection, array or map.
 * - primitives are written directly
 * - collections have their size written followed by each entry
 * - arrays have their size written followed by each entry
 * - maps are written the same way as the main config dictionary
 */
public class ConfigurationStorageHelper {

    private static final int VERSION = 0;
    private static final int BYTE = 0;
    private static final int SHORT = 1;
    private static final int BOOLEAN = 2;
    private static final int CHAR = 3;
    private static final int DOUBLE = 4;
    private static final int LONG = 5;
    private static final int FLOAT = 6;
    private static final int INTEGER = 7;
    private static final int STRING = 8;
    private static final int PROTECTED_STRING = 9;
    private static final int COLLECTION = 10;
    private static final int MAP = 11;
    private static final int ARRAY = 12;
    private static final int ONERROR = 13;

    public static String load(File configFile, Set<String> uniqueVars, Set<ConfigID> references, ConfigurationDictionary dict) throws IOException {
        try (DataInputStream dos = new DataInputStream(new BufferedInputStream(new FileInputStream(configFile)))) {

            if (dos.readByte() == VERSION) {
                String location = null;
                if (dos.readBoolean()) {
                    location = dos.readUTF();
                }
                int len = dos.readInt();
                for (int i = 0; i < len; i++) {
                    uniqueVars.add(dos.readUTF());
                }
                len = dos.readInt();
                List<ConfigID> ids = new ArrayList<>();
                for (int i = 0, j = 0, cursor = 0; i < len + cursor; i++, j++) {
                    if (cursor == 0) {
                        cursor = dos.readInt();
                        j = 0;
                    }
                    String id = null;
                    if (dos.readBoolean()) {
                        id = dos.readUTF();
                    }
                    String configPid = dos.readUTF();
                    String child = null;
                    if (dos.readBoolean()) {
                        child = dos.readUTF();
                    }
                    int parentId = dos.readInt();
                    ConfigID parent = null;
                    if (parentId > -1) {
                        parent = ids.get(parentId);
                    }
                    ConfigID thisId = new ConfigID(parent, configPid, id, child);
                    ids.add(thisId);

                    if (j == cursor) {
                        len = len + cursor;
                        cursor = 0;
                        references.add(thisId);
                    }
                }
                readMap(dos, toMapOrDictionary(dict));
                return location;
            } else {
                // TODO work out if this is safe to just ignore the persisted data.
            }

            return null;
        }
    }

    public static void store(File configFile, Dictionary<String, ?> properties, String bundleLocation,
                             Set<ConfigID> references, Set<String> uniqueVars) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(configFile, false)))) {

            dos.writeByte(VERSION);
            writePossiblyNullString(dos, bundleLocation);
            if (uniqueVars != null) {
                dos.writeInt(uniqueVars.size());
                for (String var : uniqueVars) {
                    dos.writeUTF(var);
                }
            } else {
                dos.writeInt(0);
            }
            if (references != null) {
                dos.writeInt(references.size());
                int count = 0;
                Map<ConfigID, Integer> writtenConfigIds = new HashMap<>();
                for (ConfigID id : references) {
                    count = writeConfigID(dos, id, count, 0, writtenConfigIds);
                }
            } else {
                dos.writeInt(0);
            }
            writeMap(dos, toMapOrDictionary(properties));
            dos.flush();
        }
    }

    private static void readMap(DataInputStream dis, MapIterable props) throws IOException {
        int size = dis.readInt();
        for (int i = 0; i < size; i++) {
            String key = dis.readUTF();
            Object value = readValue(dis);
            props.put(key, value);
        }
    }

    private static Object readValue(DataInputStream dis) throws IOException {
        byte type = dis.readByte();
        Object value;

        if (type == COLLECTION) {
            value = readCollection(dis);
        } else if (type == ARRAY) {
            value = readArray(dis);
        } else if (type == MAP) {
            Map<String, Object> map = new HashMap<>();
            value = map;
            readMap(dis, toMapOrDictionary(map));
        } else {
            value = readPrimative(dis, type);
        }

        return value;
    }

    // TODO how to handle null values?
    private static Object readArray(DataInputStream dis) throws IOException {
        Object value;
        byte type = dis.readByte();
        boolean primitive = dis.readBoolean();
        int size = dis.readInt();
        if (type == BYTE) {
            value = primitive ? readPByteArray(dis, size) : readByteArray(dis, size);
        } else if (type == SHORT) {
            value = primitive ? readPShortArray(dis, size) : readShortArray(dis, size);
        } else if (type == BOOLEAN) {
            value = primitive ? readPBooleanArray(dis, size) : readBooleanArray(dis, size);
        } else if (type == CHAR) {
            value = primitive ? readCharArray(dis, size) : readCharacterArray(dis, size);
        } else if (type == DOUBLE) {
            value = primitive ? readPDoubleArray(dis, size) : readDoubleArray(dis, size);
        } else if (type == LONG) {
            value = primitive ? readPLongArray(dis, size) : readLongArray(dis, size);
        } else if (type == FLOAT) {
            value = primitive ? readPFloatArray(dis, size) : readFloatArray(dis, size);
        } else if (type == INTEGER) {
            value = primitive ? readIntArray(dis, size) : readIntegerArray(dis, size);
        } else if (type == STRING) {
            value = readStringArray(dis, size);
        } else if (type == PROTECTED_STRING) {
            value = readProtectedStringArray(dis, size);
        } else if (type == ONERROR) {
            value = readOnErrorArray(dis, size);
        } else {
            value = null;
        }

        return value;
    }

    private static Object readPByteArray(DataInputStream dis, int size) throws IOException {
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readByte();
        }
        return result;
    }

    private static Object readByteArray(DataInputStream dis, int size) throws IOException {
        Byte[] result = new Byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readByte();
        }
        return result;
    }

    private static Object readPShortArray(DataInputStream dis, int size) throws IOException {
        short[] result = new short[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readShort();
        }
        return result;
    }

    private static Object readShortArray(DataInputStream dis, int size) throws IOException {
        Short[] result = new Short[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readShort();
        }
        return result;
    }

    private static Object readPBooleanArray(DataInputStream dis, int size) throws IOException {
        boolean[] result = new boolean[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readBoolean();
        }
        return result;
    }

    private static Object readBooleanArray(DataInputStream dis, int size) throws IOException {
        Boolean[] result = new Boolean[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readBoolean();
        }
        return result;
    }

    private static Object readCharArray(DataInputStream dis, int size) throws IOException {
        char[] result = new char[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readChar();
        }
        return result;
    }

    private static Object readCharacterArray(DataInputStream dis, int size) throws IOException {
        Character[] result = new Character[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readChar();
        }
        return result;
    }

    private static Object readPDoubleArray(DataInputStream dis, int size) throws IOException {
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readDouble();
        }
        return result;
    }

    private static Object readDoubleArray(DataInputStream dis, int size) throws IOException {
        Double[] result = new Double[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readDouble();
        }
        return result;
    }

    private static Object readPLongArray(DataInputStream dis, int size) throws IOException {
        long[] result = new long[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readLong();
        }
        return result;
    }

    private static Object readLongArray(DataInputStream dis, int size) throws IOException {
        Long[] result = new Long[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readLong();
        }
        return result;
    }

    private static Object readPFloatArray(DataInputStream dis, int size) throws IOException {
        float[] result = new float[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readFloat();
        }
        return result;
    }

    private static Object readFloatArray(DataInputStream dis, int size) throws IOException {
        Float[] result = new Float[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readFloat();
        }
        return result;
    }

    private static Object readIntArray(DataInputStream dis, int size) throws IOException {
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readInt();
        }
        return result;
    }

    private static Object readIntegerArray(DataInputStream dis, int size) throws IOException {
        Integer[] result = new Integer[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readInt();
        }
        return result;
    }

    private static Object readStringArray(DataInputStream dis, int size) throws IOException {
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readUTF();
        }
        return result;
    }

    private static Object readProtectedStringArray(DataInputStream dis, int size) throws IOException {
        SerializableProtectedString[] result = new SerializableProtectedString[size];
        for (int i = 0; i < size; i++) {
            result[i] = new SerializableProtectedString(dis.readUTF().toCharArray());
        }
        return result;
    }

    private static Object readOnErrorArray(DataInputStream dis, int size) throws IOException {
        OnError[] result = new OnError[size];
        OnError[] values = OnError.values();
        for (int i = 0; i < size; i++) {
            result[i] = values[dis.readInt()];
        }
        return result;
    }

    private static Object readPrimative(DataInputStream dis, int type) throws IOException {
        Object value;
        if (type == BYTE) {
            value = dis.readByte();
        } else if (type == SHORT) {
            value = dis.readShort();
        } else if (type == BOOLEAN) {
            value = dis.readBoolean();
        } else if (type == CHAR) {
            value = dis.readChar();
        } else if (type == DOUBLE) {
            value = dis.readDouble();
        } else if (type == LONG) {
            value = dis.readLong();
        } else if (type == FLOAT) {
            value = dis.readFloat();
        } else if (type == INTEGER) {
            value = dis.readInt();
        } else if (type == STRING) {
            value = dis.readUTF();
        } else if (type == PROTECTED_STRING) {
            value = new SerializableProtectedString(dis.readUTF().toCharArray());
        } else if (type == ONERROR) {
            int ordinal = dis.readInt();
            value = OnError.values()[ordinal];
        } else {
            value = null;
        }

        return value;
    }

    private static Collection<?> readCollection(DataInputStream dis) throws IOException {
        List<Object> result = new ArrayList<>();

        int len = dis.readInt();
        int type = dis.readByte();

        for (int i = 0; i < len; i++) {
            result.add(readPrimative(dis, type));
        }

        return result;
    }

    private static void writeMap(DataOutputStream dos, MapIterable map) throws IOException {
        dos.writeInt(map.size());
        for (Map.Entry<String, Object> entry : map) {
            dos.writeUTF(entry.getKey());
            Object obj = entry.getValue();
            if (obj instanceof Collection) {
                dos.writeByte(COLLECTION);
                Collection<?> data = (Collection<?>) obj;
                dos.writeInt(data.size());
                boolean writeType = true;
                for (Object colObj : data) {
                    // write primative.
                    if (writeType) {
                        writePrimitiveType(dos, colObj.getClass());
                        writeType = false;
                    }
                    writePrimitive(dos, colObj);
                }
            } else if (obj instanceof Map) {
                dos.writeByte(MAP);
                writeMap(dos, toMapOrDictionary(obj));
            } else if (obj.getClass().isArray()) {
                dos.writeByte(ARRAY);
                Class<?> arrayType = obj.getClass();
                arrayType = arrayType.getComponentType();
                writePrimitiveType(dos, arrayType);
                dos.writeBoolean(arrayType.isPrimitive());
                int len = Array.getLength(obj);
                dos.writeInt(len);
                for (int i = 0; i < len; i++) {
                    writePrimitive(dos, Array.get(obj, i));
                }
            } else {
                writePrimitiveType(dos, obj.getClass());
                writePrimitive(dos, obj);
            }
        }
    }

    private static MapIterable toMapOrDictionary(Object obj) {
        if (obj instanceof Dictionary) {
            return new DictionaryMapIterableImpl((Dictionary<String, Object>) obj);
        } else if (obj instanceof Map) {
            return new MapIterableImpl((Map<String, Object>) obj);
        }
        return null;
    }

    private static boolean writePrimitiveType(DataOutputStream dos, Class<?> type) throws IOException {
        if (type == Byte.class || type == byte.class) {
            dos.writeByte(BYTE);
        } else if (type == Short.class || type == short.class) {
            dos.writeByte(SHORT);
        } else if (type == Boolean.class || type == boolean.class) {
            dos.writeByte(BOOLEAN);
        } else if (type == Character.class || type == char.class) {
            dos.writeByte(CHAR);
        } else if (type == Double.class || type == double.class) {
            dos.writeByte(DOUBLE);
        } else if (type == Long.class || type == long.class) {
            dos.writeByte(LONG);
        } else if (type == Float.class || type == float.class) {
            dos.writeByte(FLOAT);
        } else if (type == Integer.class || type == int.class) {
            dos.writeByte(INTEGER);
        } else if (type == String.class) {
            dos.writeByte(STRING);
        } else if (type == SerializableProtectedString.class) {
            dos.writeByte(PROTECTED_STRING);
        } else if (type == OnError.class) {
            dos.writeByte(ONERROR);
        } else {
            return false;
        }
        return true;
    }

    private static boolean writePrimitive(DataOutputStream dos, Object obj) throws IOException {
        if (obj instanceof Byte) {
            dos.writeByte((byte) obj);
        } else if (obj instanceof Short) {
            dos.writeShort((short) obj);
        } else if (obj instanceof Boolean) {
            dos.writeBoolean((boolean) obj);
        } else if (obj instanceof Character) {
            dos.writeChar((char) obj);
        } else if (obj instanceof Double) {
            dos.writeDouble((double) obj);
        } else if (obj instanceof Long) {
            dos.writeLong((long) obj);
        } else if (obj instanceof Float) {
            dos.writeFloat((float) obj);
        } else if (obj instanceof Integer) {
            dos.writeInt((int) obj);
        } else if (obj instanceof String) {
            dos.writeUTF((String) obj);
        } else if (obj instanceof SerializableProtectedString) {
            dos.writeUTF(new String(((SerializableProtectedString) obj).getChars()));
        } else if (obj instanceof OnError) {
            dos.writeInt(((OnError) obj).ordinal());
        } else {
            return false;
        }
        return true;
    }

    private static int writeConfigID(DataOutputStream dos, ConfigID id, int count,
                                     int depth, Map<ConfigID, Integer> writtenConfigIds) throws IOException {
        ConfigID parent = id.getParent();
        int parentIndex = -1;

        if (parent != null) {
            Integer index = writtenConfigIds.get(id.getParent());
            if (index == null) {
                parentIndex = count = writeConfigID(dos, parent, count, depth + 1, writtenConfigIds);
            } else {
                parentIndex = index;
            }
        } else {
            dos.writeInt(depth);
        }

        writePossiblyNullString(dos, id.getId());
        dos.writeUTF(id.getPid());
        writePossiblyNullString(dos, id.getChildAttribute());
        dos.writeInt(parentIndex - 1);

        return count + 1;
    }

    private static void writePossiblyNullString(DataOutputStream dos, String value) throws IOException {
        if (value != null) {
            dos.writeBoolean(true);
            dos.writeUTF(value);
        } else {
            dos.writeBoolean(false);
        }
    }

    private static interface MapIterable extends Iterable<Map.Entry<String, Object>> {
        public int size();

        @Override
        public Iterator<Map.Entry<String, Object>> iterator();

        public void put(String name, Object val);
    }

    private static class MapIterableImpl implements MapIterable {
        private final Map<String, Object> map;

        public MapIterableImpl(Map<String, Object> map) {
            this.map = map;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public Iterator<Map.Entry<String, Object>> iterator() {
            return map.entrySet().iterator();
        }

        @Override
        public void put(String name, Object val) {
            map.put(name, val);
        }
    }

    private static class DictionaryMapIterableImpl implements MapIterable {
        private final Dictionary<String, Object> dict;

        public DictionaryMapIterableImpl(Dictionary<String, Object> dict) {
            this.dict = dict;
        }

        private class MyIterator implements Iterator<Map.Entry<String, Object>> {
            Enumeration<String> keys = dict.keys();

            @Override
            public boolean hasNext() {
                return keys.hasMoreElements();
            }

            @Override
            public Map.Entry<String, Object> next() {
                String key = keys.nextElement();

                return new SimpleEntry<String, Object>(key, dict.get(key));
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public int size() {
            return dict.size();
        }

        @Override
        public Iterator<Map.Entry<String, Object>> iterator() {
            return new MyIterator();
        }

        @Override
        public void put(String name, Object val) {
            dict.put(name, val);
        }
    }
}