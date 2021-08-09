/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;
import java.util.EventListener;
import java.util.EventObject;

/**
 * An event listener visitor is capable of firing an EventObject to an EventListener.
 * This interface is used by the EventListeners object to perform the operation
 * of firing an event to each listener in its list using the
 * EventListeners.fireEvent(Event evt, EventListenerV v) method.
 * Implementations of this class are typically Singletons.
 * Each method of a EventListener's interface will have its own Singleton class that is
 * responsible for firing the event.
 *
 * <H3>Sample visitor implementation</H3>
 * <pre>
   //This class triggers the actionPerformed() method on a ActionListener.
   class ActionPerformed implements EventListenerV{
      private static ActionPerformed singleton = new ActionPerformed();
      private ActionPerformed(){
         //prevent instance from being created using the 'new' operator.
      }
      //get the singleton instance
      public static ActionPerformed instance(){
         return singleton;
      }
      public void fireEvent(EventObject evt, EventListener l){
         //cast the event to the expected event.
         ActionEvent aEvt = (ActionEvent)evt;
         
         //cast the listener to the expected listener.
         ActionListener al = (ActionListener)l;
         
         //fire the event using the appropriate method on the listener.
         al.actionPerformed(aEvt);
      }
   }
   </pre>
   sample usage for firing an actionPerformed event to a list of ActionListeners:
   </pre>
   eventListeners.fireEvent(new ActionEvent(source, id, command), ActionPerformed.instance());
   </pre>
 */
public interface EventListenerV{
    public void fireEvent(EventObject evt, EventListener l);
}
