/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip.header;

/**
 * This interface represents the Route request-header.
 * RouteHeaders determine the route taken by a
 * Request. Each host removes the first entry and then
 * proxies the Request to the host listed in that entry,
 * also using it as the RequestURI. The operation is further
 * described in RecordRouteHeader.
 *
 * @see RecordRouteHeader
 *
 * @version 1.0
 *
 */
public interface RouteHeader extends NameAddressHeader
{
    
    /**
     * Name of RouteHeader
     */
    public final static String name = "Route";
}
