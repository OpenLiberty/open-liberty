/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.webapp;

import java.util.EventListener;
import java.util.EventObject;

import com.ibm.websphere.servlet.event.ApplicationEvent;
import com.ibm.websphere.servlet.event.ApplicationListener;
import com.ibm.websphere.servlet.event.FilterErrorEvent;
import com.ibm.websphere.servlet.event.FilterErrorListener;
import com.ibm.websphere.servlet.event.FilterEvent;
import com.ibm.websphere.servlet.event.FilterInvocationEvent;
import com.ibm.websphere.servlet.event.FilterInvocationListener;
import com.ibm.websphere.servlet.event.FilterListener;
import com.ibm.websphere.servlet.event.ServletErrorEvent;
import com.ibm.websphere.servlet.event.ServletErrorListener;
import com.ibm.websphere.servlet.event.ServletEvent;
import com.ibm.websphere.servlet.event.ServletInvocationEvent;
import com.ibm.websphere.servlet.event.ServletInvocationListener;
import com.ibm.websphere.servlet.event.ServletListener;
import com.ibm.ws.webcontainer.util.EventListenerV;
//------- EventListenerV declarations for visiting EventListeners and firing events --------//
//There is a Fire[method name] visitor class for each method in a listener interface.
//The fireEvent() methods are final so that they can be inlined by the compiler for
//performance.

//---------- ServletInvocationListener visitors -------------------------------------//

class FireOnServletStartService implements EventListenerV{
    private static final FireOnServletStartService instance = new FireOnServletStartService();
    private FireOnServletStartService(){}//prevent instances
    public static final FireOnServletStartService instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletInvocationListener)l).onServletStartService((ServletInvocationEvent)evt);
    }
}
class FireOnServletFinishService implements EventListenerV{
    private static final FireOnServletFinishService instance = new FireOnServletFinishService();
    private FireOnServletFinishService(){}//prevent instances
    public static final FireOnServletFinishService instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletInvocationListener)l).onServletFinishService((ServletInvocationEvent)evt);
    }
}

//---------- ServletErrorListener visitors -------------------------------------//
class FireOnServletInitError implements EventListenerV{
    private static final FireOnServletInitError instance = new FireOnServletInitError();
    private FireOnServletInitError(){}//prevent instances
    public static final FireOnServletInitError instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletErrorListener)l).onServletInitError((ServletErrorEvent)evt);
    }
}
class FireOnServletServiceError implements EventListenerV{
    private static final FireOnServletServiceError instance = new FireOnServletServiceError();
    private FireOnServletServiceError(){}//prevent instances
    public static final FireOnServletServiceError instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletErrorListener)l).onServletServiceError((ServletErrorEvent)evt);
    }
}
class FireOnServletDestroyError implements EventListenerV{
    private static final FireOnServletDestroyError instance = new FireOnServletDestroyError();
    private FireOnServletDestroyError(){}//prevent instances
    public static final FireOnServletDestroyError instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletErrorListener)l).onServletDestroyError((ServletErrorEvent)evt);
    }
}
class FireOnServletServiceDenied implements EventListenerV{
    private static final FireOnServletServiceDenied instance = new FireOnServletServiceDenied();
    private FireOnServletServiceDenied(){}//prevent instances
    public static final FireOnServletServiceDenied instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletErrorListener)l).onServletServiceDenied((ServletErrorEvent)evt);
    }
}

