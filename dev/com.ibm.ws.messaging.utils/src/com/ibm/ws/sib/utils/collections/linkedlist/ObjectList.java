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
package com.ibm.ws.sib.utils.collections.linkedlist;


/**
 * This class makes use of the linked list by extending the link
 * to contain a generic object.  It also acts as an example of how
 * to specialize the linked list.
 * 
 * @author drphill
 *
 */
public class ObjectList extends LinkedList {

    public static class ObjectListLink extends Link {
        private final Object _storedObject;

        private ObjectListLink(Object object) {
            super();
            _storedObject = object;
        }

        // reply the stored object
        public final Object getObject() {
            return _storedObject;
        }
    }

    /**
     * Add an object to the end of the linked list.  A new link
     * is created for the object. 
     * @param object
     */
    public final void addObject(Object object) {
        append(new ObjectListLink(object));
    }

    public final synchronized Object removeFirst() {
        Object object = null;
        ObjectListLink link = (ObjectListLink)getHead();
        if (null != link) {
            object = link.getObject();
            link.unlink();
        }
        return object;
    }

}
