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
package com.ibm.ws.webcontainer.webapp;
import com.ibm.websphere.servlet.event.ApplicationEvent;
import com.ibm.websphere.servlet.event.ApplicationListener;
import com.ibm.websphere.servlet.event.FilterErrorEvent;
import com.ibm.websphere.servlet.event.FilterErrorListener;
import com.ibm.websphere.servlet.event.FilterEvent;
import com.ibm.websphere.servlet.event.FilterInvocationEvent;
import com.ibm.websphere.servlet.event.FilterInvocationListener;
import com.ibm.websphere.servlet.event.FilterListener;
import com.ibm.websphere.servlet.event.ServletContextEventSource;
import com.ibm.websphere.servlet.event.ServletErrorEvent;
import com.ibm.websphere.servlet.event.ServletErrorListener;
import com.ibm.websphere.servlet.event.ServletEvent;
import com.ibm.websphere.servlet.event.ServletInvocationEvent;
import com.ibm.websphere.servlet.event.ServletInvocationListener;
import com.ibm.websphere.servlet.event.ServletListener;
import com.ibm.ws.webcontainer.util.EventListeners;


/**
 * Implementation of ServletContextEventSource.
 * Implementation can be registered as an EventListener an object. When an
 * event is triggered, the event will propogated to the corresponding list
 * of listeners that have been registered with this object.
 */
