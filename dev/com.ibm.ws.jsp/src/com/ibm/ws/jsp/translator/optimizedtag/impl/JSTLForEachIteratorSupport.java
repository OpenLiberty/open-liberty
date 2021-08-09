/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.optimizedtag.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class JSTLForEachIteratorSupport {

    public static Iterator createIterator(Object o) {
        Iterator iterator = null;

        if (o instanceof Object[])
            iterator = toIterator((Object[]) o);
        else if (o instanceof boolean[])
            iterator = toIterator((boolean[]) o);
        else if (o instanceof byte[])
            iterator = toIterator((byte[]) o);
        else if (o instanceof char[])
            iterator = toIterator((char[]) o);
        else if (o instanceof short[])
            iterator = toIterator((short[]) o);
        else if (o instanceof int[])
            iterator = toIterator((int[]) o);
        else if (o instanceof long[])
            iterator = toIterator((long[]) o);
        else if (o instanceof float[])
            iterator = toIterator((float[]) o);
        else if (o instanceof double[])
            iterator = toIterator((double[]) o);
        else if (o instanceof Collection)
            iterator = ((Collection) o).iterator();
        else if (o instanceof Enumeration)
            iterator = Collections.list((Enumeration) o).iterator();
        else if (o instanceof java.util.Map)
            iterator = ((Map) o).entrySet().iterator();
        else if (o instanceof String)
            iterator = toIterator((String) o);

        return iterator;
    }

    private static Iterator toIterator(final Object[] a) {
        return (new Iterator() {
            int index = 0;
            public boolean hasNext() {
                return index < a.length;
            }
            public Object next() {
                return a[index++];
            }
            public void remove() {}
        });
    }

    private static Iterator toIterator(final boolean[] a) {
        return (new Iterator() {
            int index = 0;
            public boolean hasNext() {
                return index < a.length;
            }
            public Object next() {
                return new Boolean(a[index++]);
            }
            public void remove() {}
        });
    }

    private static Iterator toIterator(final byte[] a) {
        return (new Iterator() {
            int index = 0;
            public boolean hasNext() {
                return index < a.length;
            }
            public Object next() {
                return new Byte(a[index++]);
            }
            public void remove() {}
        });
    }

    private static Iterator toIterator(final char[] a) {
        return (new Iterator() {
            int index = 0;
            public boolean hasNext() {
                return index < a.length;
            }
            public Object next() {
                return new Character(a[index++]);
            }
            public void remove() {}
        });
    }

    private static Iterator toIterator(final short[] a) {
        return (new Iterator() {
            int index = 0;
            public boolean hasNext() {
                return index < a.length;
            }
            public Object next() {
                return new Short(a[index++]);
            }
            public void remove() {}
        });
    }

    private static Iterator toIterator(final int[] a) {
        return (new Iterator() {
            int index = 0;
            public boolean hasNext() {
                return index < a.length;
            }
            public Object next() {
                return new Integer(a[index++]);
            }
            public void remove() {}
        });
    }

    private static Iterator toIterator(final long[] a) {
        return (new Iterator() {
            int index = 0;
            public boolean hasNext() {
                return index < a.length;
            }
            public Object next() {
                return new Long(a[index++]);
            }
            public void remove() {}
        });
    }

    private static Iterator toIterator(final float[] a) {
        return (new Iterator() {
            int index = 0;
            public boolean hasNext() {
                return index < a.length;
            }
            public Object next() {
                return new Float(a[index++]);
            }
            public void remove() {}
        });
    }

    private static Iterator toIterator(final double[] a) {
        return (new Iterator() {
            int index = 0;
            public boolean hasNext() {
                return index < a.length;
            }
            public Object next() {
                return new Double(a[index++]);
            }
            public void remove() {}
        });
    }

    private static Iterator toIterator(final String a) {
        StringTokenizer st = new StringTokenizer(a, ",");
        return (Collections.list(st).iterator());
    }
}
