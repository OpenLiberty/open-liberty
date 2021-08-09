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

import com.ibm.ws.objectManager.ManagedObject;
import com.ibm.ws.objectManager.Token;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.utils.ras.SibTr;


public class Anchor extends ManagedObject
{
    private static final long serialVersionUID = -4338090966469167560L;

    private static TraceComponent tc = SibTr.register(Anchor.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);
    private Token _rootStreamToken;
    private Token _uniqueKeyTableToken;
    private Token _meOwnerToken;

    public Token getRootStreamToken()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        {
            SibTr.entry(tc, "getRootStreamToken");
            SibTr.exit(tc, "getRootStreamToken", "return="+_rootStreamToken);
        }
        return _rootStreamToken;
    }

    public void setRootStreamToken(Token token)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setRootStreamToken", "Token="+token);

        _rootStreamToken = token;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setRootStreamToken");
    }

    public Token getUniqueKeyTableToken()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        {
            SibTr.entry(tc, "getUniqueKeyTableToken");
            SibTr.exit(tc, "getUniqueKeyTableToken", "return="+_uniqueKeyTableToken);
        }
        return _uniqueKeyTableToken;
    }

    public void setUniqueKeyTableToken(Token token)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setUniqueKeyTableToken", "Token="+token);
        
        _uniqueKeyTableToken = token;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setUniqueKeyTableToken");
    }

    public Token getMEOwnerToken()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        {
            SibTr.entry(tc, "getMEOwnerToken");
            SibTr.exit(tc, "getMEOwnerToken", "return="+_meOwnerToken);
        }
        return _meOwnerToken;
    }

    public void setMEOwnerToken(Token token)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setMEOwnerToken", "Token="+token);
        
        _meOwnerToken = token;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setMEOwnerToken");
    }

    /**
     * Replace the state of this object with the same object in some other state.
     * Used for to restore the before image if a transaction rolls back.
     * 
     * @param other  The object this object is to become a clone of.
     */
    public void becomeCloneOf(ManagedObject other)
    {
        Anchor o = (Anchor)other;
        _rootStreamToken = o._rootStreamToken;
        _uniqueKeyTableToken = o._uniqueKeyTableToken;
    }
}
