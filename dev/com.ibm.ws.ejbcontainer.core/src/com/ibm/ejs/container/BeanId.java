/*******************************************************************************
 * Copyright (c) 1998, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.ibm.ejs.container.activator.ActivationStrategy;
import com.ibm.ejs.container.util.ByteArray;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CSIRuntimeException;
import com.ibm.websphere.csi.EJBKey;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.StatefulSessionKey;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.failover.SfFailoverKey;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * An <code>BeanId</code> encapsulates the identity of an EJB within an EJS
 * container. <p>
 * 
 * The identity of an EJB within an EJS container is defined by the pair
 * (home, primary key). The home is identified by a string containing the
 * Java EE name of the bean as configured in the container this
 * <code>BeanId</code> is associated with. The primary key differs
 * depending on the type of EJB this <code>BeanId</code> identifies. For an
 * entity bean, the primary key is just the primary key of the entity
 * bean. For a stateful session bean, the primary key is assigned by the
 * container. For a stateless or singleton session bean, the primary key
 * is null; the home uniquely identifies stateless and singleton session beans. <p>
 * 
 * <code>BeanId</code>'s are immutable. Once created, the identity
 * they encapsulate cannot be changed. <p>
 * 
 * Note, the primary key must be an instance of <code>Serializable</code>
 * to ensure that the <code>BeanId</code> state is serializable, as well. <p>
 * 
 * <code>BeanId</code>'s properly the <code>hashCode</code>() and
 * <code>equals</code>() methods. Two <code>BeanId</code>'s hash the same
 * and compare as equal iff they encapsulate the same identity. <p>
 */

