package com.ibm.ws.sib.msgstore.impl;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.PersistentMessageStore;
import com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter; 

/**
 * This class is used in two situations. The first is a dump of the data store while the
 * MS is starting. The second is using the DataStoreDump utility which runs outside WAS.
 */
public class RawDataDumper implements XmlConstants
{
    private final PersistentMessageStore _persistentMessageStore;
    private final HashMap _tupleMap = new HashMap();
    private final FormattedWriter _writer;
    private final boolean _callbackToItem;

    public RawDataDumper(PersistentMessageStore persistentMessageStore, FormattedWriter writer)
    {
        this(persistentMessageStore, writer, false);
    }

    public RawDataDumper(PersistentMessageStore persistentMessageStore, FormattedWriter writer, boolean callbackToItem)
    {
        super();
        _persistentMessageStore = persistentMessageStore;
        _writer = writer;
        _callbackToItem = callbackToItem;
    }

    private final void _buildTupleMap(PersistentMessageStore pm) throws PersistenceException 
    {
        // get all the tuples from the database
        List list = pm.readAllStreams();
        Iterator tuples = list.iterator();
        // sort the tuples into collections of siblings keyed by their
        // parents id.
        while (tuples.hasNext())
        {
            Persistable tuple = (Persistable) tuples.next();
            long parent = tuple.getContainingStreamId();
            Long key = Long.valueOf(parent);
            ArrayList siblings = (ArrayList) _tupleMap.get(key);
            if (null == siblings)
            {
                siblings = new ArrayList();
                _tupleMap.put(key, siblings);
            }
            siblings.add(tuple);
        }
    }

    private final void _startTag(String tagName, Persistable persistable) throws IOException 
    {
        _writer.write('<');
        _writer.write(_writer.getNameSpace());  // Defect 358424
        _writer.write(tagName);
        _writer.write(' ');
        _writer.write(XML_ID);
        _writer.write("=\"");
        _writer.write(Long.toString(persistable.getUniqueId()));
        _writer.write("\" >");
    }

    // Feature SIB0112b.ms.1
    private final void _writeCallback(Persistable persistable) throws IOException 
    {
        if (_callbackToItem)
        {
            try
            {
                String className = persistable.getItemClassName();
                AbstractItem item = (AbstractItem) (Class.forName(className).newInstance());
                List<DataSlice> dataSlices = _persistentMessageStore.readDataOnly(persistable);
                if (dataSlices != null)
                {
                    item.restore(dataSlices);
                    item.xmlWriteOn(_writer);
                }
            }
            catch (IOException ioexc)
            {
                // No FFDC code needed
                // as this is fault diagnostic code, and the exception 
                // will be dumped to the output anyway
                _writer.write(ioexc);
                throw ioexc;
            }
            catch (Throwable t)
            {
                // No FFDC code needed
                // as this is fault diagnostic code, and the exception 
                // will be dumped to the output anyway
                _writer.write(t);
            }
        }
    }

    private final void _writeItem(Persistable persistable) throws IOException 
    {
        _startTag(XML_ITEM, persistable);
        _writer.indent();
        persistable.xmlWrite(_writer);
        _writeCallback(persistable);
        _writer.outdent();
        _writer.newLine();
        _writer.endTag(XML_ITEM);
    }

    private final void _writeItemReference(Persistable persistable) throws IOException 
    {
        _startTag(XML_REFERENCE, persistable);
        _writer.indent();
        persistable.xmlWrite(_writer);
        _writeCallback(persistable);
        _writer.outdent();
        _writer.newLine();
        _writer.endTag(XML_REFERENCE);
    }