//---------- ServletListener visitors -------------------------------------//
class FireOnServletStartInit implements EventListenerV{
    private static final FireOnServletStartInit instance = new FireOnServletStartInit();
    private FireOnServletStartInit(){}//prevent instances
    public static final FireOnServletStartInit instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletListener)l).onServletStartInit((ServletEvent)evt);
    }
}
class FireOnServletFinishInit implements EventListenerV{
    private static final FireOnServletFinishInit instance = new FireOnServletFinishInit();
    private FireOnServletFinishInit(){}//prevent instances
    public static final FireOnServletFinishInit instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletListener)l).onServletFinishInit((ServletEvent)evt);
    }
}
class FireOnServletStartDestroy implements EventListenerV{
    private static final FireOnServletStartDestroy instance = new FireOnServletStartDestroy();
    private FireOnServletStartDestroy(){}//prevent instances
    public static final FireOnServletStartDestroy instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletListener)l).onServletStartDestroy((ServletEvent)evt);
    }
}
class FireOnServletFinishDestroy implements EventListenerV{
    private static final FireOnServletFinishDestroy instance = new FireOnServletFinishDestroy();
    private FireOnServletFinishDestroy(){}//prevent instances
    public static final FireOnServletFinishDestroy instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletListener)l).onServletFinishDestroy((ServletEvent)evt);
    }
}
class FireOnServletAvailableForService implements EventListenerV{
    private static final FireOnServletAvailableForService instance = new FireOnServletAvailableForService();
    private FireOnServletAvailableForService(){}//prevent instances
    public static final FireOnServletAvailableForService instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletListener)l).onServletAvailableForService((ServletEvent)evt);
    }
}
class FireOnServletUnavailableForService implements EventListenerV{
    private static final FireOnServletUnavailableForService instance = new FireOnServletUnavailableForService();
    private FireOnServletUnavailableForService(){}//prevent instances
    public static final FireOnServletUnavailableForService instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletListener)l).onServletUnavailableForService((ServletEvent)evt);
    }
}
class FireOnServletUnloaded implements EventListenerV{
    private static final FireOnServletUnloaded instance = new FireOnServletUnloaded();
    private FireOnServletUnloaded(){}//prevent instances
    public static final FireOnServletUnloaded instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ServletListener)l).onServletUnloaded((ServletEvent)evt);
    }
}

//---------- ApplicationListener visitors -------------------------------------//
class FireOnApplicationStart implements EventListenerV{
    private static final FireOnApplicationStart instance = new FireOnApplicationStart();
    private FireOnApplicationStart(){}//prevent instances
    public static final FireOnApplicationStart instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ApplicationListener)l).onApplicationStart((ApplicationEvent)evt);
    }
}
class FireOnApplicationEnd implements EventListenerV{
    private static final FireOnApplicationEnd instance = new FireOnApplicationEnd();
    private FireOnApplicationEnd(){}//prevent instances
    public static final FireOnApplicationEnd instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ApplicationListener)l).onApplicationEnd((ApplicationEvent)evt);
    }
}
class FireOnApplicationAvailableForService implements EventListenerV{
    private static final FireOnApplicationAvailableForService instance = new FireOnApplicationAvailableForService();
    private FireOnApplicationAvailableForService(){}//prevent instances
    public static final FireOnApplicationAvailableForService instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ApplicationListener)l).onApplicationAvailableForService((ApplicationEvent)evt);
    }
}
class FireOnApplicationUnavailableForService implements EventListenerV{
    private static final FireOnApplicationUnavailableForService instance = new FireOnApplicationUnavailableForService();
    private FireOnApplicationUnavailableForService(){}//prevent instances
    public static final FireOnApplicationUnavailableForService instance(){ return instance; }
    public final void fireEvent(EventObject evt, EventListener l){
        ((ApplicationListener)l).onApplicationUnavailableForService((ApplicationEvent)evt);
    }
}

// begin LIDB-3598 added support for FilterInvocationListeners
//292460:    begin resolve issues concerning LIDB-3598    WASCC.web.webcontainer: redid this portion.
//---------- FilterListener visitors -------------------------------------//
class FireOnFilterFinishDestroy implements EventListenerV{
	private FireOnFilterFinishDestroy(){}
	public static final FireOnFilterFinishDestroy instance(){ return instance; }
	public final void fireEvent(EventObject evt, EventListener l){ ((FilterListener)l).onFilterFinishDestroy((FilterEvent)evt); }
	private static final FireOnFilterFinishDestroy instance = new FireOnFilterFinishDestroy();
}

