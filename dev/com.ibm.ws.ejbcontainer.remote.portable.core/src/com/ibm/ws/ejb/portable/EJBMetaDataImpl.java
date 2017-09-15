/*******************************************************************************
 * Copyright (c) 1998, 2006 IBM Corporation and others.
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBMetaData;
import javax.ejb.HomeHandle;
import javax.rmi.PortableRemoteObject;

/**
 * This class is designed to provide a portable implementation of EJBMetaData
 * instance returned by getEJBMetaData on EJS homes. By portable, we mean the
 * class can be serialized and returned to a vendor other than websphere. This
 * means we have to be careful on which classes/interfaces this class uses
 * (e.g. can not use com.ibm.websphere.ras package since it does not exist in other
 * vendors implementation).
 */
public class EJBMetaDataImpl implements EJBMetaData, Serializable
{
    private static final long serialVersionUID = 4092588565014573628L;

    static final boolean DEBUG_ON = false;

    // p113380 - start of change

    // This class is one of the 7 byvalue classes identified as part of the SUID mismatch
    // situation. Since this class,  and the other six classes implement Serializable,
    // the desire is that the container should own the process of marshalling and
    // demarshalling these classes. Therefore, the following buffer contents have been
    // agreed upon between AE WebSphere container and WebSphere390 container:
    //
    // |------- Header Information -----------||-------- Object Contents -----------|
    // [ eyecatcher ][ platform ][ version id ]
    //     byte[4]       short       short                 instance fields
    //
    // This class, and the other six, overide the default implementation of the
    // Serializable methods 'writeObject' and 'readObject'. The implementations
    // of these methods in each of the seven identified byvalue classes read
    // and write the buffer contents as mapped above for thier respective
    // classes.
    //
    // header information

    final static int EYECATCHER_LENGTH = Constants.EYE_CATCHER_LENGTH;
    final static byte[] EYECATCHER = Constants.EJB_META_DATA_EYE_CATCHER;
    final static short PLATFORM = Constants.PLATFORM_DISTRIBUTED;
    final static short VERSION_ID = Constants.EJBMETADATA_V1;

    // p113380- end of change

    /**
     * BEAN_TYPE constants
     */
    public static final int STATEFUL_SESSION = 1;
    public static final int STATELESS_SESSION = 2;
    public static final int NON_SESSION_BEAN = 3;

    /**
     * Stub to EJBHome object. Note, the stub is not written to
     * output stream, which is why it is made transient. When
     * readObject is called, it will use the HomeHandle that is
     * written to output stream to get the stub to EJBHome object.
     * See readObject method for more information.
     */
    private transient EJBHome ivEjbHome;

    //------------------------------------------------------
    // The following instance variables are transient
    // so that the defaultWriteObject call does not write
    // these instance variables.  We want our own writeObject
    // method to write the instances variables after it has
    // written header fields to output stream.
    // -----------------------------------------------------

    /**
     * Handle to EJBHome object.
     */
    private transient HomeHandle ivHomeHandle;

    /**
     * Bean's Class object.
     */
    private transient Class ivBeanClass;

    // Bean class name 184994
    private transient String ivBeanClassName;//184994

    /**
     * Home interface class.
     */
    private transient Class ivHomeClass;

    /**
     * Remote interface class.
     */
    private transient Class ivRemoteClass;

    /**
     * Set to boolean true if a SessionBean.
     */
    private transient boolean ivSession;

    /**
     * Set to boolean true if a stateless SessionBean.
     */
    private transient boolean ivStatelessSession;

    /**
     * Primary key class name if an EntityBean. Otherwise, null.
     */
    private transient Class ivPKClass;

