/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.internal;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 *
 */
final class ResourceIterators {
    private ResourceIterators() {
        throw new AssertionError("This class is not instantiable");
    }

    static class ChildIterator implements Iterator<String> {
        private final String[] children;
        private final File parent;
        private int index = 0;

        ChildIterator(File p, String[] kids) {
            parent = p;
            children = (kids == null) ? new String[0] : kids;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            if (index < children.length)
                return true;

            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String next() {
            if (index >= children.length)
                throw new NoSuchElementException("No more elements (p=" + parent.getAbsolutePath() + ")");

            String child = children[index++];
            File f = new File(parent, child);
            if (f.isDirectory())
                child += '/';

            return child;
        }

        /**
         * no-op.
         */
        @Override
        public void remove() {}
    }

    static class MatchingIterator implements Iterator<String> {
        private final Iterator<String> i;
        private final Pattern regex;
        private String next = null;

        MatchingIterator(Iterator<String> i, String regex) {
            this.i = i;
            this.regex = Pattern.compile(regex);
        }

        @Override
        public boolean hasNext() {
            String n;
            while (i.hasNext()) {
                n = i.next();
                if (regex.matcher(n).matches()) {
                    next = n;
                    return true;
                }
            }

            return false;
        }

        /**
         *
         */
        @Override
        public String next() {
            if (next == null)
                throw new NoSuchElementException("No more elements");

            return next;
        }

        /**
         *
         */
        @Override
        public void remove() {}
    }
}
