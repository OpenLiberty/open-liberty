package com.ibm.ws.sib.msgstore.task;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.PersistentDataEncodingException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.cache.links.LinkOwner;
import com.ibm.ws.sib.msgstore.persistence.Operation;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.WorkItem;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

public abstract class Task implements Operation, WorkItem
{
    private static TraceComponent tc = SibTr.register(Task.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    public static final class Type
    {
        public static final Type ADD = new Type("Add");
        public static final Type PERSIST_LOCK = new Type("PersistLock");
        public static final Type PERSIST_UNLOCK = new Type("PersistUnlock");
        public static final Type REMOVE = new Type("Remove");
        public static final Type REMOVE_LOCKED = new Type("RemoveLocked");
        public static final Type UNKNOWN = new Type("Unknown");
        public static final Type UPDATE = new Type("Update");
        public static final Type PERSIST_REDELIVERED_COUNT = new Type("PersistRedeliveredCount");

        private String _name;

        private Type(String name)
        {
            _name = name;
        }

        public String toString()
        {
            return _name;
        }
    }

    // The default size approximation for a persistable task - reflects a database
    // operation whose size does not depend on the size of the task's persistable data
    public static final int DEFAULT_TASK_PERSISTABLE_SIZE_APPROXIMATION = 500;

    private final AbstractItemLink _link;
    private AbstractItem _item = null;

    protected final AbstractItem getItem() throws SevereMessageStoreException
    {
        if (null == _item)
        {
            _item = _link.getItem();
            if (null == _item)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    SibTr.debug(this, tc, "null item");
                }
            }
        }
        return _item;
    }

    // forward pointer for task list navigation. This is manipulated directly by task list.
    Task _nextTask = null;

    public Task(AbstractItemLink link) throws SevereMessageStoreException
    {
        _link = link;
        // 258179 - Added call to getItem() to ensure callbacks can be issued following MS restart
        if (_link != null)
        {
            getItem();
        }
    }

    public abstract void abort(final PersistentTransaction transaction) throws SevereMessageStoreException;

    /** During the second stage of commit we will change the state of the item, allowing the 
     * changes to become visible (eg item becomes available).  Event callbacks are invoked in the
     * second stage.
     * @param transaction
     * @throws MessageStoreException
     */
    public abstract void commitExternal(final PersistentTransaction transaction) throws SevereMessageStoreException;

    /** Perform the fist stage of commit - all internal changes must be complete by the end of
     * this stage.
     * @param transaction
     * @throws MessageStoreException
     */
    public abstract void commitInternal(final PersistentTransaction transaction) throws SevereMessageStoreException;

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#copyDataIfVulnerable()
     */
    public void copyDataIfVulnerable() throws PersistentDataEncodingException, SevereMessageStoreException {}

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#ensureDataAvailable()
     */
    public void ensureDataAvailable() throws PersistentDataEncodingException, SevereMessageStoreException {
        copyDataIfVulnerable();
    }

    public final AbstractItemLink getLink()
    {
        return _link;
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#getPersistable()
     */
    public Persistable getPersistable()
    {
        return _link.getTuple();
    }

    final int getStorageStrategy()
    {
        return getPersistable().getStorageStrategy();
    }

    /**
     * @return one of the constants of Task.Type representing
     * the type of task.
     */
    public Task.Type getTaskType()
    {
        return Task.Type.UNKNOWN;
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#isCreateOfPersistentRepresentation()
     */
    public boolean isCreateOfPersistentRepresentation()
    {
        return false;
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#isDeleteOfPersistentRepresentation()
     */
    public boolean isDeleteOfPersistentRepresentation()
    {
        return false;
    }

    public boolean isRemoveFromList(final LinkOwner list)
    {
        return false;
    }

    public abstract void postAbort(final PersistentTransaction transaction) throws SevereMessageStoreException;

    public abstract void postCommit(final PersistentTransaction transaction) throws SevereMessageStoreException;

    /** Perform initial steps in precommit phase.  Essentially this amounts to invoking the event
     * callbacks on the item subclasses.
     * @param transaction
     * @throws MessageStoreException
     */
    public abstract void preCommit(final PersistentTransaction transaction) throws SevereMessageStoreException;

    public String toString() 
    {
        if (getLink() == null)
        {
            return "[" + getTaskType() + "]";
        }
        else
        {
            return "[" + getTaskType() + ":" + getLink().getID() + "]";
        }
    }
}
