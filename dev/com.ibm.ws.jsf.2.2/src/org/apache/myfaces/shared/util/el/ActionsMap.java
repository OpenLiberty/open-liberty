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
package org.apache.myfaces.shared.util.el;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * You can use this class to trigger an action when a boolean is set to true.
 *
 * Example : in JSF pages, for dataTable, to remove elements :
 * Backing bean (#{inboxFace}).
 * public ActionsMap getRemoveEmailUnid(){
 *         return new ActionsMap(){
 *            public void performAction(String unid) {
 *                InboxMailDAO<TInboxMail> dao = getInboxMailDAO();
 *                TInboxMail email = dao.getByPrimaryKey( unid );
 *                dao.remove( email );
 *            }
 *        };
 *    }
 * JSF page :
 * &lt;h:selectBooleanCheckbox value="#{inboxFace.removeEmailUnid[email.unid]}"/&gt;
 */
public abstract class ActionsMap implements Map
{

    private Set keys;

    public ActionsMap()
    {
        // NoOp
    }

    public ActionsMap(Set keys)
    {
        this.keys = keys;
    }

    /**
     * This method should fire the command.
     */
    public abstract void performAction(String command);

    public int size()
    {
        return keys.size();
    }

    public boolean isEmpty()
    {
        return keys.isEmpty();
    }

    public boolean containsKey(Object key)
    {
        return keys.contains( key );
    }

    public boolean containsValue(Object value)
    {
        if( ! (value instanceof Boolean) )
        {
            return false;
        }
        return ((Boolean)value).booleanValue();
    }

    public Object get( Object key)
    {
        return Boolean.FALSE;
    }

    public Boolean put(String key, Boolean value)
    {
        if( value!=null && value.booleanValue() )
        {
            performAction(key);
        }
        return Boolean.FALSE;
    }

    public Object remove(Object key)
    {
        if( keys.remove( key ) )
        {
            return Boolean.FALSE;
        }
        return null;
    }

    public void putAll(Map map)
    {
        Iterator it = map.entrySet().iterator();

        while (it.hasNext())
        {
            Entry entry = (Entry) it.next();
            Object obj = entry.getValue();
            if( (obj instanceof Boolean) && ((Boolean) obj).booleanValue() )
            {
                performAction((String) entry.getKey());
            }
        }
    }

    public void clear()
    {
        keys.clear();
    }

    public Set keySet()
    {
        return keys;
    }

    public Collection values()
    {
        return Collections.nCopies(keys.size(), Boolean.FALSE);
    }

    public Set entrySet()
    {
        Set set = new HashSet( keys.size() );

        Iterator it = keys.iterator();

        while (it.hasNext())
        {
            String command = (String) it.next();
            set.add( new CommandEntry(command) );
        }

        return set;
    }

    private class CommandEntry implements Entry
    {

        private final String command;
        private boolean commandPerformed = false;

        public CommandEntry(String command)
        {
            this.command = command;
        }

        public Object getKey()
        {
            return command;
        }

        public Object getValue()
        {
            return Boolean.valueOf(commandPerformed);
        }

        public Object setValue(Object performCommand)
        {
            if( (performCommand instanceof Boolean) && ((Boolean)performCommand).booleanValue() )
            {
                performAction( command );
                commandPerformed = true;
            }
            return Boolean.valueOf(commandPerformed);
        }
    }
}

