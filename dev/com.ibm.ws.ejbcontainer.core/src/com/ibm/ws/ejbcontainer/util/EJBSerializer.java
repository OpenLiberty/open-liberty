/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Class EJBSerializer is an abstract class that provides
 * the interface for other WAS components to use to
 * serialize a EJB reference to either a local home,
 * component, or EJB 3 business interface. It also must be
 * used to serialize a EJB reference to a EJB 3 remote
 * business interface.
 */
public abstract class EJBSerializer
{
    /**
     * Enum is used as the return value for the {@link EJBSerializer#getObjectType(Object)} method.
     */
    public enum ObjectType
    {
        NOT_AN_EJB,
        EJB_LOCAL_OBJECT, // The 2.1 EJBLocalObject wrapper.
        EJB_LOCAL_HOME, // The 2.1 EJBLocalHome wrapper.
        EJB_BUSINESS_LOCAL, // The 3.0 local business interface wrapper.
        EJB3_BUSINESS_REMOTE, // The 3.0 remote or RMI remote business interface wrapper.
        EJB_LOCAL_BEAN, // The 3.1 local bean (no-interface) wrapper.
        EJB_HOME, // The 2.1 remote EJBHome stub.
        EJB_OBJECT, // The 2.1 remote EJBObject stub.
        CORBA_STUB // Other CORBA stubs.
    }

    private static String CLASS_NAME = EJBSerializer.class.getName();

    private final static String IMPL_CLASS_NAME = "com.ibm.ejs.container.util.EJBSerializerImpl";

    /**
     * The singleton instance for all consumers to use.
     * This object is stateless, so it can be safely shared
     * by multiple threads.
     */
    private static EJBSerializer cvInstance = null;

    /**
     * Returns singleton instance.
     */
    public static EJBSerializer instance()
    {
        EJBSerializer theInstance;
        synchronized (CLASS_NAME)
        {
            if (cvInstance == null)
            {
                try
                {
                    cvInstance = (EJBSerializer) Class.forName(IMPL_CLASS_NAME).newInstance();
                } catch (Throwable t)
                {
                    FFDCFilter.processException(t, CLASS_NAME + ".cvInstance", "46");
                }
            }

            theInstance = cvInstance;
        }

        return theInstance;
    }

    /**
     * For a specified object, return a enum constant that indicates
     * whether or not the object is an EJB and if so, what kind of EJB
     * it is an instance of. This method is useful for determining if
     * the {@link #serialize(Object)} method of this class should be
     * used to serialize the object.
     * 
     * @param theObject is the specified object.
     * 
     * @return one of the ObjectType enum constants defined in this class.
     *         NOT_AN_EJB is returned if the object is not an EJB object type.
     */
    public abstract ObjectType getObjectType(Object theObject);

    /**
     * Serialize the specified object into a byte array. This method must
     * only be used if the {@link #getObjectType(Object)} method returns
     * one of the following enum constants:
     * <ul>
     * <li> {@link ObjectType#EJB_LOCAL_OBJECT} <li> {@link ObjectType#EJB_LOCAL_HOME} <li> {@link ObjectType#EJB_BUSINESS_LOCAL} <li> {@link ObjectType#EJB3_BUSINESS_REMOTE} <li>
     * {@link ObjectType#EJB_LOCAL_BEAN} </ul>
     * 
     * A IllegalArgumentException is thrown if the getObjectType method
     * returns something other than the above enum constants.
     * 
     * 
     * @param theObject is the object to be serialized. You must
     *            call the {@link #getObjectType(Object)} method in this
     *            class and verify it is one of the enum constants the {@link ObjectType} returned is
     *            something other than {@link ObjectType#NOT_AN_EJB}.
     * 
     * @return a byte[] that contains the serialized bytes of the EJB object reference.
     * 
     * @throws IllegalArgumentException is thrown if the {@link #getObjectType(Object)} method returns a enum constant that not valid for this method (see
     *             description of this method for the valid values).
     */
    public abstract byte[] serialize(Object theObject);

    /**
     * Given a specified byte[] that contains the serialized bytes of a
     * EJB object reference, deserialize the bytes to obtain the original
     * object that was passed to the {@link #serialize(Object)} method
     * of this class.
     * 
     * @param bytes is the serialized bytes of the EJB object reference.
     * 
     * @return the original object that was passed to the {@link #serialize(Object)} method of this class.
     * 
     * @throws Exception is thrown if any exception occurs during serialization
     *             or the bytes parameter is not a byte array that was returned
     *             by the {@link #serialize(Object)} method of this class.
     */
    public abstract Object deserialize(byte[] bytes) throws Exception;

}
