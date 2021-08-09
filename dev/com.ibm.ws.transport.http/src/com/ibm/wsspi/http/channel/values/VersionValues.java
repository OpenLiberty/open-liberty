/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.values;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.wsspi.genericbnf.GenericKeys;

/**
 * Class representing possible values for the HTTP message version.
 */
public class VersionValues extends GenericKeys {

    /** Counter of the number of values defined so far */
    private static final AtomicInteger NEXT_ORDINAL = new AtomicInteger(0);
    /** List keeping track of all the values, used by the matcher */
    private static final List<VersionValues> allKeys = new ArrayList<VersionValues>();
    /** Matcher used for these enum objects */
    private static final VersionValuesMatcher myMatcher = new VersionValuesMatcher();

    // Note: cannot remove undefined because serialization/binary HTTP requires
    // existing ordinals not to change
    /** Enumerated object for an undefined HTTP Version */
    public static final VersionValues UNDEF = new VersionValues("Undefined");
    /** Enumerated object for HTTP Version 0.9 */
    public static final VersionValues V09 = new VersionValues(0, 9);
    /** Enumerated object for HTTP Version 1.0 */
    public static final VersionValues V10 = new VersionValues(1, 0);
    /** Enumerated object for HTTP Version 1.1 */
    public static final VersionValues V11 = new VersionValues(1, 1);
    /** Enumerated object for HTTP Version 2.0 */
    public static final VersionValues V20 = new VersionValues(2, 0);

    /** Major version number */
    private int major = 0;
    /** Minor version number */
    private int minor = 0;
    /** Flag on whether or not this instance is an undefined one */
    private boolean undefined = false;

    /**
     * Constructor that takes the given String and creates a new VersionValues
     * object from it.
     * 
     * @param name
     */
    private VersionValues(String name) {
        super(name, NEXT_ORDINAL.getAndIncrement());
        allKeys.add(this);
        myMatcher.add(this);
    }

    /**
     * Constructor with explicit major and minor versions.
     * 
     * @param inMajor
     * @param inMinor
     */
    protected VersionValues(int inMajor, int inMinor) {
        super("HTTP/" + inMajor + "." + inMinor, NEXT_ORDINAL.getAndIncrement());
        this.major = inMajor;
        this.minor = inMinor;
        allKeys.add(this);
        myMatcher.add(this);
    }

    /**
     * Query the enumerated value that exists with the specified ordinal
     * value.
     * 
     * @param i
     * @return VersionValues
     * @throws IndexOutOfBoundsException
     */
    public static VersionValues getByOrdinal(int i) {
        return allKeys.get(i);
    }

    /**
     * Query the value of the Version's major number
     * 
     * @return int
     */
    public int getMajor() {
        return this.major;
    }

    /**
     * Query the value of the Version's minor number
     * 
     * @return int
     */
    public int getMinor() {
        return this.minor;
    }

    /**
     * Query whether this class instance is an undefined value or not.
     * 
     * @return boolean
     */
    public boolean isUndefined() {
        return this.undefined;
    }

    /**
     * Set the undefined flag on this enum object to the input value.
     * 
     * @param flag
     */
    protected void setUndefined(boolean flag) {
        this.undefined = flag;
    }

    /**
     * Find the enumerated object that matchs the input name using the given
     * offset and length into that name. If none exist, then a null value is
     * returned.
     * 
     * @param name
     * @param offset
     *            - starting point in that name
     * @param length
     *            - length to use from that starting point
     * @return VersionValues
     */
    public static VersionValues match(String name, int offset, int length) {
        if (null == name)
            return null;
        return myMatcher.match(name, offset, length, false);
    }

    /**
     * Find the enumerated object that matchs the input name using the given
     * offset and length into that name. If none exist, then a null value is
     * returned.
     * 
     * @param name
     * @param offset
     *            - starting point in that name
     * @param length
     *            - length to use from that offset
     * @return VersionValues - null only if the input name is null
     */
    public static VersionValues match(byte[] name, int offset, int length) {
        if (null == name)
            return null;
        return myMatcher.match(name, offset, length, false);
    }

