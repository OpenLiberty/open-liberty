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
package jakarta.faces.model;

import java.io.Serializable;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public class SelectItem implements Serializable
{
    private static final long serialVersionUID = 8841094741464512226L;
    // FIELDS
    private Object _value;
    private String _label;
    private String _description;
    private boolean _disabled;
    private boolean _escape;
    private boolean _noSelectionOption;

    // CONSTRUCTORS
    public SelectItem()
    {
        this(null);
    }

    public SelectItem(Object value)
    {
        this(value, value == null ? null : value.toString());
    }

    public SelectItem(Object value, String label)
    {
        this(value, label, null);
    }

    public SelectItem(Object value, String label, String description)
    {
        this(value, label, description, false);
    }

    public SelectItem(Object value, String label, String description, boolean disabled)
    {
        this(value, label, description, disabled, true);
    }

    public SelectItem(Object value, String label, String description, boolean disabled, boolean escape)
    {
        this(value, label, description, disabled, escape, false);
    }

    /**
     * 
     * @param value
     * @param label
     * @param description
     * @param disabled
     * @param escape
     * @param noSelectionOption
     * 
     * @since 2.0
     */
    public SelectItem(Object value, String label, String description, boolean disabled, boolean escape,
                      boolean noSelectionOption)
    {
        _value = value;
        _label = label;
        _description = description;
        _disabled = disabled;
        _escape = escape;
        _noSelectionOption = noSelectionOption;
    }

    // METHODS
    public String getDescription()
    {
        return _description;
    }

    public String getLabel()
    {
        return _label;
    }

    public Object getValue()
    {
        return _value;
    }

    public boolean isDisabled()
    {
        return _disabled;
    }

    public boolean isEscape()
    {
        return _escape;
    }

    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public boolean isNoSelectionOption()
    {
        return _noSelectionOption;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public void setDisabled(boolean disabled)
    {
        _disabled = disabled;
    }

    public void setEscape(boolean escape)
    {
        _escape = escape;
    }

    public void setLabel(String label)
    {
        _label = label;
    }

    /**
     * 
     * @param noSelectionOption
     * 
     * @since 2.0
     */
    public void setNoSelectionOption(boolean noSelectionOption)
    {
        _noSelectionOption = noSelectionOption;
    }

    public void setValue(Object value)
    {
        _value = value;
    }

}
