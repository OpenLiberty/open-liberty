/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2015 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
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
