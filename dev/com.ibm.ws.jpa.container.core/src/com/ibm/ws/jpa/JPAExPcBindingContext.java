/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

/**
 * JPAExPcBindingContext contains the information needed to resolve SFSB extend-scoped persistence
 * context inheritance relation at runtime.
 * 
 * Each SLSB and SFSB own an instance of JPAExPcBindingContext in the BeanO.
 * 
 * When a component/business method is invoked, the associated JPAExPcBindingContext for the bean instance
 * is used to begin/end in the ThreadContext, which can be accessed from JPAExPcBindingContextAccessorImpl.
 * 
 */
public class JPAExPcBindingContext implements Serializable
{
    private static final long serialVersionUID = 4812885700562185607L;

    private static final JPAPuId[] NoPuIds = new JPAPuId[0];

    /*
     * Parent component invocation context in the inheritance call stack.
     */
    private JPAExPcBindingContext ivParentContext;

    /*
     * Increasingly generated id. Assume 1000 component invoations is created per second, it will take
     * 0x7fffffffffffffff / 1000 / 3600 / 24 / 365 = 292471208 years before it rolls-over.
     */
    private static Long svIdBase = new Long(0);

    /*
     * An id managed by the JPA serivce which will uniquely identify a SFSB instance. It is declared
     * here as an Object instead of JPAExPcBindId to avoid circular build dependency. The user of
     * this object should not be aware of this object type.
     */
    private long ivBindId;

    /*
     * Name of the component that owns this context. For debug identification only.
     */
    private final String ivJ2eeName;

    /*
     * Is this component use Bean Managed Transaction.
     */
    private final boolean ivIsBmt;

    /*
     * Has the component begin a user transaction in this context.
     */
    private boolean ivHasBmtUserTxBegunInMethod;

    /*
     * Array of extend-scoped persistence contexts PuIds declared in this component.
     */
    transient private JPAPuId[] ivPuIds; // d468174

    /*
     * Constructor.
     */
    public JPAExPcBindingContext(String j2eeName, boolean isBMT, JPAPuId[] puIds)
    {
        synchronized (svIdBase)
        {
            this.ivBindId = ++svIdBase;
        }
        ivJ2eeName = j2eeName;
        ivIsBmt = isBMT;
        ivHasBmtUserTxBegunInMethod = false;
        ivPuIds = (puIds != null) ? puIds : NoPuIds;
    }

    /**
     * Setter method for parent Context.
     * 
     * @param parent Context
     */
    public void setParentContext(JPAExPcBindingContext parent)
    {
        ivParentContext = parent;
    }

    /**
     * Getter method for parent Context.
     * 
     * @return Parent context
     */
    public JPAExPcBindingContext getParentContext()
    {
        return ivParentContext;
    }

    /**
     * Getter method for the bind id.
     * 
     * @return Binding Id
     */
    public long getBindId()
    {
        return ivBindId;
    }

    /**
     * Getter method for component j2eeName, for debug identification.
     * 
     * @return Component j2eeName.
     */
    public String getName()
    {
        return ivJ2eeName;
    }

    /**
     * Getter method for isBmt.
     * 
     * @return true if context assoicates support BMT.
     */
    public boolean isBmt()
    {
        return ivIsBmt;
    }

    /**
     * Getter method for hasBmtUserTxBegunInMethod.
     * 
     * @return true if a User Transaction has begun in this context.
     */
    public boolean hasBmtUserTxBegunInMethod()
    {
        return ivHasBmtUserTxBegunInMethod;
    }

    /**
     * Setter method for hasBmtUserTxBegunInMethod.
     * 
     * @param hasBegan
     */
    public void setBmtUserTxBegunInMethod(boolean hasBegan)
    {
        ivHasBmtUserTxBegunInMethod = hasBegan;
    }

    /**
     * Getter method for declared extend-scoped PC JPAPuIds.
     * 
     * @return Array of PuId that is assoicated to extend-scoped persistence context in this context.
     */
    public JPAPuId[] getExPcPuIds()
    {
        return ivPuIds;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return (ivParentContext != null ? ivParentContext : "[Thread Entry]")
               + "\n\t--> " + thisToString();
    }

    /*
     * Convert and print the only the context at the top of the stack.
     */
    public String thisToString()
    {
        return "{JPAExPcBindingContext: BindId=" + ivBindId +
               ", BMT=" + (ivIsBmt ? 'T' : 'F') + ", UserTxBegun=" +
               (ivHasBmtUserTxBegunInMethod ? 'T' : 'F') + ", Name=" + ivJ2eeName +
               ", PuIds=<" + ivPuIds.length + "> " + Arrays.toString(ivPuIds) + '}';
    }

    /**
     * Read this object from the ObjectInputStream.
     * Note, this is overriding the default Serialize interface
     * implementation.
     * 
     * @see java.io.Serializable
     */
    // d468174
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        // Read non transient fields.
        in.defaultReadObject();

        // Read transient fields.
        int n = in.readInt();
        if (n == 0)
        {
            ivPuIds = NoPuIds;
        }
        else
        {
            ivPuIds = new JPAPuId[n];
            for (int i = 0; i < n; ++i)
            {
                JPAPuId id = (JPAPuId) in.readObject();
                ivPuIds[i] = id;
            }
        }
    }

    /**
     * Write this object to the ObjectOutputStream.
     * Note, this is overriding the default Serialize interface
     * implementation.
     * 
     * @see java.io.Serializable
     */
    // d468174
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        // Write non transient fields.
        out.defaultWriteObject();

        // Write transient fields.
        int n = ivPuIds.length;
        out.writeInt(n);
        for (int i = 0; i < n; ++i)
        {
            JPAPuId id = ivPuIds[i];
            out.writeObject(id);
        }
    }
}
