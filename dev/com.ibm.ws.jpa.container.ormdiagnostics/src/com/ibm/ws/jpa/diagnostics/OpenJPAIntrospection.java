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
package com.ibm.ws.jpa.diagnostics;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.ffdc.FFDCFilter;

public class OpenJPAIntrospection extends AbstractIntrospection {
    private final static String FFDCCN = OpenJPAIntrospection.class.getName();

    public static void dumpOpenJPAPCRegistry(List<ClassLoader> puInfoTCL, final PrintWriter out) {
        if (puInfoTCL == null || puInfoTCL.isEmpty()) {
            return;
        }

        for (ClassLoader c : puInfoTCL) {
            try {
                final Class pcRegClass = c.loadClass("org.apache.openjpa.enhance.PCRegistry");
                doDumpOpenJPAPCRegistry(pcRegClass, out);
                return;
            } catch (ClassNotFoundException cnfe) {
                // Next!
            }
        }
    }

    @Override
    public void dumpJPAEntityManagerFactoryState(final Object emf, final PrintWriter out) {
        doDumpOpenJPAEntityManagerFactoryState(emf, out);
    }

    private void doDumpOpenJPAEntityManagerFactoryState(final Object emf, final PrintWriter out) {
        out.println("OpenJPA EntityManagerFactory: " + getInstanceClassAndAddress(emf));

        try {
            Object brokerFactory = reflectObjValue(emf, "_factory");

            out.println("   _factory = " + brokerFactory); // DelegatingBrokerFactory _factory
            out.println("   _metaModel = " + reflectObjValue(emf, "_metaModel")); // MetamodelImpl _metaModel

            // Extract the actual BrokerFactory from the DelegatingBrokerFactory
            while (brokerFactory != null && isCastable("org.apache.openjpa.kernel.DelegatingBrokerFactory", brokerFactory.getClass())) {
                Object broker = reflectObjValue(brokerFactory, "_factory");
                if (broker == null) {
                    brokerFactory = reflectObjValue(brokerFactory, "_del"); // Inner Delegate: DelegatingBrokerFactory _del
                } else {
                    brokerFactory = broker;
                    break;
                }
            }
            out.println("   Actual Broker Factory = " + brokerFactory);

            // Dump info about the Broker Factory, if we were able to get it
            if (brokerFactory != null && isCastable("org.apache.openjpa.kernel.AbstractBrokerFactory", brokerFactory.getClass())) {
                out.println();
                out.println("   Unwrapped _factory = " + brokerFactory);
                out.println("   AbstractBrokerFactory values:");
                out.println("      _closed = " + reflectObjValue(emf, "_closed")); // boolean
                out.println("      _readOnly = " + reflectObjValue(emf, "_readOnly")); // boolean

                final Object _conf = reflectObjValue(brokerFactory, "_conf");
                printOpenJPAConfiguration(_conf, out, "      _conf: ");

                if (_conf != null) {
                    out.println();
                    final Object mdr = reflectObjValue(_conf, "metaRepository");
                    if (mdr != null) {
                        printOpenJPAMetadataRepository(mdr, out, "      mdr: ");
                    }
                }

                // Dump Broker Information -- these are persistence contexts/EntityManager instances
                Set brokers = new HashSet();
                Set _brokers = (Set) reflectObjValue(brokerFactory, "_brokers"); // weak-ref tracking of open brokers
                if (_brokers != null) {
                    brokers.addAll(_brokers); // Copy the _brokers Set into our own Set for work
                }
                if (brokers.size() > 0) {
                    out.println();
                    out.println("   Active OpenJPA Brokers (Persistence Contexts): ");
                    for (Object broker : brokers) {
                        printOpenBrokerImpl(broker, out, "      ");
                    }
                }

            }

            if (brokerFactory != null && isCastable("org.apache.openjpa.jdbc.kernel.JDBCBrokerFactory", brokerFactory.getClass())) {
                out.println("   JDBCBrokerFactory values:");
                out.println("      _synchronizedMappings = " + reflectObjValue(emf, "_synchronizedMappings")); // boolean
            }

        } catch (Throwable t) {
            FFDCFilter.processException(t, FFDCCN + ".doDumpOpenJPAEntityManagerFactoryState", "689");
        }

    }

