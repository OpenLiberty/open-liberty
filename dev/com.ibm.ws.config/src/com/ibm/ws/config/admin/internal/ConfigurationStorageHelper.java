/*******************************************************************************
 * Copyright (c) 2019,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
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
 * byte - version <br>
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
    private static final TraceComponent tc = Tr.register(ConfigurationStorageHelper.class,
                                                         ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    //

    /**
     * API for receiving stored configurations.
     *
     * The API has two steps: Loaded configuration data consists of a location
     * and several data structures. The location is usually a bundle location. The
     * data structures are major subsets of configuration data: Unique variables defined
     * by the configuration, reference IDs used by the configuration, and the table of
     * configuration data. The first step of processing a loaded configuration is
     * marshaling the loaded data into a configuration object. The second step of
     * processing the loaded data is generating a key for the marshaled configuration
     * and storing that configuration in an externally supplied table.
     *
     * In the current implementation, a particular value of the configuration is obtained
     * as a particular value of the configuration data. For example, see
     * {@link com.ibm.ws.config.admin.internal.ConfigurationStore#loadConfigurationDatas(File)},
     * which uses the {@link Constants#SERVICE_PID} value:
     *
     * <code>
     * String pid = (String) dict.get(Constants.SERVICE_PID);
     * </code>
     *
     * @param <K> The type of keys used to access the loaded configurations.
     * @param <C> The type of loaded configurations.
     */
    public static interface ConfigStorageConsumer<K, C> {
        /**
         * Consume just read configuration data.
         *
         * @param location The location of the configuration. Usually a bundle location.
         * @param uniqueVars The unique variables of the configuration.
         * @param references The reference IDs used by the configuration.
         * @param dict The actual data of the configuration.
         *
         * @return The marshaled configuration data.
         */
        C consumeConfigData(String location,
                            Set<String> uniqueVars, Set<ConfigID> references, ConfigurationDictionary dict);

        /**
         * Answer the key of the configuration data. This is usually a particular value of the
         * data.
         *
         * @param config The configuration from which to obtain a key value.
         * @return A key used to store the configuration.
         */
        K getKey(C config);
    }

    /**
     * The initial 'short strings' storage format. This format cannot
     * store strings which are over 64K in length.
     */
    public static final byte VERSION_SHORT_STRINGS = 0;

    /**
     * New 'long strings' storage format. This format handles longer strings.
     *
     * The difference between {@link #VERSION_SHORT_STRINGS} and
     * {@link #VERSION_LONG_STRINGS} is a change from string constants
     * {@link #STRING} and {@link #PROTECTED_STRING} to
     * {@link #SHORT_STRING} and {@link #LONG_STRING}, and to
     * {@link #SHORT_PROTECTED_STRING} and {@link #LONG_PROTECTED_STRING}.
     *
     * For simplicity and efficiency, no attempt is made to test if
     * a save can use the short storage format. This would require a
     * possibly expensive walk of the configuration data prior to performing
     * the save. The walk would be necessary to select the storage format,
     * which is written before writing any of the configuration data.
     *
     * The long storage format could reuse STRING for short strings. Also
     * for simplicity, the new string constants are used, and {@link DataOutputStream#writeUTF(String)}
     * is dispensed with almost entirely. Instead {@link DataOutputStream#write(byte[])} is used.
     *
     * "Almost entirely" means that particular string values: unique variables,
     * key values, and PID's are still written using {@link DataOutputStream#writeUTF(String)}
     * and are still limited to 64K bytes in length.
     */
    public static final byte VERSION_LONG_STRINGS = 1;

    /**
     * The current maximum storage format. By default, the save API will
     * save using this format, and the load API will accept all versions
     * up to this format.
     */
    public static final byte VERSION_MAX = VERSION_LONG_STRINGS;

    private static final byte BYTE = 0;
    private static final byte SHORT = 1;
    private static final byte BOOLEAN = 2;
    private static final byte CHAR = 3;
    private static final byte DOUBLE = 4;
    private static final byte LONG = 5;
    private static final byte FLOAT = 6;
    private static final byte INTEGER = 7;

    /** String type value used by the {@link #VERSION_SHORT_STRINGS} format. */
    private static final byte STRING = 8;
    /** Protected string type value used by the {@link #VERSION_SHORT_STRINGS} format. */
    private static final byte PROTECTED_STRING = 9;

    private static final byte COLLECTION = 10;
    private static final byte MAP = 11;
    private static final byte ARRAY = 12;
    private static final byte ONERROR = 13;
    private static final byte NULL = 14;
    private static final byte OBJECT = 15;

    /** Short string type value used by the {@link #VERSION_LONG_STRINGS} format. */
    private static final byte SHORT_STRING = 16;
    /** Long string type value used by the {@link #VERSION_LONG_STRINGS} format. */
    private static final byte LONG_STRING = 17;

    /** Short protected string type value used by the {@link #VERSION_LONG_STRINGS} format. */
    private static final byte SHORT_PROTECTED_STRING = 18;
    /** Long protected string type value used by the {@link #VERSION_LONG_STRINGS} format. */
    private static final byte LONG_PROTECTED_STRING = 19;

    //

    private static boolean isValidVersion(int version, int minVersion, int maxVersion) {
        if ((version < minVersion) || (version > maxVersion)) {
            Tr.warning(tc, "Unsupported configuration storage version [ " + version + " ]:" +
                           " Valid versions are [ " + minVersion + " ] to [ " + maxVersion + " ]");
            return false;
        } else {
            return true;
        }
    }

    private final int version;

    private String location;

    // 'store' sets this to a dictionary,
    // while 'load' sets it to a 'ConfigurationDictionary'.
    private final Dictionary<String, ?> readOnlyProps;
    private final Set<String> uniqueVars;
    private final Set<ConfigID> references;

    public String getLocation() {
        return location;
    }

    public Dictionary<String, ?> getReadOnlyProps() {
        return readOnlyProps;
    }

    public Set<String> getUniqueVars() {
        return uniqueVars;
    }

    public Set<ConfigID> getReferences() {
        return references;
    }

    /**
     * Initializer for loading configurations.
     *
     * @param version The serialization format version of the configurations.
     */
    public ConfigurationStorageHelper(int version) {
        this.version = version;

        this.location = null;

        this.readOnlyProps = new ConfigurationDictionary();
        this.uniqueVars = new HashSet<>();
        this.references = new HashSet<>();
    }

    @FFDCIgnore(IllegalStateException.class)
    private static ConfigurationStorageHelper newWriteHelper(int version, ExtendedConfiguration config) {
        // Guard against non-valid configurations:
        // Those with null read-only properties and those which are deleted.

        try {
            Dictionary<String, ?> readOnlyProps = config.getReadOnlyProperties();
            if (readOnlyProps == null) {
                return null;
            } else {
                return new ConfigurationStorageHelper(version, config, readOnlyProps);
            }

        } catch (IllegalStateException e) {
            // Thrown when attempting to access a deleted configuration.
            return null; // ignore
        }
    }

    /**
     * Initializer for saving configurations.
     *
     * @param version The serialization format version of the configurations.
     * @param config A configuration which is to be saved.
     * @param readOnlyProps Read only properties of the configuration.
     */
    private ConfigurationStorageHelper(int version,
                                       ExtendedConfiguration config,
                                       Dictionary<String, ?> readOnlyProps) {

        this(version, config.getBundleLocation(), readOnlyProps, config.getUniqueVariables(), config.getReferences());
    }

    // Used for testing.

    public ConfigurationStorageHelper(int version,
                                      String location,
                                      Dictionary<String, ?> readOnlyProps,
                                      Set<String> uniqueVars,
                                      Set<ConfigID> references) {

        this.version = version;

        this.location = location;

        this.readOnlyProps = readOnlyProps;
        this.uniqueVars = uniqueVars;
        this.references = references;
    }

    public <K, C> void storeConfiguration(ConfigStorageConsumer<K, C> consumer, Map<K, C> storage) {
        // Ugly type-cast.  'store' uses a Dictionary, while 'load' uses a ConfigurationDictionary.
        C config = consumer.consumeConfigData(location, uniqueVars, references, (ConfigurationDictionary) readOnlyProps);
        storage.put(consumer.getKey(config), config);
    }

    /**
     * Load (read) configurations from a file.
     *
     * Data can be in any of the currently supported formats.
     *
     * See {@link #load(File, ConfigStorageConsumer, int, int).
     *
     * @param configFile The configuration file which is to be read.
     * @param consumer A consumer of the read configuration data.
     *
     * @return A table of marshaled configuration, stored according to the consumer.
     *
     * @throws IOException Thrown if any of the reads fails.
     */
    public static <K, C> Map<K, C> load(File configFile, ConfigStorageConsumer<K, C> consumer) throws IOException {
        return load(configFile, consumer, VERSION_SHORT_STRINGS, VERSION_LONG_STRINGS);
    }

    /**
     * Load (read) configurations from a file.
     *
     * The read expects data to be formatted according to {@link #store(File, Collection, int)}.
     *
     * The data must use a format in the specified range.
     *
     * Data is returned as a table of configurations, marshaled and keyed according to the
     * {@link ConfigStorageConsumer}.
     *
     * No data is read if the file contains data in a format which is outside the
     * specified range of formats. A warning is displayed if this happens.
     *
     * @param configFile The configuration file which is to be read.
     * @param consumer A consumer of the read configuration data.
     * @param minVersion The minimum storage format version accepted by the read.
     * @param maxVersionThe maximum storage format version accepted by the read.
     *
     * @return A table of marshaled configuration, stored according to the consumer.
     *
     * @throws IOException Thrown if any of the reads fails.
     */
    public static <K, C> Map<K, C> load(File configFile, ConfigStorageConsumer<K, C> consumer, int minVersion, int maxVersion) throws IOException {
        Map<K, C> storage = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(configFile);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        DataInputStream dis = new DataInputStream(bis)) {

            int version = dis.readByte();
            if (isValidVersion(version, minVersion, maxVersion)) {
                int numConfigs = dis.readInt();
                for (int i = 0; i < numConfigs; i++) {
                    ConfigurationStorageHelper helper = new ConfigurationStorageHelper(version);
                    helper.load(dis);
                    helper.storeConfiguration(consumer, storage);
                }
            }
        }

        return storage;
    }

    protected void load(DataInputStream dis) throws IOException {
        location = (String) readObject(dis);

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

        readMapInternal(dis, toMapOrDictionary(readOnlyProps));
    }

    /**
     * Store (write) configurations to a file using the current maximum storage format.
     *
     * See {@link #VERSION_MAX} and {@link #store(File, Collection, int)}.
     *
     * @param configFile The file which receives the configurations.
     * @param configs The configurations which are to be written.
     *
     * @throws IOException Thrown if any of the writes fails.
     */
    public static void store(File configFile, Collection<? extends ExtendedConfiguration> configs) throws IOException {
        store(configFile, configs, VERSION_MAX);
    }

    /**
     * Store (write) configurations to a file.
     *
     * The file is truncated before performing the write.
     *
     * Write the version to the file, then write the count of configurations, then write each of the configurations.
     *
     * Configurations which are not valid are not counted and are not written.
     *
     * @param configFile The file which receives the configurations.
     * @param configs The configurations which are to be written.
     * @param version The version of the storage format which is to be used.
     *
     * @throws IOException Thrown if any of the writes fails.
     */
    public static void store(File configFile, Collection<? extends ExtendedConfiguration> configs, int version) throws IOException {

        try (FileOutputStream fos = new FileOutputStream(configFile, false);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        DataOutputStream dos = new DataOutputStream(bos)) {

            dos.writeByte(version);

            // Determine valid configurations.  This must before doing any of the
            // saves since the count is stored first.

            // Allocate approximate storage assuming that all of the configurations are valid.

            List<ConfigurationStorageHelper> helpers = new ArrayList<>(configs.size());
            for (ExtendedConfiguration config : configs) {
                ConfigurationStorageHelper helper = newWriteHelper(version, config);
                if (helper != null) {
                    helpers.add(helper);
                }
            }

            dos.writeInt(helpers.size());

            for (ConfigurationStorageHelper helper : helpers) {
                helper.store(dos);
            }
        }
    }

    /**
     * Write a single configuration. That is, write the location, the unique variables,
     * the references, and the read-only properties of the configuration.
     *
     * @param dos A data output stream which receives the configuration.
     *
     * @throws IOException Thrown if any of the writes fail.
     */
    protected void store(DataOutputStream dos) throws IOException {
        writeObject(dos, location);

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

        writeMapInternal(dos, toMapOrDictionary(readOnlyProps));
    }

    //

    public static void readMap(DataInputStream dis, MapIterable props) throws IOException {
        readMap(dis, props, VERSION_SHORT_STRINGS, VERSION_LONG_STRINGS);
    }

    public static void readMap(DataInputStream dis, MapIterable props, int minVersion, int maxVersion) throws IOException {
        int version = dis.readByte();
        if (!isValidVersion(version, minVersion, maxVersion)) {
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

            // When writing strings, a type byte is written for both the
            // array and for all of the array elements.
            //
            // That is to say, a 'STRING' array can have array elements
            // which use any of the STRING type values.
            //
            // When reading strings, array elements are read according
            // to the type value stored for each element.

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

            // A version is not needed when reading:
            // The version 1 reader handles both
            // the version 1 format and the version 0 format.

            case STRING: // version 0
                return dis.readUTF();
            case SHORT_STRING: // version 1
                return readShortString(dis);
            case LONG_STRING: // version 1
                return readLongString(dis);

            case PROTECTED_STRING: // version 0
                return new SerializableProtectedString(dis.readUTF().toCharArray());
            case SHORT_PROTECTED_STRING: // version 1
                return new SerializableProtectedString(readShortProtectedString(dis).toCharArray());
            case LONG_PROTECTED_STRING: // version 1
                return new SerializableProtectedString(readLongProtectedString(dis).toCharArray());

            case ONERROR:
                return OnError.values()[dis.readInt()];
            default:
                break;
        }

        throw new IllegalArgumentException("Unsupported object type: " + type);
    }

    //

    public static void writeMap(DataOutputStream dos, MapIterable map) throws IOException {
        writeMap(dos, map, VERSION_MAX);
    }

    public static void writeMap(DataOutputStream dos, MapIterable map, int version) throws IOException {
        dos.writeByte(version);

        ConfigurationStorageHelper helper = new ConfigurationStorageHelper(version);
        helper.writeMapInternal(dos, map);
    }

    private void writeMapInternal(DataOutputStream dos, MapIterable map) throws IOException {
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

    private void writeArrayType(DataOutputStream dos, Class<?> type) throws IOException {
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

            // When writing strings, a type byte is written for both the
            // array and for all of the array elements.
            //
            // That is to say, a 'STRING' array can have array elements
            // which use any of the STRING type values.
            //
            // When reading strings, array elements are read according
            // to the type value stored for each element.

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

    private void writePrimitive(DataOutputStream dos, Object obj) throws IOException {
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

    private void writeObject(DataOutputStream dos, Object obj) throws IOException {
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
            writeString(dos, (String) obj);
        } else if (type == SerializableProtectedString.class) {
            writeProtectedString(dos, (SerializableProtectedString) obj);

        } else if (type == OnError.class) {
            dos.writeByte(ONERROR);
            dos.writeInt(((OnError) obj).ordinal());
        } else {
            throw new IllegalArgumentException("Unsupported object type: " + type.getName());
        }
    }

    private int writeConfigID(DataOutputStream dos, ConfigID id, int count,
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
        protected final Dictionary<String, Object> dict;

        public DictionaryMapIterableImpl(Dictionary<String, Object> dict) {
            this.dict = dict;
        }

        public class MyIterator implements Iterator<Map.Entry<String, Object>> {
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

    // String operations ...

    /**
     * Write a string to a data output stream. Format the string
     * according to the storage format version and according to the
     * size of the string.
     *
     * @param dos A data output stream which will receive the string.
     * @param str A string which is to be written.
     *
     * @throws IOException Thrown if the write fails.
     */
    private void writeString(DataOutputStream dos, String str) throws IOException {
        if (version == 0) {
            dos.writeByte(STRING);
            dos.writeUTF(str);

        } else {
            byte[] bytes = str.getBytes("UTF-8");
            if (bytes.length <= 65535) {
                dos.writeByte(SHORT_STRING);
                dos.writeShort((short) bytes.length);
            } else {
                dos.writeByte(LONG_STRING);
                dos.writeInt(bytes.length);
            }

            dos.write(bytes);
        }
    }

    /**
     * Write a protected string to a data output stream. Format the string
     * according to the storage format version and according to the
     * size of the string.
     *
     * This method is nearly the same as {@link #writeString(DataOutputStream, String)},
     * except that logging is disabled.
     *
     * @param dos A data output stream which will receive the string.
     * @param str A string which is to be written.
     *
     * @throws IOException Thrown if the write fails.
     */
    @Trivial // Don't log protected values
    private void writeProtectedString(DataOutputStream dos, SerializableProtectedString str) throws IOException {
        String rawStr = new String(str.getChars());

        if (version == 0) {
            dos.writeByte(PROTECTED_STRING);
            dos.writeUTF(rawStr);

        } else {
            byte[] bytes = rawStr.getBytes("UTF-8");
            if (bytes.length <= 65535) {
                dos.writeByte(SHORT_PROTECTED_STRING);
                dos.writeShort((short) bytes.length);
            } else {
                dos.writeByte(LONG_PROTECTED_STRING);
                dos.writeInt(bytes.length);
            }
            dos.write(bytes);
        }
    }

    // String primitives ...

    private static String readShortString(DataInputStream dis) throws IOException {
        int length = dis.readShort();
        byte[] strBytes = new byte[length];
        dis.readFully(strBytes);
        return new String(strBytes, "UTF-8");
    }

    private static String readLongString(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        byte[] strBytes = new byte[length];
        dis.readFully(strBytes);
        return new String(strBytes, "UTF-8");
    }

    @Trivial // Don't log protected values
    private static String readShortProtectedString(DataInputStream dis) throws IOException {
        int length = dis.readShort();
        byte[] strBytes = new byte[length];
        dis.readFully(strBytes);
        return new String(strBytes, "UTF-8");
    }

    @Trivial // Don't log protected values
    private static String readLongProtectedString(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        byte[] strBytes = new byte[length];
        dis.readFully(strBytes);
        return new String(strBytes, "UTF-8");
    }
}
