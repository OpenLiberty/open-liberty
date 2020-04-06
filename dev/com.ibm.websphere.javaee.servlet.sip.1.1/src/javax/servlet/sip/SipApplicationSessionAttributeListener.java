/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip;

import java.util.EventListener;

/**
 * This listener interface can be implemented in order to get 
 * notifications of changes to the attribute lists of application sessions.
 *
 * @since 1.1
 */
public interface SipApplicationSessionAttributeListener extends EventListener {
	
	/**
	 * Notification that an attribute has been added to an application session. 
	 * Called after the attribute is added.
	 * 
	 * @param ev event identifying the affected SipApplicationSession
	 */
	void attributeAdded(SipApplicationSessionBindingEvent ev);
	
	
	/**
	 * Notification that an attribute has been removed from an application 
	 * session. Called after the attribute is removed.
	 * 
	 * @param ev event identifying the affected SipApplicationSession
	 */
	void attributeRemoved(SipApplicationSessionBindingEvent ev);
	
	
	/**
	 * Notification that an attribute has been replaced in an 
	 * application session. Called after the attribute is replaced.
	 * 
	 * @param ev event identifying the affected SipApplicationSession
	 */
	void attributeReplaced(SipApplicationSessionBindingEvent ev);
}
