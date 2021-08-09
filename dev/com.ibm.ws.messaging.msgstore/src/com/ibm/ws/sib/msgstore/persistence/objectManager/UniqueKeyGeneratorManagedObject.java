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

import com.ibm.ws.sib.msgstore.persistence.UniqueKeyGenerator;


/**
 * This object is the Java representation of a unique key generators
 * persistent data.
 */
public class UniqueKeyGeneratorManagedObject extends ManagedObject
{
    private static final long serialVersionUID = 6408654889610894998L;

    private String _generatorName;
    private long   _generatorKeyLimit;


    public UniqueKeyGeneratorManagedObject(UniqueKeyGenerator generator)
    {
        _generatorName     = generator.getName();
        _generatorKeyLimit = generator.getRange();
    }

    public String getGeneratorName()
    {
        return _generatorName;
    }


    public void setGeneratorName(String generatorName)
    {
        _generatorName = generatorName;
    }


    public long getGeneratorKeyLimit()
    {
        return _generatorKeyLimit;
    }


    public void setGeneratorKeyLimit(long generatorKeyLimit)
    {
        _generatorKeyLimit = generatorKeyLimit;
    }


    public void becomeCloneOf(ManagedObject other)
    {
        _generatorName     = ((UniqueKeyGeneratorManagedObject)other).getGeneratorName();
        _generatorKeyLimit = ((UniqueKeyGeneratorManagedObject)other).getGeneratorKeyLimit();
    }

    public String toString()
    {
        // Defect 292187
        // include the super implementation to ensure 
        // inclusion of the object id.
        StringBuffer buffer = new StringBuffer(super.toString());

        buffer.append("(UniqueKeyGenerator[ generatorName: ");
        buffer.append(_generatorName);
        buffer.append(", generatorKeyLimit: ");
        buffer.append(_generatorKeyLimit);
        buffer.append(" ])");

        return buffer.toString();
    }
}
