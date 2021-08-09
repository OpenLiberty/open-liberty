package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * 
 * Purpose.
 * 
 * Manages java objects within the scope of transactions. The objects are
 * all a subclass of ManagedObject. The ManagedObjects can be retrieved
 * by name or by putting them into a transactional collection such as a
 * LinkedList or a Tree.
 * 
 * Operation.
 * 
 * The ManagedObjects are stored in an ObjectStore. The application has a Token
 * which is a reference to the ManagedObject. When a Token is read from an
 * objector it is resolved to a single equivalent Token resident in memory
 * so that all users of a Token use the same one, and hence use the same
 * ManagedObject. Only a subset of the ManagedObjects need be resident in memory
 * so that very large trees and lists can be created.
 * 
 * ManagedObjects are managed using transactions employing classic write ahead logging.
 * ManagedObjects are locked within the scope or a transaction, updated via
 * add, delete or replace operations and then prepared and committed.
 * 
 * Alternatively the ManagedObjects can be replaced in place without
 * transaction locking but the application then has to perform recovery actions to
 * restore consistency if the transaction backs out. The ManagedObjects can request
 * a callback from the transaction manager to assist with the compensation of
 * these optimistic updates. LinkedLists and TreeMaps use this method internally.
 * 
 * 
 * Summary of significant classes.
 * 
 * ObjectManager The high level anchor that must be instantiated first.
 * ManagedObject The persistent objects must subclass this.
 * Token A handle that applications should use to hold references
 * to ManagedObjects.
 * LogOutput Writes log records.
 * LogInput Reads log records.
 * FileDirObjectStore A very simple and inefficient ObjectStore.
 * LinkedList A transactional doubly linked list.
 * TreeMap A transactional binary tree.
 * 
 * Files
 * 
 * source/com/ibm/objectManager Java source
 * Documentation JavaDoc for the ObjectManager package.
 * Test/Test.java An example of how to use the ObjectManager.
 * Test/Performance.java A performance test case which uses the ObjectManager
 * in a similar way to JetStream.
 * 
 */
