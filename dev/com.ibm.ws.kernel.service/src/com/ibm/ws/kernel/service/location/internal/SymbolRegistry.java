/*******************************************************************************
 * Copyright (c) 2010,2025 IBM Corporation and others.
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
package com.ibm.ws.kernel.service.location.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.location.MalformedLocationException;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.OsgiPropertyUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 *
 */
public class SymbolRegistry {
    final static TraceComponent tc = Tr.register(SymbolRegistry.class);

    /** ${} **/
    final static Pattern SYMBOL_DEF = Pattern.compile("\\$\\{([^\\$\\(\\)]*?)\\}");

    /** Singleton instance */
    private final static SymbolRegistry instance = new SymbolRegistry();

    final static int RECURSE_LIMIT = 100;

    /** Type of value associated with a registered variable */
    enum EntryType {
        ROOT, RESOURCE, STRING
    };

    /**
     * @return singleton instance of the symbol registry
     */
    @Trivial
    public static SymbolRegistry getRegistry() {
        return instance;
    }

    /**
     * Inner class: struct grouping a symbol and associated value.
     * The type is used for grouping/dealing with values without
     * instanceof.
     *
     * SymRegEntry is used in several different lists, with
     * different comparators depending on the index.
     */
    static class SymRegEntry {
        final EntryType type;
        final String symbol;
        final Object value;

        @Trivial
        SymRegEntry(EntryType t, String s, Object v) {
            type = t;
            symbol = s;
            value = v;
        }
    }

    static class RootRegEntry extends SymRegEntry implements Comparable<RootRegEntry> {
        final String path;

        @Trivial
        RootRegEntry(String s, LocalDirectoryResource v) {
            super(EntryType.ROOT, s, v);
            path = (v.getNormalizedPath() == null) ? v.toRepositoryPath() : v.getNormalizedPath();
        }

        /**
         * Override compareTo to sort paths from longest to shortest. When iterating
         * through known paths to find the appropriate root, this ensures the most
         * specified
         * path will match first.
         */
        @Override
        @Trivial
        public int compareTo(RootRegEntry o) {
            if (this == o)
                return 0;

            if (this.path.length() == o.path.length())
                return this.path.compareTo(o.path);

            return (this.path.length() > o.path.length()) ? -1 : 1;
        }

        @Override
        public String toString() {
            return path.length() + "-" + path;
        }
    }

    private static final String EVENT_ADDED = "ADDED " + WsLocationConstants.SYMBOL_PREFIX;
    private static final String EVENT_REPLACED = "REPLACED " + WsLocationConstants.SYMBOL_PREFIX;
    private static final String EVENT_ADDED_MIDDLE = WsLocationConstants.SYMBOL_SUFFIX + "=";

    /** Map of string symbol to entry */
    private final ConcurrentHashMap<String, SymRegEntry> stringToEntry = new ConcurrentHashMap<String, SymRegEntry>();
    private final ConcurrentSkipListSet<RootRegEntry> rootPaths = new ConcurrentSkipListSet<RootRegEntry>();

