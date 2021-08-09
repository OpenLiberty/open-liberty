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
package com.ibm.ws.security.notifications;

/**
 * Components desiring to receive notifications for changes in other
 * security components need to implement this interface. The notifying
 * component must declare the listeners in its bnd file. For example,
 * <pre>
 * com.ibm.ws.security.MyChangeNotifier; \
 * implementation:=com.ibm.ws.security.MyChangeNotifier; \
 * provide:='com.ibm.ws.security.MyChangeNotifier'; \
 * activate:=activate; \
 * deactivate:=deactivate; \
 * configuration-policy:=ignore; \
 * immediate:=true; \
 * changeListener=com.ibm.ws.security.notifications.SecurityChangeListener; \
 * optional:='changeListener'; \
 * multiple:='changeListener'; \
 * dynamic:='changeListener'; \
 * properties:='service.vendor=IBM'
 * </pre>
 */
public interface SecurityChangeListener {

    /**
     * Callback method invoked by a BaseSecurityChangeNotifier object when there
     * is change the listener is interested in.
     */
    void notifyChange();

}
