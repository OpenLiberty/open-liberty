/*******************************************************************************
 * Copyright (c) 2001, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejb.portable;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;

/**
 * HandleImpl provides a concrete implementation of a EJB Handle
 * for both Entity and Session beans living in an EJS server.
 * <p>
 * Note: Java EE 1.3 requires interoperability between different vendor's
 * container implementation. Therefore, this class can not use any
 * proprietary interfaces (be sure not to add Tr method calls).
 */
public class HandleImpl implements Handle, Serializable
{

    /*
     * d138969
     * In general, you should never change the SUID for serialization, unless you
     * want to cause this version of the class to be incompatible with all previous versions.
     * There may be a reason in the future for doing this, but unless that is desired,
     * it should not be done. A great deal of work has occurred to synchronize all the various
     * AE releases with each other and zSeries.
     * Likewise, if you introduce a new serializable class that will be shared with others outside
     * the container runtime, it is a good idea to provide an SUID. If the class changes,
     * i.e. new data members are added, new methods are added, then definately, the 2nd version
     * of the class needs to have the computed SUID of the prior version defined in it. The
     * reason for this is that the SUID is computed based on the data and methods in the class,
     * and changing those will result in a different value for the SUID.
     * In addition, careful attention needs to be paid when adding or removing data members
     * from these serializable classes. The format of the data has been agreed upon
     * between zSeries and AE. Changes to these formats will require new VERSION_IDs
     * for the datastream and new readObject and writeObject logic.
     */

    private static final long serialVersionUID = -4496370801878254926L;

    // p113380 - start of change
    // IIOP V2 Handle
    // |------- Header Information -----------||--- Serialized Object State ------|
    // [ eyecatcher ][ platform ][ version id ]
    //     byte[4]      short        short           EJBObject

    // Non IIOP V2 Handle
    // |------- Header Information -----------||--- Serialized Object State ------|
    // [ eyecatcher ][ platform ][ version id ]
    //     byte[4]      short        short           String (IOR)

    // IIOP V1 Handle
    // |------- Header Information -----------||--- Serialized Object State ------|
    // [ eyecatcher ][ platform ][ version id ]
    //     byte[4]      short        short           EJBObject HomeHandle Key

    // Non IIOP V1 Handle
    // |------- Header Information -----------||--- Serialized Object State ------|
    // [ eyecatcher ][ platform ][ version id ]
    //     byte[4]      short        short         String(IOR)    HomeHandle KeyHelper

    final static int EYECATCHER_LENGTH = Constants.EYE_CATCHER_LENGTH;
    final static byte[] EYECATCHER = Constants.HANDLE_EYE_CATCHER;
    final static short PLATFORM = Constants.PLATFORM_DISTRIBUTED;
    static final boolean DEBUG_ON = false;

    // p113380 - end of change

    /**
     * EJBObject this handle is associated with.
     */
    private transient EJBObject ivEjbObject;

    /**
     * The actual version ID used when this Handle object was serialized.
     */
    private transient short ivActualVersion;

    /**
     * When ivActualVersion is set to Constants.HANDLE_V1, this is
     * primary key object for the Entity bean associated with the
     * ivEjbObject instance variable. A null reference is used if ivEjbObject
     * refers to a Session bean or if ivActualVersion is Constants.HANDLE_V2.
     */
    private transient Serializable ivKey = null;

    /**
     * Only used when ivActualVersion is set to Constants.HANDLE_V1.
     * This is the HomeHandle objet for the home object. A null reference
     * is used when ivActualVersion is set to Constants.HANDLE_V2.
     */
    private transient HomeHandle ivHomeHandle = null;

    //158086---------------->
    /**
     * Create a new <code>HandleImpl</code> instance for the given
     * <code>EJBObject</code>.
     * <p>
     * This CTOR should only used for all SessionBean objects or
     * for those EntityBean objects that are registered
     * using an indirect IOR. Use other CTOR for all other cases.
     */
    //<----------------158086
    public HandleImpl(EJBObject object)
    {
        ivEjbObject = object;
        ivActualVersion = Constants.HANDLE_V2;
    }

    /*
     * Finds the findByPrimaryKey method in the bean's home interface
     */
    private Method findFindByPrimaryKey(Class c)
    {
        Method[] methods = c.getMethods();

        for (int i = 0; i < methods.length; ++i)
        {
            if (methods[i].getName().equals("findByPrimaryKey"))
            {

                return methods[i];
            }
        }

        return null;
    }

    /**
     * Returns the EJBObject reference.
     */

    /*
     * d138969
     * GetReference will not get called for SessionBean
     * handles. The reason is the only time getReference is called is when ivEJBObject is null.
     * The only time ivEJBObject is null is when readObject method is called and it reads a
     * Constants.HANDLE_V1 id from the input stream. That only happens for Entity beans that
     * are sent by a pre-Aquila release server. It never happens for a session bean. This can be
     * seen by looking at com.ibm.ws.ejb.portable.Constants interface for HANDLE_V1 and HANDLE_V2.
     * By definition, HANDLE_V2 must be used for session beans. Consequently, readObject should never
     * null out ivEJBObject when a session bean Handle is being read from input stream, which means
     * getReference will never be called for session beans.
     */