    public boolean addResourceSymbol(String symbol, InternalWsResource value) {
        SymRegEntry entry = new SymRegEntry(EntryType.RESOURCE, symbol, value);
        SymRegEntry prev = stringToEntry.putIfAbsent(symbol, entry);
        if (prev == null && TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(instance, tc, EVENT_ADDED + symbol + EVENT_ADDED_MIDDLE + value);

        return prev == null;
    }

    public synchronized boolean addRootSymbol(String symbol, LocalDirectoryResource value) {
        RootRegEntry entry = new RootRegEntry(symbol, value);
        SymRegEntry prev = stringToEntry.putIfAbsent(symbol, entry);

        if (prev == null) {
            if (value instanceof SymbolicRootResource)
                rootPaths.add(entry);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(instance, tc, EVENT_ADDED + symbol + EVENT_ADDED_MIDDLE + value);

            return true;
        }
        return false;
    }

    public boolean addStringSymbol(String symbol, String value) {
        SymRegEntry entry = new SymRegEntry(EntryType.STRING, symbol, value);
        SymRegEntry prev = stringToEntry.putIfAbsent(symbol, entry);
        if (prev == null && TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(instance, tc, EVENT_ADDED + symbol + EVENT_ADDED_MIDDLE + value);

        return prev == null;
    }

    public void replaceStringSymbol(String symbol, String value) {
        SymRegEntry entry = new SymRegEntry(EntryType.STRING, symbol, value);
        SymRegEntry prev = stringToEntry.put(symbol, entry);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            if (prev == null) {
                Tr.event(instance, tc, EVENT_ADDED + symbol + EVENT_ADDED_MIDDLE + value);
            } else {
                Tr.event(instance, tc, EVENT_REPLACED + symbol + EVENT_ADDED_MIDDLE + value);
            }
        }
    }

    public synchronized void removeSymbol(String symbol) {
        SymRegEntry entry = stringToEntry.remove(symbol);
        if (entry != null && entry.type == EntryType.ROOT)
            rootPaths.remove(entry);
    }

    public void clear() {
        stringToEntry.clear();
        rootPaths.clear();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(instance, tc, "Symbol registry cleared");
    }

    /**
     * @param resourcePath
     * @return
     */
    InternalWsResource resolveSymbolicResource(String symbolicPath) {
        if (symbolicPath == null)
            throw new NullPointerException("Path must be non-null");

        return resolveSymbols(symbolicPath, symbolicPath, 0);
    }

    /**
     * Resolves the given string, evaluating all symbols, and path-normalizes
     * the value.
     *
     * @param symbolicPath
     *
     * @return The resolved value, path-normalized.
     */
    String resolveSymbolicString(String symbolicPath) {
        if (symbolicPath == null)
            throw new NullPointerException("Path must be non-null");

        return resolveStringSymbols(symbolicPath, symbolicPath, true, 0, true);
    }

    /**
     * Resolves the given string, evaluating all symbols, but does NOT path-normalize
     * the value.
     *
     * @param string
     *
     * @return The resolved value, not path-normalized.
     */
    String resolveRawSymbolicString(String string) {
        if (string == null)
            throw new NullPointerException("String must be non-null");

        return resolveStringSymbols(string, string, true, 0, false);
    }

    SymbolicRootResource findRoot(String normalPath) {
        for (RootRegEntry entry : rootPaths) {
            if (normalPath.length() >= entry.path.length() && normalPath.startsWith(entry.path))
                return (SymbolicRootResource) entry.value;
        }

        // For now, returning null instead of throwing an error to allow files to be loaded
        // from outside the server root.
        return null;

        //       Tr.error(tc, "unreachableLocation", normalPath);
        //       throw new IllegalStateException("Unreachable location " + normalPath);
    }

    /**
     * Recursively process symbols (primarily string substitution) with a
     * recursion limit of 10
     *
     * @param originalPath
     *                         Remember original path for messages if there is a problem
     *                         resolving symbols
     * @param path
     *                         Revised path (recursion)
     * @param recurse
     *                         Recursion depth
     * @return Resolved WsResource, or null if an unknown symbol is
     *         encountered
     */
    private InternalWsResource resolveSymbols(final String originalPath, final String path, int recurse) {
        if (recurse > RECURSE_LIMIT)
            throw new IllegalStateException("Exceeded recursion limit (orig=" + originalPath + ",path=" + path + ")");

        Matcher m = SymbolRegistry.SYMBOL_DEF.matcher(path);
        if (m.find()) {
            String symbol = m.group(1);
            String fullMatch = m.group();

            SymRegEntry entry = stringToEntry.get(symbol);

            if (entry != null) {
                if (path.equals(fullMatch)) {
                    // If the resourcePath and the symbol are the same,
                    // get the associated entry, and either return directly
                    // or perform substitution and recurse
                    switch (entry.type) {
                        case RESOURCE:
                        case ROOT:
                            return (InternalWsResource) entry.value;
                        case STRING:
                            return resolveSymbols(originalPath, (String) entry.value, recurse + 1);
                    }
                } else {
                    int start = m.start();
                    int end = m.end();

                    String relativePath = null;

                    switch (entry.type) {
                        case RESOURCE:
                        case ROOT:
                            WsResource ar = (WsResource) entry.value;
                            if (ar.isType(WsResource.Type.FILE) && end != path.length())
                                throw new MalformedLocationException("Can not resolve additional path parameters against file resource");

                            relativePath = getRelativePath(originalPath, path, start, end, recurse);
                            return (InternalWsResource) ar.resolveRelative(relativePath);
                        case STRING:
                            String value = (String) entry.value;

                            StringBuilder sb = new StringBuilder(start + value.length() + (path.length() - end));

                            sb.append(path.substring(0, start));
                            sb.append(entry.value);
                            sb.append(path.substring(end));

                            return resolveSymbols(originalPath, sb.toString(), recurse + 1);
                    }
                }
            } else {
                String value = OsgiPropertyUtils.getProperty(symbol, null);
                if (value != null) {
                    int start = m.start();
                    int end = m.end();

                    StringBuilder sb = new StringBuilder(start + value.length() + (path.length() - end));

                    sb.append(path.substring(0, start));
                    sb.append(value);
                    sb.append(path.substring(end));

                    String newPath = sb.toString();

                    return WsLocationAdminImpl.getInstance().resolveResource(newPath);
                }

            }
        } // end if m.find()
        throw new MalformedLocationException("Unable to resolve symbolic path (path=" + path + ")");
    }

    private String getRelativePath(final String originalPath, final String path, int start, int end, int recurse) {
        if (path.length() < end)
            throw new MalformedLocationException("Invalid symbolic path: end of symbol is beyond length of path (orig=" + originalPath + ",path=" + path + ")");

        if (path.length() <= end + 1)
            return "";

        String relativePath = resolveStringSymbols(originalPath, path.substring(end + 1), false, recurse, true);

        return relativePath;
    }

    private String resolveStringSymbols(final String originalPath, String path, final boolean includeRoots, int recurse, boolean pathNormalize) {
        if (recurse > RECURSE_LIMIT)
            throw new IllegalStateException("Exceeded recursion limit (orig=" + originalPath + ",path=" + path + ")");

        if (pathNormalize) {
            path = PathUtils.normalize(path);
        }

        Matcher m = SymbolRegistry.SYMBOL_DEF.matcher(path);
        if (m.find()) {
            String symbol = m.group(1);
            String fullMatch = m.group();
            SymRegEntry entry = stringToEntry.get(symbol);

            if (entry != null) {
                switch (entry.type) {
                    default:
                    case RESOURCE:
                    case ROOT:
                        if (includeRoots) {
                            String value = ((InternalWsResource) entry.value).getNormalizedPath();

                            if (path.equals(symbol))
                                return value;
                            else {
                                return replaceAndResolveString(originalPath, path, includeRoots, recurse, m, value, pathNormalize);
                            }
                        } else
                            throw new MalformedLocationException("Reference to a symbolic root in the path of another symbol (path=" + path + ",origPath=" + originalPath + ")");
                    case STRING:
                        if (path.equals(fullMatch))
                            return resolveStringSymbols(originalPath, (String) entry.value, includeRoots, recurse + 1, pathNormalize);
                        else {
                            String value = (String) entry.value;

                            return replaceAndResolveString(originalPath, path, includeRoots, recurse, m, value, pathNormalize);
                        }
                }
            } else {
                String value = OsgiPropertyUtils.getProperty(symbol, null);
                if (value != null) {
                    return replaceAndResolveString(originalPath, path, includeRoots, recurse, m, value, pathNormalize);
                } else if (symbol.startsWith("env.")) {
                    value = System.getenv(symbol.substring(4));
                    if (value != null) {
                        return replaceAndResolveString(originalPath, path, includeRoots, recurse, m, value, pathNormalize);
                    }
                }
            }
        }

        return path;
    }

    /**
     * @param originalPath
     * @param path
     * @param includeRoots
     * @param recurse
     * @param m
     * @param value
     * @return
     */
    private String replaceAndResolveString(final String originalPath,
                                           String path,
                                           final boolean includeRoots,
                                           int recurse,
                                           Matcher m,
                                           String value,
                                           final boolean pathNormalize) {
        int start = m.start();
        int end = m.end();

        StringBuilder sb = new StringBuilder(start + value.length() + (path.length() - end));

        sb.append(path.substring(0, start));
        sb.append(value);
        sb.append(path.substring(end));

        return resolveStringSymbols(originalPath, sb.toString(), includeRoots, recurse + 1, pathNormalize);
    }

}
