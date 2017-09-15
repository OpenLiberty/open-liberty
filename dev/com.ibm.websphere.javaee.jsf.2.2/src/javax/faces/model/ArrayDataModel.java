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

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public class ArrayDataModel<E> extends DataModel<E>
{
    // FIELDS
    private int _rowIndex = -1;
    private E[] _data;

    // CONSTRUCTORS
    public ArrayDataModel()
    {
        super();
    }

    public ArrayDataModel(E[] array)
    {
        if (array == null)
        {
            throw new NullPointerException("array");
        }
        setWrappedData(array);
    }

    // METHODS
    @Override
    public int getRowCount()
    {
        if (_data == null)
        {
            return -1;
        }
        return _data.length;
    }

    @Override
    public E getRowData()
    {
        if (_data == null)
        {
            return null;
        }
        if (!isRowAvailable())
        {
            throw new IllegalArgumentException("row is unavailable");
        }
        return _data[_rowIndex];
    }

    @Override
    public int getRowIndex()
    {
        return _rowIndex;
    }

    @Override
    public Object getWrappedData()
    {
        return _data;
    }

    @Override
    public boolean isRowAvailable()
    {
        return _data != null && _rowIndex >= 0 && _rowIndex < _data.length;
    }

    @Override
    public void setRowIndex(int rowIndex)
    {
        if (rowIndex < -1)
        {
            throw new IllegalArgumentException("illegal rowIndex " + rowIndex);
        }
        int oldRowIndex = _rowIndex;
        _rowIndex = rowIndex;
        if (_data != null && oldRowIndex != _rowIndex)
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
            _data = null;
        }
        else
        {
            _data = (E[])data;
            _rowIndex = -1;
            setRowIndex(0);
        }
    }

}