    private EJBObject getReference() throws RemoteException
    {
        EJBObject object = null;
        EJBHome home = ivHomeHandle.getEJBHome();
        Class homeClass = home.getClass();

        try
        {
            Method fbpk = findFindByPrimaryKey(homeClass);
            object = (EJBObject) fbpk.invoke(home, new Object[] { ivKey });
        } catch (InvocationTargetException e)
        {
            // Unwrap the real exception and pass it back
            // FFDCFilter.processException(e, CLASS_NAME + ".getReference", "144", this);
            Throwable t = e.getTargetException();
            if (t instanceof RemoteException)
            {
                throw (RemoteException) t;
            }
            else
            {
                throw new RemoteException("Could not find bean", t);
            }
        } catch (IllegalAccessException e)
        {
            // This shouldn't happen
            // FFDCFilter.processException(e, CLASS_NAME + ".getReference", "158", this);
            throw new RemoteException("Bad home interface definition", e);

        }

        return object;
    }

    /**
     * Return <code>EJBObject</code> reference for this handle. <p>
     */
    public EJBObject getEJBObject() throws RemoteException
    {
        if (ivEjbObject == null)
        {
            ivEjbObject = getReference();
        }

        return ivEjbObject;
    }

    /**
     * Write this object to the ObjectOutputStream.
     * Note, this is overriding the default Serialize interface
     * implementation.
     *
     * @see java.io.Serializable
     */
    // p113380 - rewrote entire method for product interoperability.
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();

        // Write out header information first.
        out.write(EYECATCHER);
        out.writeShort(PLATFORM);
        out.writeShort(ivActualVersion);

        // Now write EJBObject to output stream using HandleDelegate object.
        HandleHelper.lookupHandleDelegate().writeEJBObject(ivEjbObject, out);

        // If actual version is 1, then we need to write HomeHandle and key
        // to output stream as well.
        if (ivActualVersion == Constants.HANDLE_V1)
        {
            out.writeObject(ivHomeHandle);

            //d158086---------------->

            // if this is the IBM IIOP class or we have no IIOP class
            // which means we are not running in an ibm environment
            // we leave the key as is.  this allows older handle versions
            // running on ibm thin client or client containers, to still function

            Boolean orb = HandleHelper.isORBOutputStream(out);
            if (orb != null && orb.booleanValue())
            {

                if (DEBUG_ON)
                    System.out.println("serialize IIOP stream primary key");
                out.writeObject(ivKey);
                if (DEBUG_ON)
                    System.out.println("serialized IIOP stream primary key");
            }
            else
            {
                if (DEBUG_ON)
                    System.out.println("serialize non IIOP stream primary key");
                out.writeObject(KeyHelper.serialize(ivKey));
                if (DEBUG_ON)
                    System.out.println("serialized non IIOP stream primary key");

            }
            //<----------------d158086

        }
    }

    /**
     * Read this object from the ObjectInputStream.
     * Note, this is overriding the default Serialize interface
     * implementation.
     *
     * @see java.io.Serializable
     */
    // p113380 - rewrote entire method for product interoperability.
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        byte[] tempEyeCatcher = new byte[Constants.EYE_CATCHER_LENGTH];

        //d164415start
        int bytesRead = 0;
        for (int offset = 0; offset < Constants.EYE_CATCHER_LENGTH; offset += bytesRead)
        {
            bytesRead = in.read(tempEyeCatcher, offset, Constants.EYE_CATCHER_LENGTH - offset);
            if (bytesRead == -1)
            {
                throw new IOException("end of input stream while reading eye catcher");
            }
        } //d164415 end

        for (int i = 0; i < EYECATCHER.length; i++)
        {
            if (tempEyeCatcher[i] != EYECATCHER[i])
            {
                String eyeCatcherString = new String(tempEyeCatcher);
                throw new IOException("Invalid eye catcher '" + eyeCatcherString + "' in handle input stream");
            }
        }

        // Get websphere platform and version ID from header.
        in.readShort(); // platform
        ivActualVersion = in.readShort();
        //d158086 added version checking
        if ((ivActualVersion != Constants.HANDLE_V1) && (ivActualVersion != Constants.HANDLE_V2))
        {

            throw new java.io.InvalidObjectException("Handle data stream is not of the correct version, this client should be updated.");
        }
        //d158086

        // Now read in EJBObject from the input stream using HandleDelegate object.

        //d164668 allow 2nd change for type 1 handles. If stub doesn't connect use findby logic

        try {
            ivEjbObject = HandleHelper.lookupHandleDelegate().readEJBObject(in);
        } catch (IOException ioe)
        {
            // FFDCFilter.processException(t, CLASS_NAME + "readObject", "406", this);

            if (ivActualVersion != Constants.HANDLE_V1)
            {
                throw ioe;
            }

        }

        // if actual version is version 1, then we need to read in the
        // additional data.
        if (ivActualVersion == Constants.HANDLE_V1)
        {
            // Null out the reference since we do not have a portable way
            // of knowing whether reference was obtained from the ORB
            // and the version ID indicates this is a non-robust
            // handle.  So, we need to force the getEJBObject method to
            // get a new reference by looking up home and doing a find
            // by primary key.
            ivEjbObject = null; //p125891

            ivHomeHandle = (HomeHandle) in.readObject();
            ivKey = (Serializable) in.readObject();

        }

    }

} // end class

