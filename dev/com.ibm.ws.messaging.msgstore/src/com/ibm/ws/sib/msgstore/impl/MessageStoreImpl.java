package com.ibm.ws.sib.msgstore.impl;

/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;

import javax.transaction.xa.Xid;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.AlarmManager;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.objectManager.NonExistentLogFileException;
import com.ibm.ws.sib.admin.JsEObject;
import com.ibm.ws.sib.admin.JsHealthMonitor;
import com.ibm.ws.sib.admin.JsHealthState;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.JsRecoveryMessagingEngine;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.SIBFileStore;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.CacheStatistics;
import com.ibm.ws.sib.msgstore.Configuration;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.Membership;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.MessageStoreRuntimeException;
import com.ibm.ws.sib.msgstore.MessageStoreUnavailableException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.msgstore.WASConfiguration;
import com.ibm.ws.sib.msgstore.XidInvalidException;
import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.cache.links.RootMembership;
import com.ibm.ws.sib.msgstore.cache.ref.ItemStorageManager;
import com.ibm.ws.sib.msgstore.deliverydelay.DeliveryDelayManager;
import com.ibm.ws.sib.msgstore.expiry.CacheLoader;
import com.ibm.ws.sib.msgstore.expiry.Expirer;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.PersistenceFactory;
import com.ibm.ws.sib.msgstore.persistence.PersistentMessageStore;
import com.ibm.ws.sib.msgstore.persistence.UniqueKeyGenerator;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.msgstore.transactions.impl.MSTransactionFactory;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistenceManager;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.XidManager;
import com.ibm.ws.sib.processor.ItemInterface;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.transactions.TransactionFactory;
import com.ibm.ws.sib.utils.Runtime;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This class represents the collection of stored objects.
 * <p>Instances of this class will store instances of classes implementing
 * the Item interface. </p>
 * <p>Stored Items will be persisted as required. Each Item is associated with
 * a link which will remain in memory even if the item is spilled to save memory.
 * Access to the item should always be through the link. Such access should
 * ensure single threaded access to the Item state.</p>
 * <p>Items which have been spilled will automatically be unspilled when requested
 * via their stub.</p>
 * <p>Each item is given its own unique (for this object store) ID number (a long)
 * which can be used to identify it.</p>
 * <p>Each MessageStoreImpl has an associated persistance manager which acts as an
 * interface to the particular backing storage in use.</p>
 * 
 */
public final class MessageStoreImpl extends MessageStore implements MessageStoreConstants, XmlConstants
{

    private static TraceNLS nls = TraceNLS.getTraceNLS(MessageStoreConstants.MSG_BUNDLE);

    private static final TraceComponent tc = SibTr.register(MessageStoreImpl.class,
                                                            MessageStoreConstants.MSG_GROUP,
                                                            MessageStoreConstants.MSG_BUNDLE);

    private static final String XML_CANNOT_BE_WRITTEN = "XML cannot be written as the message store has not started";
    private CacheLoader _cacheLoader = null;
    private long _cacheLoaderInterval = -1;
    private Configuration _configuration = null;
    private final Hashtable _customProperties = new Hashtable();
    private Expirer _expirer = null;
    private DeliveryDelayManager _deliveryDelayManager = null;
    private final long _expiryInterval = -1;
    private final long _deliveryDelayScanInterval = -1;
    private int _spillUpperLimit;
    private int _spillLowerLimit;
    // Defect 484799
    private int _spillUpperSizeLimit;
    private int _spillLowerSizeLimit;
    private JsHealthMonitor _healthMonitor;

    private JsHealthState _healthState;
    private ItemStorageManager _itemStorageManager;
    private final XidManager _manager;

    // Defect 413861
    private MessageStoreState _state = MessageStoreState.STATE_UNINITIALIZED;

    // Defect 560281.1
    // Use an inner class specific to this class for locking.
    private final static class StartLock {}

    private static final StartLock _startLock = new StartLock();

    private Map _membershipMap = null;

    private JsMessagingEngine _messagingEngine = null;

    private PersistentMessageStore _persistentMessageStore = null;
    private RootMembership _rootMembership;
    private UniqueKeyGenerator _tickCountGenerator;

    private TransactionFactory _transactionFactory;

    private UniqueKeyGenerator _uniqueIdentifierGenerator;
    private UniqueKeyGenerator _uniqueLockIDGenerator;

    /** PK57207 */
    private boolean _jdbcSpillSizeMsgRefsByMsgSize;

    // Defect 572575
    private boolean _datastoreLockCanBeDisabled;

    // Defect 465809

    //F1332-52014
    private boolean _restrictLongDBLock = false;
    //F1332-52015
    private long _lastDBLockedTimestamp = 0L;

    public static final AtomicServiceReference<AlarmManager> alarmManagerRef = new AtomicServiceReference<AlarmManager>(
                    "alarmManager");

    /**
     * ItemInterface service
     */
    public static final AtomicServiceReference<ItemInterface> itemInterfaceRef = new AtomicServiceReference<ItemInterface>(
                    "itemInterface");

    protected void activate(ComponentContext context) {
        alarmManagerRef.activate(context);
        itemInterfaceRef.activate(context);
    }

    protected void deactivate(ComponentContext context) {
        alarmManagerRef.deactivate(context);
        itemInterfaceRef.deactivate(context);
    }

    /**
     * Declarative Services method for setting the deferrable scheduled executor
     * service reference.
     * 
     * @param ref
     *            reference to the service
     */
    protected void setAlarmManager(ServiceReference<AlarmManager> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "setAlarmManager", ref);

