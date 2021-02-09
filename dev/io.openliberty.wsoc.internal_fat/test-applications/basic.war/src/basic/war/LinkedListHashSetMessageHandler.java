/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war;

import java.util.HashSet;
import java.util.LinkedList;

import javax.websocket.MessageHandler;

/**
 *
 */
public class LinkedListHashSetMessageHandler implements MessageHandler.Whole<LinkedList<HashSet<String>>> {

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.MessageHandler.Whole#onMessage(java.lang.Object)
     */
    @Override
    public void onMessage(LinkedList<HashSet<String>> arg0) {
        // TODO Auto-generated method stub

    }

}
