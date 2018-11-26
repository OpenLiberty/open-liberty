/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.config;

import com.ibm.ws.kernel.instrument.serialfilter.digest.Checksums;
import com.ibm.ws.kernel.instrument.serialfilter.util.CallStackWalker;
import com.ibm.ws.kernel.instrument.serialfilter.util.MessageUtil;
import com.ibm.ws.kernel.instrument.serialfilter.util.trie.ConcurrentTrie;
import com.ibm.ws.kernel.instrument.serialfilter.util.trie.Trie;

import java.io.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

final class ConfigImpl implements Config {
    private static final Checksums CHECKSUMS = Checksums.getInstance();
    private static final Class<Externalizable> EXTERN = Externalizable.class;
    private static final Class<Serializable> SERIAL = Serializable.class;
    private static final Map<? super String, ? extends ConfigSetting<String, Boolean>> MODES_BY_NAME = createModesMap();
    private final ConcurrentTrie<ValidationMode> validationModes = new ConcurrentTrie<ValidationMode>();
    private final ConcurrentTrie<PermissionMode> permissions = new ConcurrentTrie<PermissionMode>();
    private final List<Initializer> initializers;
    private ValidationMode defaultValidationMode;
    private static final String DEFAULT_KEY = SpecifierFormat.internalize("*");

    private static Map<String, ConfigSetting<String, Boolean>> createModesMap() {
        HashMap<String, ConfigSetting<String, Boolean>> result = new HashMap<String, ConfigSetting<String, Boolean>>();
        for (ValidationMode m : ValidationMode.values()) result.put(m.name(), m);
        for (PermissionMode m : PermissionMode.values()) result.put(m.name(), m);
        return result;
    }

    static abstract class Initializer { abstract void init(ConfigImpl cfg);}

    static final Initializer SET_DEFAULT_CONFIG = new Initializer() {
        void init(ConfigImpl cfg) {
            Logger log = Logger.getLogger(ConfigImpl.class.getName());
            Properties props = new Properties();
            props.setProperty("*", "REJECT");
            if (log.isLoggable(FINE)) log.fine("Setting default configuration *=REJECT");
            cfg.load(props);
        }
    };