    /**
     * Construct a new instance.
     *
     * @param beanType must be one of the following constants
     *            defined by this class: STATEFUL_SESSION, STATELESS_SESSION,
     *            or NON_SESSION_BEAN.
     * @param ejbHomeStub is a stub to EJBHome object for this bean.
     * @param beanClass is the Class object for the bean.
     * @param homeClass is the Class object of the home interface.
     * @param remoteClass is the Class object of the remote interface.
     * @param primaryKeyClass is the Class object of the primary key
     *            when beanType is NON_SESSION_BEAN. When beantype is
     *            STATEFUL_SESSION or STATELESS_SESSION, this parameter
     *            is ignored.
     */
    public EJBMetaDataImpl(int beanType
                           , EJBHome ejbHomeStub
                           , Class beanClass
                           , Class homeClass
                           , Class remoteClass
                           , Class primaryKeyClass)
    {
        ivEjbHome = ejbHomeStub;
        ivHomeHandle = new HomeHandleImpl(ejbHomeStub);
        ivBeanClass = beanClass;
        ivBeanClassName = ivBeanClass.getName();//184994
        ivHomeClass = homeClass;
        ivRemoteClass = remoteClass;
        ivPKClass = null;

        if (beanType == EJBMetaDataImpl.STATEFUL_SESSION)
        {
            ivSession = true;
            ivStatelessSession = false;
        }
        else if (beanType == EJBMetaDataImpl.STATELESS_SESSION)
        {
            ivSession = true;
            ivStatelessSession = true;
        }
        else
        {
            ivSession = false;
            ivStatelessSession = false;
            ivPKClass = primaryKeyClass;
        }
    }

    /**
     * Obtain the home interface bean reference of the enterprise
     * bean associated with this meta data instance.
     */
    public EJBHome getEJBHome()
    {
        return ivEjbHome;
    }

    /**
     * Obtain the Class object for the home interface.
     */
    public Class getHomeInterfaceClass()
    {
        return ivHomeClass;
    }

    /**
     * Obtain the Class object for the primary key interface.
     */
    public Class getPrimaryKeyClass()
    {
        if (ivPKClass == null)
        {
            throw new EJBException("Session beans do not have a primary key class");
        }
        return ivPKClass;
    }

    /**
     * Obtain the Class object for the remote interface.
     */
    public Class getRemoteInterfaceClass()
    {
        return ivRemoteClass;
    }

    /**
     * Obtain the Class object for the enterprise bean this
     * bean meta data is assoicated with.
     */
    public String getBeanClassName()
    {
        return ivBeanClassName;//184994

    }

    /**
     * Returns boolean true if bean is a SessionBean. Otherwise false.
     */
    public boolean isSession()
    {
        return ivSession;
    }

    /**
     * Returns boolean true if bean is a stateless SessionBean. Otherwise false.
     */
    public boolean isStatelessSession()
    {
        return ivStatelessSession;
    }

