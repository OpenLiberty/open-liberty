/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.pmi.wire;

import com.ibm.websphere.pmi.*;
import java.util.ArrayList;

public class WpdCollectionImpl implements WpdCollection, PmiConstants {
    private static final long serialVersionUID = -3280621639331246909L;
    // Note: we use public members here so that perfServer/CachedCollection
    //       can extend it and use these members.
    public String name;
    public int type;
    public int instrumentationLevel = LEVEL_UNDEFINED;
    public ArrayList dataMembers;
    public ArrayList subCollections;

    // Constructor: pass name only
    public WpdCollectionImpl(String name, int type) {
        this(name, type, LEVEL_UNDEFINED, null, null);
    }

    public WpdCollectionImpl(String name, int type, int level) {
        this(name, type, level, null, null);
    }

    // constructor: pass both name and members 
    // Note: collection hierarchy: node->server->module->collections
    public WpdCollectionImpl(String name, int type, int level, ArrayList dataMembers, ArrayList subCollections) {
        if (name == null)
            name = "Undefined";
        this.name = name;
        if (type == TYPE_NODE || type == TYPE_SERVER || type == TYPE_MODULE)
            this.type = type;
        else
            this.type = TYPE_COLLECTION;
        this.instrumentationLevel = level;
        this.dataMembers = dataMembers;
        this.subCollections = subCollections;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    // get the instrumentation level
    public int getLevel() {
        return instrumentationLevel;
    }

    // set the instrumentation level
    public void setLevel(int level) {
        instrumentationLevel = level;
    }

    public String toXML() {
        String result = null;
        String endTag = null;
        if (type == TYPE_NODE) {
            result = XML_NODE;
            endTag = XML_ENDNODE;
        } else if (type == TYPE_SERVER) {
            result = XML_SERVER;
            endTag = XML_ENDSERVER;
        } else if (type == TYPE_MODULE) {
            result = XML_MODULE;
            endTag = XML_ENDMODULE;
        } else {
            result = XML_COLLECTION;
            endTag = XML_ENDCOLLECTION;
        }

        result += XML_NAME + name + XML_ENDLINE;
        if (dataMembers != null) {
            for (int i = 0; i < dataMembers.size(); i++) {
                result += ((WpdData) dataMembers.get(i)).toXML();
            }
        }
        if (subCollections != null) {
            for (int i = 0; i < subCollections.size(); i++) {
                result += ((WpdCollection) subCollections.get(i)).toXML();
            }
        }

        result += endTag;

        return result;
    }

    // set data members - override old setting
    public void setDataMembers(ArrayList dataMembers) {
        this.dataMembers = dataMembers;
    }

    // set subcollections - override old setting
    public void setSubcollections(ArrayList subCollections) {
        this.subCollections = subCollections;
    }

    public ArrayList dataMembers() {
        return dataMembers;
    }

    public ArrayList subCollections() {
        return subCollections;
    }

    public synchronized boolean add(WpdData newMember) {
        if (dataMembers == null) {
            dataMembers = new ArrayList();
        }
        if (newMember == null)
            return false;
        // TODO: do we need to check if the data is already there?
        dataMembers.add(newMember);
        return true;
    }

    public synchronized boolean add(WpdCollection newMember) {
        if (subCollections == null) {
            subCollections = new ArrayList();
        }
        if (newMember == null)
            return false;
        // TODO: do we need to check if the data is already there?
        subCollections.add(newMember);
        return true;
    }

    public synchronized boolean remove(int dataId) {
        if ((dataMembers == null) || (dataMembers.size() <= 0))
            return false;
        for (int i = dataMembers.size() - 1; i >= 0; i--) {
            if (((WpdData) dataMembers.get(i)).getId() == dataId) {
                dataMembers.remove(i);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean remove(String name) {
        if ((subCollections == null) || (subCollections.size() <= 0))
            return false;
        for (int i = subCollections.size() - 1; i >= 0; i--) {
            WpdCollection collection = (WpdCollection) subCollections.get(i);
            if (collection.getName().equals(name)) {
                subCollections.remove(i);
                return true;
            }
        }
        return false;
    }

    public WpdData getData(int dataId) {
        if ((dataMembers == null) || (dataMembers.size() <= 0))
            return null;
        for (int i = 0; i < dataMembers.size(); i++) {
            WpdData data = (WpdData) dataMembers.get(i);
            if (data.getId() == dataId)
                return data;
        }
        return null;
    }

    public WpdCollection getSubcollection(String name) {
        if ((subCollections == null) || (subCollections.size() <= 0))
            return null;
        for (int i = 0; i < subCollections.size(); i++) {
            WpdCollection collection = (WpdCollection) subCollections.get(i);
            if (collection.getName().equals(name))
                return collection;
        }
        return null;
    }

    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        String myIndent = indent + "  ";
        String typeString = null;

        switch (type) {
            case TYPE_ROOT:
                typeString = "PMIROOT";
                break;
            case TYPE_NODE:
                typeString = "NODE";
                break;
            case TYPE_SERVER:
                typeString = "SERVER";
                break;
            case TYPE_MODULE:
                typeString = "MODULE";
                break;
            case TYPE_SUBMODULE:
                typeString = "SUBMODULE";
                break;
            case TYPE_INSTANCE:
                typeString = "INSTANCE";
                break;
            case TYPE_SUBINSTANCE:
                typeString = "SUBINSTANCE";
                break;
            case TYPE_COLLECTION:
                typeString = "COLLECTION";
                break;
            default:
                typeString = "WRONG_TYPE";
                break;
        }

        String res = indent + "WpdCollection name=" + name + " type=" + typeString + "\n";

        // write data members first
        if (dataMembers != null) {
            for (int i = 0; i < dataMembers.size(); i++) {
                WpdData data = (WpdData) dataMembers.get(i);
                res += myIndent + data.toString() + "\n";
            }
        }

        // write subCollections next
        if (subCollections != null) {
            for (int i = 0; i < subCollections.size(); i++) {
                WpdCollection col = (WpdCollection) subCollections.get(i);
                res += col.toString(myIndent);
            }
        }

        return res;
    }
}
