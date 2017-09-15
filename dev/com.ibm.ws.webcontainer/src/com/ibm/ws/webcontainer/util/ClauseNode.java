/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.util.ArrayList;
import java.util.List;

public class ClauseNode {
    int hashCode = -1;
    volatile Object target = null;
    volatile Object starTarget = null;
    ClauseTable children;
    int length = -1;
    int depth = -1;
    static protected final String starString = "*"; // PM06111 

    String cl;

    // PM06111 Add new constructor to take boolean to inidicate that Strings should be 
    // used as ClauseTable keys.
    public ClauseNode(ClauseNode parent, String clause, Object target) {
        hashCode = URIMatcher.computeHash(clause) & 0x7FFFFFFF;
        this.target = target;
        this.length = clause.length();
        cl = clause;
        children = new ClauseTable();

        depth = (parent == null) ? 0 : parent.depth + length + 1; // +1 for / separator
    }

    public ClauseNode add(ClauseNode node) throws Exception {

        // check to see if node already exists
        ClauseNode n = null;

        n = children.get(node.getClause()); // PM06111

        if (n != null) {
            // node already exists
            if (node.getTarget() != null) {
                if (n.getTarget() != null) {
                    throw new Exception("Mapping clash for " + node.getTarget() + ": Target " + n.getTarget() + " already exists at node " + cl);
                } else
                    n.setTarget(node.getTarget());
            }
            return n;
        }

        children.add(node.getClause(), node); // PM06111
        return children.get(node.getClause()); // PM06111        	
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Object getTarget() {
        return target;
    }

    public void setStarTarget(Object target) {
        this.starTarget = target;
    }

    public Object getStarTarget() {
        return starTarget;
    }

    public ClauseNode traverse(String clause) {
        return children.get(clause); // PM06111
    }

    public Object getTarget(String clause) {
        //get a target if the String clause matches exact

        if (cl.equals(clause)) // PM06111
            return target; // PM06111

        return null; // PM06111
    }

    // PM06111 Add method
    public String getClause() {
        return cl;
    }

    // returns all the targets under this Node
    //
    public List<Object> targets() {
        List<Object> targets = new ArrayList<Object>();
        List<ClauseNode> l = children.getList();
        for (ClauseNode node : l) {
            //potential jvm bug exposes an issue where node might be null
            //this list is generated from the values of a ConcurrentHashMap whose entries should never be null
            if (node!=null) {
                targets.addAll(node.targets());
            }
        }

        if (target != null)
            targets.add(target);
        if (starTarget != null)
            targets.add(starTarget);

        return targets;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * @param mapping The Exact or Suffix mapping to be removed from this node
     *            The mapping starts with a '/' and does <b>not</b> end with a '/'
     */
    public boolean remove(String mapping) {
        // eg., we have a request to delete /foo/bar/mapping
        // isolate 'foo' and see if there is such a child node

        if (mapping == null) {
            // exact match
            if (starTarget == null) {
                target = null;
            }
        } else {

            // skip the initial /
            int index = mapping.indexOf('/', 1);
            String childName;
            String remainder = null;
            ClauseNode node;
            if (index == -1) {
                // we need to remove a leaf node
                childName = mapping.substring(1);
            } else {
                // node to be removed is not a leaf node, hence propagate the call
                childName = mapping.substring(1, index);
                remainder = mapping.substring(index);
            }

            if (childName.equals(starString)) {
                if (starTarget != null) {
                    // remove our targets and let the parent decide if we need to be removed
                    target = null;
                    starTarget = null;
                }
            } else {
                node = traverse(childName);
                if (node != null) {
                    boolean delete = node.remove(remainder);
                    if (delete) {
                        children.remove(node.getClause()); // PM06111
                    }
                }
            }
        }

        // The leaf just removed might be the only child of this node
        // Hence we return true, indicating that this node can be removed
        return (children.size() == 0 && target == null && starTarget == null);
    }

    /**
     * @param uri
     * @param newTarget
     * @return
     */
    public Object replace(String uri, Object newTarget) throws Exception {
        if (uri == null) {
            // exact match
            if (starTarget == null) {
                if (target == null) {
                    throw new Exception("No target to replace at given node");
                }
                Object oldTarget = target;
                target = newTarget;
                return oldTarget;
            }
        } else {
            // skip the initial /
            int index = uri.indexOf('/', 1);
            String childName;
            String remainder = null;
            ClauseNode node;
            if (index == -1) {
                // we need to remove a leaf node
                childName = uri.substring(1);
            } else {
                // node to be removed is not a leaf node, hence propagate the call
                childName = uri.substring(1, index);
                remainder = uri.substring(index);
            }

            if (childName.equals(starString)) {
                if (starTarget == null) {
                    throw new Exception("No target to replace at given node");
                }

                Object oldTarget = target;
                target = newTarget;
                starTarget = newTarget;
                return oldTarget;
            } else {
                node = traverse(childName);
                if (node != null) {
                    return node.replace(remainder, newTarget);
                } else {
                    throw new Exception("No exact matching path found to replace");
                }
            }
        }

        return null;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return "CL:" + cl + "T:" + target;
    }

}