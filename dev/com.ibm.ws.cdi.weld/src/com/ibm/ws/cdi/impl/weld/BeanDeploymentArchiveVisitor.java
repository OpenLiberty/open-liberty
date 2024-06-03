/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.cdi.impl.weld;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;

public class BeanDeploymentArchiveVisitor {

    private final Stack<PeakableFilteredIterator> stack = new Stack<PeakableFilteredIterator>();

    /**
     * This method implements a depth first search over all BDAs (excluding Runtime Extensions) and their children
     * to ensure children are scanned before parents.
     *
     * Since the dependency graph of BDAs is cyclic this algorithm can be nondeterministic in which order mutual
     * dependencies are scanned.
     *
     * How it works:
     *
     * LOOP START
     * Look at the current element from the iterator at the top of the stack, marking it visited:
     * If it is not a runtime extension get an iterator of all unvisited children and put it on the stack GOTO LOOP START
     * If it has already been visited scan the current element, then advance the iterator. GOTO LOOP START.
     * If the iterator is now empty, pop the stack. If the stack is empty RETURN, else GOTO LOOP START.
     *
     */
    public void visit(WebSphereBeanDeploymentArchive root) throws CDIException {
        List<WebSphereBeanDeploymentArchive> list = Collections.singletonList(root);
        stack.add(new PeakableFilteredIterator(list.iterator()));

        while (!stack.isEmpty()) {
            PeakableFilteredIterator iter = stack.peek();

            if (iter.hasElement()) {
                WebSphereBeanDeploymentArchive bda = iter.peek();
                if (!bda.hasBeenVisited()) {
                    Iterator<WebSphereBeanDeploymentArchive> children = bda.visit();
                    stack.add(new PeakableFilteredIterator(children));
                } else {
                    if (!bda.hasBeenScanned()) {
                        bda.scan();
                    }
                    iter.progress();
                }
            } else {
                stack.pop();
            }
        }
    }

    /*
     * This class is a non generalised decorator over an iterator of BDAs. It has some notable differences
     * in behaviour from a proper iterator. All of which are to KISS rather than track enough state to implement
     * peek() and next() while honouring the iterator contract.
     */
    private class PeakableFilteredIterator {
        private WebSphereBeanDeploymentArchive bda = null;
        private final Iterator<WebSphereBeanDeploymentArchive> iterator;

        @Trivial
        public PeakableFilteredIterator(Iterator<WebSphereBeanDeploymentArchive> iterator) {
            this.iterator = iterator;
            if (iterator.hasNext()) {
                progress();
            }
        }

        /*
         * returns the nth element in the iterator that matches the filter or null if n is greater than the number
         * of valid bdas in the underlying collection. Where n = the number of invocations of progress()
         * (the first invocation happens on construction)
         *
         * Note that an element is only tested by the filter once when progress() is invoked so peek() can return
         * a non-matching element if its state changes after progress() was last invoked.
         *
         */
        @Trivial
        public WebSphereBeanDeploymentArchive peek() {
            return bda;
        }

        /*
         * @return true() if peek() will return an element, false if peek() will return null
         */
        @Trivial
        public boolean hasElement() {
            return bda != null;
        }

        /*
         * Advances the decorated iterator to the next element that matches the filter.
         */
        @Trivial
        public void progress() {
            bda = null;
            while (iterator.hasNext() && bda == null) {
                WebSphereBeanDeploymentArchive maybeNextBDA = iterator.next();
                if (!maybeNextBDA.hasBeenVisited()) {
                    bda = maybeNextBDA;
                }
            }
        }

    }

}
