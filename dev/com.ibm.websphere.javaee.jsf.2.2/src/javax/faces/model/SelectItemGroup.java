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
public class SelectItemGroup extends SelectItem
{
  private static final long serialVersionUID = 849845793056900213L;

  private static final SelectItem[] EMPTY_SELECT_ITEMS = new SelectItem[0];

    // FIELDS
    private SelectItem[] _selectItems;

    // CONSTRUCTORS
    public SelectItemGroup()
    {
        super();
        _selectItems = EMPTY_SELECT_ITEMS;
    }

    public SelectItemGroup(String label)
    {
        super("", label, null, false);
        _selectItems = EMPTY_SELECT_ITEMS;
    }

    public SelectItemGroup(String label, String description, boolean disabled, SelectItem[] selectItems)
    {
        super("", label, description, disabled);
        if (selectItems == null)
        {
            throw new NullPointerException("selectItems");
        }
        _selectItems = selectItems;
    }

    // METHODS
    public SelectItem[] getSelectItems()
    {
        return _selectItems;
    }

    public void setSelectItems(SelectItem[] selectItems)
    {
        if (selectItems == null)
        {
            throw new NullPointerException("selectItems");
        }
        _selectItems = selectItems;
    }
}
