/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.faces.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @since 2.3
 */
public class IterableDataModel<E> extends DataModel<E>
{
    private int _rowIndex = -1;
    private Iterable<E> _iterable;
    private List<E> _list;
    private Iterator<E> _iterator;
    private int _count = -1;

    public IterableDataModel()
    {
    }

    public IterableDataModel(Iterable<E> iterable)
    {
        if (iterable == null)
        {
            throw new NullPointerException("array");
        }
        setWrappedData(iterable);
    }

    @Override
    public int getRowCount()
    {
        return _count;
    }

    @Override
    public E getRowData()
    {
        if (_iterable == null)
        {
            return null;
        }
        if (!isRowAvailable())
        {
            throw new IllegalArgumentException("row is unavailable");
        }
        return _list.get(_rowIndex);
    }

    @Override
    public int getRowIndex()
    {
        return _rowIndex;
    }

    @Override
    public Object getWrappedData()
    {
        return _iterable;
    }

    @Override
    public boolean isRowAvailable()
    {
        return _iterable != null && _rowIndex >= 0 && _rowIndex < _list.size();
    }

    @Override
    public void setRowIndex(int rowIndex)
    {
        if (rowIndex < -1)
        {
            throw new IllegalArgumentException("illegal rowIndex " + rowIndex);
        }
        int oldRowIndex = _rowIndex;
        
        if (oldRowIndex < rowIndex)
        {
            //Move forward
            for (int i = 0; i < (rowIndex - oldRowIndex); i++)
            {
                if (_list == null)
                {
                    _list = new ArrayList<E>();
                }
                if (_iterator == null)
                {
                    _iterator = _iterable.iterator();
                }
                if (_iterator.hasNext())
                {
                    _list.add(_iterator.next());
                }
                else
                {
                    // Trying to move to a non existent row
                    continue;
                }
            }
        }
        else if (oldRowIndex == rowIndex)
        {
            // No change
        }
        else if (oldRowIndex > rowIndex)
        {
            // Retrieve it from cache
        }
        _rowIndex = rowIndex;
        if (_iterable != null && oldRowIndex != _rowIndex)
        {
            Object data = isRowAvailable() ? getRowData() : null;
            DataModelEvent event = new DataModelEvent(this, _rowIndex, data);
            DataModelListener[] listeners = getDataModelListeners();
            for (int i = 0; i < listeners.length; i++)
            {
                listeners[i].rowSelected(event);
            }
        }
    }

    @Override
    public void setWrappedData(Object data)
    {
        if (data == null)
        {
            setRowIndex(-1);
            _iterable = null;
            _count = -1;
        }
        else
        {
            _iterable = (Iterable<E>)data;
            _rowIndex = -1;
            setRowIndex(0);
            if (data instanceof Set)
            {
                _count = ((Set)data).size();
            }
            else if (data instanceof List)
            {
                _count = ((List)data).size();
            }
            else if (data instanceof Map)
            {
                _count = ((Map)data).size();
            }
            else if (data instanceof Collection)
            {
                _count = ((Collection)data).size();
            }
            else 
            {
                _count = -1;
            }
        }
    }
}
