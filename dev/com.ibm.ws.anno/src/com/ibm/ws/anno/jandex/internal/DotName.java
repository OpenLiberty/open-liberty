/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * All modifications made by IBM from initial source -
 * https://github.com/wildfly/jandex/blob/master/src/main/java/org/jboss/jandex/DotName.java
 * commit - 3b3960796493a13bf0bf71768b70967749e2f3db
 */

package com.ibm.ws.anno.jandex.internal;

import java.util.ArrayDeque;


public final class DotName implements Comparable<DotName> {

    static final DotName PLACEHOLDER = DotName.createSimple("");
    public static final DotName[] PLACEHOLDER_ARRAY = new DotName[0];
    static final DotName JAVA_NAME;
    static final DotName JAVA_LANG_NAME;
    static final DotName OBJECT_NAME;

    private final DotName prefix;
    private final String local;
    private int hash;
    private boolean componentized = false;
    private boolean innerClass = false;

    static {
        JAVA_NAME = new DotName(null, "java", true, false);
        JAVA_LANG_NAME = new DotName(JAVA_NAME, "lang", true, false);
        OBJECT_NAME = new DotName(JAVA_LANG_NAME, "Object", true, false);
    }

    /**
     * Constructs a simple DotName which stores the string in it's entirety. This variant is ideal
     * for temporary usage, such as looking up an entry in a Map.
     *
     * @param name A fully qualified non-null name (with dots)
     * @return a simple DotName that wraps name
     */
    public static DotName createSimple(String name) {
       return new DotName(null, name, false, false);
    }


    DotName(DotName prefix, String local, boolean noDots, boolean innerClass) {
        if (local == null) {
            throw new IllegalArgumentException("Local string can not be null");
        }

        if (prefix != null && !prefix.componentized) {
            throw new IllegalArgumentException("A componentized DotName must have a componentized prefix, or null");
        }

        this.prefix = prefix;
        this.local = local;
        this.componentized = noDots;
        this.innerClass = innerClass;
    }

    /**
     * Returns the parent prefix for this DotName or null if there is none.
     * Simple DotName variants never have a prefix.
     *
     * @return the parent prefix for this DotName
     */
    public DotName prefix() {
        return prefix;
    }

    /**
     * Returns the local portion of this DotName. In simple variants, the entire fully qualified
     * string is returned. In componentized variants, just the right most portion not including a separator
     * is returned.
     *
     * @return the non-null local portion of this DotName
     */
    public String local() {
        return local;
    }

    /**
     * Returns whether this DotName is a componentized variant.
     *
     * @return true if it is componentized, false if it is a simple DotName
     */
    public boolean isComponentized() {
        return componentized;
    }

    /**
     * Returns whether the local portion of this DotName represents an inner class.
     *
     * @return true if local is an inner class name, false otherwise
     */
    public boolean isInner() {
        return innerClass;
    }

    /**
     * Returns the regular fully qualifier class name.
     *
     * @return The fully qualified class name
     */
    public String toString() {
        return toString('.');
    }

    public String toString(char delim) {
        String string = local;
        if (prefix != null) {
            StringBuilder builder = new StringBuilder();
            builder.append(prefix.toString(delim)).append(innerClass ? '$' : delim).append(string);
            string = builder.toString();
        }

        return string;
    }

    /**
     * Returns a hash code which is based on the semantic representation of this <code>DotName</code>.
     * Whether or not a <code>DotName</code> is componentized has no impact on the calculated hash code.
     *
     * @return a hash code representing this object
     * @see Object#hashCode()
     */
    public int hashCode() {
        int hash = this.hash;
        if (hash != 0)
            return hash;

        if (prefix != null) {
            hash = prefix.hashCode() * 31 + (innerClass ? '$' : '.');

            // Luckily String.hashCode documents the algorithm it follows
            for (int i = 0; i < local.length(); i++) {
                hash = 31 * hash + local.charAt(i);
            }
        } else {
            hash = local.hashCode();
        }

        return this.hash = hash;
    }

    /**
     * Compares a <code>DotName</code> to another <code>DotName</code> and returns whether this DotName
     * is lesser than, greater than, or equal to the specified DotName. If this <code>DotName</code> is lesser,
     * a negative value is returned. If greater, a positive value is returned. If equal, zero is returned.
     *
     * @param other the DotName to compare to
     * @return a negative number if this is less than the specified object, a positive if greater, and zero if equal
     *
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo(DotName other) {

        if (componentized && other.componentized) {
            ArrayDeque<DotName> thisStack = new ArrayDeque<DotName>();
            ArrayDeque<DotName> otherStack = new ArrayDeque<DotName>();

            DotName curr = this;
            while (curr != null) {
                thisStack.push(curr);
                curr = curr.prefix();
            }

            curr = other;
            while (curr != null) {
                otherStack.push(curr);
                curr = curr.prefix();
            }

            int thisSize = thisStack.size();
            int otherSize = otherStack.size();
            int stop = Math.min(thisSize, otherSize);

            for (int i = 0; i < stop; i++) {
                DotName thisComp = thisStack.pop();
                DotName otherComp = otherStack.pop();

                int comp = thisComp.local.compareTo(otherComp.local);
                if (comp != 0)
                    return comp;
            }

            int diff = thisSize - otherSize;
            if (diff != 0)
                return diff;
        }

        // Fallback to string comparison
        return toString().compareTo(other.toString());
    }

    /**
     * Compares a DotName to another DotName and returns true if the represent
     * the same underlying semantic name. In other words, whether or not a
     * name is componentized or simple has no bearing on the comparison.
     *
     * @param o the DotName object to compare to
     * @return true if equal, false if not
     *
     * @see Object#equals(Object)
     */
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (! (o instanceof DotName))
            return false;

        DotName other = (DotName)o;
        if (other.prefix == null && prefix == null)
            return local.equals(other.local) && innerClass == other.innerClass;

        if (!other.componentized && componentized)
            return toString().equals(other.local);

        if (other.componentized && !componentized)
            return other.toString().equals(local);

        return prefix != null && innerClass == other.innerClass && local.equals(other.local) && prefix.equals(other.prefix);
    }
}