    /**
     * Find the enumerated object matching the input name. If this name has
     * never been seen prior, then a new object is created by this call.
     * 
     * @param name
     * @param offset
     *            - starting point in that input name
     * @param length
     *            - length to use from that offset
     * @return VersionValues
     * @throws IllegalArgumentException
     *             - if format is incorrect
     * @throws NullPointerException
     *             if input name is null
     */
    public static VersionValues find(byte[] name, int offset, int length) {
        return myMatcher.match(name, offset, length, true);
    }

    /**
     * Find the enumerated object matching the input name. If this name has
     * never been seen prior, then a new object is created by this call.
     * 
     * @param name
     * @return VersionValues
     * @throws IllegalArgumentException
     *             - if format is incorrect
     * @throws NullPointerException
     *             if input name is null
     */
    public static VersionValues find(String name) {
        return myMatcher.match(name, 0, name.length(), true);
    }

    /**
     * Find the enumerated object matching the input name. If this name has
     * never been seen prior, then a new object is created by this call.
     * 
     * @param name
     * @return VersionValues
     * @throws IllegalArgumentException
     *             - if format is incorrect
     * @throws NullPointerException
     *             if input name is null
     */
    public static VersionValues find(byte[] name) {
        return myMatcher.match(name, 0, name.length, true);
    }

    /**
     * Matcher for version values that will handle comparing and storing versions
     * based on the major and minor version numbers.
     */
    private static class VersionValuesMatcher {
        /** Hardcoded byte[] that starts all HTTP versions */
        private static final byte[] HTTP_BYTES = { 'H', 'T', 'T', 'P', '/' };
        /** Hardcode char[] that starts all HTTP versions */
        private static final char[] HTTP_CHARS = { 'H', 'T', 'T', 'P', '/' };

        /** List of versions stored based on the major number */
        private VersionBucket[] list = null;

        /**
         * Create a matcher.
         */
        protected VersionValuesMatcher() {
            this.list = new VersionBucket[2];
        }