public final class WebAppEventSource implements
    ServletListener,
    ServletErrorListener,
    ServletInvocationListener,
    ApplicationListener,
    ServletContextEventSource,
    FilterInvocationListener{		//     LIDB-3598 added support for FilterInvocationListener 
    private EventListeners _invocationListeners;
    private EventListeners _servletListeners;
    private EventListeners _errorListeners;
    private EventListeners _applicationListeners;
    private EventListeners _filterInvocationListeners;
    private EventListeners _filterListeners;
    private EventListeners _filterErrorListeners;

    public WebAppEventSource(){
        _invocationListeners = new EventListeners();
        _servletListeners = new EventListeners();
        _errorListeners = new EventListeners();
        _applicationListeners = new EventListeners();
        _filterListeners = new EventListeners();		//     LIDB-3598 added support for FilterInvocationListener
		//292460:    begin resolve issues concerning LIDB-3598    WASCC.web.webcontainer
        _filterErrorListeners = new EventListeners();		
        _filterInvocationListeners = new EventListeners();		
		//292460:    end resolve issues concerning LIDB-3598    WASCC.web.webcontainer
    }

    //application listener
    public final void addApplicationListener(ApplicationListener al){
        _applicationListeners.addListener(al);
    }

    public final void removeApplicationListener(ApplicationListener al){
        _applicationListeners.removeListener(al);
    }

    public final void onApplicationStart(ApplicationEvent evt){
        _applicationListeners.fireEvent(evt, FireOnApplicationStart.instance());
    }

    public final void onApplicationEnd(ApplicationEvent evt){
        _applicationListeners.fireEvent(evt, FireOnApplicationEnd.instance());
    }

    public final void onApplicationAvailableForService(ApplicationEvent evt){
        _applicationListeners.fireEvent(evt, FireOnApplicationAvailableForService.instance());
    }

    public final void onApplicationUnavailableForService(ApplicationEvent evt){
        _applicationListeners.fireEvent(evt, FireOnApplicationUnavailableForService.instance());
    }

    //servlet invocation listener
    public final void addServletInvocationListener(ServletInvocationListener sil){
        _invocationListeners.addListener(sil);
    }

    public final void removeServletInvocationListener(ServletInvocationListener sil){
        _invocationListeners.removeListener(sil);
    }

    public final void onServletStartService(ServletInvocationEvent evt){
        _invocationListeners.fireEvent(evt, FireOnServletStartService.instance());
    }

    public final void onServletFinishService(ServletInvocationEvent evt){
        _invocationListeners.fireEvent(evt, FireOnServletFinishService.instance());
    }

    //servlet listener
    public final void addServletListener(ServletListener sl){
        _servletListeners.addListener(sl);
    }

    public final void removeServletListener(ServletListener sl){
        _servletListeners.removeListener(sl);
    }

    public final void onServletStartInit(ServletEvent evt){
        _servletListeners.fireEvent(evt, FireOnServletStartInit.instance());
    }

    public final void onServletFinishInit(ServletEvent evt){
        _servletListeners.fireEvent(evt, FireOnServletFinishInit.instance());
    }

    public final void onServletStartDestroy(ServletEvent evt){
        _servletListeners.fireEvent(evt, FireOnServletStartDestroy.instance());
    }

    public final void onServletFinishDestroy(ServletEvent evt){
        _servletListeners.fireEvent(evt, FireOnServletFinishDestroy.instance());
    }

    public final void onServletAvailableForService(ServletEvent evt){
        _servletListeners.fireEvent(evt, FireOnServletAvailableForService.instance());
    }

    public final void onServletUnavailableForService(ServletEvent evt){
        _servletListeners.fireEvent(evt, FireOnServletUnavailableForService.instance());
    }

    public final void onServletUnloaded(ServletEvent evt){
        _servletListeners.fireEvent(evt, FireOnServletUnloaded.instance());
    }

    //servlet error listener
    public final void addServletErrorListener(ServletErrorListener sel){
        _errorListeners.addListener(sel);
    }

    public final void removeServletErrorListener(ServletErrorListener sel){
        _errorListeners.removeListener(sel);
    }

    public final void onServletInitError(ServletErrorEvent evt){
        _errorListeners.fireEvent(evt, FireOnServletInitError.instance());
    }

    public final void onServletDestroyError(ServletErrorEvent evt){
        _errorListeners.fireEvent(evt, FireOnServletDestroyError.instance());
    }

    public final void onServletServiceError(ServletErrorEvent evt){
        _errorListeners.fireEvent(evt, FireOnServletServiceError.instance());
    }

    public final void onServletServiceDenied(ServletErrorEvent evt){
        _errorListeners.fireEvent(evt, FireOnServletServiceDenied.instance());
    }

    public final boolean hasServletInvocationListeners(){
        return _invocationListeners.getListenerCount() != 0;
    }

    public final boolean hasServletErrorListeners(){
        return _errorListeners.getListenerCount() != 0;
    }

    public final boolean hasApplicationListeners(){
        return _applicationListeners.getListenerCount() != 0;
    }

    public final boolean hasServletListeners(){
        return _servletListeners.getListenerCount() != 0;
    }

    
    // begin LIDB-3598 added support for FilterInvocationListeners
    public final boolean hasFilterInvocationListeners()
    {
        return _filterInvocationListeners.getListenerCount() != 0;
    }

	//292460:    begin resolve issues concerning LIDB-3598    WASCC.web.webcontainer: redid this portion.
    public void onFilterStartDoFilter(FilterInvocationEvent evt)
    {
        _filterInvocationListeners.fireEvent(evt, FireOnFilterStartDoFilter.instance());
    }

    public void onFilterFinishDoFilter(FilterInvocationEvent evt)
    {
        _filterInvocationListeners.fireEvent(evt, FireOnFilterFinishDoFilter.instance());
    }

    public void addFilterInvocationListener(FilterInvocationListener fil)
    {
        _filterInvocationListeners.addListener(fil);
    }

    public void removeFilterInvocationListener(FilterInvocationListener fil)
    {
        _filterInvocationListeners.removeListener(fil);
    }


    public final boolean hasFilterListeners()
    {
        return _filterListeners.getListenerCount() != 0;
    }

    public void onFilterStartInit(FilterEvent evt)
    {
        _filterListeners.fireEvent(evt, FireOnFilterStartInit.instance());
    }

    public void onFilterFinishInit(FilterEvent evt)
    {
        _filterListeners.fireEvent(evt, FireOnFilterFinishInit.instance());
    }

    public void onFilterStartDestroy(FilterEvent evt)
    {
        _filterListeners.fireEvent(evt, FireOnFilterStartDestroy.instance());
    }

    public void onFilterFinishDestroy(FilterEvent evt)
    {
        _filterListeners.fireEvent(evt, FireOnFilterFinishDestroy.instance());
    }

    public void addFilterListener(FilterListener fil)
    {
        _filterListeners.addListener(fil);
    }

    public void removeFilterListener(FilterListener fil)
    {
        _filterListeners.removeListener(fil);
    }
    
    public final boolean hasFilterErrorListeners()
    {
        return _filterErrorListeners.getListenerCount() != 0;
    }
    
    public void onFilterInitError(FilterErrorEvent evt)
    {
        _filterErrorListeners.fireEvent(evt, FireOnFilterInitError.instance());
    }
    public void onFilterDestroyError(FilterErrorEvent evt)
    {
        _filterErrorListeners.fireEvent(evt, FireOnFilterDestroyError.instance());
    }
    public void onFilterDoFilterError(FilterErrorEvent evt)
    {
        _filterErrorListeners.fireEvent(evt, FireOnFilterDoFilterError.instance());
    }
    
    public void addFilterErrorListener(FilterErrorListener fil)
    {
        _filterErrorListeners.addListener(fil);
    }

    public void removeFilterErrorListener(FilterErrorListener fil)
    {
    	_filterErrorListeners.removeListener(fil);
    }
	//292460:    end resolve issues concerning LIDB-3598    WASCC.web.webcontainer: redid this portion.
    // end LIDB-3598 added support for FilterInvocationListeners
    
    public EventListeners getApplicationListeners() {
        return _applicationListeners;
    }
    public EventListeners getErrorListeners() {
        return _errorListeners;
    }
    public EventListeners getFilterErrorListeners() {
        return _filterErrorListeners;
    }
    public EventListeners getFilterInvocationListeners() {
        return _filterInvocationListeners;
    }
    public EventListeners getFilterListeners() {
        return _filterListeners;
    }
    public EventListeners getInvocationListeners() {
        return _invocationListeners;
    }
    public EventListeners getServletListeners() {
        return _servletListeners;
    }
}


