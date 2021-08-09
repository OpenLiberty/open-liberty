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
package com.ibm.wsspi.genericbnf;

/**
 * Matcher utility class that stores an enumerated list and will take various
 * input values to compare and find matchs.
 */
public class KeyMatcher {

    /** Is this matcher case-sensitive */
    private boolean isCaseSensitive = false;
    /** List of buckets, one per allowed leading character */
    private final KeyBucket[] buckets = new KeyBucket[255];

    /**
     * Constructor for a key matcher that uses the input case-sensitive flag
     * for comparisons.
     *
     * @param caseSensitive
     */
    public KeyMatcher(boolean caseSensitive) {
        for (int i = 0; i < this.buckets.length; i++) {
            this.buckets[i] = null;
        }
        this.isCaseSensitive = caseSensitive;
    }

    /**
     * Access the bucket for this specific character.
     *
     * @param c
     * @return HeaderBucket
     */
    protected KeyBucket getBucket(char c) {
        if (c > this.buckets.length) {
            // can't handle non-ASCII chars
            return null;
        }
        int index = c;
        // if we're case-insensitive, push uppercase into lowercase buckets
        if (!isCaseSensitive() && (c >= 'A' && c <= 'Z')) {
            index += 32;
        }
        return this.buckets[index];
    }

    /**
     * Access the bucket for this specific character.
     *
     * @param c
     * @return HeaderBucket
     */
    protected KeyBucket makeBucket(char c) {
        if (c > this.buckets.length) {
            // can't handle non-ASCII chars
            return null;
        }
        int index = c;
        // if we're case-insensitive, push uppercase into lowercase buckets
        if (!isCaseSensitive() && (c >= 'A' && c <= 'Z')) {
            index += 32;
        }
        if (null == this.buckets[index]) {
            this.buckets[index] = new KeyBucket();
        }
        return this.buckets[index];
    }

    /**
     * Query whether this matcher is case-sensitive during comparisons.
     *
     * @return boolean
     */
    protected boolean isCaseSensitive() {
        return this.isCaseSensitive;
    }

    /**
     * Add a new enumerated object to the matcher.
     *
     * @param key
     */
    public synchronized void add(GenericKeys key) {
        KeyBucket bucket = makeBucket(key.getName().charAt(0));
        if (null != bucket) {
            bucket.add(key);
        }
    }

    /**
     * Compare the input value against the stored list of objects and return a
     * match if found.
     *
     * @param name
     * @param start
     * @param length
     * @return GenericKeys, null if not found
     */
    public GenericKeys match(String name, int start, int length) {
        if (null == name || 0 == name.length() || start < 0 || length > name.length()) {
            return null;
        }
        KeyBucket bucket = getBucket(name.charAt(start));
        if (null != bucket) {
            return bucket.match(name, start, length);
        }
        return null;
    }

    /**
     * Compare the input value against the stored list of objects and return a
     * match if found.
     *
     * @param name
     * @param start
     * @param length
     * @return GenericKeys, null if not found
     */
    public GenericKeys match(byte[] name, int start, int length) {
        if (null == name || 0 == name.length || start < 0 || length > name.length) {
            return null;
        }
        KeyBucket bucket = getBucket((char) name[start]);
        if (null != bucket) {
            return bucket.match(name, start, length);
        }
        return null;
    }

    /**
     * Individual bucket that encapsulates each of the enumerated items that
     * have the same leading character.
     */
    public class KeyBucket {
        /** List of defined objects */
        private GenericKeys[] list = null;
        /** Stored list of names of those keys */
        private char[][] values = null;
        /** Number of items on that list */
        private int number = 0;

        /**
         * Constructor for this bucket.
         */
        public KeyBucket() {
            this.list = new GenericKeys[10];
            this.values = new char[10][];
        }

        /**
         * Add the input key to this bucket.
         *
         * @param key
         */
        protected void add(GenericKeys key) {
            if (this.number == this.list.length) {
                GenericKeys[] newlist = new GenericKeys[this.number + 10];
                char[][] newvalues = new char[newlist.length][];
                for (int i = 0; i < this.number; i++) {
                    newlist[i] = this.list[i];
                    newvalues[i] = this.values[i];
                }
                this.list = newlist;
                this.values = newvalues;
            }
            this.list[this.number] = key;
            this.values[this.number] = key.getName().toCharArray();
            this.number++;
        }

        /**
         * Compare the input value against the stored list of objects and return a
         * match if found.
         *
         * @param data
         * @param start
         * @param length
         * @return GenericKeys, null if not found
         */
        protected GenericKeys match(byte[] data, int start, int length) {
            int end = start + length - 1;
            int x, y;
            // save local refs to avoid problems with concurrent add() calls
            int stop = this.number;
            char[][] vlist = this.values;
            char[] temp;
            for (int i = 0; i < stop; i++) {
                temp = vlist[i];
                if (temp.length == length) {
                    // potential match, scan backwards because most things vary
                    // later on... Content-Length vs Content-Language for example
                    for (x = end, y = temp.length - 1; x >= start; x--, y--) {
                        if (!isEqual((char) data[x], temp[y])) {
                            break; // out of the inner for loop
                        }
                    }
                    if (-1 == y) {
                        // entire thing matched
                        return this.list[i];
                    }
                }
            }
            return null;
        }

        /**
         * Compare the input value against the stored list of objects and return a
         * match if found.
         *
         * @param data
         * @param start
         * @param length
         * @return GenericKeys, null if not found
         */
        protected GenericKeys match(String data, int start, int length) {
            int end = start + length - 1;
            int x, y;
            // save local refs to avoid problems with concurrent add() calls
            int stop = this.number;
            char[][] vlist = this.values;
            char[] temp;
            for (int i = 0; i < stop; i++) {
                temp = vlist[i];
                if (temp.length == length) {
                    // potential match, scan backwards because most things vary
                    // later on... Content-Length vs Content-Language for example
                    for (x = end, y = temp.length - 1; x >= start; x--, y--) {
                        if (!isEqual(data.charAt(x), temp[y])) {
                            break; // out of the inner for loop
                        }
                    }
                    if (-1 == y) {
                        // entire thing matched
                        return this.list[i];
                    }
                }
            }
            return null;
        }

        /**
         * Query whether these two characters are equal.
         *
         * @param c1
         * @param c2
         * @return true if equal, false otherwise
         */
        private boolean isEqual(char c1, char c2) {
            if (c1 == c2)
                return true;
            if (!isCaseSensitive()) {
                // check both possible variations (lower/upper case)
                return ((c1 + 32) == c2 || (c1 - 32) == c2);
            }
            return false;
        }
    }

}
