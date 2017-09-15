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

import java.util.NoSuchElementException;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.cache.links.LinkOwner;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.WorkItem;
import com.ibm.ws.sib.msgstore.transactions.impl.WorkList;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class implements a list of things to be done to the cache under a transaction.
 * Its main purpose is to provide a single entry point for the transactble callback, and then
 * create the neccessary sub-stages in the commit process to allow for all the conflicting
 * requirements to be satisfied.
 * <p>
 * It is a list of {@link Task}s
 * </p><p>
 * NOTE: this class is not thread safe - each instance is used with only one transaction, 
 * and each transaction should be used on one thread at a time.
 * </p> 
 *
 * StateModel:
 * the state model presumes the following valid sequences:
 * <ul> 
 * <li>precommit, commit, postComplete(true)</li>
 * <li>precommit, rollback, postComplete(false)</li>
 * <li>precommit, rollback, postComplete(false)</li>
 * <li>postComplete(false)??</li>
 * <li></li>
 * <li></li>
 * </ul>
 */
public final class TaskList implements WorkList 
{
    private final class FilteredIterator implements java.util.Iterator 
    {
        private Task _nextTask = _firstTask;
        private final int _storageStrategy;

        FilteredIterator(int storageStrategy)
        {
            _storageStrategy = storageStrategy;
            // Keep moving forward while we have a task that 
            // does NOT match the storage strategy we are looking for.
            while (null != _nextTask && _storageStrategy != _nextTask.getStorageStrategy())
            {
                _nextTask = _nextTask._nextTask;
            }
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public final boolean hasNext()
        {
            return(null != _nextTask);
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public final Object next()
        {
            if (null == _nextTask)
            {
                throw new NoSuchElementException();
            }

            Task replyTask = _nextTask; // remember the task we wish to return

            // move the pointer from the current task to the next task 
            // in the chain (which might be null). We need to do this 
            // outside the loop as we are already pointing at a match
            if (null != _nextTask)
            {
                _nextTask = _nextTask._nextTask;
            }
            // Keep moving forward while we have a task that 
            // does NOT match the storage strategy we are looking for.
            while (null != _nextTask && _storageStrategy != _nextTask.getStorageStrategy())
            {
                _nextTask = _nextTask._nextTask;
            }

            return replyTask;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public final void remove()
        {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    private final class Iterator implements java.util.Iterator
    {
        private Task _nextTask = _firstTask;
        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public final boolean hasNext()
        {
            return(null != _nextTask);
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public final Object next()
        {
            if (null == _nextTask)
            {
                throw new NoSuchElementException();
            }
            Task replyTask = _nextTask;
            _nextTask = _nextTask._nextTask;
            return replyTask;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public final void remove()
        {
            throw new UnsupportedOperationException("Not supported");
        }
    }


    private static final String STATE_BEGIN_ABORT = "Aborting";
    private static final String STATE_BEGIN_COMMIT = "Committing";
    private static final String STATE_BEGIN_POSTABORT = "PostAborting";
    private static final String STATE_BEGIN_POSTCOMMIT = "PostCommitting";
    private static final String STATE_BEGIN_PRECOMMIT = "Precommitting";
    private static final String STATE_END_ABORT = "Aborted";
    private static final String STATE_END_COMMIT = "Committed";
    private static final String STATE_END_POSTABORT = "PostAborted";
    private static final String STATE_END_POSTCOMMIT = "PostCommitted";
    private static final String STATE_END_PRECOMMIT = "Precommitted";
    private static final String STATE_UNTOUCHED = "Untouched";

    private static TraceComponent tc = SibTr.register(TaskList.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private Task _firstTask;
    private boolean _hasStoreAlways = false;
    private boolean _hasStoreEventually = false;
    private boolean _hasStoreMaybe = false;
    private boolean _hasStoreNever = false;
    private Task _lastTask;
    private boolean _requiresPersistence = false;

    private String _state = STATE_UNTOUCHED;
    private int _taskCount = 0;

    public final void addWork(WorkItem item)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "addWork", "WorkItem=" + item);

        Task task = (Task) item;

        int storageStrategy = task.getStorageStrategy();
        switch (storageStrategy)
        {
        case AbstractItem.STORE_ALWAYS :
            _hasStoreAlways = true;
            break;
        case AbstractItem.STORE_EVENTUALLY :
            _hasStoreEventually = true;
            break;
        case AbstractItem.STORE_MAYBE :
            _hasStoreMaybe = true;
            break;
        case AbstractItem.STORE_NEVER :
            _hasStoreNever = true;
            break;
        }

        if (null == _firstTask || null == _lastTask)
        {
            // list is empty, first and last task are the same
            _firstTask = task;
            _lastTask = task;
            task._nextTask = null;
        }
        else
        {
            // append the new task.
            _lastTask._nextTask = task;
            _lastTask = task;
            _lastTask._nextTask = null;
        }
        _taskCount++;

        // Keep track of the requirement for persistence as the tasks are added
        Persistable tuple = task.getPersistable();
        if (null != tuple)
        {
            _requiresPersistence |= tuple.requiresPersistence();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addWork");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Transactable#transactableCommit(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void commit(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "commit", "Transaction=" + transaction);

        if (STATE_END_PRECOMMIT == _state)
        {
            // normal processing
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())  SibTr.exit(tc, "commit");
            throw new TaskListException(_state);
        }

        _state = STATE_BEGIN_COMMIT;

        Task task = _firstTask;
        while (null != task)
        {
            task.commitInternal(transaction);
            task = task._nextTask;
        }

        task = _firstTask;
        while (null != task)
        {
            task.commitExternal(transaction);
            task = task._nextTask;
        }

        _state = STATE_END_COMMIT;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())  SibTr.exit(tc, "commit");
    }

    /**
     * @return number of links on the list.
     */
    public final int countLinks()
    {
        return _taskCount;
    }

    public final int countRemovingItems(final LinkOwner list)
    {
        int count = 0;
        Task task = _firstTask;
        while (null != task)
        {
            if (task.isRemoveFromList(list))
            {
                count = count + 1;
            }
            task = task._nextTask;
        }
        return count;
    }

    /**
     * Use this method to declare that a tasklist has already been
     * precommitted.  This is used when restoring tasks used to resolve 
     * 'in doubt' items from persistence.
     * By doing this we reduce the number of different paths through the
     * state model. 
     * @throws TaskListException 
     */
    public final void declareAlreadyPrecommitted() throws TaskListException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "declareAlreadyPrecommitted");

        if (STATE_UNTOUCHED == _state)
        {
            _state = STATE_END_PRECOMMIT;
        }
        else if (STATE_END_PRECOMMIT == _state)
        {
            // declared precommitted already
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "declareAlreadyPrecommitted");
            throw new TaskListException(_state);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "declareAlreadyPrecommitted");
    }

    /**
     * @return true if the receiver has tasks involving items
     * with a StoreAlways storage strategy.
     */
    public final boolean hasStoreAlways()
    {
        return _hasStoreAlways;
    }

    /**
     * @return true if the receiver has tasks involving items
     * with a StoreEventually storage strategy.
     */
    public final boolean hasStoreEventually()
    {
        return _hasStoreEventually;
    }

    /**
     * @return true if the receiver has tasks involving items
     * with a StoreMaybe storage strategy.
     */
    public final boolean hasStoreMaybe()
    {
        return _hasStoreMaybe;
    }

    /**
     * @return true if the receiver has tasks involving items
     * with a StoreNever storage strategy.
     */
    public final boolean hasStoreNever()
    {
        return _hasStoreNever;
    }

    public final java.util.Iterator iterator()
    {
        return new Iterator();
    }

    /**
     * @param storageStrategy of required tasks
     * @return iterator
     */
    public final java.util.Iterator iterator(int storageStrategy)
    {
        return new FilteredIterator(storageStrategy);
    }


    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Transactable#transactablePostComplete(com.ibm.ws.sib.msgstore.Transaction, boolean)
     */
    public final void postComplete(final PersistentTransaction transaction, final boolean committed) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "postComplete", "Transaction=" + transaction + ", Committed=" + committed);

