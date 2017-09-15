/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
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
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Properties;

import javax.ejb.EJBHome;
import javax.ejb.HomeHandle;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.rmi.PortableRemoteObject;

/**
 * HomeHandleImpl provides a concrete implementation of a HomeHandle
 * for EJBHome objects living in an EJS server.
 * <p>
 * Note: Java EE 1.3 requires interoperability between different vendor's
 * container implementation. Therefore, this class can not use any
 * proprietary interfaces (e.g. do not use Tr method calls).
 */
public class HomeHandleImpl implements HomeHandle, Serializable
{
    private static final long serialVersionUID = 3592459660841320056L;

    // p113380 - start of change
    // IIOP V2 Home
    // |------- Header Information -----------||--- Serialized Object State ----|
    // [ eyecatcher ][ platform ][ version id ]
    //     byte[4]       short       short                EJBHome

    // Non IIOP V2 Home
    // |------- Header Information -----------||--- Serialized Object State ----|
    // [ eyecatcher ][ platform ][ version id ]
    //     byte[4]       short       short                String (IOR)

    // IIOP V1 Home
    // |------- Header Information -----------||--- Serialized Object State ----|
    // [ eyecatcher ][ platform ][ version id ]
    //     byte[4]       short       short        EJBHome Properties String String

    // Non IIOP V2 Home
    // |------- Header Information -----------||--- Serialized Object State ----|
    // [ eyecatcher ][ platform ][ version id ]
    //     byte[4]       short       short        String (IOR) Properties String String

    final static byte[] EYECATCHER = Constants.HOME_HANDLE_EYE_CATCHER;
    final static short PLATFORM = Constants.PLATFORM_DISTRIBUTED;

    // p113380 - end of change

    /**
     * EJSHome object this handle is associated with.
     */
    private transient EJBHome ivEjbHome;

    /**
     * The actual version ID used when this HomeHandle object was serialized.
     */
    private transient short ivActualVersion;

    /*
     * Instance variables only used in a Constants.HOME_HANDLE_V1 implementation.
     */
    private transient String ivJndiName = null;
    private transient String ivHomeInterface = null;
    private transient Properties ivInitialContextProperties = null;

    /**
     * Create a new <code>HomeHandleImpl</code> instance for a given
     * <code>EJBHome</code>object.
     */
    public HomeHandleImpl(EJBHome home)
    {
        ivEjbHome = home;
        ivActualVersion = Constants.HOME_HANDLE_V2;
    }

    /**
     * Return <code>EJBHome</code> reference for this HomeHandle. <p>
     */
    public EJBHome getEJBHome() throws RemoteException
    {
        // When ivActualVersion is Constant.HOME_HANDLE_V1, ivEjbHome may
        // be null. So if this happens, we need to find the EJBHome object
        // by doing a lookup in JNDI. For all other versions, ivEjbHome
        // should never be null.
        if (ivEjbHome == null)
        {
            try
            {
                Class homeClass = null;
                try
                {
                    //
                    // If we are running on the server side, then the thread
                    // context loader would have been set appropriately by
                    // the container. If running on a client, then check the
                    // thread context class loader first
                    //
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    if (cl != null)
                    {
                        homeClass = cl.loadClass(ivHomeInterface);
                    }
                    else
                    {
                        throw new ClassNotFoundException();
                    }
                } catch (ClassNotFoundException ex)
                {
                    // FFDCFilter.processException(ex, CLASS_NAME + ".getEJBHome", "131", this);
                    try
                    {
                        homeClass = Class.forName(ivHomeInterface);
                    } catch (ClassNotFoundException e)
                    {
                        // FFDCFilter.processException(e, CLASS_NAME + ".getEJBHome",
                        //                             "138", this);
                        throw new ClassNotFoundException(ivHomeInterface);
                    }
                }

                InitialContext ctx = null;
                try
                {
                    // Locate the home
                    //91851 begin
                    if (this.ivInitialContextProperties == null)
                    {
                        ctx = new InitialContext();
                    }
                    else
                    {
                        try
                        {
                            ctx = new InitialContext(ivInitialContextProperties);
                        } catch (NamingException ne)
                        {
                            // FFDCFilter.processException(ne, CLASS_NAME + ".getEJBHome",
                            //                             "161", this);
                            ctx = new InitialContext();
                        }
                    }
                    //91851 end
                    ivEjbHome = (EJBHome) PortableRemoteObject.narrow(ctx.lookup(ivJndiName)
                                                                      , homeClass);
                } catch (NoInitialContextException e)
                {
                    // FFDCFilter.processException(e, CLASS_NAME + ".getEJBHome", "172", this);
                    java.util.Properties p = new java.util.Properties();
                    p.put(Context.INITIAL_CONTEXT_FACTORY
                          , "com.ibm.websphere.naming.WsnInitialContextFactory");
                    ctx = new InitialContext(p);
                    ivEjbHome = (EJBHome) PortableRemoteObject.narrow(ctx.lookup(ivJndiName)
                                                                      , homeClass);
                }
            } catch (NamingException e)
            {
                // Problem looking up the home
                // FFDCFilter.processException(e, CLASS_NAME + ".getEJBHome", "184", this);
                RemoteException re = new NoSuchObjectException("Could not find home in JNDI");
                re.detail = e;
                throw re;
            } catch (ClassNotFoundException e)
            {
                // We couldn't find the home interface's class
                // FFDCFilter.processException(e, CLASS_NAME + ".getEJBHome", "192", this);
                throw new RemoteException("Could not load home interface", e);
            }

        }

        return ivEjbHome;
    }

