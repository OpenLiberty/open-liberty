
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
/**

<h1> Message Store Interface Design.</h1>
<br>
       &nbsp;The static design for the message store interface is shown below.
   &nbsp;<br>
<img src="doc-files/package-1.gif">
<br>
<br>
<h1>Ordering of event callbacks</h1>
  There are several callback methods on {@link com.ibm.ws.sib.msgstore.AbstractItem}
 that allow implementors of subclasses to react to events generated within
 the message store. &nbsp;The order in which the callbacks occur is obviously
 of interest to the subclass implementor. &nbsp;The following diagrams illustrate
 (but not in complete detail) the order of the callbacks related to the events
 tht drive them. <br>
<h2>Adding at the API</h2>
<img src="doc-files/API.add.gif">
<br>
<h3>Interleaving of adds</h3>
 In general no presumptions can be made about the ordering of events to differrent 
items. &nbsp;In particular:<br>
 If items are added to different streams then the order of events between 
the items is undefined.<br>
 If several items are added to the same stream then the order of events is
undefined.<br>
<br>
 However, there is an implicit ordering within these that is a side effect 
of the implementation. &nbsp;If this ordering is to be relied upon <b>it must
be presented as a requirement or else it is subject to change</b>.<br>
<ul>
  <li>for each priority of each stream an 'Adder' is created for each transaction.</li>
  <li>Each adder will register the adds associated with one transaction,
one stream and one priority, <i>probably</i> in the order that the adds are
made.</li>
  <li>Each adder is registered with the transaction at the time that it is
created.</li>
  <li>Each transaction will evoke events in the adders in the order the adders
or removers were registered</li>
</ul>
 The consequences of the above are that: <br>
<ul>
  <li>For adds to the same stream under the same priority within one transaction,
the event callbacks will occur in the same order as the adds.</li>
  <li>For adds to the same stream with differing priorities within one transaction,
the callbacks will be grouped by priority, but not neccesarily in priority
order. &nbsp;The order of the priority groups will be the order in which
the first of each priority group was added. &nbsp;Within each priority group
the add order will be preserved.</li>
  <li>For adds on the multiple streams with differing priorities within one 
transaction, the callbacks will be grouped by priority, but not neccesarily 
in priority order. &nbsp;The order of the priority groups will be the order
in which the first of each priority group was added. &nbsp;Within each priority
group the order will be preserved. &nbsp;There is no ordering by stream,
so the events between items of different streams will be interleaved (but
still in priority-on-this-stream groups). &nbsp;Another way to view this
is that any ordering is relevant only to priority groups, and there is no
relationship between groups of differring priority or from different streams.<br>
  </li>
</ul>
<br>
<br>
  
<h2>Removing at the API</h2>
<img src="doc-files/API.remove.gif">
<br>
<br>
<br>
<h3>Interleaving of removes</h3>
  In general no presumptions can be made about the ordering of events to
differrent items. &nbsp;In particular:<br>
  If items are added to different streams then the order of events between 
the items is undefined.<br>
  If several items are added to the same stream then the order of events is
undefined.<br>
<br>
  However, there is an implicit ordering within these that is a side effect 
of the implementation. &nbsp;If this ordering is to be relied upon <b>it must
be presented as a requirement or else it is subject to change</b>.<br>
<ul>
  <li>for each priority of each stream a 'Remover' is created for each transaction.</li>
  <li>Each remover will register the removes associated with one transaction,
one stream and one priority, <i>probably</i> in the order that the removes
are made.</li>
  <li>Each&nbsp; remover is registered with the transaction at the time that
it is created.</li>
  <li>Each transaction will evoke events in the removers in the order the
removers were registered</li>
</ul>
  The consequences of the above are that: <br>
<ul>
  <li>For removes from the same stream under the same priority within one
transaction, the event callbacks will occur in the same order as the adds.</li>
  <li>For removes from the same stream with differing priorities within one 
transaction, the callbacks will be grouped by priority, but not neccesarily 
in priority order. &nbsp;The order of the priority groups will be the order
in which the first of each priority group was removed. &nbsp;Within each
priority group the remove order will be preserved.</li>
  <li>For removes from the multiple stream with differing priorities within
one transaction, the callbacks will be grouped by priority, but not neccesarily 
in priority order. &nbsp;The order of the priority groups will be the order
in which the first of each priority group was removed. &nbsp;Within each
priority group the remove order will be preserved. &nbsp;There is no ordering
by stream, so the events between items of different streams will be interleaved
(but still in priority-on-this-stream groups).  &nbsp;Another way to view
this is that any ordering is relevant only to priority groups, and there
is no relationship between groups of differring priority or from different
streams.</li>
</ul>
<br>
<h2>Interleaving of Adds and Removes</h2>
Adders and removers act separately, and can be interleaved. &nbsp;Events
are still grouped by priority-on-a-stream basis, but each group (priorit,
stream, action) will probably be called in the order that the group was first
'created'. &nbsp;Within the groups, the order in which the items became part
of the group will probably be preserved.<br>
<br>

 * @version 1.2.0
 */
@org.osgi.annotation.versioning.Version("1.2.0")
package com.ibm.ws.sib.msgstore;