        /**
         * Scan the input name, starting at the given offset for the input length
         * of characters and look for a match. If none is found, check the create
         * flag on whether to make a new object or not.
         * 
         * @param name
         * @param offset
         * @param length
         * @param create
         * @return VersionValues - null if not found
         * @throws IllegalArgumentException
         *             if create is true and the input is invalid format
         */
        protected VersionValues match(String name, int offset, int length, boolean create) {
            if (null == name || 8 > length) {
                // mininum value is HTTP/x.x so this is a definite non-match
                if (create) {
                    throw new IllegalArgumentException("Invalid version: " + name + " " + offset + " " + length);
                }
                return null;
            }
            int i = offset;
            char[] data = name.toCharArray();
            // test the required "HTTP/" up front
            for (int x = 0; x < 5; i++, x++) {
                if (data[i] != HTTP_CHARS[x]) {
                    if (create) {
                        throw new IllegalArgumentException("Invalid version: " + name.substring(offset, offset + length));
                    }
                    return null;
                }
            }
            // now we need to parse the major version, ignoring leading zeros
            // and possible whitespace before or after
            int major = -1;
            for (char c; i < data.length && '.' != data[i]; i++) {
                c = data[i];
                if ('0' <= c && '9' >= c) {
                    if (-1 == major) {
                        major = (c - '0');
                    } else {
                        major *= 10;
                        major += (c - '0');
                    }
                } else if (' ' == c || '\t' == c) {
                    if (0 >= major)
                        continue; // leading whitespace
                    // trailing whitespace must extend to the period delimiter
                    for (; i < data.length && '.' != data[i]; i++) {
                        if (' ' != data[i] && '\t' != data[i]) {
                            if (create) {
                                throw new IllegalArgumentException("Invalid version: " + name.substring(offset, offset + length));
                            }
                            return null;
                        }
                    }
                    break; // out of major-parsing-for-loop
                } else {
                    // found non-whitespace/non-number
                    if (create) {
                        throw new IllegalArgumentException("Invalid version: " + name.substring(offset, offset + length));
                    }
                    return null;
                }
            }
            // skip past the period delimiter
            if (-1 == major || ++i >= data.length) {
                if (create) {
                    throw new IllegalArgumentException("Invalid version: " + name.substring(offset, offset + length));
                }
                return null;
            }
            // now we need to parse the minor version, ignoring leading zeros
            // and possible whitespace before or after
            int minor = -1;
            for (char c; i < data.length && '.' != data[i]; i++) {
                c = data[i];
                if ('0' <= c && '9' >= c) {
                    if (-1 == minor) {
                        minor = (c - '0');
                    } else {
                        minor *= 10;
                        minor += (c - '0');
                    }
                } else if (' ' == c || '\t' == c) {
                    if (0 >= minor)
                        continue; // leading whitespace
                    // trailing whitespace must extend to the end
                    for (; i < data.length; i++) {
                        if (' ' != data[i] && '\t' != data[i]) {
                            if (create) {
                                throw new IllegalArgumentException("Invalid version: " + name.substring(offset, offset + length));
                            }
                            return null;
                        }
                    }
                    break; // out of minor-parsing-for-loop
                } else {
                    // found non-whitespace/non-number
                    if (create) {
                        throw new IllegalArgumentException("Invalid version: " + name.substring(offset, offset + length));
                    }
                    return null;
                }
            }
            if (-1 == minor) {
                if (create) {
                    throw new IllegalArgumentException("Invalid version: " + name.substring(offset, offset + length));
                }
                return null;
            }

            VersionBucket bucket = get(major);
            VersionValues val = (null == bucket) ? null : bucket.get(minor);
            if (null == val && create) {
                synchronized (VersionValues.class) {
                    // protect against 2 threads getting here on the new value by
                    // testing again inside a sync block
                    bucket = get(major);
                    val = (null == bucket) ? null : bucket.get(minor);
                    if (null == val) {
                        val = new VersionValues(major, minor);
                        val.setUndefined(true);
                    }
                } // end-sync
            }
            return val;
        }