        if (committed)
        {
            if (STATE_END_COMMIT == _state)
            {
                // normal course
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "postComplete");
                throw new TaskListException(_state);
            }
            _state = STATE_BEGIN_POSTCOMMIT;
        }
        else
        {
            if (STATE_BEGIN_ABORT == _state)
            {
                // blew up in our rollback
            }
            else if (STATE_END_ABORT == _state)
            {
                // blew up in someone elses rollback - after ours
            }
            else if (STATE_END_PRECOMMIT == _state)
            {
                // blew up in the persistence layers commit
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "postComplete");
                throw new TaskListException(_state);
            }
            _state = STATE_BEGIN_POSTABORT;
        }

        if (committed)
        {
            Task task = _firstTask;
            while (null != task)
            {
                task.postCommit(transaction);
                task = task._nextTask;
            }
        }
        else
        {
            Task task = _firstTask;
            while (null != task)
            {
                task.postAbort(transaction);
                task = task._nextTask;
            }
        }

        if (committed)
        {
            _state = STATE_END_POSTCOMMIT;
        }
        else
        {
            _state = STATE_END_POSTABORT;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "postComplete");
    }

    public final void preCommit(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "preCommit", "Transaction=" + transaction);

        if (STATE_UNTOUCHED != _state)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "preCommit");
            throw new TaskListException(_state);
        }

        _state = STATE_BEGIN_PRECOMMIT;
        Task task = _firstTask;
        while (null != task)
        {
            task.preCommit(transaction);
            task = task._nextTask;
        }

        _state = STATE_END_PRECOMMIT;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "preCommit");
    }

    public final boolean requiresPersistence()
    {
        return _requiresPersistence;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Transactable#transactableAbort(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void rollback(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rollback", "Transaction=" + transaction);

        if (STATE_UNTOUCHED == _state)
        {
            // MS user rollback
        }
        else if (STATE_END_PRECOMMIT == _state)
        {
            // normal rollback
        }
        else if (STATE_BEGIN_PRECOMMIT == _state)
        {
            // blew up in pre-commit
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rollback");
            throw new TaskListException(_state); // PK81848.2
        }

        _state = STATE_BEGIN_ABORT;
        Task task = _firstTask;
        while (null != task)
        {
            task.abort(transaction);
            task = task._nextTask;
        }

        _state = STATE_END_ABORT;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rollback");
    }

    public final String toString() 
    {
        StringBuffer buf = new StringBuffer();

        buf.append("TaskList (");
        buf.append(_state);
        buf.append(" ");
        int count = _taskCount;

        buf.append(Integer.toString(count));
        if (1 == count)
        {
            buf.append(" link) {");
        }
        else
        {
            buf.append(" links) {");
        }

        if (count != 0)
        {
            Task task = _firstTask;
            while (null != task)
            {
                buf.append("\n  "+task);
                task = task._nextTask;
            }
        }

        buf.append("\n)");

        return buf.toString();
    }

    public String toXmlString() 
    {
        StringBuffer retval = new StringBuffer();

        retval.append("<work-list>\n");
        Task task = _firstTask;
        while (null != task)
        {
            retval.append("<work-item>");
            retval.append(task);
            retval.append("</work-item>\n");
            task = task._nextTask;
        }
        retval.append("</work-list>\n");

        return retval.toString();
    }
}
