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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;

public class BeanDeploymentArchiveScanner {

    /**
     * This method implements a depth first search over all BDAs (excluding Runtime Extensions) and their children
     * to ensure children are scanned before parents.
     * <p>
     * Since the dependency graph of BDAs is cyclic this algorithm can be nondeterministic in which order mutual
     * dependencies are scanned.
     * <p>
     * How it works:
     * <p>
     * Two operations are performed on each BDA:
     * <ul>
     * <li>First it is <em>visited</em> - a BDA is identified as needing scanned and a list of its children is obtained.
     * <li>Later it is <em>scanned</em> - this is when CDI actually examines the classes in the archive
     * </ul>
     * Each BDA stores a flag to indicate whether it has been visited or scanned. This ensures each BDA will only be visited and scanned once, no matter how many times this method
     * is called.
     * <p>
     * The algorithm used:
     *
     * <pre>
     * LOOP START
     * Look for the first unvisited child of the BDA at the top of the stack
     * - If a child is found, visit it and push it onto the stack. GOTO LOOP START
     * - If a child is not found, pop the BDA off the stack and scan it. GOTO LOOP START
     * </pre>
     *
     * This ensures:
     * <ul>
     * <li>the BDAs on the stack are those which have been visited, but not yet scanned
     * <ul>
     * <li>all BDAs not in the graph have either not been visited yet, or have been both visited and scanned
     * </ul>
     * <li>every BDA on the stack is a direct dependency of the BDA below it
     * <ul>
     * <li>therefore, if A depends on B, and there is no route through the dependency graph from from B back to A, B will never appear below A in the stack
     * <li>therefore, if A is on the stack, either B has already been scanned or B will be added on the stack above A
     * <li>therefore B will be scanned before A
     * </ul>
     * <p>
     * Implementation notes:
     * <ul>
     * <li>Java has a Stack class, but Deque is preferred for implementing stacks
     * <li>There are probably better ways of scanning BDAs in the correct order. This algorithm replicates the old recursive logic we used to have to avoid changing the scanning order.
     * <li>Storing an iterator of children on the stack is a small optimization to make finding the next unvisited BDA faster. nextUnvisitedBda() could search through the list of
     * children of the BDA from the start every time.
     *
     * @param root the BDA to start the scan from
     */
    public static void recursiveScan(WebSphereBeanDeploymentArchive root) throws CDIException {
        Deque<StackElement> stack = new ArrayDeque<>();

        if (!root.hasBeenVisited()) {
            stack.push(new StackElement(root, root.visit()));
        }

        while (!stack.isEmpty()) {
            StackElement e = stack.peek();

            WebSphereBeanDeploymentArchive child = nextUnvisitedBda(e.childIterator);
            if (child != null) {
                // If there are unvisited children, they are pushed onto the stack
                // so that they will be scanned before us
                stack.push(new StackElement(child, child.visit()));
            } else {
                // When all our direct children have been visited and scanned, we can be scanned
                if (!e.bda.hasBeenScanned()) {
                    e.bda.scan();
                }
                stack.pop();
            }
        }
    }

    /**
     * Takes BDAs from an iterator until one is found where {@link WebSphereBeanDeploymentArchive#hasBeenVisited()} returns {@code false} and returns it.
     *
     * @param i the iterator
     * @return the next unvisited BDA retrieved from the iterator, or {@code null} if none is found
     */
    private static WebSphereBeanDeploymentArchive nextUnvisitedBda(Iterator<WebSphereBeanDeploymentArchive> i) {
        while (i.hasNext()) {
            WebSphereBeanDeploymentArchive bda = i.next();
            if (!bda.hasBeenVisited()) {
                return bda;
            }
        }
        return null;
    }

    private static final class StackElement {
        /** the BDA to be scanned when this stack element is removed from the stack */
        private final WebSphereBeanDeploymentArchive bda;
        /** the children of {@link #bda} which should be scanned before it is scanned */
        private final Iterator<WebSphereBeanDeploymentArchive> childIterator;

        private StackElement(WebSphereBeanDeploymentArchive bda, Iterator<WebSphereBeanDeploymentArchive> childIterator) {
            this.bda = bda;
            this.childIterator = childIterator;
        }
    }

}
