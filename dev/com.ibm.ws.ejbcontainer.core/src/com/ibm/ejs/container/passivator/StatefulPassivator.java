/*******************************************************************************
 * Copyright (c) 1998, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.passivator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.StatefulBeanO;
import com.ibm.ejs.container.passivator.EJBObjectInfo.FieldInfo;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.SessionBeanStore;
import com.ibm.websphere.csi.StreamUnavailableException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.managedobject.ManagedObjectContext;

/**
 * <code>StatefulPassivator</code> is responsible for activating and
 * passivating stateful session beans.<p>
 */
public abstract class StatefulPassivator
{
    private final static TraceComponent tc =
                    Tr.register(StatefulPassivator.class, "EJBContainer", "com.ibm.ejs.container.container");

    private static final String CLASS_NAME = "com.ibm.ejs.container.passivator.StatefulPassivator";

    private final SessionBeanStore ivBeanStore;
    protected final EJSContainer ivContainer;
    private boolean ivTerminating = false;
    //PK69093 ******* WARNING ***********
    //When adding instance variables to this class, ensure that those instance variable are thread safe.  The
    //'de facto rule' of this class is that at most only three files are open at one time - one for activate to
    //read a bean's state, one for passivate to write a bean's state, and one for remove to remove a previously
    //persisted bean's state.  The next three variables will be used as synchronization locks to enforce the
    //previously stated rule (i.e. to ensure only one thread can do a passivate, activate, or remove at any given time).
    private final Object ivActivateLock; //PK69093
    private final Object ivPassivateLock; //PK69093
    private final Object ivRemoveLock; //PK69093

    private final SfFailoverCache ivStatefulFailoverCache; //LIDB2018-1

