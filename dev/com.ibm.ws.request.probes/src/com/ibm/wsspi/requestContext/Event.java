/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


package com.ibm.wsspi.requestContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Event {

    private volatile String type;
    private volatile Object contextInfo;
    private volatile long startTime;
    private volatile long endTime;
    private volatile List<Event> childEvents = new ArrayList<Event>();
    private volatile Event parentEvent;

    public Event() {}
    
    
    public Event(String type, Object contextInfo) {
        this.type = type;
        this.contextInfo = contextInfo;
    }

    
    public Event(String type) {
        this.type = type;
    }

    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getContextInfo() {
        return contextInfo;
    }

    public void setContextInfo(Object contextInfo) {
        this.contextInfo = contextInfo;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public List<Event> getChildEvents() {
        return Collections.unmodifiableList(childEvents);
    }

    public void setChildEvents(List<Event> childEvents) {
        this.childEvents = childEvents;
    }

    public Event getParentEvent() {
        return parentEvent;
    }

    public void setParentEvent(Event parentEvent) {
        this.parentEvent = parentEvent;
    }
    
    public void addChild(Event currentEvent) {
    	
    	if(childEvents == null) {
			List<Event> events = new ArrayList<Event>();
			events.add(currentEvent);
			this.setChildEvents(events);
		}else {
			childEvents.add(currentEvent);
		}
		
    }

	@Override
	public String toString() {
		return "Event [eventType=" + ((type != null) ? type : null) 
				+ ", contextInfo=" + ((contextInfo != null) ? contextInfo : null)
				+ ", startTime=" + startTime + ", endTime=" + endTime + "]";
	}
    
}