    /**
     * We will implement the readObject method for this
     * object to controll the demarshalling.
     *
     * Note, this is overriding the default implementation of
     * the Serializable interface.
     *
     * @see java.io.Serializable
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        try
        {
            in.defaultReadObject();

            // p113380 - start of change
            // Read in eye catcher.
            byte[] ec = new byte[EYECATCHER_LENGTH];

            //d164415 start
            int bytesRead = 0;
            for (int offset = 0; offset < EYECATCHER_LENGTH; offset += bytesRead)
            {
                bytesRead = in.read(ec, offset, EYECATCHER_LENGTH - offset);
                if (bytesRead == -1)
                {
                    throw new IOException("end of input stream while reading eye catcher");
                }
            }
            //d164415 end

            // Validate that the eyecatcher matches
            for (int i = 0; i < EYECATCHER_LENGTH; i++)
            {
                if (EYECATCHER[i] != ec[i])
                {
                    String eyeCatcherString = new String(ec);
                    throw new IOException("Invalid eye catcher '" + eyeCatcherString + "' in EJBMetaData input stream");
                }
            }

            // read in the rest of the header.
            in.readShort(); // platform
            in.readShort(); // version
            // p113380 - end of change

            // Read in the common data
            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.readObject entry");
            }

            ivSession = in.readBoolean();
            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.readObject: ivSession = " + ivSession);
            }

            ivStatelessSession = in.readBoolean();
            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.readObject: ivStatelessSession = " + ivStatelessSession);
            }

            ClassLoader loader = (ClassLoader) AccessController.doPrivileged( // 363037
            new PrivilegedAction() {
                public java.lang.Object run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            }
                            );

            ivBeanClassName = in.readUTF();//184994
            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.readObject: ivBeanClass is " + ivBeanClassName);//184994
            }

            ivHomeClass = loader.loadClass(in.readUTF());
            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.readObject: ivHomeClass is " + ivHomeClass);
            }

            ivRemoteClass = loader.loadClass(in.readUTF());
            if (ivSession == false)
            {
                ivPKClass = loader.loadClass(in.readUTF());
                if (DEBUG_ON)
                {
                    System.out.println("EJBMetaDataImpl.readObject loading PKEY class");
                    System.out.println("EJBMetaDataImpl.readObject: ivPKClass is " + ivPKClass);
                }
            }

            ivHomeHandle = (HomeHandle) in.readObject();
            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.readObject: read HomeHandle");
            }

            EJBHome ejbHomeStub = ivHomeHandle.getEJBHome();
            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.readObject: got EJBHome, doing narrow");
            }

            ivEjbHome = (EJBHome) PortableRemoteObject.narrow(ejbHomeStub, ivHomeClass);
            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.readObject normal exit");
            }
        } catch (IOException e)
        {
            // FFDCFilter.processException(e, CLASS_NAME + ".readObject", "346", this);
            if (DEBUG_ON)
            {
                System.out.println("***ERROR**** EJBMetaDataImpl.readObject caught unexpected exception");
                e.printStackTrace();
            }
            throw e;
        } catch (ClassNotFoundException e)
        {
            // FFDCFilter.processException(e, CLASS_NAME + ".readObject", "356", this);
            if (DEBUG_ON)
            {
                System.out.println("***ERROR**** EJBMetaDataImpl.readObject caught unexpected exception");
                e.printStackTrace();
            }
            throw e;
        }
    }

    /**
     * We will implement writeObject in order to controll
     * the marshalling for this object.
     * Note, this is overriding the default implementation of
     * the Serializable interface.
     *
     * @see java.io.Serializable
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        try
        {
            out.defaultWriteObject();

            // p113380 - start of change
            // write out the header information
            out.write(EYECATCHER);
            out.writeShort(PLATFORM);
            out.writeShort(VERSION_ID);
            // p113380 - end of change

            // write out the common data
            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.writeObject: ivSession = " + ivSession);
            }
            out.writeBoolean(ivSession);

            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.writeObject: ivStatelessSession = " + ivStatelessSession);
            }
            out.writeBoolean(ivStatelessSession);

            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.writeObject: ivBeanClass is " + ivBeanClassName);//184994
            }
            out.writeUTF(ivBeanClassName);//184994

            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.writeObject: ivHomeClass is " + ivHomeClass.getName());
            }
            out.writeUTF(ivHomeClass.getName());

            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.writeObject: ivRemoteClass is " + ivRemoteClass.getName());
            }
            out.writeUTF(ivRemoteClass.getName());

            if (ivSession == false)
            {
                if (DEBUG_ON)
                {
                    System.out.println("EJBMetaDataImpl.writeObject: ivPKClass is " + ivPKClass.getName());
                }
                out.writeUTF(ivPKClass.getName());
            }

            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.writeObject: writing HomeHandle");
            }
            out.writeObject(ivHomeHandle);

            if (DEBUG_ON)
            {
                System.out.println("EJBMetaDataImpl.writeObject normal exit");
            }
        } catch (IOException e)
        {
            // FFDCFilter.processException(e, CLASS_NAME + ".writeObject", "439", this);
            if (DEBUG_ON)
            {
                System.out.println("***ERROR**** EJBMetaDataImpl.readObject caught unexpected exception");
                e.printStackTrace();
            }
            throw e;
        }
    }

} // EJBMetaData