    static final Initializer READ_INTERNAL_CONFIG = new Initializer() {
        void init(ConfigImpl cfg) {
            Logger log = Logger.getLogger(ConfigImpl.class.getName());
            final String defaultConfigPropsFile = "default.properties";
            InputStream in = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                public InputStream run() {
                    return ConfigImpl.class.getResourceAsStream(defaultConfigPropsFile);
                }
            });
            if (in == null) {
                // The resource should always be available, so this is an internal error
                throw new Error("Could not read internal configuration file");
            }
            Properties props = new Properties();
            try {
                props.load(in);
                if (log.isLoggable(FINE)) log.fine("Reading default config from " + in);
                cfg.load(props);
                if (log.isLoggable(FINE)) log.fine("Finished reading default config.");
            } catch (IOException e) {
                // If the user has overridden the default config elsewhere in the class path,
                // the user-provided file might be badly formatted
                log.severe(MessageUtil.format("SF_ERROR_DEFAULT_CONFIGURATION", in, e.getMessage()));
            } finally {
                try {in.close();} catch (IOException suppressed) {}
            }
        }
    };

    static final Initializer READ_SYSTEM_CONFIG = new Initializer() {
        void init(ConfigImpl cfg) {
            Logger log = Logger.getLogger(ConfigImpl.class.getName());
            String filename = AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(Config.FILE_PROPERTY);
                }
            });
            if (filename == null) {
                if (log.isLoggable(FINE)) log.fine("No configuration file specified for serialization validation.");
                return;
            }
            final File f = new File(filename);
            if (!!!f.isFile()) {
                log.severe(MessageUtil.format("SF_ERROR_SYSTEM_CONFIGURATION_NOT_FIND", filename));
                return;
            }
            try {
                Properties props = new Properties();
                FileReader fileReader = AccessController.doPrivileged(new PrivilegedExceptionAction<FileReader>() {
                    public FileReader run() throws IOException {
                        return new FileReader(f);
                    }
                });
                props.load(fileReader);
                if (log.isLoggable(FINE)) log.fine("Reading system config from " + filename);
                cfg.load(props);
                if (log.isLoggable(FINE)) log.fine("Finished reading system config.");
            } catch (PrivilegedActionException e) {
                log.severe(MessageUtil.format("SF_ERROR_SYSTEM_CONFIGURATION", filename, e));
            } catch (IOException e) {
                log.severe(MessageUtil.format("SF_ERROR_SYSTEM_CONFIGURATION", filename, e));
            }
        }
    };

    ConfigImpl(Initializer...initializers) {
        this.initializers = Collections.unmodifiableList(new ArrayList<Initializer>(Arrays.asList(initializers)));
        init();
    }

    private void init() {
        // configure from scratch using the provided initializers
        for (Initializer init : initializers) init.init(this);

        // validation mode lookups search using each stack frame and only
        // resort to the default if no stack frame matches any entry,
        // so a catch-all entry would short-circuit this logic

        // instead store the default setting read this from the parsed config
        defaultValidationMode = validationModes.get(DEFAULT_KEY);
        if (defaultValidationMode == null) {
            String message = MessageUtil.format("SF_ERROR_NO_DEFAULT_MODE");
            Logger.getLogger(ConfigImpl.class.getName()).severe(message);
            throw new Error(message);
        }
        validationModes.remove(DEFAULT_KEY);

    }

    @Override
    public void reset() {
        permissions.clear();
        validationModes.clear();
        init();
    }

    @Override
    public ValidationMode getDefaultMode() {return defaultValidationMode;}

    @Override
    public boolean allows(final Class<?> cls, final Class<?> [] toSkip) {
        return allows(cls, toSkip, true);
    }
    @Override
    public boolean allows(final Class<?> cls, final Class<?> [] toSkip, boolean enableMessage) {
        Logger log = null;
        if (enableMessage) {
            log = Logger.getLogger(ConfigImpl.class.getName());
        }
        // short-circuit if this parent hierarchy has already been checked
        if (cls != toSkip[0]) {
            if (isForbidden(cls)) {
                if (log != null) log.severe(MessageUtil.format("SF_ERROR_NOT_PERMIT", cls.getName()));
                return false;
            }
            // if child class is externalizable, only check externalizable ancestors
            // if child class is serializable, only check serializable ancestors
            final Class<?> iface = externalizableOrSerializable(cls);
            for (Class c = cls.getSuperclass(); c != null && iface.isAssignableFrom(c); c = c.getSuperclass()) {
                if (isForbidden(c)) {
                    if (log != null) log.severe(MessageUtil.format("SF_ERROR_NOT_PERMIT_SUPERCLASS", cls.getName(), c.getName()));
                    return false;
                }
            }
        }
        // flag that the parent hierarchy has been checked
        toSkip[0] = cls.getSuperclass();
        return true;
    }

    private Class<? extends Serializable> externalizableOrSerializable(Class<?> cls) {
        return EXTERN.isAssignableFrom(cls) ? EXTERN : SERIAL;
    }

    private boolean isForbidden(Class<?> c) {
        Logger log = Logger.getLogger(ConfigImpl.class.getName());
        final String cName = c.getName();
        final String name = SpecifierFormat.internalize(cName);
        final Trie.Entry<PermissionMode> e = permissions.getLongestPrefixEntry(name);
        if (log.isLoggable(FINEST))
            log.finest("Searching on class " + c + " returns " + SpecifierFormat.externalize(e.getKey()) + "=" + e.getValue());
        return e.getValue().isProhibitive;
    }

    @Override
    public ValidationMode getModeForStack(Class<?> caller) {
        Logger log = Logger.getLogger(ConfigImpl.class.getName());
        // Avoid examining the stack if there are no context-specific settings
        if (validationModes.isEmpty()) return defaultValidationMode;
        CallStackWalker stack = CallStackWalker.forCurrentThread().skipTo(ObjectInputStream.class);
        // Because this method is called from a constructor, there may be a chain of <init> calls
        // near the top of the stack, with one or more entries per class in the hierarchy.

        // Create a set of the classes we want to skip * reserve order of insertion
        Set<Class<?>> streamClasses = new LinkedHashSet<Class<?>>();

        // Loop 1: Visit the superclasses up to and including ObjectInputStream
        for(Class<?> c = caller; c != InputStream.class; c = c.getSuperclass())
            streamClasses.add(c);

        // Loop 2: Skip past stream constructors in call stack
        while (stack.size() > 0
                && streamClasses.contains(stack.topClass())
                && "<init>".equals(stack.topMethod()))
            stack.pop();

        // Loop 3: The remaining frames are all 'caller' frames for the object being constructed
        // NOTE: subsequent frames are more general, so the first match takes precedence.
        for (; stack.size() > 0; stack.pop()) {
            if (log.isLoggable(FINER))
                log.finer("Searching for validation mode setting for " + stack.topClass().getName() + '#' + stack.topMethod());
            ValidationMode modeForCaller = getModeForMethod(stack);
            if (modeForCaller != null) {
                if (log.isLoggable(FINE)) log.fine("Found validation mode " + modeForCaller + " for caller method " + stack.topClass().getName() + '#' + stack.topMethod());
                return modeForCaller;
            }
        }

        // Loop 4: Look for stream-specific validation setting
        for (Class<?> c : streamClasses) {
            if (log.isLoggable(FINER)) log.finer("Searching for validation mode setting for " + c.getName());
            ValidationMode modeForStream = getValidationMode(c.getName());
            if (modeForStream != null) {
                if (log.isLoggable(FINE))
                    log.fine("Found validation mode " + modeForStream + " for stream class " + stack.topClass());
                return modeForStream;
            }
        }

        if (log.isLoggable(FINE)) log.fine("No mode configured, returning default validation    mode: " + defaultValidationMode);
        return defaultValidationMode;
    }

    private ValidationMode getModeForMethod(CallStackWalker stack) {
        return getValidationMode(stack.topClass().getName() + "#" + stack.topMethod());
    }

    @Override
    public ValidationMode getValidationMode(String s) {
        Logger log = Logger.getLogger(ConfigImpl.class.getName());
        SpecifierFormat f = SpecifierFormat.fromString(s);

        switch (f) {
            case CLASS:
            case METHOD:
                return validationModes.getLongestPrefixValue(SpecifierFormat.internalize(s));
            case METHOD_PREFIX:
            case PREFIX:
                log.severe(MessageUtil.format("SF_ERROR_GET_MODE_VALUE_PREFIX", s));
                return null;
            case DIGEST:
                log.severe(MessageUtil.format("SF_ERROR_GET_MODE_VALUE_DIGEST", s));
                return null;
            case UNKNOWN:
            default:
                log.severe(MessageUtil.format("SF_ERROR_GET_MODE_VALUE_UNKNOWN", s));
                return null;
        }
    }

    @Override
    public boolean setValidationMode(ValidationMode mode, String s) {
        Logger log = Logger.getLogger(ConfigImpl.class.getName());
        if (log.isLoggable(FINEST)) log.finest("Setting validation mode " + mode + " for '" + s + "'");
        SpecifierFormat f = SpecifierFormat.fromString(s);
        switch (f) {
            case CLASS:
            case PREFIX:
            case METHOD:
            case METHOD_PREFIX:
                return mode != validationModes.put(SpecifierFormat.internalize(s), mode);
            case DIGEST:
                log.severe(MessageUtil.format("SF_ERROR_SET_MODE_VALUE_DIGEST", s));
                return false;
            case UNKNOWN:
            default:
                log.severe(MessageUtil.format("SF_ERROR_SET_MODE_VALUE_UNKNOWN", s));
                return false;
        }
    }

    @Override
    public boolean setPermission(final PermissionMode mode, final String s) {
        Logger log = Logger.getLogger(ConfigImpl.class.getName());
        if (log.isLoggable(FINEST)) log.finest("Setting permission " + mode + " for '" + s + "'");
        SpecifierFormat f = SpecifierFormat.fromString(s);
        switch (f) {
            case CLASS:
            case PREFIX:
            case DIGEST:
                return mode == permissions.put(SpecifierFormat.internalize(s), mode);
            case METHOD:
            case METHOD_PREFIX:
                log.severe(MessageUtil.format("SF_ERROR_SET_PERMISSION_VALUE_METHOD", s));
                return false;
            case UNKNOWN:
            default:
                log.severe(MessageUtil.format("SF_ERROR_SET_PERMISSION_VALUE_UNKNOWN", s));
                return false;
        }
    }

    @Override
    public void load(Properties props) {
        Logger log = Logger.getLogger(ConfigImpl.class.getName());
        for (Entry<Object, Object> entry : props.entrySet()) {

            if (!!! (entry.getKey() instanceof String)) {
                log.severe(MessageUtil.format("SF_ERROR_LOAD_PROPERTY_KEY", entry.getKey()));
                continue;
            }
            if (!!! (entry.getValue() instanceof String)) {
                log.severe(MessageUtil.format("SF_ERROR_LOAD_PROPERTY_VALUE", entry.getValue()));
            }
            String key = (String)entry.getKey();
            for (String val : ((String)entry.getValue()).trim().split(" *[ ,] *")) {
                ConfigSetting<String, Boolean> mode = MODES_BY_NAME.get(val);
                if (mode == null) {
                    log.severe(MessageUtil.format("SF_ERROR_LOAD_PROPERTY_NOT_MATCH", val, entry));
                    continue;
                }
                mode.apply(this, key);
            }
        }
    }
}