        /**
         * Scan the input name, starting at the given offset for the input length
         * of characters and look for a match. If none is found, check the create
         * flag on whether to make a new object or not.
         * 
         * @param data
         * @param offset
         * @param length
         * @param create
         * @return VersionValues - null if not found
         * @throws IllegalArgumentException
         *             if create is true and the input is invalid format
         */
        protected VersionValues match(byte[] data, int offset, int length, boolean create) {
            if (null == data || 8 > length) {
                // mininum value is HTTP/x.x so this is a definite non-match
                if (create) {
                    throw new IllegalArgumentException("Invalid version: " + data + " " + offset + " " + length);
                }
                return null;
            }
            int i = offset;
            // test the required "HTTP/" up front
            for (int x = 0; x < 5; i++, x++) {
                if (data[i] != HTTP_BYTES[x]) {
                    if (create) {
                        throw new IllegalArgumentException("Invalid version: " + new String(data, offset, length));
                    }
                    return null;
                }
            }
            // now we need to parse the major version, ignoring leading zeros
            // and possible whitespace before or after
            int major = -1;
            for (byte c; i < data.length && '.' != data[i]; i++) {
                c = data[i];
                if ('0' <= c && '9' >= c) {
                    if (-1 == major) {
                        major = (c - '0');
                    } else {
                        major *= 10;
                        major += (c - '0');
                    }
                } else if (' ' == c || '\t' == c) {
                    if (0 >= major)
                        continue; // leading whitespace
                    // trailing whitespace must extend to the period delimiter
                    for (; i < data.length && '.' != data[i]; i++) {
                        if (' ' != data[i] && '\t' != data[i]) {
                            if (create) {
                                throw new IllegalArgumentException("Invalid version: " + new String(data, offset, length));
                            }
                            return null;
                        }
                    }
                    break; // out of major-parsing-for-loop
                } else {
                    // found non-whitespace/non-number
                    if (create) {
                        throw new IllegalArgumentException("Invalid version: " + new String(data, offset, length));
                    }
                    return null;
                }
            }
            // skip past the period delimiter
            if (-1 == major || ++i >= data.length) {
                if (create) {
                    throw new IllegalArgumentException("Invalid version: " + new String(data, offset, length));
                }
                return null;
            }
            // now we need to parse the minor version, ignoring leading zeros
            // and possible whitespace before or after
            int minor = -1;
            for (byte c; i < data.length && '.' != data[i]; i++) {
                c = data[i];
                if ('0' <= c && '9' >= c) {
                    if (-1 == minor) {
                        minor = (c - '0');
                    } else {
                        minor *= 10;
                        minor += (c - '0');
                    }
                } else if (' ' == c || '\t' == c) {
                    if (0 >= minor)
                        continue; // leading whitespace
                    // trailing whitespace must extend to the end
                    for (; i < data.length; i++) {
                        if (' ' != data[i] && '\t' != data[i]) {
                            if (create) {
                                throw new IllegalArgumentException("Invalid version: " + new String(data, offset, length));
                            }
                            return null;
                        }
                    }
                    break; // out of minor-parsing-for-loop
                } else {
                    // found non-whitespace/non-number
                    if (create) {
                        throw new IllegalArgumentException("Invalid version: " + new String(data, offset, length));
                    }
                    return null;
                }
            }
            if (-1 == minor) {
                if (create) {
                    throw new IllegalArgumentException("Invalid version: " + new String(data, offset, length));
                }
                return null;
            }

            VersionBucket bucket = get(major);
            VersionValues val = (null == bucket) ? null : bucket.get(minor);
            if (null == val && create) {
                synchronized (VersionValues.class) {
                    // protect against 2 threads getting here on the new value by
                    // testing again inside a sync block
                    bucket = get(major);
                    val = (null == bucket) ? null : bucket.get(minor);
                    if (null == val) {
                        val = new VersionValues(major, minor);
                        val.setUndefined(true);
                    }
                } // end-sync
            }
            return val;
        }

        /**
         * Add the input key to this bucket.
         * 
         * @param key
         */
        protected synchronized void add(VersionValues key) {
            int major = key.getMajor();
            if (major >= this.list.length) {
                VersionBucket[] newlist = new VersionBucket[major + 1];
                for (int i = 0; i < this.list.length; i++) {
                    newlist[i] = this.list[i];
                }
                this.list = newlist;
            }
            VersionBucket bucket = this.list[major];
            if (null == bucket) {
                bucket = new VersionBucket();
                this.list[major] = bucket;
            }
            bucket.add(key);
        }

        /**
         * Access the storage for a given major version number, if it exists.
         * 
         * @param major
         * @return VersionBucket - null if none defined for this major version
         */
        private VersionBucket get(int major) {
            if (major >= this.list.length) {
                return null;
            }
            return this.list[major];
        }
    }

    /**
     * Bucket that holds all the defined objects for a single major version
     * number.
     */
    private static class VersionBucket {
        /** List of defined values for this major version */
        private VersionValues[] list = null;

        /**
         * Create a new bucket of minor version numbers.
         * 
         */
        protected VersionBucket() {
            this.list = new VersionValues[0];
        }

        /**
         * Add the input key to this bucket.
         * 
         * @param key
         */
        protected void add(VersionValues key) {
            int minor = key.getMinor();
            if (minor >= this.list.length) {
                VersionValues[] newlist = new VersionValues[minor + 1];
                for (int i = 0; i < this.list.length; i++) {
                    newlist[i] = this.list[i];
                }
                this.list = newlist;
            }
            this.list[minor] = key;
        }

        /**
         * Query any stored value based on the input minor version number.
         * 
         * @param minor
         * @return VersionValues - null if not found
         */
        protected VersionValues get(int minor) {
            if (minor >= this.list.length) {
                return null;
            }
            return this.list[minor];
        }
    }
}
