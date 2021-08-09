/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
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
import com.ibm.ws.http.channel.h2internal.priority.Node.NODE_STATUS;
import com.ibm.ws.http.channel.h2internal.priority.Node.WRITE_COUNT_ACTION;
import com.ibm.ws.http.channel.internal.HttpMessages;

public class Tree {

    /**
     * Tree assumptions
     * Each connection can contain multiple streams. One tree will be used per connection.
     * Each Node of the tree represents one stream of the connection. Except the root node which represents no stream
     * A Node is a child of a parent if the stream of the Node is a dependent stream of the parent.
     * The root node does not represent a stream in the connection, but is most senior parent, and is where recursive tree/node methods will usually start
     *
     * Threading assumptions
     * only one thread can act on the tree at one time, therefore all methods in this object are synchronized.
     * the Node objects are not thread-safe, only way to manipulate the Node objects should be via the Tree owning the Node.
     *
     * No methods return the Node object, since the Node object is not thread safe. The streamID of the node is returned for finding the next stream
     * to write on. All other operation return "true" for sucess, and "false" for failure (usually meaning the node in question was not found)
     *
     */

    private static final TraceComponent tc = Tr.register(Tree.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    Node root;

    public Tree() {

        // root node has its own stream ID and priority
        root = new Node(Node.ROOT_STREAM_ID, Node.ROOT_PRIORITY);
    }

    public synchronized boolean findNode(int streamID) {

        // avoid returning node, since node processing should only happen in this synchronized tree

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "findNode entry: starting at root for stream ID: " + streamID);
        }

        Node node = root.findNode(streamID);

        if (node != null) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized Node getRoot() {
        return root;
    }

