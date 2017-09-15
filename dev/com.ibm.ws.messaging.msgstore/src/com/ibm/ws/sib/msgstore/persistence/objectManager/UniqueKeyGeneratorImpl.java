package com.ibm.ws.sib.msgstore.persistence.objectManager;
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

import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.persistence.*;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A class to generate unique numbers for use in the message store.
 */
public class UniqueKeyGeneratorImpl implements UniqueKeyGenerator
{
    private static TraceComponent tc = SibTr.register(UniqueKeyGeneratorImpl.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private String _name;
    private long  _range;
    private long  _midrange;

    // The globally unique key properties
    private long   _globalUnique;
    private long   _globalUniqueLimit;
    private long   _globalUniqueThreshold;

    // Defect 560281.1
    // Use an inner class specific to this class for locking.
    private final static class GlobalUniqueLock {}
    private final GlobalUniqueLock _globalUniqueLock = new GlobalUniqueLock();

    // The per-instance unique key properties
    private long   _instanceUnique = -100L;

    // Defect 560281.1
    // Use an inner class specific to this class for locking.
    private final static class InstanceUniqueLock {}
    private final InstanceUniqueLock _instanceUniqueLock = new InstanceUniqueLock(); 

    // The asynchronous range manager 
    private RangeManager _globalUniqueManager;


    public UniqueKeyGeneratorImpl(RangeManager rangeManager, String name, long range)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", "RangeManager="+rangeManager+", Name="+name+", Range="+range);

        _name     = name;
        _range    = range;
        _midrange = _range >> 1;

        _globalUniqueManager = rangeManager;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
    }


    void initialize() throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "initialize");

        synchronized (_globalUniqueLock)
        {
            if (_globalUniqueManager.entryExists(this))
            {
                _globalUnique = _globalUniqueManager.updateEntry(this);
            }
            else
            {
                _globalUnique = _globalUniqueManager.addEntry(this);
            }

            _globalUniqueThreshold = _globalUnique + _midrange;
            _globalUniqueLimit     = _globalUnique + _range;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "initialize");
    }


    public String getName()
    {
        return _name;
    }


    public long getRange()
    {
        return _range;
    }


    /**
     * Returns a unique numeric value across the entire life of the message store.
     * At least until the <code>long</code> value wraps around.
     *
     * @return a value which is unique across the entire life of the
     * message store
     *
     */
    public long getUniqueValue() throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getUniqueValue");

        long retval;
            
        synchronized (_globalUniqueLock) 
        {
            if (_globalUnique == _globalUniqueThreshold) 
            {
                _globalUniqueManager.scheduleUpdate(this);
            }
            else if (_globalUnique == _globalUniqueLimit) 
            {
                _globalUniqueManager.updateEntry(this);
                nextRangeMaximumAvailable();
            }
            
            retval = _globalUnique++;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getUniqueValue", "return="+retval);
        return retval;
    }


    /**
     * Callback to inform the generator that the current range of available 
     * unique keys has now grown.
     */
    public void nextRangeMaximumAvailable()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "nextRangeMaximumAvailable");

        synchronized (_globalUniqueLock) 
        {
            _globalUniqueThreshold = _globalUniqueLimit + _midrange;
            _globalUniqueLimit     = _globalUniqueLimit + _range;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "nextRangeMaximumAvailable");
    }


    /**
     * Returns a unique value that is unique only to the current instance of the
     * <code>UniqueKeyGenerator</code>
     *
     * @return A value which is unique to the current instance of the unique
     * key generator
     */
    public long getPerInstanceUniqueValue()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getPerInstanceUniqueValue");

        long retval;

        synchronized(_instanceUniqueLock)
        {
            retval = _instanceUnique--;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getPerInstanceUniqueValue", "return="+retval);
        return retval;
    }
}
