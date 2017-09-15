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

/*
 * This class is the main TRM class an implementation of which is called by
 * Admin during startup and shutdown of each Messaging Engine
 */

package com.ibm.ws.sib.trm;

import com.ibm.ws.sib.admin.JsEngineComponentWithEventListener;                                               //206161.6
import com.ibm.ws.sib.admin.JsMonitoredComponent;
import com.ibm.ws.sib.trm.contact.CommsErrorListener;
import com.ibm.ws.sib.trm.dlm.DestinationLocationManager;
import com.ibm.ws.sib.trm.links.LinkManager;
import com.ibm.ws.sib.trm.links.ibl.InterBusLinkManager;
import com.ibm.ws.sib.trm.links.mql.MQLinkManager;
import com.ibm.ws.sib.trm.topology.RoutingManager;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
//import com.ibm.wsspi.cluster.Identity;
import java.util.Map;

public interface TrmMeMain extends JsEngineComponentWithEventListener, JsMonitoredComponent {                 //206161.6

 /**
  * Return a reference to the topology routing & management Comms Error Listener
  * object.
  *
  * @return The CommsErrorListener for topology routing & management
  */

 public CommsErrorListener getCommsErrorListener ();

 /**
  *
  * Return a reference to the topology routing & management Routing Manager
  * object.
  *
  * @return The RoutingManager for topology routing & management
  */

 public RoutingManager getRoutingManager ();

 /**
  * Return a reference to the topology routing & management Destination Location
  * Manager (DLM) object
  *
  * @return The DestinationLocationManager for topology routing & management
  */

  public DestinationLocationManager getDestinationLocationManager ();

 /**
  * Map a messaging engine name to a messaging engine UUID. Only active
  * messaging engines in the current bus can be mapped. TRM must be started
  * as returned by the isStarted() method otherwise null will be returned.
  *
  * @param name name of the messaging engine
  *
  * @return SIBUuid8 of the messaging engine or null if the messaging engine
  * name could not be mapped.
  */

 public SIBUuid8 getMeUuid (String name);
 
 /**
  * Return a boolean indicating whether topology routing & management has
  * completed starting or not.
  *
  * @return true if started otherwise false
  */

 public boolean isStarted ();

 /**
  * Get a reference to the generic Link Manager
  *
  * @return Reference to the generic link manager
  */

 public LinkManager getLinkManager ();

 /**
  * Get a reference to the inter-bus Link Manager
  *
  * @return Reference to the inter-bus link manager
  */

 public InterBusLinkManager getInterBusLinkManager ();

 /**
  * Get a reference to the MQ Link Manager
  *
  * @return Reference to the MQ link manager
  */

 public MQLinkManager getMQLinkManager ();

 /**
  * Dump information for all resources according to argument
  *
  * @param fw The formatted write the dump to
  *
  * @param arg Dump argument string
  */

 public void dump (FormattedWriter fw, String arg);

 /**
  * Return formatted dump information for all resources
  *
  * @return Formatted dump information
  */

 public String dump ();

 /**
  * Return formatted dump information for the specified cluster
  *
  * @return Formatted dump information
  */

 public String dump (Map m);

 /**
  * Dump formatted Neighbourhood information
  *
  * @return Formatted neighbourhood information
  */

 public String dumpNeighbourhood ();

 /**
  * Return Identity for ME UUID 
  * 
  * @return Identity
  
 
 public Identity getAffinityKey ();*/
}
