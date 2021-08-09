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

package com.ibm.ws.annocache.jandex.internal;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Compressed storage for java qualified names.
 * 
 * Qualified names are converted into named CONS cells, with maximal sharing
 * of predecessor cells.
 *
 * Most class information is in compact hierarchies, which should result
 * in a lot of sharing of cells.
 */
public final class SparseDotName implements Comparable<SparseDotName> {
    public static final boolean SIMPLE = true;
    public static final boolean INNER_CLASS = true;

    public static final SparseDotName PLACEHOLDER = SparseDotName.createSimple("");

    public boolean isPlaceholder() {
    	return ( simple && tail.isEmpty() ); 
    }

    public static final SparseDotName[] PLACEHOLDER_ARRAY = new SparseDotName[0];
    public static final SparseDotName[] EMPTY_ARRAY = PLACEHOLDER_ARRAY;

    // public static final DotName JAVA_NAME = new DotName(null, "java", COMPONENTIZED, !INNER_CLASS);
    // public static final DotName JAVA_LANG_NAME = new DotName(JAVA_NAME, "lang", COMPONENTIZED, !INNER_CLASS);
    // public static final DotName OBJECT_NAME = new DotName(JAVA_LANG_NAME, "Object", COMPONENTIZED, !INNER_CLASS);

    //

    public static SparseDotName createSimple(byte[] bytes) {
        return createSimple(new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Constructs a simple name which stores the string in it's entirety.
     * This variant is ideal for temporary usage, such as looking up an entry
     * in a mapping.
     * 
     * Sparse readers restrict simple names to field and method names.  Sparse
     * readers do not allow dots ('.') are not allowed in simple names.
     *
     * @param name A dot free name.
     *
     * @return A new simple name.
     */
    public static SparseDotName createSimple(String name) {
        return new SparseDotName(null, name, SIMPLE, !INNER_CLASS);
    }

    public SparseDotName(SparseDotName head, String tail, boolean simple, boolean innerClass) {
        if ( tail == null ) {
            throw new IllegalArgumentException("Tail cannot be null");

        } else if ( simple && (tail.indexOf('.') != -1) ) {
            throw new IllegalArgumentException("Simple name [ " + tail + " ] must not have any dots ('.')");
        } else if ( simple && (head != null) ) {
            throw new IllegalArgumentException("Simple name [ " + tail + " ] must have a null prefix");
        } else if ( (head != null) && head.simple ) {
            throw new IllegalArgumentException("Prefix [ " + head + " ] of [ " + tail + " ] must not be simple");

        } else if ( innerClass && simple ) {
            throw new IllegalArgumentException("Simple name [ " + tail + " ] cannot be an inner class name");
        } else if ( innerClass && (head == null) ) {
            throw new IllegalArgumentException("Inner class name [ " + tail + " ] must have a prefix");
        }

        this.simple = simple;

        this.head = head;
        this.tail = tail;

        this.innerClass = innerClass;

        this.hash = 0; // Assigned on demand
    }

    //

    public String toString() {
        if ( head == null ) {
            return tail;

        } else {
            int size = 0;

            SparseDotName sizeCursor = this;
            while ( sizeCursor != null ) {
                SparseDotName prevCursor = sizeCursor.head;

                size += sizeCursor.tail.length();
                if ( prevCursor != null ) {
                    size++;
                }

                sizeCursor = prevCursor;
            }

            StringBuilder builder = new StringBuilder(size);

            SparseDotName fillCursor = this;
            while ( fillCursor != null ) {
                SparseDotName prevCursor = fillCursor.head;

                builder.insert(0, fillCursor.tail);

                if ( prevCursor != null ) {
                    builder.insert(0, (fillCursor.innerClass ? '$' : '.') );
                }

                fillCursor = prevCursor;
            }

            return builder.toString();
        }
    }

    //

    private final boolean innerClass;

    public boolean isInner() {
        return innerClass;
    }

    //

    private final boolean simple;

    public boolean isSimple() {
        return simple;
    }

    //

    private final SparseDotName head;
    private final String tail;

    /**
     * Returns the prefix of this name.  A Non-componentizied name has a null prefix.
     *
     * @return The prefix of this name.
     */
    public SparseDotName prefix() {
        return head;
    }

    /**
     * Returns the local portion of this name. If non-componentized, the entire
     * fully qualified name is returned. If componentized, the rightmost token of
     * the fully qualified name is returned.
     *
     * @return The local portion of this name.
     */
    public String local() {
        return tail;
    }

    //

    // TODO: This is not thread safe!

    private int hash;

    public int hashCode() {
        int useHash = this.hash;
        if ( useHash != 0 ) {
            return useHash;
        }

        // Continue using the pattern set by 'String.hashCode'.

        if ( head != null ) {
            useHash = head.hashCode();
            useHash = (31 * useHash) + (innerClass ? '$' : '.');
            useHash = (31 * useHash) + tail.hashCode();

        } else {
            // When simple, 'head' is always null, and this case is always used.
            useHash = tail.hashCode();
        }

        return this.hash = useHash;
    }

    // TODO: This does NOT take into account the inner class setting!

    @Override
    public int compareTo(SparseDotName otherDotName) {
        if ( !simple || !otherDotName.simple ) {
            return toString().compareTo( otherDotName.toString() );
        }

        List<SparseDotName> thisStack = new ArrayList<SparseDotName>(10);
        for ( SparseDotName thisHead = this; thisHead != null; thisHead = thisHead.head ) { 
            thisStack.add(0, thisHead);
        }

        List<SparseDotName> otherStack = new ArrayList<SparseDotName>(10);
        for ( SparseDotName otherHead = otherDotName; otherHead != null; otherHead = otherHead.head ) { 
            otherStack.add(0, otherHead);
        }

        int thisSize = thisStack.size();
        int otherSize = otherStack.size();
        int minSize = Math.min(thisSize, otherSize);

        for ( int headNo = 0; headNo < minSize; headNo++ ) {
            SparseDotName thisHead = thisStack.get(headNo);
            SparseDotName otherHead = otherStack.get(headNo);

            int nextComparison = thisHead.tail.compareTo(otherHead.tail);
            if ( nextComparison != 0 ) {
                return nextComparison;
            }
        }

        // "a.b.c" vs "a.b"   : (thisSize - otherSize ==  1)
        // "a.b"   vs "a.b.c" : (thisSize - otherSize == -1)
        // "a.b.c" vs "a.b.c" : (thisSize - otherSize ==  0)

        return ( thisSize - otherSize );
    }

    public boolean equals(Object other) {
        if ( this == other ) {
            return true;
        } else if ( !(other instanceof SparseDotName) ) {
            return false;
        }

        SparseDotName otherDotName = (SparseDotName) other;

        if ( simple != otherDotName.simple ) {
            return false;
        } else if ( innerClass != otherDotName.innerClass ) {
            return false;
        } else if ( !simple && ((head == null) != (otherDotName.head == null)) ) {
            return false;
        } else if ( !tail.equals(otherDotName.tail) ) {
            return false;
        } else if ( !simple && (head != null) && !head.equals(otherDotName.head) ) {
            return false;
        } else {
            return true;
        }
    }

    public boolean matches(String simpleName) {
        if ( !simple ) {
            return false;
        } else {
            return ( tail.equals(simpleName) );
        }
    }
}