    /**
     * Write this object to the ObjectOutputStream.
     * Note, this is overriding the default Serialize interface
     * implementation.
     *
     * @see java.io.Serializable
     */
    // p113380 - rewrite entire method of product interoperability.
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();

        // Write out header information.
        out.write(EYECATCHER);
        out.writeShort(PLATFORM);
        out.writeShort(ivActualVersion);

        // Write EJBHome to output stream using HandleDelegate object.
        HandleHelper.lookupHandleDelegate().writeEJBHome(ivEjbHome, out);

        // If actual version is V1, then we need to write the V1 data.
        if (ivActualVersion == Constants.HOME_HANDLE_V1)
        {
            out.writeObject(ivInitialContextProperties);
            out.writeUTF(ivHomeInterface);
            out.writeUTF(ivJndiName);
        }
    }

    /**
     * Read this object from the ObjectInputStream.
     * Note, this is overriding the default Serialize interface
     * implementation.
     *
     * @see java.io.Serializable
     */
    // p113380 - rewrite entire method of product interoperability.
    private void readObject(java.io.ObjectInputStream in)
                    throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        byte[] tempEyeCatcher = new byte[Constants.EYE_CATCHER_LENGTH];

        // d164415 start
        int bytesRead = 0;
        for (int offset = 0; offset < Constants.EYE_CATCHER_LENGTH; offset += bytesRead)
        {
            bytesRead = in.read(tempEyeCatcher, offset, Constants.EYE_CATCHER_LENGTH - offset);
            if (bytesRead == -1)
            {
                throw new IOException("end of input stream while reading eye catcher");
            }
        } // d164415 end

        for (int i = 0; i < EYECATCHER.length; i++)
        {
            if (tempEyeCatcher[i] != EYECATCHER[i])
            {
                String eyeCatcherString = new String(tempEyeCatcher);
                throw new IOException("Invalid eye catcher '" + eyeCatcherString +
                                      "' in handle input stream");
            }
        }

        // Get websphere platform and version ID from header.
        in.readShort(); // platform
        ivActualVersion = in.readShort();
        //d158086 added version checking
        if ((ivActualVersion != Constants.HOME_HANDLE_V1) && (ivActualVersion != Constants.HOME_HANDLE_V2))
        {
            throw new java.io.InvalidObjectException("Home Handle data stream is not of the correct version, this client should be updated.");
        }
        //d158086

        // Use HandleDelegate object to read in EJBHome object.

        //d164668 allow 2nd change for type 1 home handles if stub doesn't connect use jndi logic

        try {
            ivEjbHome = HandleHelper.lookupHandleDelegate().readEJBHome(in);
        } catch (IOException ioe)
        {
            // FFDCFilter.processException(t, CLASS_NAME + "readObject", "335", this);

            if (ivActualVersion != Constants.HOME_HANDLE_V1)
            {
                throw ioe;
            }
        }

        if (ivActualVersion == Constants.HOME_HANDLE_V1)
        {
            ivInitialContextProperties = (Properties) in.readObject();
            ivHomeInterface = in.readUTF();
            ivJndiName = in.readUTF();

            // Null out the reference since we do not have a portable way
            // to determine if we got reference from the ORB and the version ID
            // indicates this is a non-robust  handle.  So, we need to force the
            // getEJBHome method to get a new reference by doing a JNDI lookup.
            ivEjbHome = null; //p125891
        }
    }

} // end class