    private void printOpenJPAConfiguration(final Object config, final PrintWriter out, final String indent) {
        out.println(indent + "OpenJPA Configuration = " + config);
        if (config == null || indent == null) {
            return;
        }

        try {
            if (config != null) {
                out.println(indent + " ._appCL = " + reflectObjValue(config, "_appCL")); // ClassLoader
                // Dump all config options exposed by getter methods
                final List<Method> getMethods = getMethodsWithPrefix(config.getClass(), "get");
                int nullValCount = 0;
                for (Method m : getMethods) {
                    try {
                        final Object val = reflectMethodCall(config, m);
                        if (val == null) {
                            nullValCount++;
                        } else {
                            out.print(indent + " ." + m.getName().substring(3) + " = ");
                            out.println(poa(val, indent + "      ", true));
                        }
                    } catch (Throwable t) {
                        out.println(indent + " . problem calling " + m.getName());
                    }
                }
                out.println(indent + " # of get methods = " + getMethods.size());
                out.println(indent + " # of null properties = " + nullValCount); // 500+ average null values which we don't need to see
            }
        } catch (Throwable t) {

        }
    }

    private void printOpenJPAMetadataRepository(final Object mdr, final PrintWriter out, final String indent) throws Exception {
        out.println(indent + "OpenJPA MetaDataRepository = " + mdr);
        if (mdr == null || indent == null) {
            return;
        }

        final boolean isMDR = isCastable("org.apache.openjpa.meta.MetaDataRepository", mdr.getClass());
        final boolean isMR = isCastable("org.apache.openjpa.jdbc.meta.MappingRepository", mdr.getClass());
        if (isMDR || isMR) {
            out.println(indent + " ._factory = " + reflectObjValue(mdr, "_factory")); // MetaDataFactory
            out.println(indent + " ._filterRegisteredClasses = " + reflectObjValue(mdr, "_filterRegisteredClasses")); // boolean
            out.println(indent + " ._locking = " + reflectObjValue(mdr, "_locking")); // boolean
            out.println(indent + " ._preload = " + reflectObjValue(mdr, "_preload")); // boolean
            out.println(indent + " ._preloadComplete = " + reflectObjValue(mdr, "_preloadComplete")); // boolean

            out.println();
            final Map<?, ?> _metas = (Map) reflectObjValue(mdr, "_metas"); // Map<Class<?>, ClassMetaData>
            out.println(indent + " _metas = " + getInstanceClassAndAddress(_metas) + " (" + _metas.size() + " items)");
            final HashMap<?, ?> metasCopy = new HashMap(_metas);
            for (Map.Entry<?, ?> entry : metasCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _metaStringMap = (Map) reflectObjValue(mdr, "_metaStringMap"); // Map<String, ClassMetaData>
            out.println(indent + " _metaStringMap = " + getInstanceClassAndAddress(_metaStringMap) + " (" + _metaStringMap.size() + " items)");
            final HashMap<?, ?> metasStringMapCopy = new HashMap(_metaStringMap);
            for (Map.Entry<?, ?> entry : metasStringMapCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _impls = (Map) reflectObjValue(mdr, "_impls"); // Map<Class<?>, Collection<Class<?>>>
            out.println(indent + " _impls = " + getInstanceClassAndAddress(_impls) + " (" + _impls.size() + " items)");
            final HashMap<?, ?> implsCopy = new HashMap(_impls);
            for (Map.Entry<?, ?> entry : implsCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _ifaces = (Map) reflectObjValue(mdr, "_ifaces"); // Map<Class<?>, Class<?>>
            out.println(indent + " _ifaces = " + getInstanceClassAndAddress(_ifaces) + " (" + _ifaces.size() + " items)");
            final HashMap<?, ?> ifacesCopy = new HashMap(_ifaces);
            for (Map.Entry<?, ?> entry : ifacesCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _queries = (Map) reflectObjValue(mdr, "_queries"); // Map<String, QueryMetaData>
            out.println(indent + " _queries = " + getInstanceClassAndAddress(_queries) + " (" + _queries.size() + " items)");
            final HashMap<?, ?> queriesCopy = new HashMap(_queries);
            for (Map.Entry<?, ?> entry : queriesCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _seqs = (Map) reflectObjValue(mdr, "_seqs"); // Map<String, QueryMetaData>
            out.println(indent + " _seqs = " + getInstanceClassAndAddress(_seqs) + " (" + _seqs.size() + " items)");
            final HashMap<?, ?> seqsCopy = new HashMap(_seqs);
            for (Map.Entry<?, ?> entry : seqsCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _aliases = (Map) reflectObjValue(mdr, "_aliases"); // Map<String, List<Class<?>>
            out.println(indent + " _aliases = " + getInstanceClassAndAddress(_aliases) + " (" + _aliases.size() + " items)");
            final HashMap<?, ?> aliasesCopy = new HashMap(_aliases);
            for (Map.Entry<?, ?> entry : aliasesCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _metamodel = (Map) reflectObjValue(mdr, "_metamodel"); // Map<Class<?>, Class<?>>
            out.println(indent + " _metamodel = " + getInstanceClassAndAddress(_metamodel) + " (" + _metamodel.size() + " items)");
            final HashMap<?, ?> metamodelCopy = new HashMap(_metamodel);
            for (Map.Entry<?, ?> entry : metamodelCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }

            out.println();
            final Map<?, ?> _subs = (Map) reflectObjValue(mdr, "_subs"); // Map<Class<?>, List<Class<?>>>
            out.println(indent + " _subs = " + getInstanceClassAndAddress(_subs) + " (" + _subs.size() + " items)");
            final HashMap<?, ?> _subsCopy = new HashMap(_subs);
            for (Map.Entry<?, ?> entry : _subsCopy.entrySet()) {
                out.println(indent + "   " + entry.getKey() + " = " + poa(entry.getValue(), indent + "      ", true));
            }
        }
        if (isMR) {
            // MappingRepository extends MetaDataRepository
            out.println();
            out.println(indent + " MappingRepository Specific Values");
            out.println(indent + "  ._dict = " + reflectObjValue(mdr, "_dict")); // DBDictionary
            out.println(indent + "  ._defaults = " + reflectObjValue(mdr, "_defaults")); // MappingDefaults
            out.println(indent + "  ._results = " + poa(reflectObjValue(mdr, "_results"), indent + "     ", true)); // Map<Object, QueryResultMapping>
            out.println(indent + "  ._schema = " + reflectObjValue(mdr, "_schema")); // SchemaGroup
            out.println(indent + "  ._installer = " + reflectObjValue(mdr, "_installer")); // StrategyInstaller
        }
    }

    private void printOpenBrokerImpl(final Object brokerImpl, final PrintWriter out, final String indent) throws Exception {
        out.println(indent + "OpenJPA BrokerImpl = " + brokerImpl);
        if (brokerImpl == null || indent == null) {
            return;
        }

        try {
            out.println(indent + " ._compat = " + reflectObjValue(brokerImpl, "_compat")); // Compatibility
            out.println(indent + " ._runtime = " + reflectObjValue(brokerImpl, "_runtime")); // ManagedRuntime
            out.println(indent + " ._lm = " + reflectObjValue(brokerImpl, "_lm")); // LockManager
            out.println(indent + " ._im = " + reflectObjValue(brokerImpl, "_im")); // InverseManager
            out.println(indent + " ._call = " + reflectObjValue(brokerImpl, "_call")); // OpCallbacks
            out.println(indent + " ._instm = " + reflectObjValue(brokerImpl, "_instm")); // InstrumentationManager
            out.println(indent + " ._loader = " + reflectObjValue(brokerImpl, "_loader")); // ClassLoader

            out.println(indent + " ._nontransRead = " + reflectObjValue(brokerImpl, "_nontransRead"));
            out.println(indent + " ._nontransWrite = " + reflectObjValue(brokerImpl, "_nontransWrite"));
            out.println(indent + " ._retainState = " + reflectObjValue(brokerImpl, "_retainState"));
            out.println(indent + " ._autoClear = " + reflectObjValue(brokerImpl, "_autoClear"));
            out.println(indent + " ._restoreState = " + reflectObjValue(brokerImpl, "_restoreState"));
            out.println(indent + " ._optimistic = " + reflectObjValue(brokerImpl, "_optimistic"));
            out.println(indent + " ._ignoreChanges = " + reflectObjValue(brokerImpl, "_ignoreChanges"));
            out.println(indent + " ._multithreaded = " + reflectObjValue(brokerImpl, "_multithreaded"));
            out.println(indent + " ._managed = " + reflectObjValue(brokerImpl, "_managed"));
            out.println(indent + " ._syncManaged = " + reflectObjValue(brokerImpl, "_syncManaged"));
            out.println(indent + " ._connRetainMode = " + reflectObjValue(brokerImpl, "_connRetainMode"));
            out.println(indent + " ._evictDataCache = " + reflectObjValue(brokerImpl, "_evictDataCache"));
            out.println(indent + " ._populateDataCache = " + reflectObjValue(brokerImpl, "_connRetainMode"));
            out.println(indent + " ._connRetainMode = " + reflectObjValue(brokerImpl, "_populateDataCache"));
            out.println(indent + " ._largeTransaction = " + reflectObjValue(brokerImpl, "_largeTransaction"));
            out.println(indent + " ._autoDetach = " + reflectObjValue(brokerImpl, "_autoDetach"));
            out.println(indent + " ._detachState = " + reflectObjValue(brokerImpl, "_detachState"));
            out.println(indent + " ._detachedNew = " + reflectObjValue(brokerImpl, "_detachedNew"));
            out.println(indent + " ._orderDirty = " + reflectObjValue(brokerImpl, "_orderDirty"));
            out.println(indent + " ._cachePreparedQuery = " + reflectObjValue(brokerImpl, "_cachePreparedQuery"));
            out.println(indent + " ._cacheFinderQuery = " + reflectObjValue(brokerImpl, "_cacheFinderQuery"));
            out.println(indent + " ._suppressBatchOLELogging = " + reflectObjValue(brokerImpl, "_suppressBatchOLELogging"));
            out.println(indent + " ._allowReferenceToSiblingContext = " + reflectObjValue(brokerImpl, "_allowReferenceToSiblingContext"));
            out.println(indent + " ._postLoadOnMerge = " + reflectObjValue(brokerImpl, "_postLoadOnMerge"));
            out.println(indent + " ._flags = " + reflectObjValue(brokerImpl, "_flags"));
            out.println(indent + " ._isSerializing = " + reflectObjValue(brokerImpl, "_isSerializing"));
            out.println(indent + " ._closed = " + reflectObjValue(brokerImpl, "_closed"));
            out.println(indent + " ._transEventManager = " + reflectObjValue(brokerImpl, "_transEventManager"));
            out.println(indent + " ._transCallbackMode = " + reflectObjValue(brokerImpl, "_transCallbackMode"));
            out.println(indent + " ._lifeEventManager = " + reflectObjValue(brokerImpl, "_lifeEventManager"));
            out.println(indent + " ._lifeCallbackMode = " + reflectObjValue(brokerImpl, "_lifeCallbackMode"));
            out.println(indent + " ._dmLite = " + reflectObjValue(brokerImpl, "_dmLite"));
            out.println(indent + " ._initializeWasInvoked = " + reflectObjValue(brokerImpl, "_initializeWasInvoked"));
            out.println(indent + " ._fromWriteBehindCallback = " + reflectObjValue(brokerImpl, "_fromWriteBehindCallback"));
            out.println(indent + " ._fcs = " + reflectObjValue(brokerImpl, "_fcs"));
            out.println(indent + " ._printParameters = " + reflectObjValue(brokerImpl, "_printParameters"));

            final Map _userObjects = (Map) reflectObjValue(brokerImpl, "_userObjects"); // Map<Object, Object>
            out.println(indent + " ._userObjects size = " + ((_userObjects != null) ? _userObjects.size() : 0));

            final Map _cache = (Map) reflectObjValue(brokerImpl, "_cache"); // ManagedCache
            out.println(indent + " ._cache = " + _cache);

        } catch (Throwable t) {

        }
    }

    /*
     * OpenJPA Diagnostic Enhancements
     */
    private static void doDumpOpenJPAPCRegistry(final Class pcRegClass, final PrintWriter out) {
        out.println();
        out.println("################################################################################");
        out.println("OpenJPA PCRegistry Dump");
        out.println("################################################################################");

        // Access the PCRegistry._metas Map<Class<?>,Meta> static field
        try {
            final Field _metas = pcRegClass.getDeclaredField("_metas");
            final boolean accessible = _metas.isAccessible();
            Object _metasObjVal = null; // Type is Map<Class<?>,Meta>
            try {
                _metas.setAccessible(true);
                _metasObjVal = _metas.get(null);
            } finally {
                _metas.setAccessible(accessible);
            }

            if (_metasObjVal == null) {
                return;
            }

            final Map<?, ?> _metasMap = (Map<?, ?>) _metasObjVal;
            for (Map.Entry<?, ?> entry : _metasMap.entrySet()) {
                final Class cls = (Class) entry.getKey();
                final Object metaObj = entry.getValue();

                out.println();
                out.println("Class " + cls.getName() + " " + getObjectAddress(cls));
                out.println("  ClassLoader: " + poa(cls.getClassLoader()));
                out.println("  CodeSource: " + poa(cls.getProtectionDomain().getCodeSource()));

                if (metaObj == null) {
                    continue;
                }

                try {
                    final Class metaObjClass = metaObj.getClass();
                    final String alias = (String) reflectObjValue(metaObj, "alias");
                    final Object pc = reflectObjValue(metaObj, "pc"); // PersistenceCapable type
                    final Class pcSuper = (Class) reflectObjValue(metaObj, "pcSuper");
                    final String pcTxt = getInstanceClassAndAddress(pc); // Calling a PC's toString() can be dangerous.  Don't do it.

                    out.println("  Meta alias: " + alias); // metaObjClass.getDeclaredField("alias").get(metaObj)); // String
                    out.println("  Meta pc: " + poa(pcTxt)); // metaObjClass.getDeclaredField("pc").get(metaObj)); // PersistenceCapable
                    out.println("  Meta pcSuper: " + poa(pcSuper)); // metaObjClass.getDeclaredField("pcSuper").get(metaObj));

                    out.print("  Meta fieldNames: ");
                    final Object[] fieldNames = (Object[]) reflectObjValue(metaObj, "fieldNames"); // metaObjClass.getDeclaredField("fieldNames").get(metaObj); // String[]
                    for (Object o : fieldNames) {
                        out.print(poa(o));
                        out.print(" ");
                    }
                    out.println();

                    out.print("  Meta fieldTypes: ");
                    final Object[] fieldTypes = (Object[]) reflectObjValue(metaObj, "fieldTypes"); // (Object[]) metaObjClass.getDeclaredField("fieldTypes").get(metaObj); // Class<?>[]
                    for (Object o : fieldTypes) {
                        out.print(poa(o));
                        out.print(" ");
                    }
                    out.println();
                } catch (Throwable t) {
                    FFDCFilter.processException(t, FFDCCN + ".doDumpOpenJPAPCRegistry", "708");
                }
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, FFDCCN + ".doDumpOpenJPAPCRegistry", "713");
        }
    }
}