    /**
     * Create new passivator instance using the given bean store. <p>
     */
    public StatefulPassivator(SessionBeanStore beanStore,
                              EJSContainer container,
                              SfFailoverCache failoverCache) //LIDB2018-1
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) //LIDB2018-1
            Tr.entry(tc, "StatefulPassivator");

        ivBeanStore = beanStore;
        ivContainer = container;
        ivStatefulFailoverCache = failoverCache; //LIDB2018-1
        this.ivActivateLock = new Object(); //PK69093
        this.ivPassivateLock = new Object(); //PK69093
        this.ivRemoveLock = new Object(); //PK69093

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "StatefulPassivator");
    }

    /**
     * Passivates the object to the bean store. <p>
     *
     * @param obj BeanO that corresponds to the bean to passivate <p>
     */
    public void passivate(StatefulBeanO beanO, BeanMetaData bmd) // d648122
    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) //130050
            Tr.entry(tc, "passivate: " + beanO);

        if (beanO.isRemoved() || isTerminating())
        {
            if (isTraceOn && tc.isEventEnabled()) //130050
                Tr.event(tc, "Bean removed!");
            return;
        }

        BeanId bid = beanO.getId();
        Object sb = beanO.ivEjbInstance; // d367572.7
        boolean exceptionCaught = false; //155114
        ObjectOutputStream beanStream = null; //155114

        // LI2775-107.2 Begins WS18354.02a,MD19305C
        Object credToken = ivContainer.getEJBRuntime().pushServerIdentity();
        // LI2775-107.2 Ends

        ObjectOutputStream beanStream2 = null; // d430549.11

        // Get the JPAExPcBindingContext for this SFSB.
        Object exPC = beanO.getJPAExPcBindingContext(); //d468174

        try
        {
            // LIDB2018-1 begins
            // Check whether SFSB failover is enabled. Note, we could
            // factor out this code so it is the same regardless of
            // whether SFSB failover is enabled or not.  However, to
            // ensure no impact to existing performance when not enabled,
            // we chose to not factor into a common implementation.
            ByteArrayOutputStream baos = null;
            long lastAccessTime = beanO.getLastAccessTime();

            //PK69093 - Only allow one stream to be open for writing at one time.
            synchronized (ivPassivateLock)
            {
                if (beanO.sfsbFailoverEnabled())
                {
                    // For failover, initially write to a ByteArrayOutputStream. This will later be converted to
                    // bytes and then persisted

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "failover is enabled");

                    // If the EJB module is 3.0 or greater, use the new format, which is later
                    if (getEJBModuleVersion(beanO) >= BeanMetaData.J2EE_EJB_VERSION_3_0)
                    {
                        // Set up the bean stream to initially write to a ByteArrayOutputStream
                        baos = new ByteArrayOutputStream(1024);
                        beanStream = createPassivationOutputStream(new GZIPOutputStream(baos));
                    }
                    else
                    {
                        // pre-3.0 module - Use old format since we may be in a mixed cluster environment
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "processing EJB 2.1 module or prior");

                        // Serialize and compress the data in SFSB.
                        byte[] bytes = getCompressedBytes(sb, lastAccessTime, exPC);

                        // Get a file outstream that does not compress the data
                        // since we already have the data compressed.
                        OutputStream ostream = ivBeanStore.getOutputStream(bid);
                        beanStream = createPassivationOutputStream(ostream);

                        // Write length of compressed data.
                        beanStream.writeInt(bytes.length);
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "length of compressed bytes is: " + bytes.length);

                        // Write compressed data to file.
                        beanStream.write(bytes);
                        beanStream.close();

                        // Replicate compressed data to failover servers by calling
                        // the method that updates the failover cache entry for this SFSB.
                        beanO.updateFailoverEntry(bytes, lastAccessTime);

                        return;
                    }
                }
                else // failover not enabled - setup the beanStream
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "failover is NOT enabled");
                    beanStream = createPassivationOutputStream(ivBeanStore.getGZIPOutputStream(bid));
                }

                // Passivate sb in the 3.0 format
                EJBObjectInfo objectInfo = null;
                Map<String, Map<String, Field>> passivatorFields = getPassivatorFields(bmd); // d648122

                if (sb instanceof Serializable)
                {
                    objectInfo = createSerializableObjectInfo(sb);
                }
                else
                {
                    objectInfo = createNonSerializableObjectInfo(sb, passivatorFields); // d648122
                }

                // Write last access time.
                beanStream.writeLong(lastAccessTime);

                // Write the persistence context
                beanStream.writeObject(exPC);

                // Write the SFSB state.
                beanStream.writeObject(objectInfo);

                // Write the managed object state of the EJB instance
                writeManagedObjectContext(beanStream, beanO.ivEjbManagedObjectContext);

                Object[] interceptors = beanO.ivInterceptors;
                if (interceptors == null)
                {
                    beanStream.writeInt(-1);
                }
                else
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Processing " + interceptors.length + " interceptors");

                    beanStream.writeInt(interceptors.length);
                    for (int i = 0; i < interceptors.length; i++)
                    {
                        Object interceptor = interceptors[i];

                        EJBObjectInfo interceptorObjectInfo = null;
                        if (interceptor instanceof Serializable)
                        {
                            interceptorObjectInfo = createSerializableObjectInfo(interceptor);
                        }
                        else
                        {
                            interceptorObjectInfo = createNonSerializableObjectInfo(interceptor, passivatorFields); // d648122
                        }

                        beanStream.writeObject(interceptorObjectInfo);
                    }
                }

                beanStream.close();

                // If failover is enabled, now convert to a byte array and persist it
                if (beanO.sfsbFailoverEnabled() && baos != null) //d468174
                {
                    byte[] bytes = baos.toByteArray();
                    beanStream2 = createPassivationOutputStream(ivBeanStore.getOutputStream(bid));
                    beanStream2.writeInt(bytes.length);
                    beanStream2.write(bytes);
                    beanStream2.close();

                    // Replicate compressed data to failover cache servers by calling
                    // the method that updates the failover cache entry for this SFSB.
                    beanO.updateFailoverEntry(bytes, lastAccessTime);
                }
                // LIDB2018-1 ends
            }
        } catch (CSIException ex)
        {
            exceptionCaught = true; //155114
            FFDCFilter.processException(ex, CLASS_NAME + ".passivate", "113", this);
            throw new RemoteException("passivation failed", ex);
        } catch (Throwable e) //155114
        {
            // d584932 - Catch Throwable, not Exception, to ensure that we
            // set exceptionCaught when Errors are thrown.
            exceptionCaught = true; //155114
            FFDCFilter.processException(e, CLASS_NAME + ".passivate", "107", this);
            Tr.warning(tc, "CANNOT_PASSIVATE_STATEFUL_BEAN_CNTR0001W",
                       new Object[] { beanO.toString(), this, e }); //p111002.3
            throw new RemoteException("passivation failed", e);
        } finally //155114
        {
            // LI2775-107.2 Begins WS18354.02a,MD19305C
            if (credToken != null) // d646413.2
            {
                ivContainer.getEJBRuntime().popServerIdentity(credToken);
            }
            // LI2775-107.2 Ends

            if (exceptionCaught) // 155114
            {
                // attempt to close and remove the beanStore
                try
                {
                    if (beanStream != null)
                    {
                        beanStream.close();
                        // PK69093 - beanStore.remove will access a file, as such lets synch this call
                        // such that only one file can be open for remove at a time.
                        synchronized (ivRemoveLock)
                        {
                            ivBeanStore.remove(bid);
                        }
                    }

                    if (beanStream2 != null) // d430549.11
                    {
                        beanStream2.close();
                    }
                } catch (Exception ex)
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "exception closing stream", ex);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) //130050
            Tr.exit(tc, "passivate");
    }

    /**
     * Activates the object from an external file. <p>
     *
     * @param obj BeanO that corresponds to the bean to activate<p>
     *
     * @exception RemoteException <p>
     */
    public void activate(StatefulBeanO beanO, BeanMetaData bmd) // d648122
    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) //130050
            Tr.entry(tc, "activate: " + beanO);

        Object sb = null; // d367572.7
        ManagedObjectContext ejbState = null;
        BeanId bid = beanO.getId();
        ClassLoader classLoader = bmd.classLoader; // d648122

        // LI2775-107.2 Begins WS18354.02a,MD19305C
        Object credToken = ivContainer.getEJBRuntime().pushServerIdentity();

        // LI2775-107.2 Ends

        ObjectInputStream beanStream = null;
        GZIPInputStream istream = null;
        try
        {
            //PK69093 - Only allow one stream to be open for reading at one time
            synchronized (ivActivateLock)
            {
                // LIDB2018-1 begins
                // Check whether SFSB failover is enabled.
                if (!beanO.sfsbFailoverEnabled())
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Failover is NOT enabled");

                    istream = ivBeanStore.getGZIPInputStream(bid);
                    beanStream = createActivationInputStream(istream, beanO, classLoader);
                }
                else
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Failover is enabled");

                    // SFSB failover is enabled.  Determine whether to
                    // get compressed data from passivation file or
                    // from failover cache.
                    byte[] data = null;
                    if (ivStatefulFailoverCache.beanExists(bid))
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Retrieving data from failover cache");

                        // Exists in failover cache, so activate using data in failover cache.
                        boolean isSticky = ivStatefulFailoverCache.inStickyUOW(bid); // d209857
                        data = ivStatefulFailoverCache.getAndRemoveData(bid, beanO.ivSfFailoverClient); // d209857 //d229518
                        if (isSticky)
                        {
                            // SFSB failover occured while in middle of a sticky BMT.
                            // This is not supported, so throw an exception.
                            throw new NoSuchObjectException("Bean Managed Transaction is active, SFSB failover not supported");
                        }

                        if (data == null)
                        {
                            // For some reason failover cache entry exists, but replication
                            // of SFSB to this server was unsuccessful.
                            throw new NoSuchObjectException("SFSB replication failed.");
                        }
                    }
                    else
                    {
                        //SFSB not in failover cache. so get data from passivation file.
                        InputStream fis = ivBeanStore.getInputStream(bid);
                        ObjectInputStream beanStream2 = createActivationInputStream(fis, beanO, classLoader);
                        int n = beanStream2.readInt();
                        data = new byte[n];

                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "length of compressed bytes is: " + n);

                        int bytesRead = 0;
                        for (int offset = 0; offset < n; offset += bytesRead)
                        {
                            bytesRead = beanStream2.read(data, offset, n - offset);
                            if (isTraceOn && tc.isDebugEnabled())
                            {
                                Tr.debug(tc, "bytes read from input stream is: " + bytesRead);
                            }

                            if (bytesRead == -1)
                            {
                                throw new IOException("end of input stream while reading compressed bytes");
                            }
                        }

                        beanStream2.close();
                    }

                    // create an input stream for decompressing the SFSB data.
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    istream = new GZIPInputStream(bais);
                    beanStream = createActivationInputStream(istream, beanO, classLoader);
                }

                // If failover was enabled and the EJB module is pre-3.0, the old format is used
                boolean oldFormat = beanO.sfsbFailoverEnabled() && (getEJBModuleVersion(beanO) < BeanMetaData.J2EE_EJB_VERSION_3_0);

                //  d204278.2 begin
                // First read in last access time from input stream
                // and set it in the StatefulBeanO.
                long lastAccessTime = beanStream.readLong();
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "read from data last access time: " + lastAccessTime);
                beanO.setLastAccessTime(lastAccessTime);
                // d204278.2 end

                // Read the JPAExPcBindingContext.
                // TODO: ejbteam - Since we are very late in the EJB3 FP cycle I will not
                // make this change now, but we really should only be passivating and
                // activating this value when this EJB has ContainerManaged Extended
                // Persistence Context.    When this change is made we will need to
                // change this method, but also the passivate method and StatefulBeanO.remove().
                // It can also be removed from the logic for activating and passivating
                // 2.0 and 2.1 SFSB's since they will not have an extendedPC
                // An incomplete change will result in NPE's in certain circumstances.  //d477342
                Object exPC = beanStream.readObject();
                beanO.setJPAExPcBindingContext(exPC);

                Map<String, Map<String, Field>> passivatorFields = null;
                if (oldFormat)
                {
                    // Now read SFSB data from the input stream and close
                    // the input stream.
                    sb = beanStream.readObject(); // d367572.7
                    ejbState = null;
                }
                else
                {
                    EJBObjectInfo sbObjectInfo = (EJBObjectInfo) beanStream.readObject();
                    passivatorFields = getPassivatorFields(bmd); // d648122

                    if (sbObjectInfo.isSerializable())
                    {
                        sb = sbObjectInfo.getSerializableObject();
                    }
                    else
                    {
                        sb = activateObjectFromInfo(sbObjectInfo, bmd.enterpriseBeanClass, passivatorFields); // d648122
                    }

                    ejbState = readManagedObjectContext(beanStream, bmd, sb); // F87720

                    int expectedNumInterceptors = 0;
                    if (bmd.ivInterceptorMetaData != null && bmd.ivInterceptorMetaData.ivInterceptorClasses != null)
                    {
                        expectedNumInterceptors = bmd.ivInterceptorMetaData.ivInterceptorClasses.length;
                    }

                    // Handle interceptors
                    int n = beanStream.readInt();
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "number of interceptors is " + n);

                    if (n == -1)
                    {
                        n = 0;
                    }

                    if (n != expectedNumInterceptors) // RTC97224
                    {
                        throw new RemoteException("interceptor count " + n +
                                                  " in serialization data does not match actual count " + expectedNumInterceptors);
                    }

                    if (n != 0)
                    {
                        Class<?>[] interceptorClasses = bmd.ivInterceptorMetaData.ivInterceptorClasses;
                        Object[] interceptors = new Object[n];

                        for (int i = 0; i < n; i++)
                        {
                            EJBObjectInfo interceptorObjectInfo = (EJBObjectInfo) beanStream.readObject();
                            if (interceptorObjectInfo.isSerializable())
                            {
                                interceptors[i] = interceptorObjectInfo.getSerializableObject();
                            }
                            else
                            {
                                interceptors[i] = activateObjectFromInfo(interceptorObjectInfo, interceptorClasses[i], passivatorFields); // d648122
                            }

                        }
                        beanO.setInterceptors(interceptors);
                    }

                    beanO.setLastAccessTime(lastAccessTime);
                }

                beanStream.close();
            }

            beanO.setEnterpriseBean(sb, ejbState);

            // PK69093 - beanStore.remove will access a file, as such lets synch this call
            // such that only one file can be open for remove at a time.
            synchronized (ivRemoveLock)
            {
                // Now delete the file used to passivate the bean.
                ivBeanStore.remove(bid); //d248470
            }

            // LIDB2018-1 ends
        } catch (StreamUnavailableException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".activate", "146", this);
            throw new NoSuchObjectException("");
        } catch (CSIException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".activate", "149", this);
            throw new RemoteException("activation failed", ex);
        } catch (ClassNotFoundException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".activate", "152", this);
            Tr.warning(tc, "CANNOT_ACTIVATE_STATEFUL_BEAN_CNTR0003W",
                       new Object[] { beanO.toString(), this, e }); //p111002.3
            throw new RemoteException("", e);
        } catch (RemoteException rex)
        {
            // RemoteException (including NoSuchObjectExcetpion) is a subclass
            // of IOException... but it should not get wrapped inside another
            // RemoteException, which IOException catch block will do.   d350987
            FFDCFilter.processException(rex, CLASS_NAME + ".activate", "576", this);
            Tr.warning(tc, "CANNOT_ACTIVATE_STATEFUL_BEAN_CNTR0003W",
                       new Object[] { beanO.toString(), this, rex });
            throw rex;
        } catch (IOException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".activate", "157", this);
            Tr.warning(tc, "CANNOT_ACTIVATE_STATEFUL_BEAN_CNTR0003W",
                       new Object[] { beanO.toString(), this, e }); //p111002.3
            throw new RemoteException("", e);
            // LI2775-107.2 Begins WS18354.02a,MD19305C
        } finally
        {
            if (credToken != null) // d646413.2
            {
                ivContainer.getEJBRuntime().popServerIdentity(credToken);
            }
            // LI2775-107.2 Begins WS18354.02a,MD19305C
        }

        if (isTraceOn && tc.isEntryEnabled()) //130050
            Tr.exit(tc, "activate");
    }

    /**
     * Version of remove which can be used with only a beanId passed in
     * as input.
     *
     * @param beanId of the SFSB to be removed.
     *
     * @param removeFromFailoverCache indicates whether or not to remove from
     *            failover cache if found in failover cache. Note, the intent is
     *            only a timeout or SFSB.remove() call should cause failover cache
     *            entry to be removed. Shutdown of server should not cause failover
     *            cache entry to be removed. Thus, the caller of this method uses
     *            this parameter to indicate whether or not to remove SFSB from
     *            local failover cache.
     *
     * @throws RemoteException if a failure occurs removing the bean.
     */
    public void remove(BeanId beanId, boolean removeFromFailoverCache)
                    throws RemoteException //LIDB2018-1
    {
        // LIDB2018-1 begins
        if (ivStatefulFailoverCache == null || !ivStatefulFailoverCache.beanExists(beanId))
        {
            //PK69093 - beanStore.remove will access a file, as such lets synch this call
            //such that only one file can be open for remove at a time.
            synchronized (ivRemoveLock)
            {
                // failover cache does not exist (e.g. SFSB failover is not enabled)
                // or SFSB is not in failover cache.  Therefore, SFSB must be in
                // the passivation file for the bean. So remove it from bean store.
                ivBeanStore.remove(beanId);
            }
        }
        else
        {
            // SFSB is in the failover cache, so remove it from failover Cache.
            if (removeFromFailoverCache)
            {
                ivStatefulFailoverCache.removeCacheEntry(beanId);
            }
        }
        // LIDB2018-1 ends
    }

    /**
     * Not the best of solutions, but an effective one. If the container is
     * terminating, then it informs the passivator. If the passivator is
     * informed of termination, then it stops writing files to the
     * file bean store.
     */
    public synchronized void terminate()
    {
        ivTerminating = true;
    }

    //PK69093 - added
    /**
     * Used to determine if this class has ben notified that the server is shutting down (terminating).
     */
    private synchronized boolean isTerminating()
    {
        return ivTerminating;
    }

    /**
     * Creates an object output stream for passivation.
     */
    protected abstract ObjectOutputStream createPassivationOutputStream(OutputStream os)
                    throws IOException;

    /**
     * Creates an object input stream for activation.
     */
    protected abstract ObjectInputStream createActivationInputStream(InputStream is,
                                                                     StatefulBeanO beanO,
                                                                     ClassLoader classLoader)
                    throws IOException;

    /**
     * Writes the state for the bean.
     */
    // F87720
    protected abstract void writeManagedObjectContext(ObjectOutputStream oos,
                                                      ManagedObjectContext state)
                    throws IOException;

    /**
     * Reads the state for the bean.
     */
    // F87720
    protected abstract ManagedObjectContext readManagedObjectContext(ObjectInputStream ois,
                                                                     BeanMetaData bmd,
                                                                     Object instance)
                    throws IOException,
                    ClassNotFoundException;

    /**
     * ZIPs the state data in a SFSB and returns as a byte array.
     *
     * @param sb is the SFSB instance to zip.
     * @param lasAccessTime
     * @param exPC is the Persistence Context for this SFSB.
     *
     * @exception IOException
     *
     */
    //LIDB2018-1 added entire method.
    private byte[] getCompressedBytes(Object sb,
                                      long lastAccessTime,
                                      Object exPC) throws IOException // d367572.7
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getCompressedBytes", sb);

        // Serialize SessionBean to a byte[].
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        GZIPOutputStream gout = new GZIPOutputStream(baos);
        ObjectOutputStream beanStream2 = createPassivationOutputStream(gout);

        if (isTraceOn && tc.isDebugEnabled()) //d204278.2
            Tr.debug(tc, "writing failover data with last access time set to: " + lastAccessTime);

        // First write the last access time.
        beanStream2.writeLong(lastAccessTime); //d204278.2

        // Write the persistence context
        beanStream2.writeObject(exPC);

        // Now the SFSB data.
        beanStream2.writeObject(sb);
        gout.finish();
        gout.close();
        beanStream2.close();
        byte[] bytes = baos.toByteArray();
        baos.close();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getCompressedBytes");

        return bytes;
    }

    /**
     * Collect the list of declared fields in the specified class and its super
     * classes and store the accessible fields in <tt>allFields</tt>. This
     * method does no work if the specified class is serializable. Static and
     * transient fields are ignored.
     *
     * @param klass the class hierarchy to inspect
     * @param allFields the map in which to store the results, if any
     */
    private static void collectPassivatorFields(Class<?> klass, Map<String, Map<String, Field>> allFields) // d648122
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "collectPassivatorFields: " + klass.getName());

        if (Serializable.class.isAssignableFrom(klass))
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "collectPassivatorFields: serializable");
            return;
        }

        for (Class<?> classIter = klass; classIter != Object.class; classIter = classIter.getSuperclass())
        {
            String className = classIter.getName();
            Map<String, Field> classPassivatorFields = allFields.get(className);

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, classIter + ", cached = " + (classPassivatorFields != null));

            if (classPassivatorFields == null)
            {
                // Use a LinkedHashMap for fast iteration in
                // createNonSerializableObjectInfo.
                classPassivatorFields = new LinkedHashMap<String, Field>();
                allFields.put(className, classPassivatorFields);

                Field[] fields = classIter.getDeclaredFields();
                AccessibleObject.setAccessible(fields, true);

                for (Field field : fields)
                {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "ignoring " + field);
                    }
                    else
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "adding " + field);
                        classPassivatorFields.put(field.getName(), field);
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "collectPassivatorFields");
    }

    /**
     * Get the passivator fields for the specified bean.
     *
     * @param bmd the bean metadata
     * @return the passivator fields
     */
    public Map<String, Map<String, Field>> getPassivatorFields(final BeanMetaData bmd) // d648122
    {
        // Volatile data race.  We do not care if multiple threads do the work
        // since they will all get the same result.
        Map<String, Map<String, Field>> result = bmd.ivPassivatorFields;
        if (result == null)
        {
            result = AccessController.doPrivileged(new PrivilegedAction<Map<String, Map<String, Field>>>()
            {
                @Override
                public Map<String, Map<String, Field>> run()
                {
                    Map<String, Map<String, Field>> allFields = new HashMap<String, Map<String, Field>>();

                    collectPassivatorFields(bmd.enterpriseBeanClass, allFields);
                    if (bmd.ivInterceptorMetaData != null)
                    {
                        for (Class<?> klass : bmd.ivInterceptorMetaData.ivInterceptorClasses)
                        {
                            collectPassivatorFields(klass, allFields);
                        }
                    }

                    return allFields;
                }
            });

            bmd.ivPassivatorFields = result;
        }

        return result;
    }

    /**
     * Creates an EJBSerializableObjectInfo object for a non-serializable object. This is either
     * a stateful bean or an interceptor. Field info from this object for fields that need to
     * be passivated are added, so the object can be reinstantiated and fields set during activation.
     *
     * @param obj the non-serializable object that needs to have its fields serialized
     * @param passivatorFields the passivator fields for the bean
     *
     * @return the EJBSerializableObjectInfo that contains the serializable objects
     *
     * @throws RemoteException
     */
    // d430549.10
    private EJBObjectInfo createNonSerializableObjectInfo(Object obj, Map<String, Map<String, Field>> passivatorFields)
                    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createNonSerializableObjectInfo", obj);

        // d460047, d648122 - re-work
        EJBObjectInfo ejbObjectInfo = new EJBObjectInfo();
        ejbObjectInfo.setSerializable(false);
        Class<?> clazz = obj.getClass();
        ejbObjectInfo.setClassName(clazz.getName());

        for (Class<?> classIter = clazz; classIter != Object.class; classIter = classIter.getSuperclass())
        {
            String className = classIter.getName();
            Map<String, Field> classPassivatorFields = passivatorFields.get(className);
            List<FieldInfo> fieldInfoList = new ArrayList<FieldInfo>(classPassivatorFields.size());

            for (Field field : classPassivatorFields.values())
            {
                FieldInfo fieldInfo = generateFieldInfo(obj, field);
                fieldInfoList.add(fieldInfo);
            }

            ejbObjectInfo.addFieldInfo(className, fieldInfoList);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createNonSerializableObjectInfo", ejbObjectInfo);

        return ejbObjectInfo;
    }

    /**
     * Creates an EJBSerializableObjectInfo object for a serializable object. This is either
     * a stateful bean or an interceptor.
     *
     * @param parentObj the object that is serializable.
     *
     * @return the EJBSerializableObjectInfo that contains this object
     */
    // d430549.10
    private EJBObjectInfo createSerializableObjectInfo(Object parentObj)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createSerializableObjectInfo", parentObj);

        EJBObjectInfo objectInfo = new EJBObjectInfo();
        objectInfo.setSerializable(true);
        objectInfo.setSerializableObject(parentObj);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createSerializableObjectInfo", objectInfo);
        return objectInfo;
    }

    /**
     * Generates FieldInfo for a particular field in the object
     *
     * @param obj the object that contains the field
     *
     * @param field the Field object for which info is to be generated
     *
     * @return the FieldInfo object containing the field info
     *
     * @throws RemoteException
     */
    // d430549.10
    private FieldInfo generateFieldInfo(Object obj, Field field)
                    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "generateFieldInfo", field);

        FieldInfo fieldInfo = new FieldInfo();
        String fieldName = field.getName();
        Object fieldObj = null;
        try
        {
            fieldObj = field.get(obj); // Get the actual object for this field
        } catch (IllegalArgumentException e)
        {
            // This shouldn't happen
            FFDCFilter.processException(e, CLASS_NAME + ".generateFieldInfo", "851", this);
            throw new RemoteException("passivation failed", e);
        } catch (IllegalAccessException e)
        {
            // This shouldn't happen since setAccessible(true) was called for this field in
            // collectPassivatorFields
            FFDCFilter.processException(e, CLASS_NAME + ".generateFieldInfo", "857", this);
            throw new RemoteException("passivation failed", e);
        }

        fieldInfo.name = fieldName;
        fieldInfo.value = fieldObj;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "generateFieldInfo", fieldInfo);

        return fieldInfo;
    }

    /**
     * Activate a non-seriallizable object based on information in an EJBObjectInfo
     * object. The object may be a stateful bean or an interceptor.
     *
     * @param ejbObjectInfo the object containing the object information
     * @param objClass the expected object class
     * @param passivatorFields the passivator fields for the bean
     *
     * @return the activated object
     *
     * @throws RemoteException
     */
    // d430549.10
    private Object activateObjectFromInfo(EJBObjectInfo ejbObjectInfo,
                                          Class<?> objClass,
                                          Map<String, Map<String, Field>> passivatorFields)
                    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "activateObjectFromInfo", new Object[] { ejbObjectInfo, objClass });

        // d460047, d648122 - re-work section
        Object obj = null;
        try
        {
            if (!objClass.getName().equals(ejbObjectInfo.getClassName())) // RTC97224
            {
                throw new RemoteException("serialization data for class " + ejbObjectInfo.getClassName() +
                                          " does not match actual class name " + objClass.getName());
            }

            obj = objClass.newInstance();

            Map<String, List<FieldInfo>> fieldInfoMap = ejbObjectInfo.getFieldInfoMap();
            for (Class<?> classIter = objClass; classIter != Object.class; classIter = classIter.getSuperclass())
            {
                String className = classIter.getName();
                Map<String, Field> classPassivatorFields = passivatorFields.get(className);

                List<FieldInfo> fieldInfoList = fieldInfoMap.get(className);
                for (FieldInfo fieldInfo : fieldInfoList)
                {
                    Field field = classPassivatorFields.get(fieldInfo.name);
                    field.set(obj, fieldInfo.value);
                }
            }
        } catch (Throwable e) // d430549.11
        {
            FFDCFilter.processException(e, CLASS_NAME + ".activateObjectFromInfo", "843", this);
            throw new RemoteException("activation failed", e);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "activateObjectFromInfo", obj);
        return obj;
    }

    /**
     * Retrieve the version of the EJB module
     *
     * @param beanO
     * @return EJB module version
     */
    // d430549.10
    private int getEJBModuleVersion(StatefulBeanO beanO)
    {
        BeanId beanId = beanO.getId();
        BeanMetaData beanMetaData = beanId.getBeanMetaData();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "EJB module version is " + beanMetaData.ivModuleVersion);
        return beanMetaData.ivModuleVersion;
    }
}
