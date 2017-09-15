/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.ConsumerMonitorListenerCache;
import com.ibm.ws.sib.comms.client.DestinationListenerCache;
import com.ibm.ws.sib.comms.server.clientsupport.CATMainConsumer;
import com.ibm.ws.sib.comms.server.clientsupport.CachedSessionProperties;
import com.ibm.ws.sib.comms.server.clientsupport.ChunkedMessageWrapper;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A ConversationState object is used to hold the objects that are manipulated
 * during the life of a conversation. The objects are held in an Object Store which is
 * implemented as a single-dimensional array.
 * 
 * @author rajam
 */
public class ConversationState
{
    /** Trace */
    private static TraceComponent tc = SibTr.register(ConversationState.class,
                                                      CommsConstants.MSG_GROUP,
                                                      CommsConstants.MSG_BUNDLE);
    /** NLS handle */
    private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

    /** Class name for FFDC's */
    private static String CLASS_NAME = ConversationState.class.getName();

    /** Log class info on load */
    static
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/ConversationState.java, SIB.comms, WASX.SIB, aa1225.01 1.48");
    }

    public static final int OBJECT_TABLE_ORIGIN = 10; // Why not start at 0 - this is wasting space ???
    public static final int INITIAL_SIZE_OF_OBJECT_TABLE = OBJECT_TABLE_ORIGIN + 32;
    static final int MINUS_ONE = -1;
    static final int OBJECT_TABLE_EXTEND_FACTOR = 2;

    private int maxIndex = INITIAL_SIZE_OF_OBJECT_TABLE - 1;
    private int highWatermark = OBJECT_TABLE_ORIGIN;
    private int freeSlot = OBJECT_TABLE_ORIGIN;
    private int maxTableSize = 0;
    private Object[] objectTable;
    private boolean foundFreeSlot;
    private short connectionObjectId;

    /** The unique request number for exchange calls */
    private int requestNumber = 0;

    /**
     * When doing SICoreConnection.receive the destination is
     * cached to save time creating sessions. The session
     * properties are cached here.
     */
    private CachedSessionProperties cachedProps = null;

    /** The actual CATMainConsumer object is cached here */
    private CATMainConsumer cachedConsumer = null;

    /** High Level CommsConnection associated with the Conversation */
    private CommsConnection cc = null;

    /** A map of ids to partial chunked messages */
    private final HashMap<Long, ChunkedMessageWrapper> inProgressMessages = new HashMap<Long, ChunkedMessageWrapper>();

    /**
     * Flag indicating that the ME associated with the single SICoreConnection which is used by the owning Conversation has terminated.
     */
    private volatile boolean hasMETerminated = false;

    /**
     * Default Constructor
     */
    public ConversationState()
    {
        this(Short.MAX_VALUE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Constructor - invoked if a maximum table size if provided
     * 
     * @param maxTabSize
     */
    public ConversationState(int maxTabSize)
    {
        this(INITIAL_SIZE_OF_OBJECT_TABLE, maxTabSize);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", "" + maxTabSize);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Constructor - invoked if initial table size and maximum table size are provided
     * 
     * @param initialTableSize
     * @param maxTabSize
     */
    public ConversationState(int initialTableSize, int maxTabSize)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[]
            {
             "" + initialTableSize,
             "" + maxTabSize
            });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "ConversationState", "CS> Create a new Conversation State (Object Store)");

        if ((initialTableSize <= 0) || (maxTabSize <= 0))
        {
            throw new NegativeArraySizeException();
        }
        if (initialTableSize < OBJECT_TABLE_ORIGIN)
        {
            throw new IllegalArgumentException();
        }
        if (initialTableSize > maxTabSize)
        {
            throw new IllegalArgumentException();
        }

        maxTableSize = maxTabSize;
        objectTable = new Object[initialTableSize];

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * setConnectionObjectId - sets and stores the connection object id
     * 
     * @param connectionObjectId
     */
    public void setConnectionObjectId(short connectionObjectId)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setConnectionObjectId", Short.valueOf(connectionObjectId));
        this.connectionObjectId = connectionObjectId;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setConnectionObjectId");
    }

    /**
     * @return Returns the connection object id associated with this conversation.
     */
    public short getConnectionObjectId()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConnectionObjectId");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConnectionObjectId", Short.valueOf(connectionObjectId));
        return connectionObjectId;
    }

    /**
     * Sets the cached destination.
     * 
     * @param props
     */
    public void setCachedConsumerProps(CachedSessionProperties props)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCachedConsumerProps", props);
        this.cachedProps = props;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCachedConsumerProps");
    }

    /**
     * @return Returns the cached session props.
     */
    public CachedSessionProperties getCachedConsumerProps()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCachedConsumerProps");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCachedConsumerProps", cachedProps);
        return cachedProps;
    }

    /**
     * @return Returns the cached CATMainConsumer object
     */
    public CATMainConsumer getCachedConsumer()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCachedConsumer");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCachedConsumer", cachedConsumer);
        return cachedConsumer;
    }

    /**
     * Sets the cached CATMainConsumer
     * 
     * @param consumer
     */
    public void setCachedConsumer(CATMainConsumer consumer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCachedConsumer", consumer);
        this.cachedConsumer = consumer;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCachedConsumer");
    }

    /**
     * @return - int - Number of objects in the store
     */
    public int getObjectCount()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getObjectCount");
        int count = 0;
        for (int i = OBJECT_TABLE_ORIGIN; i < maxIndex; i++)
        {
            if (objectTable[i] != null)
                count++;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getObjectCount", Integer.valueOf(count));
        return count;
    }

    /*
     * Empty the object store
     */
    public synchronized void emptyObjectStore() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "emptyObjectStore");

        for (int i = 0; i < objectTable.length; i++) {
            objectTable[i] = null;
        }

        freeSlot = OBJECT_TABLE_ORIGIN;
        highWatermark = OBJECT_TABLE_ORIGIN;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "emptyObjectStore");
    }

    /**
     * addObject - adds a given object to the object store
     * 
     * @param object An object that is to be saved into the object store
     * @return Returns an integer that identifies the location of the object in
     *         the object store
     * 
     * @throws ConversationStateFullException if there is no more space.
     */
    public synchronized int addObject(Object object) throws ConversationStateFullException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addObject", "object=" + object);

        int objectIndex = 0;

        if (freeSlot == MINUS_ONE)
        { // Object Store full ?
            extendObjectTable(); // extend it
        }

        objectIndex = freeSlot;
        objectTable[objectIndex] = object; // Save object
        // Set high water mark
        highWatermark = Math.max(highWatermark, freeSlot);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "High Water Mark = ", Integer.valueOf(highWatermark));

        foundFreeSlot = false;
        // Find next free slot
        for (int i = (highWatermark + 1); i <= maxIndex; i++)
        {
            if (objectTable[i] == null)
            {
                freeSlot = i; // Save index of next free slot
                foundFreeSlot = true;
                break;
            }
        }

        if (!foundFreeSlot)
        { // Not found a free slot yet
            for (int i = OBJECT_TABLE_ORIGIN; i <= (highWatermark - 1); i++)
            {
                if (objectTable[i] == null)
                {
                    freeSlot = i; // Save index of next free slot
                    foundFreeSlot = true;
                    break;
                }
            }
        }
        // No free slots, extend table on next add
        if (!foundFreeSlot)
        {
            freeSlot = MINUS_ONE; // Indicate need for extension
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "Next free slot = ", Integer.valueOf(freeSlot));
            SibTr.debug(tc, "Max Index = ", Integer.valueOf(maxIndex));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addObject", "" + objectIndex);
        return objectIndex; // Pass back object index
    }

    /**
     * extendObjectTable - extends the object store if it is found to be full. The table
     * can be extended until a maximum size. The maximum size can be supplied, or a
     * default size.
     * 
     * @throws ConversationStateFullException if there is no more space.
     */
    private synchronized void extendObjectTable() throws ConversationStateFullException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "extendObjectTable");

        int newTableSize = (((maxIndex + 1) * OBJECT_TABLE_EXTEND_FACTOR) - OBJECT_TABLE_ORIGIN);

        // new table size
        if (newTableSize > maxTableSize)
        {
            if ((maxIndex + 1) < maxTableSize)
            {
                newTableSize = maxTableSize;
            }
            else
            {
                ConversationStateFullException e = new ConversationStateFullException();

                // Lets FFDC this so we can capture the information about what has filled up the store
                FFDCFilter.processException(e, CLASS_NAME + ".extendObjectTable",
                                            CommsConstants.CONVERSATIONSTATE_EXTENDOBJECTTABLE_01,
                                            new Object[] { getLastItemsInStore(), this });

                throw e;
            }
        }

        Object[] newObjectTable = new Object[newTableSize]; // Create new, extended table
        // Copy old table to new
        System.arraycopy(objectTable, 0, newObjectTable, 0, (maxIndex + 1));

        freeSlot = (maxIndex + 1); // set next free slot
        maxIndex = (newTableSize - 1); // Set new max index
        objectTable = newObjectTable; // Point to new table

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "Next free slot = ", Integer.valueOf(freeSlot));
            SibTr.debug(tc, "Max Index = ", Integer.valueOf(maxIndex));
            SibTr.debug(tc, "High Water Mark = ", Integer.valueOf(highWatermark));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "extendObjectTable");
    }

    /**
     * getObject - retrieve an object from the object store.
     * 
     * @param objectIndex An integer value that identifies the location of the object in
     *            the object store.
     * @return Returns the requested object.
     * 
     * @throws IndexOutOfBoundsException
     * @throws NoSuchElementException
     */
    public synchronized Object getObject(int objectIndex)
                    throws IndexOutOfBoundsException, NoSuchElementException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getObject", "" + objectIndex);

        Object object;

        if ((objectIndex < OBJECT_TABLE_ORIGIN) || (objectIndex > maxIndex))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Invalid object index");
            throw new IndexOutOfBoundsException();
        }

        object = objectTable[objectIndex];

        if (object == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "No such element existed!");
            throw new NoSuchElementException();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getObject", object);
        return object;
    }

    /**
     * removeObject - remove an object from the object store.
     * 
     * @param objectIndex An integer value that identifies the location of the object in
     *            the object store.
     * @return Returns the object that was in that position
     * 
     * @throws IndexOutOfBoundsException
     * @throws NoSuchElementException
     */
    public synchronized Object removeObject(int objectIndex)
                    throws IndexOutOfBoundsException, NoSuchElementException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeObject", Integer.valueOf(objectIndex));

        if ((objectIndex < OBJECT_TABLE_ORIGIN))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Invalid object index");
            throw new IndexOutOfBoundsException();
        }
        else if ((objectIndex > maxIndex))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Invalid object index");
            return null;
        }
        if (objectTable[objectIndex] == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "No object exists at the entry");
            throw new NoSuchElementException();
        }

        Object returnObject = objectTable[objectIndex];
        objectTable[objectIndex] = null;

        /*
         * If no free slots, make the slot we just freed the next available free slot
         */
        if (freeSlot == MINUS_ONE)
        {
            freeSlot = objectIndex;
        }
        /*
         * If slot we just free is less than next available free slot, set next
         * available free slot to this.
         */
        else
        {
            freeSlot = Math.min(freeSlot, objectIndex);
        }
        /*
         * If the high water mark is equal to the index of the slot that we just
         * freed, decrement the high water mark by one until we reach the next slot
         * in use.
         */
        if (highWatermark == objectIndex)
        {
            while (objectTable[highWatermark] == null)
            {
                if (highWatermark == OBJECT_TABLE_ORIGIN)
                {
                    break;
                }

                highWatermark = (highWatermark - 1);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeObject", returnObject);
        return returnObject;
    }

    /**
     * @return Returns all the objects in the store
     */
    public synchronized List getAllObjects()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getAllObjects");

        List<Object> objs = new ArrayList<Object>();

        for (int i = OBJECT_TABLE_ORIGIN; i < maxIndex; i++)
        {
            if (objectTable[i] != null)
                objs.add(objectTable[i]);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getAllObjects", objs);
        return objs;
    }

    /**
     * dumpTable - dumps the specified number of entries in the Object Table.
     * 
     * @param numberOfEntries The number of entries to be dump.
     * 
     */
    public void dumpObjectTable(int numberOfEntries)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dumpObjectTable");

        if (numberOfEntries < 0)
        {
            throw new IllegalArgumentException();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            for (int i = OBJECT_TABLE_ORIGIN; i < (OBJECT_TABLE_ORIGIN + numberOfEntries); i++)
            {
                SibTr.debug(this, tc, "objectTable: " + i, objectTable[i]);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "dumpObjectTable");
    }

    /**
     * @return Returns the CommsConnection associated with the Conversation
     */
    public CommsConnection getCommsConnection()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCommsConnection");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCommsConnection", cc);
        return cc;
    }

    /**
     * Sets the CommsConnection associated with the Conversation
     * 
     * @param cc
     */
    public void setCommsConnection(CommsConnection cc)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCommsConnection", cc);
        this.cc = cc;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCommsConnection");
    }

    /**
     * This method will set the initial request number for this conversation.
     * 
     * @param initialRequestNumber
     */
    public void setInitialRequestNumber(int initialRequestNumber)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setInitialRequestNumber", Integer.valueOf(initialRequestNumber));
        this.requestNumber = initialRequestNumber;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setInitialRequestNumber");
    }

    /**
     * This method will return a unique number that can be used for JFAP exchanges.
     * as this can be for ME-ME comms unique number is always increased by two.
     * As such, if the initiating peer uses odd request numbers they will always use unique
     * numbers.
     * 
     * @return Returns a unique number.
     */
    public synchronized int getUniqueRequestNumber()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getUniqueRequestNumber");

        // Always add two
        requestNumber += 2;

        if (requestNumber > Short.MAX_VALUE)
        {
            // Reset the number to 0 for even, or 1 for odd
            requestNumber = requestNumber % 2;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getUniqueRequestNumber", "" + requestNumber);
        return requestNumber;
    }

    /**
     * This method can be used to create a String that details the last 100 items added to the store.
     * It is best used at the time when the object store is full to indicate what is taking up all
     * the space in the store.
     * 
     * @return Returns a String containing information about what is in the store.
     */
    public synchronized String getLastItemsInStore()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getLastItemsInStore");
        String lastItems = "";

        // The last item in the store will be at position maxIndex. Dump all the items 100 below that
        for (int x = maxIndex; x > (maxIndex - 101); x--)
        {
            lastItems = " [" + x + "]: " + objectTable[x] + "\r\n" + lastItems;
        }

        lastItems = "The last 100 items in the store:\r\n\r\n" + lastItems;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getLastItemsInStore");
        return lastItems;
    }

    /**
     * @param wrapperId
     * @return Returns a chunked message wrapper for the appropriate wrapper Id.
     */
    public ChunkedMessageWrapper getChunkedMessageWrapper(long wrapperId)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getChunkedMessageWrapper", Long.valueOf(wrapperId));
        ChunkedMessageWrapper wrapper = inProgressMessages.get(Long.valueOf(wrapperId));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getChunkedMessageWrapper", wrapper);
        return wrapper;
    }

    /**
     * Puts a chunked message wrapper into our map.
     * 
     * @param wrapperId
     * @param wrapper
     */
    public void putChunkedMessageWrapper(long wrapperId, ChunkedMessageWrapper wrapper)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "putChunkedMessageWrapper", new Object[] { Long.valueOf(wrapperId), wrapper });
        inProgressMessages.put(Long.valueOf(wrapperId), wrapper);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "putChunkedMessageWrapper");
    }

    /**
     * Removes a chunked message wrapper from our map.
     * 
     * @param wrapperId
     */
    public void removeChunkedMessageWrapper(long wrapperId)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeChunkedMessageWrapper", Long.valueOf(wrapperId));
        inProgressMessages.remove(Long.valueOf(wrapperId));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeChunkedMessageWrapper");
    }

    /**
     * Indicates that the ME associated with this ConversationState object has terminated.
     * Once this is called {@link ConversationState#hasMETerminated()} will return true.
     */
    public void setMETerminated()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMETerminated");
        hasMETerminated = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMETerminated");
    }

    /**
     * Returns true if the ME associated with this ConversationState has terminated and false otherwise.
     * 
     * @return
     */
    public boolean hasMETerminated()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "hasMETerminated");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "hasMETerminated", hasMETerminated);

        return hasMETerminated;
    }

    // SIB0137.comms.2 start

    /**
     * Get the DestinationListenerCache associated with this conversation
     */
    private volatile DestinationListenerCache destinationListenerCache = null;

    public DestinationListenerCache getDestinationListenerCache() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDestinationListenerCache");

        if (destinationListenerCache == null) { // double-checked lock idiom works in Java 1.5 with a volatile
            synchronized (this) {
                if (destinationListenerCache == null)
                    destinationListenerCache = new DestinationListenerCache();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDestinationListenerCache", destinationListenerCache);
        return destinationListenerCache;
    }

    // SIB0137.comms.2 end

    // F011127 START

    /**
     * Get the ConsumerMonitorListenerCache associated with this conversation
     */
    private volatile ConsumerMonitorListenerCache consumerMonitorListenerCache = null;

    public ConsumerMonitorListenerCache getConsumerMonitorListenerCache() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConsumerMonitorListenerCache");

        if (consumerMonitorListenerCache == null) {
            synchronized (this) {
                if (consumerMonitorListenerCache == null)
                    consumerMonitorListenerCache = new ConsumerMonitorListenerCache();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConsumerMonitorListenerCache", consumerMonitorListenerCache);
        return consumerMonitorListenerCache;
    }

    //F011127 END
}
