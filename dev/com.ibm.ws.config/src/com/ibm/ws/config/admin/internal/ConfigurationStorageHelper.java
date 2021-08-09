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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ConfigurationDictionary;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * A helper class for storing and retrieving the persisted config information.
 * <p>
 * {@link #load(File, ConfigStorageConsumer)} / {@link #store(File, Collection)} format: <br>
 * byte - version, set to constant {@link #VERSION} <br>
 * int - number of configurations <br>
 * followed by the list of conditions as loaded by {@link #load(DataInputStream, Set, Set, ConfigurationDictionary)}
 * <p>
 * {@link #load(DataInputStream, Set, Set, ConfigurationDictionary)} / {@link #store(DataOutputStream, Dictionary, String, Set, Set)} format: <br>
 * utf - bundleLocation <br>
 * int - number of unique variables <br>
 * utf - unique variables, write the number of entries dictated by prior <br>
 * int - number of config references <br>
 * configIDs - the config ids written parent to child in order. <br>
 * int - optionaly. Written if this a parent to indicate how many entries in this tree <br>
 * utf - the id <br>
 * utf - the pid <br>
 * utf - the child attribute <br>
 * int - an index to which config id is the parent. If an entry is -1 it means it has no parent. <br>
 * int - the number of entries in the config dictionary <br>
 * entries - the entries in the config dictionary <br>
 * utf - the name of the entry <br>
 * int - the type of the entry <br>
 * <value> - the value. How this is written depends on whether it is primitive, collection, array or map. <br>
 * - primitives are written directly <br>
 * - collections have their size written followed by each entry <br>
 * - arrays have their size written followed by each entry <br>
 * - maps are written the same way as the main config dictionary <br>
 */
public class ConfigurationStorageHelper {

    public static interface ConfigStorageConsumer<K, T> {
        T consumeConfigData(String location, Set<String> uniqueVars, Set<ConfigID> references, ConfigurationDictionary dict);

        K getKey(T consumerType);
    }

    private static final byte VERSION = 0;
    private static final byte BYTE = 0;
    private static final byte SHORT = 1;
    private static final byte BOOLEAN = 2;
    private static final byte CHAR = 3;
    private static final byte DOUBLE = 4;
    private static final byte LONG = 5;
    private static final byte FLOAT = 6;
    private static final byte INTEGER = 7;
    private static final byte STRING = 8;
    private static final byte PROTECTED_STRING = 9;
    private static final byte COLLECTION = 10;
    private static final byte MAP = 11;
    private static final byte ARRAY = 12;
    private static final byte ONERROR = 13;
    private static final byte NULL = 14;
    private static final byte OBJECT = 15;

    public static <K, T> Map<K, T> load(File configFile, ConfigStorageConsumer<K, T> consumer) throws IOException {
        Map<K, T> result = new HashMap<>();
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(configFile)))) {
            if (dis.readByte() == VERSION) {
                int numConfigs = dis.readInt();
                for (int i = 0; i < numConfigs; i++) {
                    Set<String> uniqueVars = new HashSet<>();
                    Set<ConfigID> references = new HashSet<>();
                    ConfigurationDictionary dict = new ConfigurationDictionary();
                    String location = load(dis, uniqueVars, references, dict);
                    T value = consumer.consumeConfigData(location, uniqueVars, references, dict);
                    result.put(consumer.getKey(value), value);
                }
            }
        }
        return result;
    }

    static String load(DataInputStream dis, Set<String> uniqueVars, Set<ConfigID> references, ConfigurationDictionary dict) throws IOException {
        String location = (String) readObject(dis);
        int len = dis.readInt();
        for (int i = 0; i < len; i++) {
            uniqueVars.add(dis.readUTF());
        }
        len = dis.readInt();
        List<ConfigID> ids = new ArrayList<>();
        for (int i = 0, j = 0, cursor = 0; i < len + cursor; i++, j++) {
            if (cursor == 0) {
                cursor = dis.readInt();
                j = 0;
            }
            String id = (String) readObject(dis);
            String configPid = dis.readUTF();
            String child = (String) readObject(dis);
            int parentId = dis.readInt();
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
        readMapInternal(dis, toMapOrDictionary(dict));
        return location;

    }

    @FFDCIgnore(IllegalStateException.class)
    public static void store(File configFile, Collection<? extends ExtendedConfiguration> configs) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(configFile, false)))) {
            dos.writeByte(VERSION);
            // This secondary list is needed so we can collect the valid number of configs:
            // non-null properties and not deleted (indicated with an IllegalStateException)
            List<Object[]> configDatas = new ArrayList<>(configs.size());
            for (ExtendedConfiguration config : configs) {
                try {
                    Dictionary<String, ?> properties = config.getReadOnlyProperties();
                    Set<ConfigID> references = config.getReferences();
                    Set<String> uniqueVars = config.getUniqueVariables();
                    String bundleLocation = config.getBundleLocation();
                    if (properties != null) {
                        configDatas.add(new Object[] { properties, references, uniqueVars, bundleLocation });
                    }
                } catch (IllegalStateException e) {
                    // ignore FFDC
                }
            }
            // now we have all the valid configs and their dictionaries to save
            dos.writeInt(configDatas.size());
            for (Object[] configData : configDatas) {
                @SuppressWarnings("unchecked")
                Dictionary<String, ?> properties = (Dictionary<String, ?>) configData[0];
                @SuppressWarnings("unchecked")
                Set<ConfigID> references = (Set<ConfigID>) configData[1];
                @SuppressWarnings("unchecked")
                Set<String> uniqueVars = (Set<String>) configData[2];
                String bundleLocation = (String) configData[3];
                store(dos, properties, bundleLocation, references, uniqueVars);
            }
        }
    }

    static void store(DataOutputStream dos, Dictionary<String, ?> properties, String bundleLocation,
                      Set<ConfigID> references, Set<String> uniqueVars) throws IOException {

        writeObject(dos, bundleLocation);
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
        writeMapInternal(dos, toMapOrDictionary(properties));
    }

    public static void readMap(DataInputStream dis, MapIterable props) throws IOException {
        if (dis.readByte() != VERSION) {
            return;
        }
        readMapInternal(dis, props);
    }

    private static void readMapInternal(DataInputStream dis, MapIterable props) throws IOException {
        int size = dis.readInt();
        for (int i = 0; i < size; i++) {
            String key = dis.readUTF();
            Object value = readMapValue(dis);
            props.put(key, value);
        }
    }

    private static Object readMapValue(DataInputStream dis) throws IOException {
        byte type = dis.readByte();
        Object value;

        if (type == COLLECTION) {
            value = readCollection(dis);
        } else if (type == ARRAY) {
            value = readArray(dis);
        } else if (type == MAP) {
            Map<String, Object> map = new HashMap<>();
            value = map;
            readMapInternal(dis, toMapOrDictionary(map));
        } else if (type == OBJECT) {
            value = readObject(dis);
        } else {
            throw new IllegalArgumentException("Unsupported map value type: " + type);
        }

        return value;
    }

    // TODO how to handle null values?
    private static Object readArray(DataInputStream dis) throws IOException {
        Object value;
        boolean primitive = dis.readBoolean();
        byte type = dis.readByte();
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
            throw new IllegalArgumentException("Unsupported type for array: " + type);
        }

        return value;
    }

    private static byte[] readPByteArray(DataInputStream dis, int size) throws IOException {
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readByte();
        }
        return result;
    }

    private static Byte[] readByteArray(DataInputStream dis, int size) throws IOException {
        Byte[] result = new Byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = (Byte) readObject(dis);
        }
        return result;
    }

    private static short[] readPShortArray(DataInputStream dis, int size) throws IOException {
        short[] result = new short[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readShort();
        }
        return result;
    }

    private static Short[] readShortArray(DataInputStream dis, int size) throws IOException {
        Short[] result = new Short[size];
        for (int i = 0; i < size; i++) {
            result[i] = (Short) readObject(dis);
        }
        return result;
    }

    private static boolean[] readPBooleanArray(DataInputStream dis, int size) throws IOException {
        boolean[] result = new boolean[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readBoolean();
        }
        return result;
    }

    private static Boolean[] readBooleanArray(DataInputStream dis, int size) throws IOException {
        Boolean[] result = new Boolean[size];
        for (int i = 0; i < size; i++) {
            result[i] = (Boolean) readObject(dis);
        }
        return result;
    }

    private static char[] readCharArray(DataInputStream dis, int size) throws IOException {
        char[] result = new char[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readChar();
        }
        return result;
    }

    private static Character[] readCharacterArray(DataInputStream dis, int size) throws IOException {
        Character[] result = new Character[size];
        for (int i = 0; i < size; i++) {
            result[i] = (Character) readObject(dis);
        }
        return result;
    }

    private static double[] readPDoubleArray(DataInputStream dis, int size) throws IOException {
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readDouble();
        }
        return result;
    }

    private static Double[] readDoubleArray(DataInputStream dis, int size) throws IOException {
        Double[] result = new Double[size];
        for (int i = 0; i < size; i++) {
            result[i] = (Double) readObject(dis);
        }
        return result;
    }

    private static long[] readPLongArray(DataInputStream dis, int size) throws IOException {
        long[] result = new long[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readLong();
        }
        return result;
    }

    private static Long[] readLongArray(DataInputStream dis, int size) throws IOException {
        Long[] result = new Long[size];
        for (int i = 0; i < size; i++) {
            result[i] = (Long) readObject(dis);
        }
        return result;
    }

    private static float[] readPFloatArray(DataInputStream dis, int size) throws IOException {
        float[] result = new float[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readFloat();
        }
        return result;
    }

    private static Float[] readFloatArray(DataInputStream dis, int size) throws IOException {
        Float[] result = new Float[size];
        for (int i = 0; i < size; i++) {
            result[i] = (Float) readObject(dis);
        }
        return result;
    }

    private static int[] readIntArray(DataInputStream dis, int size) throws IOException {
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = dis.readInt();
        }
        return result;
    }

    private static Integer[] readIntegerArray(DataInputStream dis, int size) throws IOException {
        Integer[] result = new Integer[size];
        for (int i = 0; i < size; i++) {
            result[i] = (Integer) readObject(dis);
        }
        return result;
    }

    private static String[] readStringArray(DataInputStream dis, int size) throws IOException {
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
            result[i] = (String) readObject(dis);
        }
        return result;
    }

    private static SerializableProtectedString[] readProtectedStringArray(DataInputStream dis, int size) throws IOException {
        SerializableProtectedString[] result = new SerializableProtectedString[size];
        for (int i = 0; i < size; i++) {
            result[i] = (SerializableProtectedString) readObject(dis);
        }
        return result;
    }

    private static OnError[] readOnErrorArray(DataInputStream dis, int size) throws IOException {
        OnError[] result = new OnError[size];
        for (int i = 0; i < size; i++) {
            result[i] = (OnError) readObject(dis);
        }
        return result;
    }

    private static Collection<?> readCollection(DataInputStream dis) throws IOException {
        List<Object> result = new ArrayList<>();

        int len = dis.readInt();
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                result.add(readObject(dis));
            }
        }
        return result;
    }

    private static Object readObject(DataInputStream dis) throws IOException {
        byte type = dis.readByte();
        switch (type) {
            case NULL:
                return null;
            case BYTE:
                return dis.readByte();
            case SHORT:
                return dis.readShort();
            case BOOLEAN:
                return dis.readBoolean();
            case CHAR:
                return dis.readChar();
            case DOUBLE:
                return dis.readDouble();
            case LONG:
                return dis.readLong();
            case FLOAT:
                return dis.readFloat();
            case INTEGER:
                return dis.readInt();
            case STRING:
                return dis.readUTF();
            case PROTECTED_STRING:
                return new SerializableProtectedString(dis.readUTF().toCharArray());
            case ONERROR:
                return OnError.values()[dis.readInt()];
            default:
                break;
        }
        throw new IllegalArgumentException("Unsupported object type: " + type);
    }

    public static void writeMap(DataOutputStream dos, MapIterable map) throws IOException {
        dos.writeByte(VERSION);
        writeMapInternal(dos, map);
    }

    private static void writeMapInternal(DataOutputStream dos, MapIterable map) throws IOException {
        if (map == null) {
            dos.writeInt(-1);
            return;
        }
        dos.writeInt(map.size());
        for (Map.Entry<String, Object> entry : map) {
            dos.writeUTF(entry.getKey());
            Object obj = entry.getValue();
            if (obj instanceof Collection) {
                dos.writeByte(COLLECTION);
                Collection<?> data = (Collection<?>) obj;
                dos.writeInt(data.size());
                for (Object colObj : data) {
                    writeObject(dos, colObj);
                }
            } else if (obj instanceof Map) {
                dos.writeByte(MAP);
                writeMapInternal(dos, toMapOrDictionary(obj));
            } else if (obj != null && obj.getClass().isArray()) {
                dos.writeByte(ARRAY);
                Class<?> arrayType = obj.getClass();
                arrayType = arrayType.getComponentType();
                dos.writeBoolean(arrayType.isPrimitive());
                writeArrayType(dos, arrayType);
                int len = Array.getLength(obj);
                dos.writeInt(len);
                for (int i = 0; i < len; i++) {
                    if (arrayType.isPrimitive()) {
                        writePrimitive(dos, Array.get(obj, i));
                    } else {
                        writeObject(dos, Array.get(obj, i));
                    }
                }
            } else {
                dos.writeByte(OBJECT);
                writeObject(dos, obj);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static MapIterable toMapOrDictionary(Object obj) {
        if (obj instanceof Dictionary) {
            return new DictionaryMapIterableImpl((Dictionary<String, Object>) obj);
        } else if (obj instanceof Map) {
            return new MapIterableImpl((Map<String, Object>) obj);
        }
        return null;
    }

    private static void writeArrayType(DataOutputStream dos, Class<?> type) throws IOException {
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
            throw new IllegalArgumentException("Unsupported object type: " + type.getName());
        }
    }

    private static void writePrimitive(DataOutputStream dos, Object obj) throws IOException {
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
        } else {
            throw new IllegalArgumentException("Unsupported object type: " + String.valueOf(obj));
        }
    }

    private static void writeObject(DataOutputStream dos, Object obj) throws IOException {
        if (obj == null) {
            dos.writeByte(NULL);
            return;
        }
        Class<?> type = obj.getClass();
        if (type == Byte.class) {
            dos.writeByte(BYTE);
            dos.writeByte((byte) obj);
        } else if (type == Short.class) {
            dos.writeByte(SHORT);
            dos.writeShort((short) obj);
        } else if (type == Boolean.class) {
            dos.writeByte(BOOLEAN);
            dos.writeBoolean((boolean) obj);
        } else if (type == Character.class) {
            dos.writeByte(CHAR);
            dos.writeChar((char) obj);
        } else if (type == Double.class) {
            dos.writeByte(DOUBLE);
            dos.writeDouble((double) obj);
        } else if (type == Long.class) {
            dos.writeByte(LONG);
            dos.writeLong((long) obj);
        } else if (type == Float.class) {
            dos.writeByte(FLOAT);
            dos.writeFloat((float) obj);
        } else if (type == Integer.class) {
            dos.writeByte(INTEGER);
            dos.writeInt((int) obj);
        } else if (type == String.class) {
            dos.writeByte(STRING);
            dos.writeUTF((String) obj);
        } else if (type == SerializableProtectedString.class) {
            dos.writeByte(PROTECTED_STRING);
            dos.writeUTF(new String(((SerializableProtectedString) obj).getChars()));
        } else if (type == OnError.class) {
            dos.writeByte(ONERROR);
            dos.writeInt(((OnError) obj).ordinal());
        } else {
            throw new IllegalArgumentException("Unsupported object type: " + type.getName());
        }
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

        writeObject(dos, id.getId());
        dos.writeUTF(id.getPid());
        writeObject(dos, id.getChildAttribute());
        dos.writeInt(parentIndex - 1);

        return count + 1;
    }

    public static interface MapIterable extends Iterable<Map.Entry<String, Object>> {
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