public class BeanId
                implements EJBKey
{
    private static final TraceComponent tc = Tr.register(BeanId.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container"); //d121510

    private static final String CLASS_NAME = "com.ibm.ejs.container.BeanId";

    /**
     * True iff this instance is a bean for a home. <p>
     */
    boolean _isHome;

    /**
     * Primary key of the EJB this BeanId identifies <p>
     */
    Serializable pkey;

    //89554
    /**
     * Java EE Name of the EJB this BeanId identifies <p>
     */
    J2EEName j2eeName;

    /**
     * Byte array containing serialized representation of this BeanId.
     */
    ByteArray byteArray;//105890.1

    /**
     * Reference to a live instance of our home interface
     */
    HomeInternal home;

    /**
     * ActivationStrategy for beans in the associated home. Cached for
     * performance.
     **/
    // d140003.29
    private ActivationStrategy activationStrategy;

    /**
     * Precomputed hash value for this <code>BeanId</code>.
     */
    int hashValue;

    /**
     * Cached failover key used for stateful bean failover.
     */
    private SfFailoverKey ivFailoverKey; //d536149

    /**
     * d536149
     * <br/>
     * Returns the failover key for this BeanId. This key is used in various
     * maps to reference failover data for Stateful Bean Failover.
     * <br/>
     * Precondition: This method must only be called for stateful beans
     * with failover enabled. If this condition is not met, unpredictable
     * exceptions could occur, as a failover key would be created for a bean
     * that is unable to be replicated via failover.
     * <br/>
     * 
     * @return the failover key for this beanId instance. This method will never
     *         return null (see precondition)
     */
    public final SfFailoverKey getFailoverKey()
    {
        //we only need to initialize failoverKey if this bean is failover-enabled
        if (ivFailoverKey == null)
        {
            ivFailoverKey = EJSContainer.getDefaultContainer().getEJBRuntime().createFailoverKey(this);
        }
        return ivFailoverKey;
    }

    /**
     * Constants used in creating the byte array without serialization
     */

    // Identifier for a bean which is a home object
    private static final byte HOME_BEAN = 0x00;

    // Bean is a stateless bean
    private static final byte STATELESS_BEAN = 0x01;

    // Bean is a stateful bean
    private static final byte STATEFUL_BEAN = 0x02;

    // Bean is a entity bean
    private static final byte ENTITY_BEAN = 0x03;

    // Bean is a message driven bean
    private static final byte MESSAGEDRIVEN_BEAN = 0x04; // d121554

    // Bean is a singleton bean
    private static final byte SINGLETON_BEAN = 0x05; //F743-508

    // Marks the bean as one with bean managed transactions
    private static final byte USES_BEAN_MANAGED_TX = 0x10;

    // Marks the bean as being module version-capable                      F54184
    private static final byte MODULE_VERSION_CAPABLE = 0x20;

    /**
     * A mapping of types to the list of type ids above. This map does not
     * consider the {@link USES_BEAN_MANAGED_TX} flag.
     * 
     * @see #getBeanType(BeanMetaData)
     */
    private static final byte[] TYPE_TO_TYPE_ID_MAP; // d621921

    static
    {
        TYPE_TO_TYPE_ID_MAP = new byte[8];
        TYPE_TO_TYPE_ID_MAP[InternalConstants.TYPE_SINGLETON_SESSION] = SINGLETON_BEAN;
        TYPE_TO_TYPE_ID_MAP[InternalConstants.TYPE_STATELESS_SESSION] = STATELESS_BEAN;
        TYPE_TO_TYPE_ID_MAP[InternalConstants.TYPE_STATEFUL_SESSION] = STATEFUL_BEAN;
        TYPE_TO_TYPE_ID_MAP[InternalConstants.TYPE_BEAN_MANAGED_ENTITY] = ENTITY_BEAN;
        TYPE_TO_TYPE_ID_MAP[InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY] = ENTITY_BEAN;
        TYPE_TO_TYPE_ID_MAP[InternalConstants.TYPE_MESSAGE_DRIVEN] = MESSAGEDRIVEN_BEAN;
    }

    /**
     * Constants which identify the type of primary key used by this bean
     */
    private static final byte SERIALIZED_PKEY = 0x00;

    // These are the special types of pkeys we optimize for.
    // These types do not use serialization. Speeds up session beans for sure
    // entity beans using primitive primary keys of the common types will
    // also benefit
    private static final byte INTEGER_PKEY = 0x01;

    private static final byte STRING_PKEY = 0x03;

    private static final byte LONG_PKEY = 0x04;

    private static final byte BYTE_ARRAY_PKEY = 0x08;

    // Constants used for length computations

    private static final int BEAN_TYPE_LEN = 1;

    private static final int J2EE_NAME_LEN = 4;

    @SuppressWarnings("unused")
    private static final int PKEY_TYPE_LEN = 1;

    /**
     * Static initialization of the header
     */
    private static final byte[] header = new byte[]
    {
     (byte) 0xAC, (byte) 0xAC,// MAGIC
     0x00, 0x02, // MAJOR_VERSION
     0x00, 0x01, // MINOR_VERSION
    };

    private static final int HEADER_LEN = header.length;

    /**
     * Cached and reusable ByteArrayOutputStreams for writePKeyBytes.
     **/
    // d173022.12 d175235
    private static final ByteArrayOutputStream[] svBAOSs =
                    new ByteArrayOutputStream[50];

    /**
     * Number of cached ByteArrayOutputStream object in svBAOSs.
     **/
    // d175235
    private static int svBAOSsSize = 0;

    /**
     * No-arg constructor required for the Serializable subclass ExternalizedBeanId.
     */
    BeanId()
    {
        pkey = null;
        j2eeName = null;//89554
        _isHome = false;
    }

    /**
     * Same as constructor with home, primary key, and 'isHome' parameter,
     * where 'isHome' parameter set to 'false'.
     **/
    public BeanId(HomeInternal home, Serializable pkey)
    {
        this(home, pkey, false);
    }

    /**
     * Create a new <code>BeanId</code> that identifies the EJB defined
     * by the given home and primary key. <p>
     * 
     * @param home an instance of <code>EJSHome</code> which is the home
     *            of the EJB whose identity this BeanId represents <p>
     * 
     * @param pkey a <code>Serializable</code> that represents the primary
     *            key of the EJB whose identity this BeanId represents <p>
     * 
     * @exception CSIRuntimeException thrown if
     *                <code>home</code> is null; <code>pkey</code> may
     *                be null <p>
     */
    public BeanId(HomeInternal home, Serializable pkey, boolean isHome)
    {//89554
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "BeanId", new Object[] { home, pkey, Boolean.valueOf(isHome) });

        if (home == null)
        {
            // d140714
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Home given for BeanId is null, " +
                             "this is most likely an internal PM or container issue");
            throw new CSIRuntimeException("Home given for BeanId is null, " +
                                          "this is most likely an internal PM or container issue");
            // d140714
        }

        this.home = home;
        this._isHome = isHome;
        this.j2eeName = home.getJ2EEName();

        //-------------------------------------------------------------
        // A bean id with a null primary key must only be used to
        // identify stateless session beans, which are only identified
        // by their type (home).
        //-------------------------------------------------------------

        //89554
        this.pkey = pkey;
        //hashValue = this.home.hashCode() + this.pkey.hashCode();
        hashValue = computeHashValue(j2eeName, pkey, _isHome);
        //end 89554

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "BeanId");
    } // BeanId

    /**
     * Create a partially formed <code>BeanId</code> that identifies the EJB
     * defined by the given j2eename and primary key. <p>
     * 
     * This <code>BeanId</code> is used specifically when an ejb's initialization
     * is deferred, and only contains enough internal state to create an IOR that
     * may be used to bind a reference to a home or stateless/singleton session ejb remote
     * business interface into naming.<p>
     * 
     * @param j2eeName an instance of <code>J2EEName</code> which is the J2EEName
     *            of the EJB whose identity this BeanId represents.
     * @param pkey a <code>Serializable</code> that represents the primary
     *            key of the EJB whose identity this BeanId represents.
     * @param isHome true if the BeanId represents the home itself;
     *            false if the BeanId represents an instance.
     * 
     * @exception CSIRuntimeException thrown if
     *                <code>home</code> is null; <code>pkey</code> may
     *                be null <p>
     */
    // d427315.1
    public BeanId(J2EEName j2eeName, Serializable pkey, boolean isHome)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "BeanId : " + j2eeName + " : " + pkey);

        this.home = null;
        this._isHome = isHome;
        this.j2eeName = j2eeName;

        //-------------------------------------------------------------
        // A bean id with a null primary key must only be used to
        // identify stateless session beans, which are only identified
        // by their type (home).
        //-------------------------------------------------------------

        this.pkey = pkey;
        hashValue = computeHashValue(j2eeName, pkey, _isHome);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "BeanId : " + this);
    } // BeanId

    /**
     * Get the JNDI name for the home associated with this
     * <code>BeanId</code>. <p>
     * 
     * This method always returns the JNDI name, regardless of whether
     * this beanId identifies a "home" bean or a "real" bean.
     */
    public final String getJNDIName()
    {//89554
        return home.getJNDIName(pkey);
    } // getJNDIName

    /**
     * Returns a fully initialized BeanId. The resulting BeanId is guaranteed
     * to have a valid home field. This method only needs to be called if this
     * object was constructed via BeanId(J2EEName, Serializable, boolean).
     * 
     * @return a fully initialized BeanId
     * @throws ClassNotFoundException if the object is not fully initialized
     *             and an exception occurs initializing
     * @throws IOException if the object is not fully initialized and an
     *             exception occurs initializing
     */
    public BeanId getInitializedBeanId()
                    throws ClassNotFoundException, IOException
    {
        if (home == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getInitializedBeanId: initializing " + this);
            EJSContainer container = EJSContainer.getDefaultContainer();
            return BeanId.getBeanId(getByteArray(), container);
        }

        return this;
    }

    /**
     * Get the home associated with this <code>BeanId</code>. <p>
     * 
     * @return The <code>EJSHome</code> instance associated with this
     *         <code>BeanId</code>. <p>
     */
    public final HomeInternal getHome()
    {
        return home;
    } // getHome

    /**
     * Get the BeanMetaData associated with this BeanId.
     */
    public final BeanMetaData getBeanMetaData() //LIDB2018-1
    {
        return home.getBeanMetaData(this);
    }

    /**
     * Get primary key associated with this BeanId. <p>
     * 
     * @return <code>Serializable</code> primary key associated with
     *         this <code>EJBId</code> <p>
     */
    public final Serializable getPrimaryKey()
    {
        return pkey;
    } // getPrimaryKey

    //89554
    /**
     * Get Java EE name associated with this BeanId. <p>
     * 
     * @return <code>Serializable</code> Java EE name associated with
     *         this <code>EJBId</code> <p>
     */
    public final J2EEName getJ2EEName()
    {
        // homes use a dummy J2EEName for the home of homes
        // and the bean's J2EEName as the primary key
        return (_isHome) ? (J2EEName) pkey : j2eeName;//89554
    } // getPrimaryKey

    /**
     * Returns the ActivationStrategy associated with the Home
     * for this BeanId. <p>
     * 
     * This method was added to improve performance. It will cache
     * the activation strategy in the BeanId, so that the method
     * call to get the ActivationStrategy from the Home may be
     * avoided for future operations. <p>
     * 
     * Note: ActivationStrategy is NOT cached in the constructor
     * and deserialize, so that it is only cached when needed,
     * and not to impact the performance of deserialize which
     * may run in a synchronized block. <p>
     * 
     * @return ActivationStrategy associated with the Home for
     *         this BeanId.
     **/
    // d140003.29
    public final ActivationStrategy getActivationStrategy()
    {
        if (activationStrategy == null)
            activationStrategy = home.getActivationStrategy();

        return activationStrategy;
    }

    /**
     * Return hashcode value for this BeanId. <p>
     * 
     * Two BeanId's that encapsulate the same identity return identical
     * hashcode values. <p>
     * 
     * @return int containing hashcode value for this BeanId <p>
     */
    @Override
    public final int hashCode()
    {
        return hashValue;
    } // hashCode

    /**
     * Returns true iff the given <code>Object</code> is an
     * <code>BeanId</code> that encapsulates the same EJB identity as
     * this <code>BeanId</code>. <p>
     * 
     * This override of the default Object.equals is required, even though
     * there are type specific overloads, in case the caller does not have
     * (or know) the parameter as the specific type. <p>
     * 
     * @param o the <code>Object</code> to compare for equality with this
     *            <code>BeanId</code> <p>
     */
    @Override
    public final boolean equals(Object o)
    {
        // Before checking the contents, perform the quick check to
        // see if the value passed is this object.  In many cases, the
        // BeanId in the wrapper and beanO are the same object.         d140003.12
        if (this == o)
        {
            return true;
        }

        if (o instanceof BeanId)
        {
            BeanId e = (BeanId) o;
            if (e.hashValue == this.hashValue) // d173022.1
            {
                if (_isHome)
                {
                    // Homes use a dummy J2EEName for the home of Home
                    // and the bean's J2EEName as the primary key.
                    // So not need to check J2EEName.
                    return (pkey == e.pkey || pkey != null && pkey.equals(e.pkey)); // d173022.1
                }
                else
                {
                    // Need to check both j2eeName and pkey.
                    return ((j2eeName == e.j2eeName || j2eeName.equals(e.j2eeName)) && // d173022.1, d621921
                    (pkey == e.pkey || (pkey != null && pkey.equals(e.pkey))));
                }
            }
        }
        return false;
    } // equals

    /**
     * Returns true iff the given <code>Object</code> is an
     * <code>BeanId</code> that encapsulates the same EJB identity as
     * this <code>BeanId</code>. <p>
     * 
     * This type specific version is provided for performance, and avoids
     * any instanceof or casting. <p>
     * 
     * @param beanId the <code>Object</code> to compare for equality with this
     *            <code>BeanId</code> <p>
     */
    // d195605
    public final boolean equals(BeanId beanId)
    {
        // Before checking the contents, perform the quick check to
        // see if the value passed is this object.  In many cases, the
        // BeanId in the wrapper and beanO are the same object.         d140003.12
        if (this == beanId)
        {
            return true;
        }

        if (beanId != null) // d273615
        {
            if (beanId.hashValue == this.hashValue) // d173022.1
            {
                if (_isHome)
                {
                    // Homes use a dummy J2EEName for the home of Home
                    // and the bean's J2EEName as the primary key.
                    // So not need to check J2EEName.
                    return (pkey == beanId.pkey || pkey != null && pkey.equals(beanId.pkey)); // d173022.1
                }
                else
                {
                    // Need to check both j2eeName and pkey.
                    return ((j2eeName == beanId.j2eeName || j2eeName.equals(beanId.j2eeName)) && // d173022.1, d676679
                    (pkey == beanId.pkey || (pkey != null && pkey.equals(beanId.pkey))));
                }
            }
        }

        return false;
    } // equals

    // d156807.3 Begins
    /**
     * Returns true iff the specified parameters would result in the
     * creation of a <code>BeanId</code> that encapsulates the same
     * EJB identity as this <code>BeanId</code>. <p>
     * 
     * This is a special purpose equals method that allows the bean
     * activation path to look for a pre-existing BeanId in the
     * BeanId Cache. <p>
     * 
     * @param compareHome Home of the BeanId to be created
     * @param comparePkey primary key of the BeanId to be created
     * @param isHome true if the BeanId to be created represents a Home
     **/
    public final boolean equals(HomeInternal compareHome,
                                Serializable comparePkey,
                                boolean isHome)
    {
        if (isHome)
        {
            // Homes use a dummy J2EEName for the home of Home
            // and the bean's J2EEName as the primary key.
            // So not need to check J2EEName.
            return (pkey == comparePkey || pkey != null && pkey.equals(comparePkey));
        }
        else
        {
            // Need to check both j2eeName and pkey.
            J2EEName compareJ2EEName = compareHome.getJ2EEName();
            return ((j2eeName == compareJ2EEName || j2eeName.equals(compareJ2EEName)) && (pkey == comparePkey || pkey != null && pkey.equals(comparePkey)));
        }
    }

    // d156807.3 Ends

    /**
     * Return string representation of this BeanId. <p>
     */
    @Override
    public String toString()
    {
        // ACK! Probably want to add a toString to EJSHome
        return "BeanId(" + j2eeName + ", " + pkey + ")";//89554
    } // toString

    public final String getIdString()
    {
        return j2eeName + ":" + pkey;//89554
    }

    /**
     * A more performance friendly version of getBytes(). This method does
     * not use serialization to create a byte array. It relies on plain
     * byte arrays to create keys for beans.
     */
    public final ByteArray getByteArray()
    {
        if (byteArray != null)
        {
            return byteArray;
        }
        else
        {
            getByteArrayBytes();//this will initialize byteArray
            return byteArray;
        }
    }

    public final byte[] getByteArrayBytes()
    {
        // This is a public method which is called in addition to
        // getByteArray so check the cached bytes here as well.            d121176
        if (byteArray != null)
        {
            return byteArray.getBytes();
        }

        try
        {
            // Get the corresponding bmd; will be null for EJBFactory
            BeanMetaData bmd = getBeanMetaDataInternal();

            // d172033.x Begins
            ByteArrayOutputStream baos = getByteArrayStream();
            baos.write(header);
            baos.write(getBeanType(bmd, _isHome));

            byte[] j2eeNameBytes = getJ2EENameBytes(bmd, _isHome);
            int j2eeNameBytesLength = j2eeNameBytes.length;
            // LIDB2775-23.0 Begins
            if (EJSPlatformHelper.isZOS())
            {
                baos.write((byte) (j2eeNameBytesLength >> 24));
                baos.write((byte) (j2eeNameBytesLength >> 16));
                baos.write((byte) (j2eeNameBytesLength >> 8));
                baos.write((byte) (j2eeNameBytesLength));
            } else
            {
                for (int i = 0; i < 4; i++)
                    baos.write(j2eeNameBytesLength >> (i * 8));
            }
            // LIDB2775-23.0 Ends
            baos.write(j2eeNameBytes);
            writePKeyBytes(baos, bmd);

            byte[] serialized = baos.toByteArray();
            byteArray = new ByteArray(serialized);
            freeByteArrayStream(baos);
            // d172033.x Ends
        } catch (Exception ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".getByteArrayBytes",
                                        "550", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Unable to serialize primary key for bean",
                         new Object[] { this, ex });
            return null;
        }

        return byteArray.getBytes();
    }

    /**
     * Get the BeanMetaData associated with this BeanId whether this is
     * a complete BeanId or just a partial BeanId. <p>
     * 
     * Will return the corresponding bean metadata if this BeanId represents
     * a home, component, or business interface. <p>
     * 
     * Null will be returned if this BeanId is for an EJBFactory.
     */
    // F54184
    private BeanMetaData getBeanMetaDataInternal()
    {
        J2EEName beanName = _isHome ? (J2EEName) pkey : j2eeName;

        // Partially initialized BeanIds have a null home field, but we can still
        // get the partially initialized BeanMetaData from the home record.
        if (home == null) {
            HomeRecord hr = EJSContainer.homeOfHomes.getHomeRecord(beanName);
            return (hr == null) ? null : hr.bmd;
        }
        return home.getBeanMetaData(beanName);
    }

    /**
     * Returns the J2EEName bytes to include in the serialized BeanId. <p>
     * 
     * If the BeanId is associated with a versioned module, then the
     * returned bytes will be the unversioned J2EEName. <p>
     */
    private byte[] getJ2EENameBytes(BeanMetaData bmd, boolean isHome)
    {
        if (!isHome && bmd.ivUnversionedJ2eeName != null) {
            return bmd.ivUnversionedJ2eeName.getBytes();
        }
        return j2eeName.getBytes();
    }

    /**
     * Returns the bean type id for serialization.
     * 
     * @param bmd the non-null bean metadata
     * @param isHome true if for the home instance
     * @return the type id
     */
    private static byte getBeanType(BeanMetaData bmd, boolean isHome) // d621921
    {
        // 621921 - Always use BeanMetaData to decide which bean type id to use.

        if (isHome) // d621921
        {
            // Note: EJBFactory (EJBLink) does not support module versioning.
            if (bmd != null && bmd._moduleMetaData.isVersionedModule()) // F54184
            {
                return HOME_BEAN | MODULE_VERSION_CAPABLE;
            }
            return HOME_BEAN;
        }

        byte typeId = TYPE_TO_TYPE_ID_MAP[bmd.type];
        if (bmd.usesBeanManagedTx)
        {
            typeId |= USES_BEAN_MANAGED_TX;
        }
        if (bmd._moduleMetaData.isVersionedModule()) // F54184
        {
            typeId |= MODULE_VERSION_CAPABLE;
        }
        return typeId;
    }

    /**
     * Returns a BeanId from its serialized (byteArray) version. A more
     * performance friendly replacement for the serialization code used in
     * deserialize. <p>
     * 
     * Note: the passed ByteArray will become part of the state of the
     * returned BeanId, so should not be modified after calling this
     * method. <p>
     * 
     * @param byteArray contains byte array of serialized BeanId to be
     *            deserialized.
     * @param container EJB Container in which the home for this BeanId lives.
     * 
     * @return deserialized BeanId .
     * 
     * @exception IOException if a problem occurs deserializing the BeanID
     * @exception ClassNotFoundException if a class in the serialized
     *                BeanId cannot be located by the class loader.
     **/
    public static BeanId getBeanId(ByteArray byteArray, // d140003.12
                                   EJSContainer container)
                    throws IOException,
                    ClassNotFoundException
    {
        BeanId id = null;
        int pkeyIndex;

        Serializable pkey = null;
        byte[] j2eeNameBytes;
        boolean isHome = false;
        J2EEName j2eeName = null;
        byte[] bytes = byteArray.getBytes(); // d140003.12

        // Match up the header with the new format. If it does not match
        // then we have an incoming type 1 BeanId.
        for (int i = 0; i < HEADER_LEN; i++)
        {
            if (bytes[i] != header[i])
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Header mismatch, bytes do not represent a BeanId.");

                // Should be rare that we get an old BeanId.
                throw new IOException("Invalid BeanId header '" + new String(bytes) + "', bytes not a BeanId");
            }
        }

        // Make a determination on the type of the bean from the type byte
        byte typeId = bytes[HEADER_LEN];

        // Determine if the bean module is version capable                  F54184
        boolean isVersionCapable = ((typeId & MODULE_VERSION_CAPABLE) != 0);

        switch (typeId & ~MODULE_VERSION_CAPABLE)
        {
        // If it is a home bean, set the isHome flag. We then have
        // to read the J2EEName and the primary key
            case HOME_BEAN:
                isHome = true;
                // Fall through

                // Bean types that contain state must read the J2EEName and primary key
            case STATEFUL_BEAN:
            case STATEFUL_BEAN + USES_BEAN_MANAGED_TX:
            case ENTITY_BEAN:
                j2eeNameBytes = readJ2EENameBytes(bytes);
                j2eeName = container.getJ2EENameFactory().create(j2eeNameBytes);
                if (!isHome && isVersionCapable) {
                    j2eeName = EJSContainer.homeOfHomes.getVersionedJ2EEName(j2eeName);
                }
                pkeyIndex = HEADER_LEN +
                            BEAN_TYPE_LEN +
                            J2EE_NAME_LEN +
                            j2eeNameBytes.length;
                pkey = readPKey(bytes, pkeyIndex, j2eeName);
                break;

            // Simplest case : A stateless bean. Read the Java EE name
            // and we are done
            // MessageDriven are just like Stateless....                  d176974
            case SINGLETON_BEAN:
            case SINGLETON_BEAN + USES_BEAN_MANAGED_TX:
            case STATELESS_BEAN:
            case STATELESS_BEAN + USES_BEAN_MANAGED_TX:
            case MESSAGEDRIVEN_BEAN:
            case MESSAGEDRIVEN_BEAN + USES_BEAN_MANAGED_TX:
                j2eeNameBytes = readJ2EENameBytes(bytes);
                j2eeName = container.getJ2EENameFactory().create(j2eeNameBytes);
                if (isVersionCapable) {
                    j2eeName = EJSContainer.homeOfHomes.getVersionedJ2EEName(j2eeName);
                }
                // pkey = null;
                break;

            default:
                // Nothing can be done.... this is not a type 1 BeanId, so either
                // the stream is corrupt, or a new bean type has been added,
                // but not added to the list above.                           LI2281-3
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Unable to parse bean id: unsupported EJB type: " +
                                 bytes[HEADER_LEN]);
                throw new IOException("Unsupported EJB Type: " + bytes[HEADER_LEN]);
        }

        // Now, lookup the home based on the J2EEName read above.
        HomeInternal home = container.getHomeOfHomes().getHome(j2eeName);

        // If the home was not found, then the application has either not
        // been installed or started, or possibly failed to start.
        // Log a meaningful warning, and throw a meaningful exception.    LI2281-3
        if (home == null)
        {
            Tr.warning(tc, "HOME_NOT_FOUND_CNTR0092W", j2eeName.toString());
            throw new EJBNotFoundException(j2eeName.toString());
        }

        // If we are dealing with a stateful bean, then the pkey has to
        // be converted back into it's CSI implementation before it can
        // be used.
        if (home.isStatefulSessionHome())
        {
            pkey = EJSContainer.sessionKeyFactory.create((byte[]) pkey);
        }

        // For homes, convert the byte array to a J2EEName object
        if (isHome)
        {
            if (home != EJSContainer.homeOfHomes && // d621921
                home != EJSContainer.homeOfHomes.ivEJBFactoryHome) // d639148
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "invalid home bean type id for " + j2eeName, byteArray);
                throw new IOException("Invalid home bean type id");
            }

            pkey = container.getJ2EENameFactory().create((byte[]) pkey);
            if (isVersionCapable) {
                pkey = EJSContainer.homeOfHomes.getVersionedJ2EEName((J2EEName) pkey);
            }
        }
        else
        {
            if (getBeanType(home.getBeanMetaData(j2eeName), false) != typeId) // d621921
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "invalid bean type id for " + j2eeName, byteArray);
                throw new IOException("Invalid bean type id");
            }
        }

        // Create the BeanId from the deserialized pieces.... note that
        // this includes calculating the hash value.
        id = new BeanId(home, pkey, isHome);

        // Set the byteArray used to create this BeanId in the BeanId
        // itself, so it won't have to be created later.  Note that the
        // caller must not modify the passed byteArray after this.  d140003.12
        id.byteArray = byteArray;

        return id;
    }

    private static byte[] readJ2EENameBytes(byte[] bytes)
                    throws CSIException
    {
        int j2eeNameLength = 0;
        int j2eeNameIndex = HEADER_LEN + BEAN_TYPE_LEN;

        // LIDB2775-23.0 Begins
        if (EJSPlatformHelper.isZOS())
        {
            j2eeNameLength = ((bytes[j2eeNameIndex + 0] & 0xff) << 24) |
                             ((bytes[j2eeNameIndex + 1] & 0xff) << 16) |
                             ((bytes[j2eeNameIndex + 2] & 0xff) << 8) |
                             ((bytes[j2eeNameIndex + 3] & 0xff));
        }
        else
        {
            j2eeNameLength = ((bytes[j2eeNameIndex + 0] & 0xff)) |
                             ((bytes[j2eeNameIndex + 1] & 0xff) << 8) |
                             ((bytes[j2eeNameIndex + 2] & 0xff) << 16) |
                             ((bytes[j2eeNameIndex + 3] & 0xff) << 24);
        }
        // LIDB2775-23.0 Ends

        j2eeNameIndex += J2EE_NAME_LEN;

        if (j2eeNameIndex + j2eeNameLength > bytes.length) // d716926
        {
            String message = "Invalid J2EEName length: " + j2eeNameLength +
                             " + " + j2eeNameIndex + " > " + bytes.length;

            if (j2eeNameLength >= (1 << 24))
            {
                int j2eeNameLengthReversed = Integer.reverseBytes(j2eeNameLength);
                if (j2eeNameIndex + j2eeNameLengthReversed < (1 << 24))
                {
                    message += " (length " + j2eeNameLengthReversed +
                               " written in a different byte order?)";
                }
            }

            throw new CSIException(message);
        }

        byte[] temp = new byte[j2eeNameLength];
        System.arraycopy(bytes, j2eeNameIndex, temp, 0, j2eeNameLength);

        return temp;
    }

    private static Serializable readPKey(byte[] bytes, int pkeyIndex,
                                         J2EEName j2eeName)
                    throws IOException,
                    ClassNotFoundException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "readPKey", new Object[] { Integer.valueOf(pkeyIndex), j2eeName });

        Serializable pkey = null;
        int pkeyLen = (bytes.length - pkeyIndex) - 1;

        int index = pkeyIndex;
        pkeyIndex++;

        byte type = bytes[index++];

        switch (type)
        {
            case SERIALIZED_PKEY:
                pkey = readSerializedPKey(bytes, pkeyIndex, pkeyLen, j2eeName); // d173022.12
                break;

            case INTEGER_PKEY:
                int ivalue = 0;
                for (int i = 3; i >= 0; i--)
                {
                    ivalue ^= bytes[index + i] & 0xff;
                    if (i > 0)
                        ivalue = ivalue << 8;
                }
                pkey = Integer.valueOf(ivalue);
                break;

            case LONG_PKEY:
                long lvalue = 0;
                for (int i = 7; i >= 0; i--)
                {
                    lvalue ^= ((long) bytes[index + i]) & 0xff;
                    if (i > 0)
                        lvalue = lvalue << 8;
                }
                pkey = Long.valueOf(lvalue);
                break;

            case STRING_PKEY:
                pkey = new String(bytes, pkeyIndex, pkeyLen); // d173022.12
                break;

            case BYTE_ARRAY_PKEY:
                byte[] b = new byte[pkeyLen];
                System.arraycopy(bytes, pkeyIndex,
                                 b, 0, pkeyLen);
                pkey = b;
                break;

            default:
                break;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "readPKey", pkey);
        return pkey;
    }

    private static Serializable readSerializedPKey(byte[] bytes, int start,
                                                   int len, // d173022.12
                                                   J2EEName j2eeName)
                    throws IOException,
                    ClassNotFoundException
    {
        ByteArrayInputStream bais =
                        new ByteArrayInputStream(bytes, start, len); // d173022.12
        ClassLoader loader = EJSContainer.getClassLoader(j2eeName);
        EJBRuntime ejbRuntime = EJSContainer.getDefaultContainer().getEJBRuntime();
        final ObjectInputStream objIstream =
                        ejbRuntime.createObjectInputStream(bais, loader); // RTC71814

        Serializable pkey = null;
        try
        {
            pkey = (Serializable) AccessController.doPrivileged(
                            new PrivilegedExceptionAction<Object>() {
                                public Object run() throws ClassNotFoundException, IOException {
                                    return objIstream.readObject();
                                }
                            }
                            );
        } catch (PrivilegedActionException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".readSerializedPKey", "863");
            Exception e2 = e.getException();
            if (e2 instanceof ClassNotFoundException)
                throw (ClassNotFoundException) e2;
            else if (e2 instanceof IOException)
                throw (IOException) e2;
        }

        return pkey;
    }

    /**
     * Returns a byte array containing the bytes associated with the
     * Java EE Name for the specified servant key.
     * 
     * @exception CSIException thrown if unable to determine the Java EE name
     *                bytes for the specified key.
     */
    // d145400.1
    protected static byte[] getJ2EENameBytes(byte[] bytes)
                    throws CSIException
    {
        // ****Important Note******:  Much of this code was copied to com.ibm.ejs.oa.EJBOAKeyImpl.java
        //                            in ecutils and modified slightly.  Changes to this code may also
        //                            require corresponding changes to that code.
        //

        boolean isHome = false;
        byte[] j2eeNameBytes = null;

        // Match up the header with the new format. If it does not match
        // then we have an incoming type 1 BeanId.
        for (int i = 0; i < HEADER_LEN; i++)
        {
            if (bytes[i] != header[i])
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Header mismatch, attempting to deserialize BeanId");

                // Should be rare that we get an old beanid
                throw new CSIException("Parser Error: header mismatch");
            }
        }

        // Make sure this is a valid bean type, and if so, then read the
        // Java EE Name bytes from the key bytes.
        switch (bytes[HEADER_LEN])
        {
            case HOME_BEAN:
                isHome = true; // LI2281-3
            case STATEFUL_BEAN:
            case STATEFUL_BEAN + USES_BEAN_MANAGED_TX:
            case ENTITY_BEAN:
            case SINGLETON_BEAN:
            case SINGLETON_BEAN + USES_BEAN_MANAGED_TX:
            case STATELESS_BEAN:
            case STATELESS_BEAN + USES_BEAN_MANAGED_TX:
            case MESSAGEDRIVEN_BEAN: // d176974
            case MESSAGEDRIVEN_BEAN + USES_BEAN_MANAGED_TX: // d176974
                j2eeNameBytes = readJ2EENameBytes(bytes);
                break;

            default:
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Unable to parse bean id: unsupported EJB type: " +
                                 bytes[HEADER_LEN]);
                throw new CSIException("Unsupported EJB Type: " + bytes[HEADER_LEN]);
        }

        // For Home beans, the 'J2EEName' is for HomeOfHomes, and the
        // primary key is the real J2EEName...                            LI2281-3
        if (isHome)
        {
            try
            {
                int pkeyIndex = HEADER_LEN +
                                BEAN_TYPE_LEN +
                                J2EE_NAME_LEN +
                                j2eeNameBytes.length;
                j2eeNameBytes = (byte[]) readPKey(bytes, pkeyIndex, null);
            } catch (Throwable th)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Unable to parse bean id: home j2eeName: " + th);
                throw new CSIException("Unable to read j2eeName bytes", th);
            }
        }

        return j2eeNameBytes;
    }

    /**
     * Returns the primary key associated with the specified servant key. <p>
     * 
     * Null will be returned for homes and bean types that do not contain a
     * primary key, such as Stateless Session beans. <p>
     * 
     * @param bytes servant key (or serialized BeanId).
     * @return primary key associated with specified servant key bytes.
     * @exception CSIException thrown if unable to determine the primary key
     *                for the specified key.
     */
    // LI2281-3
    protected static Serializable getPrimaryKey(byte[] bytes)
                    throws CSIException
    {
        int pkeyIndex;
        boolean isStateful = false;
        Serializable pkey = null;
        byte[] j2eeNameBytes = null;
        J2EEName j2eeName = null;

        // Match up the header with the new format. If it does not match
        // then we have an incoming type 1 BeanId.
        for (int i = 0; i < HEADER_LEN; i++)
        {
            if (bytes[i] != header[i])
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Header mismatch, attempting to deserialize BeanId");

                // Should be rare that we get an old beanid
                throw new CSIException("Parser Error: header mismatch");
            }
        }

        // Make sure this is a valid bean type, and if so, if there is a
        // primary key, then read the Java EE Name bytes from the key bytes,
        // followed by the primary key.
        switch (bytes[HEADER_LEN])
        {
            case HOME_BEAN:
            case SINGLETON_BEAN:
            case SINGLETON_BEAN + USES_BEAN_MANAGED_TX:
            case STATELESS_BEAN:
            case STATELESS_BEAN + USES_BEAN_MANAGED_TX:
            case MESSAGEDRIVEN_BEAN:
            case MESSAGEDRIVEN_BEAN + USES_BEAN_MANAGED_TX:
                // No primary key associated with these types
                break;

            case STATEFUL_BEAN:
            case STATEFUL_BEAN + USES_BEAN_MANAGED_TX:
                isStateful = true;
            case ENTITY_BEAN:
                j2eeNameBytes = readJ2EENameBytes(bytes);
                j2eeName = EJSContainer.getDefaultContainer().getJ2EENameFactory().create(j2eeNameBytes);
                pkeyIndex = HEADER_LEN +
                            BEAN_TYPE_LEN +
                            J2EE_NAME_LEN +
                            j2eeNameBytes.length;
                try
                {
                    pkey = readPKey(bytes, pkeyIndex, j2eeName);
                } catch (Throwable th)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Unable to parse bean id: primary key: " + th);
                    throw new CSIException("Unable to read primary key", th);
                }
                break;

            default:
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Unable to parse bean id: unsupported EJB type: " +
                                 bytes[HEADER_LEN]);
                throw new CSIException("Unsupported EJB Type: " + bytes[HEADER_LEN]);
        }

        // If we are dealing with a stateful bean, then the pkey has to
        // be converted back into it's CSI implementation before it can
        // be used.
        if (isStateful)
        {
            pkey = EJSContainer.sessionKeyFactory.create((byte[]) pkey);
        }

        return pkey;
    }

    //-------------------------------------------
    //
    // Methods from the SecurityBeanId interface
    //
    //-------------------------------------------

    /**
     * Get the method name for the given method id.
     */
    public final String getMethodName(int id)
    {
        return home.getMethodName(pkey, id, _isHome);
    } // getMethodName

    /**
     * Return true iff this bean id refers to a home bean. <p>
     */
    public final boolean isHome()
    {
        return _isHome;
    } // isHome

    /**
     * Return class name of class implementing this bean.
     */
    public final String getEnterpriseBeanClassName()
    {
        return home.getEnterpriseBeanClassName(pkey);
    } // getEnterpriseBeanClassName

    //89554
    protected static int computeHashValue(J2EEName j2eeName, Serializable pkey,
                                          boolean isHome)
    {
        return j2eeName.hashCode() +
               ((pkey == null) ? 0 : pkey.hashCode()) +
               ((isHome) ? 1 : 0);
    }

    /**
     * Returns boolean true if this BeanId is for a home object
     * or a BMP or CMP entity bean.
     */
    //d121510 - added entire method.
    public final boolean useLSD() //LIDB859-4
    {
        if (_isHome)
        {
            return true;
        }
        else
        {
            BeanMetaData bmd = home.getBeanMetaData(j2eeName);
            if (bmd.type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY || // 126512
                bmd.type == InternalConstants.TYPE_SINGLETON_SESSION || // F743-508
                bmd.type == InternalConstants.TYPE_STATELESS_SESSION || // PQ81101
                bmd.type == InternalConstants.TYPE_BEAN_MANAGED_ENTITY || // 126512
                bmd.ivSFSBFailover) // d209849
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    // d173022.12 Begins
    /**
     * Return the next available ByteArrayOutputStream object from the pool.
     */
    private final ByteArrayOutputStream getByteArrayStream()
    {
        ByteArrayOutputStream rtnBaos = null;

        // Get a ByteArrayOutputStream from the pool.  Note that this must be
        // synchronized as multiple threads may be accessing this pool, and the
        // pool may return null if empty.                                  d175235
        synchronized (svBAOSs)
        {
            if (svBAOSsSize > 0)
            {
                --svBAOSsSize;
                rtnBaos = svBAOSs[svBAOSsSize];
                svBAOSs[svBAOSsSize] = null;
            }
        }

        // If the pool was empty, then new up a new one (outside the synchronized
        // block for performance), otherwise reset the one form the pool.  d175235
        if (rtnBaos == null)
        {
            rtnBaos = new ByteArrayOutputStream();
        }
        else
        {
            // clear the byte array and reset back to zero.
            rtnBaos.reset();
        }

        return rtnBaos;
    }

    /**
     * Return the input ByteArrayOutputStream back to the pool.
     */
    private final void freeByteArrayStream(ByteArrayOutputStream baos)
    {
        // If the pool is not full, then add to the pool. Note that this must
        // be synchronized, as multiple threads may access the pool.       d175235
        synchronized (svBAOSs)
        {
            if (svBAOSsSize < svBAOSs.length)
            {
                svBAOSs[svBAOSsSize] = baos;
                ++svBAOSsSize;
            }
        }
    }

    /**
    */
    private final void writePKeyBytes(ByteArrayOutputStream baos, BeanMetaData bmd)
                    throws IOException
    {
        if (pkey != null)
        {
            if (_isHome)
            {
                baos.write(BYTE_ARRAY_PKEY);
                if (bmd != null && bmd.ivUnversionedJ2eeName != null) {
                    baos.write(bmd.ivUnversionedJ2eeName.getBytes());
                } else {
                    baos.write(((J2EEName) pkey).getBytes());
                }
            }
            else if (home.isStatefulSessionHome())
            {
                // The only types of pkeys known to us are the long values
                // used by StatefulSessionKeys in our implementation and the
                // the byte arrays used by CICS/390
                baos.write(BYTE_ARRAY_PKEY);
                baos.write(((StatefulSessionKey) pkey).getBytes());
            }
            else
            {
                if (pkey.getClass().getName().startsWith("java.lang"))
                {
                    if (pkey instanceof Integer)
                    {
                        baos.write(INTEGER_PKEY);
                        int value = ((Integer) pkey).intValue();
                        for (int i = 0; i < 4; i++)
                            baos.write(value >> (i * 8));
                    }
                    else if (pkey instanceof Long)
                    {
                        baos.write(LONG_PKEY);
                        long value = ((Long) pkey).longValue();
                        for (int i = 0; i < 8; i++)
                            baos.write((byte) (value >> (i * 8)));
                    }
                    else if (pkey instanceof String)
                    {
                        baos.write(STRING_PKEY);
                        baos.write(((String) pkey).getBytes());
                    }
                    else
                    {
                        writeSerializedPKey(baos);
                    }
                }
                else
                {
                    writeSerializedPKey(baos);
                }
            }
        }
    }

    /**
    */
    private final void writeSerializedPKey(ByteArrayOutputStream baos)
                    throws IOException
    {
        baos.write(SERIALIZED_PKEY);
        ObjectOutputStream objOstream = new ObjectOutputStream(baos);

        objOstream.writeObject(pkey);
        objOstream.flush();
    }
    // d173022.12 Ends
} // BeanId
