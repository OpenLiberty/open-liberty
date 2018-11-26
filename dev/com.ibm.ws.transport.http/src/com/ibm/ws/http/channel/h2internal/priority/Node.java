/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.priority;

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.H2WriteQEntry;
import com.ibm.ws.http.channel.internal.HttpMessages;

public class Node {

    private static final TraceComponent tc = Tr.register(Node.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /**
     * Node Assumptions
     * This class is not at all thread safe, it is assumed the calling code will ensure only one thread will be in a Node object at one time.
     * Node objects should only be accessed and manipulated via the owning Tree object.
     *
     */

    static public enum NODE_STATUS {
        REQUESTING_WRITE, // Stream/Node wants to write a frame
        WRITE_LATCHED, // thread is waiting for the write to complete
        NOT_REQUESTING, // Stream/Node is not writing, but is not closed
        CLOSED, // Stream/Node is closed
        ACTION_NO_CHANGE, // This isn't a status/state, but a signal to methods that the status is not changing
        ACTION_RESET_IF_LATCHED // signal to move the state to NOT_REQUESTING if the current state is WRITE_LATCHED
    }

    static public enum WRITE_COUNT_ACTION {
        CLEAR,
        INCREMENT,
        NO_ACTION,
    }

    static public int DEFAULT_NODE_PRIORITY = 16;
    static public int ROOT_STREAM_ID = 0;
    static public int ROOT_PRIORITY = -1;

    Node parent = null; // the parent of this node, should not be null unless this is the root node
    ArrayList<Node> dependents = new ArrayList<Node>(); // direct dependents/children of this node. Array List Index are 0 based

    int streamID;
    int priority = 16; // stream priority, between 1 (low) and 256 (high), Default is 16, according to the spec.
    NODE_STATUS status = NODE_STATUS.NOT_REQUESTING;

    boolean priorityRatioPositive = false; // if true it means this node should be allowed to write if other higher priority nodes have false ratios.
    int writeCount = 0; // number of writes that node has made since the last write count reset.

    int dependentWriteCount = 0; // the total number of writes that directly dependent nodes (children) have made since the last reset

    H2WriteQEntry entry = null;

    public Node(int nodeStreamID, int nodePriority) {
        streamID = nodeStreamID;
        priority = nodePriority;
    }

    /**
     * Starting with this node, find the node matching the input stream ID, look at this node and all dependents recursively.
     * To search the whole tree start by calling findNode on the root node.
     *
     * @param nodeStreamID
     * @return
     */
    protected Node findNode(int nodeStreamID) {

        // mainline debug in this recursive method is way to verbose, so only debug when we find what we are looking for

        if (nodeStreamID == this.streamID) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "findNode exit: bottom of recursion, found node: " + this);
            }
            return this;
        } else {
            Iterator<Node> iter = dependents.iterator();
            Node found = null;
            while (iter.hasNext()) {
                found = iter.next().findNode(nodeStreamID);
                if (found != null) {
                    return found;
                }
            }

            return null;
        }
    }

    /**
     * Add a new dependent to this node.
     *
     * @param toAdd
     */
    protected void addDependent(Node nodeToAdd) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addDependent entry: node to add: " + nodeToAdd);
        }
        dependents.add(nodeToAdd);
    }

    /**
     * Remove a dependent from the dependent list this node is keeping
     *
     * @param toRemove
     */
    protected void removeDependent(Node nodeToRemove) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeDependent entry: node to remove: " + nodeToRemove);
        }
        dependents.remove(nodeToRemove);
    }

    protected void clearDependents() {
        dependents = new ArrayList<Node>();
    }

    /**
     * clear counts for all direct dependents of this node.
     * also clear the dependent write counter
     */
    protected void clearDependentsWriteCount() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "clearDependentsWriteCount entry: for this node: " + this);
        }

        dependentWriteCount = 0;

        if ((dependents == null) || (dependents.size() == 0)) {
            return;
        }

        for (int i = 0; i < dependents.size(); i++) {
            dependents.get(i).setWriteCount(0);
        }

    }

    /**
     * Sort array list of dependencies so highest weight is at the lowest array list index, up to the lowest weight.
     * Us a bubble sort because the list should be short, and/or it should not have much movement from iteration to iteration
     * Is: A/B > C/D ?
     * Answer: if A*D > C*B then A/B > C/D (due to creating common denominators)
     * if ( your-priority/children-priority-sum > your-hits/children-write-count) then do your operation.
     * if your-priority * children-write-count > your-hits * children-priority-sum,
     * then signal you are allowed to do your operation -> setPriorityRatioPositive(true)
     *
     */
    protected void sortDependents() {

        int bigIndex = 0;
        int smallIndex = 0;
        int prioritySum = 0;
        boolean noChange = true;
        boolean firstLoop = true;

        Node nI = null;
        Node nIMinus1 = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sortDependents entry: sort dependents of this node " + this);
        }

        if ((dependents == null) || (dependents.size() == 0)) {
            // nothing to sort
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "sortDependents exit");
            }
            return;
        }

        // If only 1 dependent, then by default it is ready to go
        if (dependents.size() == 1) {
            dependents.get(0).setPriorityRatioPositive(true);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "sortDependents exit");
            }
            return;
        }

        bigIndex = dependents.size() - 1;

        // Determine the sum of the current priorities
        for (int i = bigIndex; i >= 0; i--) {
            prioritySum += dependents.get(i).getPriority();
        }

        // first time through we need to determine ratios, after that only do the comparisons.
        while (true) {

            noChange = true; // signals if we made at least one ordering change in this pass/loop

            // determine priorityRatio for biggest index before entering loop, then just determine ratios for each new node examined
            if (firstLoop) {
                nI = dependents.get(bigIndex);
                if ((dependentWriteCount == 0)
                    || ((nI.getPriority() * dependentWriteCount) > (nI.getWriteCount() * prioritySum))) {
                    nI.setPriorityRatioPositive(true);
                } else {
                    nI.setPriorityRatioPositive(false);
                }
            }

            // Each iteration of this for will move the (next) highest priority to the (next) front of the array.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "looping through with indexes of: " + bigIndex + " and: " + smallIndex);
            }
            for (int i = bigIndex; i >= smallIndex + 1; i--) {

                nI = dependents.get(i);
                nIMinus1 = dependents.get(i - 1);

                // determine priorityRatio for biggest index before entering loop, then just determine ratios for each new node examined
                if (firstLoop) {
                    if ((dependentWriteCount == 0)
                        || ((nIMinus1.getPriority() * dependentWriteCount) > (nIMinus1.getWriteCount() * prioritySum))) {
                        nIMinus1.setPriorityRatioPositive(true);
                    } else {
                        nIMinus1.setPriorityRatioPositive(false);
                    }
                }

                // move up the node that has a positive ratio.  if both positive or negative, then move up the node with the higher priority.
                if (nIMinus1.getPriorityRatioPositive() == false) {
                    if ((nI.getPriorityRatioPositive() == true) || (nI.getPriority() > nIMinus1.getPriority())) {
                        noChange = false;
                        // move up nI
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "swapping node (n-1): " + nIMinus1 + " with node (n): " + nI);
                        }
                        dependents.set(i, nIMinus1);
                        dependents.set(i - 1, nI);
                    }
                } else if ((nI.getPriorityRatioPositive() == true) && (nI.getPriority() > nIMinus1.getPriority())) {
                    noChange = false;
                    // move up nI
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "swapping node (n-1): " + nIMinus1 + " with node (n): " + nI);
                    }
                    dependents.set(i, nIMinus1);
                    dependents.set(i - 1, nI);
                }
            }

            firstLoop = false;

            // highest priority has bubbled up to the smallest index for the above loop
            // move up the small index and loop again if there was any ordering change last loop
            smallIndex = smallIndex + 1;

            if ((noChange) || (bigIndex == smallIndex)) {
                // sort it complete
                break;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sortDependents exit");
        }
    }

    /**
     * recursively go through the tree finding the highest priority node that is requesting to write.
     *
     * Assume nodes are arranged according to which dependents should write next from low index to high index at all levels.
     * Recursively find the next node ready to write, where first option is the highest rank node at a given level, then all
     * the nodes in its tree, before moving to the next highest rank node at the same level, recursively.
     *
     * @return
     */
    protected Node findNextWrite() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "findNextWrite entry: on node " + this + "With status: " + status + "and positive ratio of: " + getPriorityRatioPositive());
        }

        if (status == NODE_STATUS.REQUESTING_WRITE) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "findNextWrite exit: node to write next is: " + this.toStringDetails());
            }
            return this;
        } else {
            // go through all dependents in order
            for (int i = 0; i < dependents.size(); i++) {
                Node n = dependents.get(i);

                Node nextWrite = n.findNextWrite();
                if (nextWrite != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "findNextWrite exit: next write node found. stream-id: " + nextWrite.getStreamID() + " node hc: " + nextWrite.hashCode());
                    }
                    return nextWrite;
                }

            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "findNextWrite exit: null");
            }
            return null;
        }
    }

    /**
     * increment the count since of the number of writes the direct dependents have done since that last reset.
     */
    protected int incrementDependentWriteCount() {
        dependentWriteCount++;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "incrementDependentWriteCount entry: new dependentWriteCount of: " + dependentWriteCount + " for node: " + this);
        }
        return dependentWriteCount;
    }

    /**
     * Give this node a new parent.
     * Remove this node as a dependent of the old parent
     * Add this node as a dependent of the new parent.
     *
     * @param newParent The new parent for this node. Null is allowed is is basically removing this node from the tree.
     */
    protected void setParent(Node newParent, boolean removeDep) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setParent entry: new parent will be: " + newParent + " for node: " + this);
        }

        Node oldParent = parent;

        parent = newParent;
        if (newParent != null) {
            parent.addDependent(this);
        }

        // removing dependents can cause ConcurrentModificationException if calling is iterating over a list of dependent nodes
        // therefore removeDep should be false if setParent is being called while iterating or looping over a list of
        // dependent nodes.
        if (removeDep) {
            // remove this node from the old parents list of dependents.
            if (oldParent != null) {
                if (newParent == null) {
                    oldParent.removeDependent(this);
                } else if (oldParent.getStreamID() != parent.getStreamID()) {
                    oldParent.removeDependent(this);
                }
            }
        }
    }

    protected void incrementWriteCount() {
        writeCount++;
    }

    // ---------- Put all the boring getters and setters at the bottom of the file ------------------------------------------------------

    public Node getParent() {
        return parent;
    }

    public ArrayList<Node> getDependents() {
        return dependents;
    }

    public int getStreamID() {
        return streamID;
    }

    public int getWriteCount() {
        return writeCount;
    }

    protected void setWriteCount(int x) {
        writeCount = x;
    }

    public int getPriority() {
        return priority;
    }

    protected void setPriority(int x) {
        priority = x;
    }

    public boolean getPriorityRatioPositive() {
        return priorityRatioPositive;
    }

    protected void setPriorityRatioPositive(boolean x) {
        priorityRatioPositive = x;
    }

    public NODE_STATUS getStatus() {
        return status;
    }

    protected void setStatus(NODE_STATUS x) {
        status = x;
    }

    protected void setEntry(H2WriteQEntry x) {
        entry = x;
    }

    public H2WriteQEntry getEntry() {
        return entry;
    }

    @Override
    public String toString() {
        String s = "hashcode: " + this.hashCode() + " stream id: " + this.streamID + " ";
        return s;
    }

    public String toStringDetails() {
        String s = "hashcode: " + this.hashCode() + " stream id: " + this.streamID
                   + " prioirity: " + this.priority + " status: " + this.status
                   + " ratio: " + this.priorityRatioPositive + " write count: " + this.writeCount + " dep write count: " + this.dependentWriteCount + " ";
        return s;
    }

    public StringBuffer dumpDependents(StringBuffer s) {

        if (s == null) {
            s = new StringBuffer("\nDump of Tree: ");
        }

        if (dependents.size() > 0) {
            s.append("\n" + dependents.size() + " Dependents of: " + this);
        }

        s.append("\nDependents of: " + this);
        // dump details of dependents of this Node
        for (int i = 0; i < dependents.size(); i++) {
            Node n = dependents.get(i);
            s.append("\n  " + n.toStringDetails());
        }

        // tell dependents to dump their dependents
        for (int i = 0; i < dependents.size(); i++) {
            Node n = dependents.get(i);
            n.dumpDependents(s);
        }

        return s;
    }

}
