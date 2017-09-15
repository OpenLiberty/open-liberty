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
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

/** A hash map with greater parallelism
 */
public class MultiHashMap implements Map, XmlConstants
{
    private static final class SubMap extends HashMap 
    {
        private static final long serialVersionUID = -4648138397952456765L;

        private final synchronized Object get(long key)
        {
            return get(Long.valueOf(key));
        }
        private final synchronized Object put(long key, Object value)
        {
            return put(Long.valueOf(key), value);
        }
        private final synchronized Object remove(long key)
        {
            return remove(Long.valueOf(key));
        }
    }

    private final int _subMapCount;
    private final SubMap[] _subMaps;

    /**
     * 
     */
    public MultiHashMap(int subMapCount)
    {
        super();
        _subMapCount = subMapCount;
        _subMaps = new SubMap[_subMapCount];
        for (int i = 0; i < _subMaps.length; i++)
        {
            _subMaps[i] = new SubMap();
        }
    }

    private final SubMap _subMap(final long key)
    {
        return _subMaps[(int) Math.abs(key) % _subMapCount];
    }

    /**
     * used in MessageStoreImpl.findById
     * @param key
     */
    public final AbstractItemLink get(final long key)
    {
        return(AbstractItemLink)_subMap(key).get(key);
    }

    /**
     * used in MessageStoreImpl.register
     * @param key
     * @param value
     */
    public final void put(final long key, final AbstractItemLink value)
    {
        _subMap(key).put(key, value);
    }

    /**
     * used in MessageStoreImpl.unregister
     * @param key
     */
    public final AbstractItemLink remove(final long key)
    {
        return(AbstractItemLink)_subMap(key).remove(key);
    }

    public final void clear()
    {
        for (int i = 0; i < _subMaps.length; i++)
        {
            _subMaps[i].clear();
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.impl.Map#xmlWriteOn(com.ibm.ws.sib.utils.ras.FormattedWriter)
     */
    public void xmlWriteOn(FormattedWriter writer) throws IOException 
    {
        writer.newLine();
        writer.startTag(XML_ITEM_MAP);
        writer.indent();
        for (int i = 0; i < _subMaps.length; i++)
        {
            Iterator iterator = _subMaps[i].values().iterator();
            while (iterator.hasNext())
            {
                writer.newLine();
                AbstractItemLink ail = (AbstractItemLink) iterator.next();
                ail.xmlShortWriteOn(writer);
            }
        }
        writer.outdent();
        writer.newLine();
        writer.endTag(XML_ITEM_MAP);
    }
}