    private final void _writeItemStream(Persistable persistable) throws IOException 
    {
        _startTag(XML_ITEM_STREAM, persistable);
        _writer.indent();
        persistable.xmlWrite(_writer);
        _writeCallback(persistable);
        _writer.newLine();
        ArrayList childStreamTuples = (ArrayList) _tupleMap.remove(Long.valueOf(persistable.getUniqueId()));
        _writer.startTag(XML_ITEM_STREAMS);
        _writer.indent();
        if (null != childStreamTuples)
        {
            Iterator it = childStreamTuples.iterator();
            while (it.hasNext())
            {
                Persistable tuple = (Persistable) it.next();
                if (tuple.getTupleType() == TupleTypeEnum.ITEM_STREAM)
                {
                    _writer.newLine();
                    _writeItemStream(tuple);
                    _writer.flush();
                }
            }
        }
        _writer.outdent();
        _writer.newLine();
        _writer.endTag(XML_ITEM_STREAMS);
        _writer.newLine();
        _writer.startTag(XML_REFERENCE_STREAMS);
        _writer.indent();
        if (null != childStreamTuples)
        {
            Iterator it = childStreamTuples.iterator();
            while (it.hasNext())
            {
                Persistable tuple = (Persistable) it.next();
                if (tuple.getTupleType() == TupleTypeEnum.REFERENCE_STREAM)
                {
                    _writer.newLine();
                    _writeReferenceStream(tuple);
                    _writer.flush();
                }
            }
        }

        _writer.outdent();
        _writer.newLine();
        _writer.endTag(XML_REFERENCE_STREAMS);
        _writer.newLine();
        _writer.startTag(XML_ITEMS);
        _writer.indent();
        try
        {
            List list = _persistentMessageStore.readNonStreamItems(persistable);
            Iterator it = list.iterator();
            while (it.hasNext())
            {
                Persistable tuple = (Persistable) it.next();
                _writer.newLine();
                _writeItem(tuple);
                _writer.flush();
            }
        }
        catch (PersistenceException e)
        {
            // No FFDC code needed
            // as this is fault diagnostic code, and the exception 
            // will be dumped to the output anyway
            _writer.write("Exception reading items");
            _writer.write(e);
        }
        _writer.outdent();
        _writer.newLine();
        _writer.endTag(XML_ITEMS);
        _writer.outdent();
        _writer.newLine();
        _writer.endTag(XML_ITEM_STREAM);
    }

    private final void _writeReferenceStream(Persistable persistable) throws IOException 
    {
        _startTag(XML_REFERENCE_STREAM, persistable);
        _writer.indent();
        persistable.xmlWrite(_writer);
        _writeCallback(persistable);
        _writer.newLine();
        _writer.startTag(XML_REFERENCES);
        _writer.indent();
        try
        {
            List list = _persistentMessageStore.readNonStreamItems(persistable);
            Iterator it = list.iterator();
            while (it.hasNext())
            {
                Persistable tuple = (Persistable) it.next();
                _writer.newLine();
                _writeItemReference(tuple);
                _writer.flush();
            }
        }
        catch (PersistenceException e)
        {
            // No FFDC code needed
            // as this is fault diagnostic code, and the exception 
            // will be dumped to the output anyway
            _writer.write("Exception reading items");
            _writer.write(e);
        }
        _writer.outdent();
        _writer.newLine();
        _writer.endTag(XML_REFERENCES);
        _writer.outdent();
        _writer.newLine();
        _writer.endTag(XML_REFERENCE_STREAM);
    }

    public final void dump() throws IOException 
    {
        _writer.startTag(XML_MESSAGE_STORE);
        _writer.indent();
        try
        {
            _writer.newLine();
            _writer.startTag(XML_ITEM_STREAMS);
            _writer.indent();
            try
            {
                Persistable rootPersistable = _persistentMessageStore.readRootPersistable();

                _buildTupleMap(_persistentMessageStore);
                ArrayList children = (ArrayList) _tupleMap.remove(Long.valueOf(rootPersistable.getUniqueId()));
                for (int i = 0; null != children && i < children.size(); i++)
                {
                    Persistable tuple = (Persistable) children.get(i);
                    _writer.newLine();
                    _writeItemStream(tuple);
                    _writer.flush();
                }

            }
            catch (MessageStoreException e)
            {
                // No FFDC code needed
                // as this is fault diagnostic code, and the exception 
                // will be dumped to the output anyway
                _writer.write("Exception reading itemStreams");
                _writer.write(e);
            }
            _writer.outdent();
            _writer.newLine();
            _writer.endTag(XML_ITEM_STREAMS);

        }
        catch (IOException e)
        {
            // No FFDC code needed
            // as this is fault diagnostic code, and the exception 
            // will be dumped to the output anyway
            _writer.write("exception reading root item stream");
            _writer.write(e);
        }
        _writer.outdent();
        _writer.newLine();
        _writer.endTag(XML_MESSAGE_STORE);
        _writer.newLine();
        _writer.flush();
    }
}
