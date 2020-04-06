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
 * Listener interface implemented by SIP servlet applications using timers.
 * 
 * <p>The application specifies an implementation of this interface
 * in a <code>listener</code> element of the SIP deployment descriptor.
 * There may be at most one <code>TimerListener</code> defined.
 * 
 * @see TimerService
 */
public interface TimerListener extends EventListener {
    /**
     * Notifies the listener that the specified timer has expired.
     * 
     * @param timer the timer that has expired
     */
    void timeout(ServletTimer timer);
}