    /**
     * Add a node to the tree under the given parent.
     * According to the spec, If exclusive is true then:
     * 1. make the added node the only child of the parent.
     * 2. all previous children of the parent become children of the added node.
     *
     * If the node was added to a set of current children, then restart the priority counting, since this added node's write count will start at 0
     *
     * @param node
     * @param parentStreamID
     * @param exclusive
     */
    public synchronized boolean addNode(Node nodeToAdd, int parentStreamID, boolean exclusive) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addNode entry: Node to add: " + nodeToAdd);
        }

        if (nodeToAdd == null) {
            return false;
        }

        Node parentNode = root.findNode(parentStreamID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "found parent node of: " + parentNode.hashCode());
        }

        if (parentNode != null) {

            // found the node that is to be the parent
            nodeToAdd.setParent(parentNode, true);

            if (exclusive) {

                makeExclusiveDependency(nodeToAdd, parentNode);

            } else {

                // need to start the write counting over, since a new node was added at this level, and re-sort
                parentNode.clearDependentsWriteCount();
                parentNode.sortDependents();
                // special debug - too verbose for big trees
                //if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                //    Tr.debug(tc, "addNode after sorting: tree is now:" + dumpTree());
                //}

            }
            return true;
        }

        return false;
    }

    /**
     * Allow the stream/node status to change and perform needed operations on the write counter which is used for priority weighting
     * If the write count has changed, then all the nodes at this level need to be re-sorted to reflect the current/updated priority weighting
     *
     * @param streamID
     * @param status
     * @param writeCountAction
     * @return
     */
    public synchronized boolean updateNode(int streamID, NODE_STATUS status, WRITE_COUNT_ACTION writeCountAction, H2WriteQEntry entry) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (entry == null) {
                Tr.debug(tc, "updateNode entry: null" + " streamID: " + streamID + " status: " + status + " writeCountAction: " + writeCountAction);
            } else {
                Tr.debug(tc, "updateNode entry: " + entry.hashCode() + " streamID: " + streamID + " status: " + status + " writeCountAction: " + writeCountAction);
            }
        }

        Node nodeToUpdate = root.findNode(streamID);

        if (nodeToUpdate == null) {
            return false;
        }

        if (entry != null) {
            nodeToUpdate.setEntry(entry);
        }

        // Change status unless told not to
        if (status != NODE_STATUS.ACTION_NO_CHANGE) {
            if (status == NODE_STATUS.ACTION_RESET_IF_LATCHED) {
                if (nodeToUpdate.getStatus() == NODE_STATUS.WRITE_LATCHED) {
                    nodeToUpdate.setStatus(NODE_STATUS.NOT_REQUESTING);
                }
            } else {
                nodeToUpdate.setStatus(status);
            }
        }

        // Do the count action, unless it is NO_ACTION
        if (writeCountAction == WRITE_COUNT_ACTION.INCREMENT) {
            nodeToUpdate.incrementWriteCount();
            Node parentNode = nodeToUpdate.getParent();
            if (parentNode != null) {
                parentNode.incrementDependentWriteCount();

                // need to re-arrange all nodes at this level because of count change
                parentNode.sortDependents();
                // special debug - too verbose for big trees
                //if ((nodeToUpdate.writeCount % 100) == 0) {
                //    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                //        Tr.debug(tc, "updateNode after sorting: tree is now:" + getTreeDump());
                //    }
                //}

            } else if (writeCountAction == WRITE_COUNT_ACTION.CLEAR) {
                nodeToUpdate.setWriteCount(0);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updateNode exit: node updated to: " + nodeToUpdate.toStringDetails());
        }

        return true;
    }

    /**
     * Algorithm according to the spec:
     * Next write is the top weighted node at the next level given all priorities and current number of writes compared to the others.
     * If that node is not wanting to write, then before moving to the next top weighted sibling node, try the children under the currently selected node.
     * This is a recurive algorithm. So, once a node is selected, one of it children/grandchildren, etc... will write, unless none of them want to,
     * before moving on to a sibling node.
     *
     * @return node to perform the next write, or null if no nodes want to write.
     */
    public synchronized H2WriteQEntry findNextWriteEntry() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "findNextWriteEntry entry: from root " + root);
        }

        Node node = root.findNextWrite();
        if (node != null) {
            H2WriteQEntry e = node.getEntry();
            return e;
        }

        return null;
    }

    /**
     * Change the priority of the desired stream/node.
     * Reset all the sibling weighted priorities and re-sort the siblings, since once on priority changes, all priority ratios need to be updated and changed.
     *
     * @param streamID
     * @param newPriority
     * @return the Node that was changed, null if the node could not be found
     */
    public synchronized boolean changeNodePriority(int streamID, int newPriority) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "changeNodePriority entry: streamID to change: " + streamID + " new Priority: " + newPriority);
        }

        Node nodeToChange = root.findNode(streamID);
        Node parentNode = nodeToChange.getParent();
        if ((nodeToChange == null) || (parentNode == null)) {
            return false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "node to change: " + nodeToChange + " has parent node of: " + parentNode);
        }

        // change the priority, clear the write counts and sort the nodes at that level in the tree
        nodeToChange.setPriority(newPriority);

        parentNode.clearDependentsWriteCount();
        parentNode.sortDependents();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "changeNodePriority exit: node changed: " + nodeToChange.toStringDetails());
        }

        return true;
    }

    //  Exclusive Dependency, making node D with Parent A exclusive
    //                     A
    //   A                 |
    //  / \      ==>       D
    // B   C              / \
    //                   B   C
    //
    // Figure 4: Example of Exclusive Dependency Creation

    /**
     * Helper method to find the nodes given the stream IDs, and then call the method that does the real work.
     *
     * @param depStreamID
     * @param exclusiveParentStreamID
     * @return the Node that was changed, null if the node or the parent node could not be found
     */

    public synchronized boolean makeExclusiveDependency(int depStreamID, int exclusiveParentStreamID) {

        Node depNode = root.findNode(depStreamID);
        Node exclusiveParentNode = root.findNode(exclusiveParentStreamID);
        return makeExclusiveDependency(depNode, exclusiveParentNode);

    }

    /**
     * According to the spec, to make a dependent node exclusive:
     * 1. make the node the only child of the parent.
     * 2. all previous children of the parent become children of the added node.
     *
     * If a set of siblings nodes has any addition or deletion, then restart the priority counting, since priority numbers are derived from each other
     *
     * @param depNode
     * @param exclusiveParentNode
     * @return the Node that was changed, null if the node or the parent node could not be found
     */
    public synchronized boolean makeExclusiveDependency(Node depNode, Node exclusiveParentNode) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "makeExclusiveDependency entry: depNode: " + depNode + " exclusiveParentNode: " + exclusiveParentNode);
        }

        if ((depNode == null) || (exclusiveParentNode == null)) {
            return false;
        }
        // the dependent node that will have an exclusive parent, that dependent node will need to be the parent of the current parent's children
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "make dependent Node parent of all current Parent dependents");
        }
        ArrayList<Node> dependents = exclusiveParentNode.getDependents();
        for (int i = 0; i < dependents.size(); i++) {
            Node n = dependents.get(i);
            if (n.getStreamID() != depNode.getStreamID()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "node stream-id: " + n.getStreamID() + " will now have a parent stream of: " + depNode.getStreamID());
                }
                n.setParent(depNode, false);
            }
        }
        exclusiveParentNode.clearDependents();

        // make desired node be the only dependent of this parent
        depNode.setParent(exclusiveParentNode, true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "set up exclusive parent, clear counts and resort nodes");
        }

        // clear and re-sort where needed
        depNode.setWriteCount(0);
        depNode.clearDependentsWriteCount();
        depNode.sortDependents();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "makeExclusiveDependency exit: depNode changed to: " + depNode.toStringDetails());
        }

        return true;
    }

    // 5.3.3.  Reprioritization
    // Stream priorities are changed using the PRIORITY frame.  Setting a
    // dependency causes a stream to become dependent on the identified parent stream.
    //
    // Dependent streams move with their parent stream if the parent is
    // reprioritized.  Setting a dependency with the exclusive flag for a
    // reprioritized stream causes all the dependencies of the new parent
    // stream to become dependent on the reprioritized stream.
    //
    // If a stream is made dependent on one of its own dependencies, the
    // formerly dependent stream is first moved to be dependent on the
    // reprioritized stream's previous parent.  The moved dependency retains its weight.
    //
    //  For example, consider an original dependency tree where B and C
    //  depend on A, D and E depend on C, and F depends on D.  If A is made
    //  dependent on D, then D takes the place of A.  All other dependency
    //  relationships stay the same, except for F, which becomes dependent on
    //  A if the reprioritization is exclusive.
    //
    //      x                x                x                 x
    //      |               / \               |                 |
    //      A              D   A              D                 D
    //     / \            /   / \            / \                |
    //    B   C     ==>  F   B   C   ==>    F   A       OR      A
    //       / \                 |             / \             /|\
    //      D   E                E            B   C           B C F
    //      |                                     |             |
    //      F                                     E             E
    //                 (intermediate)   (non-exclusive)    (exclusive)
    //
    //               Figure 5: Example of Dependency Reordering

    /**
     * Implement the above spec functionality
     *
     * @param depStreamID
     * @param newParentStreamID
     * @param exclusive
     */
    public synchronized boolean changeParent(int depStreamID, int newPriority, int newParentStreamID, boolean exclusive) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "changeParent entry: depStreamID: " + depStreamID + " newParentStreamID: " + newParentStreamID + " exclusive: " + exclusive);
        }

        // notice that the new dependent node and parent do not have to be directly related coming into this method.
        // Therefore, find both nodes starting at the root.
        Node depNode = root.findNode(depStreamID);
        Node newParentNode = root.findNode(newParentStreamID);
        if ((depNode == null) || (newParentNode == null)) {
            return false;
        }

        // If the new parent can not be found in the tree under the dependent node, then changing parents is straight forward.
        if (depNode.findNode(newParentStreamID) == null) {
            // simple move, since the new parent is not a dependent of the stream that is changing parents
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "new parent is not a dependent of the stream that is changing parents");
            }
            Node oldParent = depNode.getParent();

            // move the dependent node, reset write counters, and re-sort the affected set of siblings according to original priorities
            depNode.setParent(newParentNode, true);
            depNode.setPriority(newPriority);

            newParentNode.clearDependentsWriteCount();
            newParentNode.sortDependents();

            // where it left from needs to be re-sorted also
            oldParent.clearDependentsWriteCount();
            oldParent.sortDependents();

            // according to spec: after setting the new parent, if this node is to be exclusive, then invode the exclusive algorithm on it.
            if (exclusive) {
                makeExclusiveDependency(depNode, newParentNode);
            }

        } else {
            // so much for simple.  The parent is in the dependency tree underneath the node that we now want to be dependent on the new Parent.
            // in the above example: A - depNode,  D - newParentNode.
            // The below steps are following how that spec says to handle this case (or at least it is suppose to).
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "new parent is a dependent of the stream that is changing parents");
            }

            // move the current newParentNode to be the dependent on the new dependent node's previous parent.
            Node grandParent = depNode.getParent();
            Node oldDepParent = newParentNode.getParent();
            newParentNode.setParent(grandParent, true); // set Parent will also remove newParentNode as a dependent of its current/old parent

            // now make the new dependent node depend on the newParentNode
            depNode.setParent(newParentNode, true);
            depNode.setPriority(newPriority);

            // clear and re-sort the effective levels we touched
            grandParent.clearDependentsWriteCount();
            grandParent.sortDependents();

            newParentNode.clearDependentsWriteCount();
            newParentNode.sortDependents();

            oldDepParent.clearDependentsWriteCount();
            oldDepParent.sortDependents();

            // and finally if the new dependency node is suppose to end up exclusive, make it so now.
            if (exclusive) {
                makeExclusiveDependency(depNode, newParentNode);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "changeParent exit: depNode on exit: " + depNode.toStringDetails());
        }

        return true;

    }

    //    H2 Priority removal algorithm
    //    when stream removal happens in a dependency tree, how to re-prioritize children nodes?
    //    Priority runs 1 - 256, bigger is higher priority.
    //
    //    Example, A and B have the same parent:
    //    A - 100
    //    B -  50
    //    C (dependent on A): 200
    //    D (dependent on A): 40
    //    E (dependent on A): 10
    //
    //    remove A, so C, D, E have the same parent of B now
    //    what should C and D and E priorities now be?
    //
    //    new priority = (removed parent priority) * ( moved up dependency priority / sum of dependencies priority inclusive)
    //    avoid integer division zero-ing out the fraction, so formula is:
    //    new priority = (removed parent priority *  moved up dependency priority ) / sum of dependencies priority inclusive)
    //
    //    C = 100 * 200/(200+40+10) = 80
    //    D = 100 * 40/(200+40+10) = 16
    //    E = 100 * 10/(200+40+10) = 4
    //
    //    Ending:
    //    B - 50
    //    C - 80
    //    D - 16
    //    E - 4
    //
    public synchronized boolean removeNode(int streamIDToRemove) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeNode entry: streamIDToRemove: " + streamIDToRemove);
        }

        int prioritySum = 0;
        int nodeToRemovePriority;

        // Find the node in the tree
        Node nodeToRemove = root.findNode(streamIDToRemove);
        if (nodeToRemove == null) {
            return false;
        }

        nodeToRemovePriority = nodeToRemove.getPriority();
        Node newParent = nodeToRemove.getParent();

        ArrayList<Node> dependents = nodeToRemove.getDependents();

        // find sum of dependent priorities
        Iterator<Node> iter = dependents.iterator();
        while (iter.hasNext()) {
            prioritySum += iter.next().getPriority();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sum of dependent priorities is: " + prioritySum);
            Tr.debug(tc, "reset dependent priorities and set the new parent");
        }

        // reset dependent priorities and set the new parent
        iter = dependents.iterator();
        while (iter.hasNext()) {
            Node depNode = iter.next();
            depNode.setParent(newParent, false);
            if (prioritySum != 0) {
                int priority = (nodeToRemovePriority * depNode.getPriority()) / prioritySum;
                if (priority == 0) {
                    priority = 1;
                }
                depNode.setPriority(priority);
            }
        }
        nodeToRemove.clearDependents();

        // remove node from the tree
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "remove the node from the tree");
        }
        nodeToRemove.setParent(null, true);

        return true;
    }

    /**
     * Change the priority of the desired stream/node.
     * Reset all the sibling weighted priorities and re-sort the siblings, since once on priority changes, all priority ratios need to be updated and changed.
     *
     * @param streamID
     * @param newPriority
     * @param newParent
     * @param exclusive
     * @return the Node that was changed, null if the node could not be found
     */
    public synchronized boolean updateNodeFrameParameters(int streamID, int newPriority, int newParentStreamID, boolean exclusive) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updateNodeFrameParameters entry: streamID to udpate: " + streamID + " new Priority: " + newPriority
                         + " new Parent stream ID: " + newParentStreamID + " exclusive: " + exclusive);
        }

        Node nodeToChange = root.findNode(streamID);
        Node oldParent = nodeToChange.getParent();
        if ((nodeToChange == null) || (oldParent == null)) {
            return false;
        }

        int oldParentStreamID = oldParent.getStreamID();
        int oldPriority = nodeToChange.getPriority();

        // change parent if specified
        if ((newParentStreamID != oldParentStreamID) || (exclusive)) {
            changeParent(streamID, newPriority, newParentStreamID, exclusive);
        } else {
            // parent won't change, therefore only change the priority and write counts, if it is different
            if (newPriority != oldPriority) {
                nodeToChange.setPriority(newPriority);
                oldParent.clearDependentsWriteCount();
                oldParent.sortDependents();
                // special debug - too verbose for big trees
                //if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                //    Tr.debug(tc, "updateNodeFrameParameters after sorting: tree is now:" + getTreeDump());
                //}

            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updateNodeFrameParameters exit: node changed: " + nodeToChange.toStringDetails());
        }

        return true;
    }

    public synchronized String getTreeDump() {

        StringBuffer s = new StringBuffer("\nDump Tree: " + this.hashCode());
        root.dumpDependents(s);

        return s.toString();
    }
}
