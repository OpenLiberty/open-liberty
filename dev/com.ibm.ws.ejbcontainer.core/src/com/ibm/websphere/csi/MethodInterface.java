/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBMethodInterface;

/**
 * <code>MethodInterface</code> defines legal values for the method interface
 * type associated with the {@link EJBMethodInfo} interface. <p>
 * 
 * The <code>MethodInterface</code> values may also be used to obtain all of
 * the methods for an interface implemented by an EJB (see {@link EJBComponentMetaData#getEJBMethodMetaData
 * EJBComponentMetaData.getEJBMethodMetaData}). <p>
 * 
 * <code>MethodInterface</code> is a java enumeration type used for compile
 * time checking of valid values. The values are the objects themselves, and
 * there is a single instance for each value, so == checking should be performed
 * rather than equals() comparisons. The numeric value assigned to each is really
 * only for mapping to the xml deployment descriptors. The numeric value is
 * irrelevant in regards to equality. <p>
 * 
 * <DL>
 * <DT>The valid method interfaces an EJB method may belong to are:
 * <DD>{@link #REMOTE} <DD>{@link #HOME} <DD>{@link #LOCAL} <DD>{@link #LOCAL_HOME} <DD>{@link #SERVICE_ENDPOINT} <DD>{@link #TIMED_OBJECT} <DD>{@link #MESSAGE_LISTENER} <DD>
 * {@link #LIFECYCLE_INTERCEPTOR} </DL>
 * 
 * The integer values associated with each <code>MethodInterface</code> do
 * correspond to the integer values for the corresponding interface types
 * defined in {@link org.eclipse.jst.j2ee.ejb.MethodElementKind}. <p>
 * 
 * Note: an instance of {@link EJBMethodInfo} must be associated with
 * a specific interface type. Therefore, unlike <code>MethodElementKind</code>,
 * there is no value corresponding to <code>UNSPECIFIED</code>.
 * Also, the EJB Specification does not provide a mechanism for identifying
 * all possible interface types, so <code>MethodInterface</code> contains some
 * values not specified by <code>MethodElementKind</code>. <p>
 * 
 * @see EJBMethodInfo
 * @see EJBMethodInfo#getInterfaceType
 * @see EJBComponentMetaData
 * @see EJBComponentMetaData#getEJBMethodMetaData
 * @see org.eclipse.jst.j2ee.ejb.MethodElementKind
 */
public class MethodInterface
{
    public static final MethodInterface REMOTE = new MethodInterface(EJBMethodInterface.REMOTE);

    public static final MethodInterface HOME = new MethodInterface(EJBMethodInterface.HOME);

    public static final MethodInterface LOCAL =
                    new MethodInterface(EJBMethodInterface.LOCAL);

    public static final MethodInterface LOCAL_HOME =
                    new MethodInterface(EJBMethodInterface.LOCAL_HOME);

    public static final MethodInterface SERVICE_ENDPOINT =
                    new MethodInterface(EJBMethodInterface.SERVICE_ENDPOINT);

    public static final MethodInterface TIMED_OBJECT =
                    new MethodInterface(EJBMethodInterface.TIMER);

    public static final MethodInterface MESSAGE_LISTENER =
                    new MethodInterface(EJBMethodInterface.MESSAGE_ENDPOINT);

    public static final MethodInterface LIFECYCLE_INTERCEPTOR =
                    new MethodInterface(EJBMethodInterface.LIFECYCLE_INTERCEPTOR); // F743-1751

    /**
     * Construct new MethodInterface instance with EJBMethodInterface.<p>
     * 
     * All constructors are private, to insure the object instances, which are
     * the valid 'values', are singletons, and thus == can be used for equality
     * rather than equals(). <p>
     **/

    private MethodInterface(EJBMethodInterface emi)
    {
        this.ivValue = emi.value();
        this.ivName = emi.specName();
    }

    /**
     * Returns this enumeration value's unique integer value. <p>
     * 
     * This value should not be used for equality checking; instead compare the
     * two MethodInterface instances using ==. This numeric value is only
     * intended for mapping to the xml (i.e. WCCM) values. <p>
     **/
    public int getValue()
    {
        return ivValue;
    }

    /**
     * Returns a hash code value for this enumeration value. The hash code is
     * just this enumeration value's integer value. <p>
     **/
    @Override
    public int hashCode()
    {
        return ivValue;
    }

    /**
     * @deprecated Do not use this method, instead use the == operator.
     **/
    @Deprecated
    // d174057
    @Override
    public boolean equals(Object obj)
    {
        return (this == obj);
    }

    /**
     * Returns a string value corresponding to this enumeration value. <p>
     **/
    @Override
    public String toString()
    {
        return ivName;
    }

    /**
     * Returns number of possible <code>MethodInterface</code>. <p>
     **/
    public static int getNumValues()
    {
        return svNumValues;
    }

    /**
     * Returns a table of all MethodInterface values. <p>
     **/
    public static MethodInterface[] getAllValues()
    {
        // This list must be created every time, to insure the caller does
        // not modify an internal list. Declaring a static final list does
        // not work, since it only protects the reference to the list, and
        // not the list entries.
        return new MethodInterface[]
        {
         REMOTE,
         HOME,
         LOCAL,
         LOCAL_HOME,
         SERVICE_ENDPOINT,
         TIMED_OBJECT,
         MESSAGE_LISTENER,
         LIFECYCLE_INTERCEPTOR, // F743-1751
        };
    }

    /**
     * Unique value for each legal <code>MethodInterface</code> for
     * fast lookups.
     **/
    private final int ivValue;

    /**
     * String representation of each legal <code>MethodInterface</code> for
     * trace purposes.
     **/
    private final String ivName;

    /** Number of legal MethodInterface values. **/
    private static final int svNumValues = getAllValues().length; // d174057

}