class FireOnFilterFinishInit implements EventListenerV{
	private FireOnFilterFinishInit(){}
	public static final FireOnFilterFinishInit instance() {  return instance; }
	public final void fireEvent(EventObject evt, EventListener l) {  ((FilterListener)l).onFilterFinishInit((FilterEvent)evt);}
	private static final FireOnFilterFinishInit instance = new FireOnFilterFinishInit(); 
}
class FireOnFilterStartDestroy implements EventListenerV {
	private FireOnFilterStartDestroy() {}
	public static final FireOnFilterStartDestroy instance() {   return instance;}
	public final void fireEvent(EventObject evt, EventListener l) {  ((FilterListener)l).onFilterStartDestroy((FilterEvent)evt); }
	private static final FireOnFilterStartDestroy instance = new FireOnFilterStartDestroy(); 
}
class FireOnFilterStartInit implements EventListenerV{
	private FireOnFilterStartInit() {}
	public static final FireOnFilterStartInit instance() {   return instance;}
	public final void fireEvent(EventObject evt, EventListener l){     ((FilterListener)l).onFilterStartInit((FilterEvent)evt);}
	private static final FireOnFilterStartInit instance = new FireOnFilterStartInit();
}

//---------- FilterInvocationListener visitors -------------------------------------//
class FireOnFilterStartDoFilter implements EventListenerV{
	private FireOnFilterStartDoFilter() { }
	public static final FireOnFilterStartDoFilter instance() {     return instance; }
	public final void fireEvent(EventObject evt, EventListener l) {     ((FilterInvocationListener)l).onFilterStartDoFilter((FilterInvocationEvent)evt); }
	private static final FireOnFilterStartDoFilter instance = new FireOnFilterStartDoFilter();
}

class FireOnFilterFinishDoFilter implements EventListenerV{
	private FireOnFilterFinishDoFilter(){}
	public static final FireOnFilterFinishDoFilter instance() {   return instance; }
	public final void fireEvent(EventObject evt, EventListener l) { ((FilterInvocationListener)l).onFilterFinishDoFilter((FilterInvocationEvent)evt); }
	private static final FireOnFilterFinishDoFilter instance = new FireOnFilterFinishDoFilter(); 
}

//---------- FilterErrorListener visitors -------------------------------------//
class FireOnFilterInitError implements EventListenerV{
	private FireOnFilterInitError(){}
	public static final FireOnFilterInitError instance(){ return instance; }
	public final void fireEvent(EventObject evt, EventListener l){ ((FilterErrorListener)l).onFilterInitError((FilterErrorEvent)evt); }
	private static final FireOnFilterInitError instance = new FireOnFilterInitError();
}
class FireOnFilterDestroyError implements EventListenerV{
	private FireOnFilterDestroyError(){}
	public static final FireOnFilterDestroyError instance(){ return instance; }
	public final void fireEvent(EventObject evt, EventListener l){ ((FilterErrorListener)l).onFilterDestroyError((FilterErrorEvent)evt); }
	private static final FireOnFilterDestroyError instance = new FireOnFilterDestroyError();
}
class FireOnFilterDoFilterError implements EventListenerV{
	private FireOnFilterDoFilterError(){}
	public static final FireOnFilterDoFilterError instance(){ return instance; }
	public final void fireEvent(EventObject evt, EventListener l){ ((FilterErrorListener)l).onFilterDoFilterError((FilterErrorEvent)evt); }
	private static final FireOnFilterDoFilterError instance = new FireOnFilterDoFilterError();
}
//292460:    end resolve issues concerning LIDB-3598    WASCC.web.webcontainer: redid this portion.
//end LIDB-3598 added support for FilterInvocationListeners