        alarmManagerRef.setReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "setAlarmManager");
    }

    /**
     * Declarative Services method for unsetting the non-deferrable scheduled
     * executor service reference.
     * 
     * @param ref
     *            reference to the service
     */
    protected void unsetAlarmManager(ServiceReference<AlarmManager> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "unsetAlarmManager", ref);

        alarmManagerRef.unsetReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "unsetAlarmManager");
    }

    /**
     * Declarative Services method for setting the ItemInterface service
     * reference.
     * 
     * @param ref
     *            reference to the service
     */
    protected void setItemInterface(ServiceReference<ItemInterface> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "setItemInterface", ref);
        itemInterfaceRef.setReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "setItemInterface");
    }

    /**
     * Declarative Services method for unsetting the ItemInterface service
     * reference.
     * 
     * @param ref
     *            reference to the service
     */
    protected void unsetItemInterface(ServiceReference<ItemInterface> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "unsetItemInterface", ref);
        itemInterfaceRef.setReference(ref);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "unsetItemInterface");

    }

    /**
     * Static method for getting the ItemStream instance from the runtime
     * .
     * 
     * @param itemClassName
     *            Class name for which the instance needs to be returned
     */
    public static AbstractItem getItemStreamInstance(String itemClassName) {
        AbstractItem itemStream = itemInterfaceRef.getService().getItemStreamInstance(itemClassName);;
        return itemStream;
    }

    /**
     * Null constructor is required for dynamic instantiation.
     */
    public MessageStoreImpl()
    {
        super();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");

        // This is the Xid Manager for this
        // MessageStore instance.
        _manager = new XidManager(this);
        _expirer = new Expirer(this);
        _deliveryDelayManager = new DeliveryDelayManager(this);
        _cacheLoader = new CacheLoader(this);

        _healthState = JsHealthState.getOK();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Message store state is : " + _state);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#findById(long)
     * TODO: we can optimize this method out later when we use
     * stream level hashtables.
     */
    public final AbstractItem _findById(long itemID) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_findById", Long.valueOf(itemID));

        AbstractItem item = null;
        AbstractItemLink itemLink = getLink(itemID);
        if (null != itemLink)
        {
            item = itemLink.getItem();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_findById", item);
        return item;
    }

    /**
     * Return a reference to the expirer.
     * 
     * @return an expirer reference.
     */
    public final Expirer _getExpirer()
    {
        return _expirer;
    }

    public final DeliveryDelayManager _getDeliveryDelayManager() {
        return _deliveryDelayManager;
    }

    /**
     * Return a reference to the Membership object for the AbstractItem.
     * A sneaky trick for testing purposes only.
     * 
     * @return a Membership reference.
     */
    @Override
    public final Membership _getMembership(AbstractItem ai)
    {
        return super._getMembership(ai);
    }

    /**
     * Return a reference to the JsMessagingEngine object for the message store.
     * 
     * @return a JsMessagingEngine reference.
     */
    public final JsMessagingEngine _getMessagingEngine()
    {
        return _messagingEngine;
    }

    /**
     * Return a reference to the RootMembership object for the message store.
     * For testing purposes only.
     * 
     * @return a RootMembership reference.
     */
    public final RootMembership _getRootMembership()
    {
        return _rootMembership;
    }

    //723935
    /**
     * Return a reference to the MessageStoreState object
     * 
     * @return an MessageStoreState reference
     */
    public MessageStoreState _getState()
    {
        return _state;
    }

    /*
     * Used to call the (package-private) {@link AbstractItem#setMembership()}
     * method from inside the implementation package. A sneaky trick.
     * 
     * @param membership
     * 
     * @param item
     */
    @Override
    public final void _setMembership(Membership membership, AbstractItem item)
    {
        super._setMembership(membership, item);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#xmlWriteRawOn(com.ibm.ws.sib.msgstore.FormattedWriter, boolean)
     */
    private final void _xmlWriteRawOn(FormattedWriter writer, boolean callBackToItem) throws IOException
    {
        new RawDataDumper(_persistentMessageStore, writer, callBackToItem).dump();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.MessageStore#put(com.ibm.ws.sib.store.AbstractItem, com.ibm.ws.sib.msgstore.Transaction)
     */
    @Override
    public void add(ItemStream itemStream, long lockID, Transaction transaction) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "add", new Object[] { itemStream, transaction });

        if (_rootMembership != null)
        {
            _rootMembership.addItemStream(itemStream, lockID, transaction);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "add");
        }
        else
        {
            MessageStoreUnavailableException msue;
            if (!_startupExceptions.isEmpty())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore failed to start!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore failed to start!", _startupExceptions.get(0));
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "add");
            throw msue;
        }
    }

    /**
     * Notification that the configuration of the bus has changed.
     * 
     * @param newBus The new bus configuration
     * @param busChg true if the bus has changed
     * @param destChg true if a destination has changed
     * @param medChg true if a mediation has changed
     */
    @Override
    public void busReloaded(Object newBus, boolean busChanged,
                            boolean destChg, boolean medChg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "busReloaded");
            SibTr.exit(this, tc, "busReloaded");
        }
    }

    /**
     * Commit the given transaction.
     * Given a string representing an xid, create a PersistentTranId
     * from it and then request the XidManager to commit it.
     * Part of MBean interface for resolving in-doubt transactions.
     * 
     * @param xid a string representing an xid.
     */
    @Override
    public void commitPreparedTransaction(String xid) throws TransactionException, PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "commitPreparedTransaction", xid);

        if (_manager != null)
        {
            try
            {
                PersistentTranId pid = new PersistentTranId(xid);
                _manager.commit(pid, false);
            } catch (ArrayIndexOutOfBoundsException e)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Invalid XID string", xid);
                throw new XidInvalidException(nls.getString("INVALID_XID_STRING_SIMS1010"));
            } catch (StringIndexOutOfBoundsException e)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Invalid XID string", xid);
                throw new XidInvalidException(nls.getString("INVALID_XID_STRING_SIMS1010"));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "commitPreparedTransaction");
        return;
    }

    @Override
    public final void destroy()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "destroy");

        synchronized (_startLock)
        {
            if (_state != MessageStoreState.STATE_STOPPED)
            {
                throw new IllegalStateException(nls.getFormattedMessage("INVALID_MSGSTORE_STATE_SIMS0505", new Object[] { _state }, null));
            }

            _configuration = null;

            _state = MessageStoreState.STATE_UNINITIALIZED;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Message store state is : " + _state);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "destroy");
    }

    // Defect 572575
    /**
     * This method allows the datastore exclusive access lock to
     * be disabled for a period of time. While disabled in this
     * manner the ME will continue to function as normal and will
     * not stop work from being carried out unlike in the case
     * where the lock is lost due to unknown reasons.
     * 
     * @param period The length of time in milliseconds that the
     *            datastore lock will be disabled.
     */
    @Override
    public final void disableDataStoreLock(Long period)
    {
        // As a double safety check to ensure that the lock does not 
        // get disabled unless it is intended we require a property to 
        // be set on the ME to enable toggling of the lock.
        if (_datastoreLockCanBeDisabled)
        {
            //lohith liberty change
            /*
             * // Only call if we are running with the Datastore
             * // implementation.
             * if (_persistentMessageStore != null &&
             * _persistentMessageStore instanceof com.ibm.ws.sib.msgstore.persistence.impl.PersistentMessageStoreImpl)
             * {
             * ((com.ibm.ws.sib.msgstore.persistence.impl.PersistentMessageStoreImpl)_persistentMessageStore).disableDataStoreLock(period.longValue());
             * }
             */
        }
        else
        {
            SibTr.info(tc, "DATASTORE_LOCK_CANNOT_BE_DISABLED_CWSIS1596");
        }
    }

    /**
     * Dump message store dynamic diagnostic information.
     * 
     * @param fw the FormattedWriter pass in by the ME
     */
    public void dump(FormattedWriter fw)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dump");

        try
        {
            xmlRequestWriteOnFile(fw);
        } catch (IOException e)
        {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "Exception caught writing Mmessage store dump!", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "dump");
    }

    /**
     * Dump message store.
     * This method is called from the Messaging Engine MBean.
     * 
     * @param fw The FormattedWriter passed in by the ME.
     * @param arg the dump specification if any. This may be
     *            null to invoke the internal diagnostics dump
     *            "raw" to invoke the raw (persistence) dump.
     *            "all" to invoke the internal & raw dump, including item information
     */
    @Override
    public void dump(FormattedWriter fw, String arg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dump", arg);

        if (arg != null)
        {
            if (arg.equalsIgnoreCase("raw"))
            {
                dumpRaw(fw);
            }
            else if (arg.equalsIgnoreCase("all"))
            {
                dumpAll(fw);
            }
            else
            {
                dump(fw);
            }
        }
        else
        {
            dump(fw);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "dump");
    }

    /**
     * Dump the message store persistence diagnostic information.
     * 
     * @param fw The FormattedWriter passed in by the ME
     */
    public void dumpRaw(FormattedWriter fw)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dumpRaw");

        try
        {
            xmlRequestWriteRawDataOnFile(fw);
        } catch (IOException e)
        {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "Exception caught writing Mmessage store dump!", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "dumpRaw");
    }

    /**
     * Dump message store dynamic diagnostic information and the
     * the message store persistence diagnostic information, including
     * information about the Items themselves.
     * 
     * @param fw The FormattedWriter passed in by the ME
     */
    public void dumpAll(FormattedWriter fw)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dumpAll");

        try
        {
            xmlRequestWriteOnFile(fw);
            xmlWriteRawOn(fw, true);
            fw.flush();
        } catch (IOException e)
        {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "Exception caught writing Mmessage store dump!", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "dumpAll");
    }

    /**
     * Notification that the configuration of the engine has changed.
     * 
     * @param engine The messaging engine whose configuration has been reloaded
     */
    @Override
    public void engineReloaded(Object messagingEngine)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "engineReloaded");

        // Defect 337421.1
        // Our config has been reloaded so we need to essentially
        // re-initialize so that the next time that we run start()
        // we pick up any changes.
        initialize((JsMessagingEngine) messagingEngine, true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "engineReloaded");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#expirerStart()
     */
    @Override
    public final void expirerStart() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "expirerStart");

        _expirer.start(_expiryInterval, _messagingEngine);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "expirerStart");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#expirerStop()
     */
    @Override
    public final void expirerStop()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "expirerStop");

        _expirer.stop();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "expirerStop");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#deliveryDelayManagerStart()
     */
    @Override
    public void deliveryDelayManagerStart() throws SevereMessageStoreException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deliveryDelayManagerStart");
        _deliveryDelayManager.start(_deliveryDelayScanInterval, _messagingEngine);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deliveryDelayManagerStart");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#deliveryDelayManagerStop()
     */
    @Override
    public void deliveryDelayManagerStop() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deliveryDelayManagerStop");
        _deliveryDelayManager.stop();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deliveryDelayManagerStop");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#findById(long)
     */
    @Override
    public AbstractItem findById(long itemID) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findById", Long.valueOf(itemID));

        AbstractItem item = _findById(itemID);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "findById", item);
        return item;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStoreInterface#findByIdInRoot(long)
     */
    @Override
    public ItemStream findByStreamId(long itemStreamId) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findByIdInRoot", Long.valueOf(itemStreamId));

        ItemStream item = (ItemStream) _findById(itemStreamId);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "findByIdInRoot", item);
        return item;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.MessageStore#nonDestructiveGet(com.ibm.ws.sib.store.Filter)
     */
    @Override
    public ItemStream findFirstMatching(Filter filter) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findFirstMatching", filter);

        if (_rootMembership != null)
        {
            ItemStream item = _rootMembership.findFirstMatchingItemStream(filter);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "findFirstMatching", item);
            return item;
        }
        else
        {
            MessageStoreUnavailableException msue;
            if (!_startupExceptions.isEmpty())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore failed to start!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore failed to start!", _startupExceptions.get(0));
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "findFirstMatching");
            throw msue;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#getCacheStatistics()
     */
    public final CacheStatistics getCacheStatistics()
    {
        return _itemStorageManager;
    }

    public Configuration getConfig()
    {
        return _configuration;
    }

    /**
     * Returns the current size of the expiry index.
     * 
     * @return the size of the expiry index.
     */
    @Override
    public final int getExpiryIndexSize()
    {
        return _expirer.size();
    }

    /*
     * JsMonitoredComponent Implementation //
     */
    @Override
    public final JsHealthState getHealthState()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getHealthState");
            SibTr.exit(this, tc, "getHealthState", "return=" + _healthState);
        }
        return _healthState;
    }

    public final AbstractItemLink getLink(long itemID)
    {
        return _membershipMap.get(itemID);
    }

    public final ItemStorageManager getManagedCache()
    {
        return _itemStorageManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.MessageStore#getMembership(long)
     */
    @Override
    public Membership getMembership(long id)
    {
        return _membershipMap.get(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#getNonStoredCacheStatistics()
     */
    @Override
    public CacheStatistics getNonStoredCacheStatistics()
    {
        return _itemStorageManager.getNonStoredCacheStatistics();
    }

    public final PersistentMessageStore getPersistentMessageStore()
    {
        return (this._persistentMessageStore);
    }

    /**
     * Obtain a list of XIDs which are in-doubt.
     * Part of MBean interface for resolving in-doubt transactions.
     * 
     * @return the XIDs as an array of strings
     */
    @Override
    public String[] listPreparedTransactions()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPreparedTransactions");

        String[] col = null;

        if (_manager != null)
        {
            // Obtain array of in-doubt xids from the XidManager
            Xid[] xids = null;
            xids = _manager.listRemoteInDoubts();
            if (xids != null)
            {
                // Get the string representation of each xid
                // and add to the collection
                col = new String[xids.length];
                for (int i = 0; i < xids.length; i++)
                {
                    col[i] = ((PersistentTranId) xids[i]).toString();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "xid " + col[i] + " in-doubt");
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPreparedTransactions");
        return col;
    }

    /**
     * Reply property value. Value returned will be from one of
     * these sources, in order of search:
     * <ul>
     * <li>Custom Properties</li>
     * <li>SIB Properties</li>
     * <li>System Properties</li>
     * <li>default supplied</li>
     * </ul>
     * 
     * @param key Name of property for which the value is required.
     *            The standard prefix of "sib.msgstore." will be appended by this
     *            method and should not form part of the key supplied.
     * @param defaultValue Default value to return if no value found.
     * @return Property value.
     */
    public final String getProperty(String key, String defaultValue)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProperty", new Object[] { "Key=" + key, "Default=" + defaultValue });

        String fullKey = MessageStoreConstants.STANDARD_PROPERTY_PREFIX + key;

        // consult custom properties first
        String value = (String) _customProperties.get(fullKey);
        if (null == value)
        {
            value = RuntimeInfo.getProperty(fullKey);
        }
        if (null == value)
        {
            // If still nothing then go to system proprties
            value = System.getProperty(fullKey);
        }
        if (null == value)
        {
            // if still nothing, then use the supplied default.
            value = defaultValue;
        }
        else
        {
            // If the value used differs from the default, issue a confirmation
            // message for serviceability reasons
            if (!value.equals(defaultValue))
            {
                Runtime.changedPropertyValue(fullKey, value);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProperty", "return=" + value);
        return value;
    }

    public int getSpillLowerLimit()
    {
        return _spillLowerLimit;
    }

    public int getSpillUpperLimit()
    {
        return _spillUpperLimit;
    }

    // Defect 484799
    public int getSpillLowerSizeLimit()
    {
        return _spillLowerSizeLimit;
    }

    // Defect 484799
    public int getSpillUpperSizeLimit()
    {
        return _spillUpperSizeLimit;
    }

    // PK57207 - return tuning parm for jdbcSpillSizeMsgRefsByMsgSize
    public boolean getJdbcSpillSizeMsgRefsByMsgSize()
    {
        return _jdbcSpillSizeMsgRefsByMsgSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#getStoredCacheStatistics()
     */
    @Override
    public CacheStatistics getStoredCacheStatistics()
    {
        return _itemStorageManager.getStoredCacheStatistics();
    }

    //188494
    @Override
    public TransactionFactory getTransactionFactory()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getTransactionFactory");
            SibTr.exit(this, tc, "getTransactionFactory", "return=" + _transactionFactory);
        }
        return _transactionFactory;
    }

    @Override
    public final long getUniqueLockID(int storageStrategy) throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getUniqueLockID");

        if (_uniqueIdentifierGenerator != null && _uniqueLockIDGenerator != null)
        {
            long key = AbstractItem.NO_ID;
            if (AbstractItem.STORE_NEVER == storageStrategy)
            {
                // Non-persistent unique key required
                key = _uniqueIdentifierGenerator.getPerInstanceUniqueValue();
            }
            else
            {
                key = _uniqueLockIDGenerator.getUniqueValue();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getUniqueLockID", Long.valueOf(key));
            return key;
        }
        else
        {
            MessageStoreUnavailableException msue;
            if (!_startupExceptions.isEmpty())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore failed to start!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore failed to start!", _startupExceptions.get(0));
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getUniqueLockID");
            throw msue;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#getUniqueTickCount()
     */
    @Override
    public final long getUniqueTickCount() throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getUniqueTickCount");

        if (_tickCountGenerator != null)
        {
            long key = AbstractItem.NO_ID;

            key = _tickCountGenerator.getUniqueValue();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getUniqueTickCount", Long.valueOf(key));
            return key;
        }
        else
        {
            MessageStoreUnavailableException msue;
            if (!_startupExceptions.isEmpty())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore failed to start!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore failed to start!", _startupExceptions.get(0));
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getUniqueTickCount");
            throw msue;
        }
    }

    public final long getUniqueValue(int storageStrategy) throws PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getUniqueValue", Integer.valueOf(storageStrategy));

        if (_uniqueIdentifierGenerator != null)
        {
            long key = AbstractItem.NO_ID;
            if (AbstractItem.STORE_NEVER == storageStrategy)
            {
                // non-persistent unique key required
                key = _uniqueIdentifierGenerator.getPerInstanceUniqueValue();
            }
            else
            {
                // persistent unique key required
                key = _uniqueIdentifierGenerator.getUniqueValue();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getUniqueValue", Long.valueOf(key));
            return key;
        }
        else
        {
            MessageStoreUnavailableException msue;
            if (!_startupExceptions.isEmpty())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore failed to start!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore failed to start!", _startupExceptions.get(0));
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getUniqueValue");
            throw msue;
        }
    }

    //188494
    public XidManager getXidManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getXidManager");
            SibTr.exit(this, tc, "getXidManager", "return=" + _manager);
        }
        return _manager;
    }

    @Override
    public final void initialize(final Configuration config)
    {
        initialize(config, false);
    }

    private final void initialize(final Configuration config, boolean isReload)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "initialize", new Object[] { "isReload=" + isReload, "Config=" + config });

        synchronized (_startLock)
        {
            // Defect 337421.1
            // If we are reloading then we will be in stopped state
            // once stop has been called 
            if (!isReload)
            {
                if (_state != MessageStoreState.STATE_UNINITIALIZED)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "initialize");
                    throw new IllegalStateException(nls.getFormattedMessage("INVALID_MSGSTORE_STATE_SIMS0505", new Object[] { _state }, null));
                }

                _state = MessageStoreState.STATE_STOPPED;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Message store state is : " + _state);
            }

            _configuration = config;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "initialize");
    }

    /**
     * Initalizes the message store using the given JetStream configuration. Copies the relevant
     * pieces of data into a MessageStoreConfiguration object which is then used to initialize
     * the message store. Initializes PMI.
     * 
     * @param engineConfiguration
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#initialize(com.ibm.ws.sib.admin.JsMessagingEngine)
     */
    public void initialize(Object engineConfiguration)
    {
        //Venu change Liberty: changed the parameters
        initialize((JsMessagingEngine) engineConfiguration, false);
    }

    @Override
    public void initialize(JsMessagingEngine engineConfiguration)
    {
        initialize(engineConfiguration, false);
    }

    /**
     * Initalizes the message store using the given JetStream configuration. Copies the relevant
     * pieces of data into a MessageStoreConfiguration object which is then used to initialize
     * the message store. Will only initialize PMI if this is not a reload.
     * 
     * @param engineConfiguration
     * @param isReload
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#initialize(com.ibm.ws.sib.admin.JsMessagingEngine)
     */
    private void initialize(JsMessagingEngine engineConfiguration, boolean isReload)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "initialize", new Object[] { "isReload=" + isReload, "Config=" + engineConfiguration });

        _messagingEngine = engineConfiguration;

        final WASConfiguration configuration = WASConfiguration.getDefaultWasConfiguration();

        // Always support warm-restart. D178260
        configuration.setCleanPersistenceOnStart(false);

        //Venu Liberty change: changed the way we get FileStore config object
        SIBFileStore fs = (SIBFileStore) _messagingEngine.getFilestore();

        //there is no getLogFile !!  lohith liberty change
        configuration.setObjectManagerLogDirectory(fs.getPath());
        configuration.setObjectManagerLogSize(fs.getLogFileSize());

        configuration.setObjectManagerPermanentStoreDirectory(fs.getPath());
        configuration.setObjectManagerMinimumPermanentStoreSize(fs.getMinPermanentFileStoreSize());
        configuration.setObjectManagerMaximumPermanentStoreSize(fs.getMaxPermanentFileStoreSize());
        configuration.setObjectManagerPermanentStoreSizeUnlimited(fs.isUnlimitedPermanentStoreSize());

        configuration.setObjectManagerTemporaryStoreDirectory(fs.getPath());
        configuration.setObjectManagerMinimumTemporaryStoreSize(fs.getMinTemporaryFileStoreSize());
        configuration.setObjectManagerMaximumTemporaryStoreSize(fs.getMaxTemporaryFileStoreSize());
        configuration.setObjectManagerTemporaryStoreSizeUnlimited(fs.isUnlimitedTemporaryStoreSize());
        /*
         * }
         */
        // Finally, make sure that the message store type in the engine configuration has
        // the corresponding configuration information. Otherwise, we're not going to get very far...
        if ((engineConfiguration.getMessageStoreType() == JsMessagingEngine.MESSAGE_STORE_TYPE_DATASTORE) &&
            (engineConfiguration.datastoreExists()))
        {
            configuration.setPersistentMessageStoreClassname(MessageStoreConstants.PERSISTENT_MESSAGE_STORE_CLASS_DATABASE);

            // Defect 449837
            if (!isReload)
            {
                SibTr.info(tc, "MESSAGING_ENGINE_PERSISTENCE_DATASTORE_SIMS1568", new Object[] { engineConfiguration.getName() });
            }
        }
        else if ((engineConfiguration.getMessageStoreType() == JsMessagingEngine.MESSAGE_STORE_TYPE_FILESTORE) &&
                 (engineConfiguration.filestoreExists()))
        {
            configuration.setPersistentMessageStoreClassname(MessageStoreConstants.PERSISTENT_MESSAGE_STORE_CLASS_OBJECTMANAGER);

            // Defect 449837
            if (!isReload)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Messaging engine " + engineConfiguration.getName() + " is using a file store.");
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "initialize");
            throw new IllegalStateException(nls.getString("MSGSTORE_CONFIGURATION_ERROR_SIMS0503"));
        }

        initialize(configuration, isReload);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "initialize");
    }

    /**
     * @return true if items are allowed to expire (eg expirer is running)
     *         or false if items cannot expire.
     */
    public final boolean itemsCanExpire()
    {
        boolean reply = false;
        if (_expirer != null)
        {
            reply = _expirer.isRunning();
        }
        return reply;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#newNonLockingCursor(com.ibm.ws.sib.msgstore.Filter)
     */
    @Override
    public final NonLockingCursor newNonLockingCursor(Filter filter) throws MessageStoreException
    {
        if (_rootMembership != null)
        {
            return _rootMembership.newNonLockingItemStreamCursor(filter);
        }
        else
        {
            MessageStoreUnavailableException msue;
            if (!_startupExceptions.isEmpty())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore failed to start!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore failed to start!", _startupExceptions.get(0));
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            }

            throw msue;
        }
    }

    public final void register(AbstractItemLink itemLink)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "register", itemLink);

        final long id = itemLink.getID();
        _membershipMap.put(id, itemLink);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "register");
    }

    /**
     * register new Item link.
     * 
     * @param newLink
     * @param item
     */
    public final void registerLink(final AbstractItemLink newLink, final AbstractItem item)
    {
        register(newLink);
        _setMembership(newLink, item);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.MessageStore#destructiveGet(com.ibm.ws.sib.store.Filter, com.ibm.ws.sib.msgstore.Transaction)
     */
    @Override
    public ItemStream removeFirstMatching(Filter filter, Transaction transaction) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeFirstMatching", new Object[] { filter, transaction });

        if (_rootMembership != null)
        {
            ItemStream item = _rootMembership.removeFirstMatchingItemStream(filter, (PersistentTransaction) transaction);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "removeFirstMatching", "return=" + item);
            return item;
        }
        else
        {
            MessageStoreUnavailableException msue;
            if (!_startupExceptions.isEmpty())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore failed to start!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore failed to start!", _startupExceptions.get(0));
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Operation not possible as MessageStore is unavailable!");
                msue = new MessageStoreUnavailableException("Operation not possible as MessageStore is unavailable!");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "removeFirstMatching");
            throw msue;
        }
    }

    /*
     * JsMonitoredComponent Implementation
     */
    @Override
    public final void reportGlobalError()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "reportGlobalError");

        // Set our health status to GLOBAL_ERROR
        // to stop any other servers attempting
        // to recover our session.
        if (_healthState.couldBeWorse())
        {
            _healthState = JsHealthState.getGlobalError();
        }

        if (_healthMonitor == null)
        {
            if (_messagingEngine != null && _messagingEngine instanceof JsHealthMonitor)
            {
                _healthMonitor = (JsHealthMonitor) _messagingEngine;
                _healthMonitor.reportGlobalError();
            }
        }
        else
        {
            _healthMonitor.reportGlobalError();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "reportGlobalError");
    }

    /*
     * JsMonitoredComponent Implementation
     */
    @Override
    public final void reportLocalError()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "reportLocalError");

        // Only need to set to LOCAL_ERROR
        // if we are currently OK. We don't want
        // to overwrite a GLOBAL_ERROR.
        if (_healthState.isOK())
        {
            _healthState = JsHealthState.getLocalError();
        }

        if (_healthMonitor == null)
        {
            if (_messagingEngine != null && _messagingEngine instanceof JsHealthMonitor)
            {
                _healthMonitor = (JsHealthMonitor) _messagingEngine;
                _healthMonitor.reportLocalError();
            }
        }
        else
        {
            _healthMonitor.reportLocalError();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "reportLocalError");
    }

    /**
     * Rollback the given transaction.
     * Given a string representing an xid, create a PersistentTranId
     * from it and then request the XidManager to roll it back.
     * Part of MBean interface for resolving in-doubt transactions.
     * 
     * @param xid a string representing the xid.
     */
    @Override
    public void rollbackPreparedTransaction(String xid) throws TransactionException, PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rollbackPreparedTransaction", xid);

        if (_manager != null)
        {
            try
            {
                PersistentTranId pid = new PersistentTranId(xid);
                _manager.rollback(pid);
            } catch (ArrayIndexOutOfBoundsException e)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Invalid XID string", xid);
                throw new XidInvalidException(nls.getString("INVALID_XID_STRING_SIMS1010"));
            } catch (StringIndexOutOfBoundsException e)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Invalid XID string", xid);
                throw new XidInvalidException(nls.getString("INVALID_XID_STRING_SIMS1010"));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "rollbackPreparedTransaction");
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#setConfig(JsEObject)
     */
    public void setConfig(JsEObject config) {}

    /**
     * @param interval The interval to be used when the CacheLoader is started
     */
    public void setCacheLoaderInterval(long interval)
    {
        _cacheLoaderInterval = interval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#setCustomProperty(java.lang.String, java.lang.String)
     */
    @Override
    public void setCustomProperty(String key, String value)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCustomProperty", new Object[] { "Key=" + key, "Value=" + value });

        _customProperties.put(key, value);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCustomProperty");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#start(int)
     */
    @Override
    public void start(int arg0) throws Exception
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "start");

        //Venu Liberty change
        //activating the Alarm manager (i.e this triggers Alarm manager's Activate).. 
        // Alaram manager has to be ready for Message Store Expirer
        alarmManagerRef.getService();

        //PM44028
        // Reset the health state: we'll assume the database is healthy unless/until some 
        // information suggests otherwise.
        _healthState = JsHealthState.getOK();

        synchronized (_startLock)
        {
            if (_state != MessageStoreState.STATE_STOPPED)
            {
                throw new IllegalStateException(nls.getFormattedMessage("INVALID_MSGSTORE_STATE_SIMS0505", new Object[] { _state }, null));
            }

            _state = MessageStoreState.STATE_STARTING;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Message store state is : " + _state);
        }

        // Defect 465809
        // reset the startup Exception list
        _startupExceptions.clear();

        // choose map type and size.
        String mapType = getProperty(PROP_ITEM_MAP_TYPE, PROP_ITEM_MAP_TYPE_DEFAULT);
        String strMapSize = getProperty(PROP_ITEM_MAP_SIZE, PROP_ITEM_MAP_SIZE_DEFAULT);
        int mapSize = -1;
        if (null != strMapSize)
        {
            mapSize = Integer.parseInt(strMapSize);
        }
        if (PROP_ITEM_MAP_TYPE_FASTMAP.equals(mapType))
        {
            String strMapParallelism = getProperty(PROP_ITEM_MAP_PARALLELISM, PROP_ITEM_MAP_PARALLELISM_DEFAULT);
            int mapParallelism = -1;
            if (null != strMapParallelism)
            {
                mapParallelism = Integer.parseInt(strMapParallelism);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "using itemLinkMap(" + mapSize + "/" + mapParallelism + ")");

            _membershipMap = new ItemLinkMap(mapSize, mapParallelism);
        }
        else
        {
            // default
            if (mapSize > 50 || mapSize < 5)
            {
                mapSize = 20;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "using multiMap(" + mapSize + ")");

            _membershipMap = new MultiHashMap(mapSize);
        }

        // PK57432
        String propValue = getProperty(PROP_JDBC_SPILL_SIZE_MSG_REFS_BY_MSG_SIZE,
                                       PROP_JDBC_SPILL_SIZE_MSG_REFS_BY_MSG_SIZE_DEFAULT);
        _jdbcSpillSizeMsgRefsByMsgSize = "true".equalsIgnoreCase(propValue);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(this, tc, "Using jdbcSpillSizeMsgRefsByMsgSize=" + _jdbcSpillSizeMsgRefsByMsgSize);
        }

        try
        {
            _itemStorageManager = new ItemStorageManager();
            _itemStorageManager.initialize(this);

            _persistentMessageStore = PersistenceFactory.getPersistentMessageStore(this, _manager, _configuration);
            _persistentMessageStore.start();

            // 246935
            try
            {
                String value = getProperty(PROP_SPILL_UPPER_LIMIT, PROP_SPILL_UPPER_LIMIT_DEFAULT);
                _spillUpperLimit = Integer.parseInt(value);
                value = getProperty(PROP_SPILL_LOWER_LIMIT, PROP_SPILL_LOWER_LIMIT_DEFAULT);
                _spillLowerLimit = Integer.parseInt(value);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Using spillUpperLimit=" + _spillUpperLimit + "; spillLowerLimit=" + _spillLowerLimit);

                // Defect 484799
                value = getProperty(PROP_SPILL_UPPER_SIZE_LIMIT, PROP_SPILL_UPPER_SIZE_LIMIT_DEFAULT);
                _spillUpperSizeLimit = Integer.parseInt(value);
                value = getProperty(PROP_SPILL_LOWER_SIZE_LIMIT, PROP_SPILL_LOWER_SIZE_LIMIT_DEFAULT);
                _spillLowerSizeLimit = Integer.parseInt(value);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Using spillUpperSizeLimit=" + _spillUpperSizeLimit + "; spillLowerSizeLimit=" + _spillLowerSizeLimit);
            } catch (NumberFormatException e)
            {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Exception " + e + " while parsing spill limits");
                throw e;
            }

            //This portion of start up processing is not interruptable
            synchronized (_startLock)
            {
                if (_state != MessageStoreState.STATE_STARTING)
                {
                    throw new IllegalStateException(nls.getFormattedMessage("INVALID_MSGSTORE_STATE_SIMS0505", new Object[] { _state }, null));
                }

                _manager.restart(_persistentMessageStore);

                String xmlOn = getProperty(PROP_DUMP_RAW_XML_ON_STARTUP, null);
                if (null != xmlOn)
                {
                    FileWriter fw = new FileWriter(xmlOn);
                    FormattedWriter writer = new FormattedWriter(fw);
                    _xmlWriteRawOn(writer, false);
                    writer.flush();
                    writer.close();
                }

                // This is the TransactionFactory for this
                // MessageStore instance. Must call after
                // restart above to ensure that the PM
                // will cast as expected.
                String strMaxTransactionSize =
                                getProperty(PROP_TRANSACTION_SEND_LIMIT, PROP_TRANSACTION_SEND_LIMIT_DEFAULT);
                int maxTransactionSize = Integer.parseInt(strMaxTransactionSize);
                _transactionFactory = new MSTransactionFactory(this, (PersistenceManager) _persistentMessageStore);
                _transactionFactory.setMaximumTransactionSize(maxTransactionSize);

                //If the root item stream is not found, create one.
                Persistable persistable = _persistentMessageStore.readRootPersistable();

                if (null == persistable)
                {
                    throw new MessageStoreRuntimeException("ROOT_PERSISTABLE_EXCEPTION_SIMS0504");
                }

                _uniqueIdentifierGenerator = _persistentMessageStore.getUniqueKeyGenerator("UniqueIdentifier", 1000000);
                _uniqueLockIDGenerator = _persistentMessageStore.getUniqueKeyGenerator("UniqueLockValue", 500000);
                _tickCountGenerator = _persistentMessageStore.getUniqueKeyGenerator("UniqueTickCount", 500000);

                _rootMembership = new RootMembership(this, persistable);
                _rootMembership.initialize();

                // Start the CacheLoader last
                _cacheLoader.start(_cacheLoaderInterval, _messagingEngine);

                _state = MessageStoreState.STATE_STARTED;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Message store state is : " + _state);
            }

            // Defect 572575
            // Only read in the value of the flag to allow disabling of the 
            // datastore lock after startup processing has completed. This
            // removes the neccessity to handle it being true during initial
            // lock acquisition.
            String strDatastoreLockCanBeDisabled = getProperty(PROP_DATASTORE_LOCK_CAN_BE_DISABLED,
                                                               PROP_DATASTORE_LOCK_CAN_BE_DISABLED_DEFAULT);

            _datastoreLockCanBeDisabled = Boolean.parseBoolean(strDatastoreLockCanBeDisabled);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "_datastoreLockCanBeDisabled=" + _datastoreLockCanBeDisabled);
        } catch (Exception e)
        {
            if (!(e.getCause() instanceof NonExistentLogFileException))
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.impl.MessageStoreImpl.start", "755", this);

            SibTr.error(tc, "STARTUP_EXCEPTION_SIMS0002", new Object[] { e });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "Exception: ", e);

            // Defect 326323
            // Save our startup exception.
            // Defect 465809
            // Add our startup exception to the saved list
            setStartupException(e);

            // close everything we have opened
            stop(0); // 247659
            //Report that we have failed to start cleanly.
            reportLocalError();
            throw new Exception(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "start");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#stop(int)
     */
    @Override
    public final void stop(int arg0)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "stop", Integer.valueOf(arg0));

        //Shut down processing can only begin when the lock is available.
        synchronized (_startLock)
        {
            if (_state == MessageStoreState.STATE_UNINITIALIZED)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "stop");
                throw new IllegalStateException(nls.getFormattedMessage("INVALID_MSGSTORE_STATE_SIMS0505", new Object[] { _state }, null));
            }

            if (null != _expirer)
                _expirer.stop();

            if (null != _deliveryDelayManager)
                _deliveryDelayManager.stop();

            if (null != _cacheLoader)
                _cacheLoader.stop();

            if (null != _membershipMap)
                _membershipMap.clear();

            if (null != _persistentMessageStore)
                _persistentMessageStore.stop(arg0);

            // Null out any references to our cache/om cache
            _rootMembership = null;
            _itemStorageManager = null;
            _tickCountGenerator = null;
            _uniqueIdentifierGenerator = null;
            _uniqueLockIDGenerator = null;

            _state = MessageStoreState.STATE_STOPPED;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Message store state is : " + _state);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "stop");
    }

    /**
     * reply a string representationof the receiver.
     * 
     * @see Object#toString()
     */
    @Override
    public final String toString()
    {
        //      FormatBuffer buf = new FormatBuffer();
        //      printOn(buf);
        //      return buf.toString();
        return super.toString() + "[State:" + _state + "]";
    }

    public final void unregister(AbstractItemLink itemLink)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unregister", itemLink);

        _membershipMap.remove(itemLink.getID());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "unregister");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#writeXmlOn(com.ibm.ws.sib.msgstore.FormatBuffer)
     */
    @Override
    public void xmlWriteOn(FormattedWriter writer) throws IOException
    {
        writer.startTag(XML_MESSAGE_STORE);
        writer.indent();
        synchronized (_startLock)
        {
            if (_state == MessageStoreState.STATE_STARTED)
            {
                _rootMembership.xmlWriteItemStreamsOn(writer);
            }
            else
            {
                writer.newLine();
                writer.write(XML_CANNOT_BE_WRITTEN);
            }
        }

        if (null != _itemStorageManager)
        {
            _itemStorageManager.xmlWriteOn(writer);
        }

        if (null != _membershipMap)
        {
            _membershipMap.xmlWriteOn(writer);
        }

        if (null != _persistentMessageStore)
        {
            _persistentMessageStore.xmlWriteOn(writer);
        }

        if (null != _expirer)
        {
            _expirer.xmlWriteOn(writer);
        }

        if (null != _cacheLoader)
        {
            _cacheLoader.xmlWriteOn(writer);
        }

        if (null != _deliveryDelayManager) {
            _deliveryDelayManager.xmlWriteOn(writer);
        }
        writer.outdent();
        writer.newLine();
        writer.endTag(XML_MESSAGE_STORE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.MessageStore#xmlWriteRawOn(com.ibm.ws.sib.msgstore.FormattedWriter, boolean)
     */
    @Override
    public final void xmlWriteRawOn(FormattedWriter writer, boolean callbackToItem) throws IOException
    {
        synchronized (_startLock)
        {
            if (_state == MessageStoreState.STATE_STARTED)
            {
                _xmlWriteRawOn(writer, callbackToItem);
            }
            else
            {
                writer.startTag(XML_MESSAGE_STORE);
                writer.indent();
                writer.newLine();
                writer.write(XML_CANNOT_BE_WRITTEN);
                writer.outdent();
                writer.newLine();
                writer.endTag(XML_MESSAGE_STORE);
            }
        }
    }

    // Defect 337421.1
    /*************************************************************************/
    /* JsReloadableComponent */
    /*************************************************************************/

    @Override
    public void setCustomPropertyByReload(String key, String value)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCustomPropertyByReload", new Object[] { "Key=" + key, "Value=" + value });

        _customProperties.put(key, value);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCustomPropertyByReload");
    }

    @Override
    public void unsetCustomPropertyByReload(String key)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unsetCustomPropertyByReload", "Key=" + key);

        _customProperties.remove(key);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "unsetCustomPropertyByReload");
    }

    // We don't make use of any attributes.
    public void setConfigByReload(JsEObject newConfig) {}

    // We won't get called with this method
    @Override
    public void reloadComponent(JsEObject config) {}

    @Override
    public boolean isDeleteable()
    {
        return false;
    }

    /*************************************************************************/
    /* JsReloadableComponent */
    /*************************************************************************/

    //F1332-52014
    /**
     * Set whether to use new database locking algoritham which
     * holds short duration locks
     * 
     * @param restrictLongDBLock boolean value of true or false
     */
    public void setRestrictLongDBLock(boolean restrictLongDBLock)
    {
        //No trace required
        this._restrictLongDBLock = restrictLongDBLock;
    }

    /**
     * Returns whether the new database locking algorithm is used or not
     * 
     * @return restrictLongDBLock
     */
    public boolean isRestrictLongDBLock()
    {
        //No trace required
        return _restrictLongDBLock;
    }

    //F1332-52014

    //F1332-52015 starts
    /*
     * Set JVM's System.currentTimeMillis() by NewDBLockingThread
     * which indicates that when was the last time this ME updated the timestamp
     * in the database. This is later used by Table.java to check the
     * NewDBLockingThread is running properly before completing the database
     * updates
     */
    public void setLastDBLockedTimestamp(long lastDBLockedTimestamp)
    {
        //No trace required
        _lastDBLockedTimestamp = lastDBLockedTimestamp;
    }

    /*
     * Returns the timestamp when the NewDBLockingThread last updated the DB with
     * the ownership timestamp
     */
    public long getLastDBLockedTimestamp()
    {
        //No trace required
        return _lastDBLockedTimestamp;
    }

    //F1332-52015 ends

    //lohith liberty change, these are currently dummy as MessageStore is not used as a component by Admin
    @Override
    public final void initialize(JsRecoveryMessagingEngine recoveryME, String mode)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "initialize", new Object[] { recoveryME, mode });
        this._messagingEngine = recoveryME;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "initialize", "Assigned new recoverME to messages store");

    }

    @Override
    public void setConfig(LWMConfig config) {
        // TODO Auto-generated method stub

    }

}
