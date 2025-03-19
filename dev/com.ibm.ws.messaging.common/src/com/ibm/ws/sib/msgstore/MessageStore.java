package com.ibm.ws.sib.msgstore;

/* ==============================================================================
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ==============================================================================
 */

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.exception.ConfigurationError;
import com.ibm.ws.exception.ConfigurationWarning;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.messaging.lifecycle.Singleton;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsHealthMonitor;
import com.ibm.ws.sib.admin.JsMonitoredComponent;
import com.ibm.ws.sib.admin.JsReloadableComponent;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class represents the collection of stored {@link Item}s.
 * Only {@link Item}s can be stored directly in the MessageStore.
 * <p>
 * Instances of MessageStore are created indirectly using {@link MessageStore#createInstance()}.
 * For example:
 * <PRE>
 * MessageStore msgStore = MessageStore.createInstance();
 * msgStore.initialize(config);
 * msgStore.start();
 * ...........
 * msgStore.stop();
 * </PRE>
 * </p>
 * <p>
 * Instances of this interface provide for methods for managing the root {@link ItemStream}s needed by users. Each user will probably wish to create
 * their own named {@link ItemStream} and store all their data in it.
 * This is analogous to creating a directory in a file system, and then using
 * the directory to store all one users data.
 * Here is an example of how to create root item stream:<br>
 * First create a NamedItemStream with a getName method. The name should be
 * persisted as part of the NamedItemStream's data.
 * <PRE>
 * public class NamedItemStream extends ItemStream {
 * .......
 * public String getName() {
 * return "MY_NAME";
 * }
 * .......
 * }
 * </PRE>
 *
 * Then during start-up check for the existence of your NamedItemStream, and create
 * one if neccessary:
 * <PRE>
 * Filter filter = new Filter() {
 * public boolean matches(AbstractItem item) {
 * if (item instanceof NamedItemStream) {
 * return "MY_NAME".equals(((NamedItemStream)item).getName());
 * }
 * }
 * }
 * NamedItemStream itemStream = (NamedItemStream)messageStore.findFirstMatching(filter);
 * if (null == itemStream) {
 * itemStream = new NamedItemStream();
 * messageStore.add(itemStream);
 * }
 * </PRE>
 * </p>
 * <p>
 * Instances of this class provide factory methods for creating transactions.
 * </p>
 */
public abstract class MessageStore implements Singleton, JsEngineComponent, JsMonitoredComponent, JsHealthMonitor, JsReloadableComponent, MessageStoreInterface
{
    private static final String MSGSTORE_CLASS_NAME = "com.ibm.ws.sib.msgstore.impl.MessageStoreImpl";
    private static TraceComponent tc = SibTr.register(MessageStore.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    // Defect 465809
    protected List<Exception> _startupExceptions = new ArrayList<Exception>();

    // 724676
    protected boolean _hasRedeliveryCountColumn = false;
    // check for whether the server is stopping or not
    public volatile boolean isServerStopping = false;

    protected MessageStore() {}

    /**
     * Used to call the (package-private) {@link AbstractItem#getMembership()} method from inside the implementation package. A sneaky trick.
     *
     * @param item
     *
     * @return
     */
    protected Membership _getMembership(AbstractItem item)
    {
        return item._getMembership();
    }

    /*
     * Used to call the (package-private) {@link AbstractItem#setMembership()}
     * method from inside the implementation package. A sneaky trick.
     *
     * @param membership
     *
     * @param item
     */
    protected void _setMembership(Membership membership, AbstractItem item)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_setMembership", new Object[] { membership, item });

        item._setMembership(membership);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_setMembership");
    }

    // Defect 465809
    // This method allows all exceptions that occurred at startup
    // to be retrieved in a java.util.List of Exceptions.
    public List<Exception> getStartupExceptions()
    {
        return _startupExceptions;
    }

    // Defect 465809
    // This method allows an exception that occurred at startiup time
    // to be stored away. As start cannot throw exceptions this method
    // allows the MS to fail via its HealthState but still allow any
    // exceptions that occurred to be retrieved.
    public void setStartupException(Exception exception)
    {
        _startupExceptions.add(exception);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.msgstore.MessageStoreInterface#add(com.ibm.ws.sib.msgstore.ItemStream, com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    @Override
    public void add(ItemStream itemStream, Transaction transaction) throws MessageStoreException
    {
        add(itemStream, AbstractItem.NO_LOCK_ID, transaction);
    }

    /**
     * Allow the expirer to begin its work. No expiry will take place until
     * this method has been called.
     *
     * Note: for logistical reasons this method will not be implemented
     * until after the message processor has implemented the calls to it.
     */
    @Override
    public void expirerStart() throws SevereMessageStoreException {}

    /**
     * Allow the expirer to stop its work. No expiry will take place after
     * this method has been called.
     *
     */
    @Override
    public void expirerStop() {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.msgstore.MessageStoreInterface#deliveryDelayManagerStart()
     */
    @Override
    public void deliveryDelayManagerStart() throws SevereMessageStoreException {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.msgstore.MessageStoreInterface#deliveryDelayManagerStop()
     */
    @Override
    public void deliveryDelayManagerStop() {}

    /**
     * Reply the {@link Membership} identified by the given value.
     *
     * @param id
     * @return {@link Membership} or null if non exists.
     */
    protected abstract Membership getMembership(long id);

    @Override
    public void serverStarted() {}

    // Implementing this method so that we can get notification when WAS server is in stopping state.
    // This implementation is needed so that we can stop unnecessary retrying for long time(15 minutes for first time)
    // to obtain connection in DatasourceController.performFirstAction() method
    // when WAS server has already given notification that its in stopping state.

    /**
     * Send notification to message store that the server is stopping.
     * This notification will only be read for first time when message store is in starting state
     */
    @Override
    public void serverStopping()
    {
        isServerStopping = true;
    }

    /*
     * (non-Javadoc)
     * JsEngineComponent used to require a method start() with no args.
     * All our tests were coded to use this, then the interface changed
     * to a start that took an integer. I have implemented this method
     * to redirect to the new start() method so all our tests still
     * work (assuming that zero is the default supplied value).
     */
    public void start() throws Exception
    {
        start(0);
    }

    /**
     * Request that the receiver prints its xml representation
     * (recursively) onto the specified file.
     *
     * @param writer
     * @throws IOException
     */
    @Override
    public final void xmlRequestWriteOnFile(FormattedWriter writer) throws IOException
    {
        xmlWriteOn(writer);
        writer.flush();
    }

    /**
     * Request that the receiver prints its xml representation
     * (recursively) onto standard out.
     *
     * @throws IOException
     */
    @Override
    public final void xmlRequestWriteOnSystemOut() throws IOException
    {
        FormattedWriter writer = new FormattedWriter(new OutputStreamWriter(System.out));
        xmlWriteOn(writer);
        writer.flush();
    }

    /**
     * Request that the receiver prints an xml representation of the
     * persistent data (recursively) onto the specified file.
     *
     * @param writer
     *
     * @throws IOException
     */
    public final void xmlRequestWriteRawDataOnFile(FormattedWriter writer) throws IOException
    {
        xmlWriteRawOn(writer, false);
        writer.flush();
    }

    /**
     * Request that the receiver prints an xml representation of the
     * persistent data (recursively) onto standard out.
     *
     * @throws IOException
     */
    public final void xmlRequestWriteRawDataOnSystemOut() throws IOException
    {
        FormattedWriter writer = new FormattedWriter(new OutputStreamWriter(System.out));
        xmlWriteRawOn(writer, false);
        writer.flush();
    }

    /**
     * Write out the raw (persisted) data to the writer.
     *
     * @param writer
     * @param callBackToItem specifies whether or not to include data from a raw item
     * @throws IOException
     */
    public void xmlWriteRawOn(FormattedWriter writer, boolean callBackToItem) throws IOException {}

    /**
     * Getter for _hasRedeliveryCountColumn
     *
     * @return
     */
    public boolean isRedeliveryCountColumnAvailable()
    {
        return _hasRedeliveryCountColumn;
    }

    /**
     * Setter for _hasRedeliveryCountColumn
     *
     * @param hasRedeliveryCountColumn
     */
    public void setRedeliveryCountColumn(boolean hasRedeliveryCountColumn)
    {
        _hasRedeliveryCountColumn = hasRedeliveryCountColumn;
    }

    public final void stop(int mode) {
        stop(mode, null);
    }

    public abstract void stop(int mode, Throwable reason);
}
