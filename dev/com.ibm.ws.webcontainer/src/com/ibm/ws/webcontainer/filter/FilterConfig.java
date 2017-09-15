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
package com.ibm.ws.webcontainer.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.servlet.ServletConfig;
import com.ibm.ws.webcontainer.servlet.TargetConfig;
import com.ibm.ws.webcontainer.util.ArrayEnumeration;
import com.ibm.ws.webcontainer.util.EmptyEnumeration;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.filter.IFilterMapping;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class FilterConfig extends TargetConfig implements IFilterConfig {
    
    private static final TraceComponent tc = Tr.register(FilterConfig.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private DispatcherType[] dispatchMode;

    // begin 296658 allow FilterConfig to override the default classloader used
    // WASCC.web.webcontainer
    private ClassLoader filterClassLoader = null;
    // end 296658 allow FilterConfig to override the default classloader used
    // WASCC.web.webcontainer

    private boolean isInternal = false;

    private WebAppConfig webAppConfig;

    private Filter filter;

    private Class<? extends Filter> filterClass;

    private List<String> urlPatternMappings;

    private List<String> servletNameMappings;

    protected static TraceNLS nls = TraceNLS.getTraceNLS(ServletConfig.class, "com.ibm.ws.webcontainer.resources.Messages");

    public FilterConfig(String id, WebAppConfig webAppConfig) {
        super(id);
        this.name = id;
        this.setDisplayName(id);
        this.webAppConfig = webAppConfig;
        this.dispatchMode = null;
    }

    /**
     * @return String
     */
    public String getFilterName() {
        return name;
    }

    /**
     * @return java.util.Enumeration
     */
    @SuppressWarnings("unchecked")
    public Enumeration getInitParameterNames() {
        if (initParams == null)
            return EmptyEnumeration.instance();
        return new ArrayEnumeration(initParams.keySet().toArray());
    }

    public void loadFrom(Object o) {

    }

//    public static final int FILTER_REQUEST = 0;
//    public static final int FILTER_FORWARD = 1;
//    public static final int FILTER_INCLUDE = 2;
//    public static final int FILTER_ERROR = 3;
    
	public void setDispatchMode(int[] dispatchModeInts) {
		//convert to DispatcherType
		dispatchMode = new DispatcherType [dispatchModeInts.length];
		
		for (int i=0;i<dispatchModeInts.length;i++){
			 int dispatchModeCur=dispatchModeInts[i];
			 switch (dispatchModeCur) {
			 	case 0:  dispatchMode[i]=DispatcherType.REQUEST; break;
	            case 1:  dispatchMode[i]=DispatcherType.FORWARD; break;
	            case 2:  dispatchMode[i]=DispatcherType.INCLUDE; break;
	            case 3:  dispatchMode[i]=DispatcherType.ERROR; break;
	            default: dispatchMode[i]=DispatcherType.REQUEST;break;
	        }

		}
    }

    public void setDispatchType(DispatcherType[] dispatchMode) {
        this.dispatchMode = dispatchMode;
    }

    public DispatcherType[] getDispatchType() {
        return dispatchMode;
    }

    // begin 296658 allow FilterConfig to override the default classloader used
    // WASCC.web.webcontainer
    public ClassLoader getFilterClassLoader() {
        return this.filterClassLoader;
    }

    public void setFilterClassLoader(ClassLoader filterClassLoader) {
        this.filterClassLoader = filterClassLoader;
    }

    // end 296658 allow FilterConfig to override the default classloader used
    // WASCC.web.webcontainer

    public boolean isInternal() {
        return isInternal;
    }

    public void setInternal(boolean isInternal) {
        this.isInternal = isInternal;

    }

    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames)
            throws IllegalStateException, IllegalArgumentException {

        if (servletNames == null) {
            throw new IllegalArgumentException(nls.getString("add.filter.mapping.to.null.servlet.names"));
        } else if (servletNames.length == 0) {
            throw new IllegalArgumentException(nls.getString("add.filter.mapping.to.empty.servlet.names"));
        }
        
        if (this.context!=null&&this.context.isInitialized()) {
            throw new IllegalStateException(nls.getString("Not.in.servletContextCreated"));
        }
        
        if (this.servletNameMappings==null){
            this.servletNameMappings = new ArrayList<String>();
        }


        for (String servletName : servletNames) {
            this.servletNameMappings.add(servletName);
            
            IServletConfig sConfig = this.webAppConfig.getServletInfo(servletName);
            IFilterMapping fmapping = new FilterMapping(null, this, sConfig);
            
            if (dispatcherTypes!=null){ // will default to REQUEST
	            DispatcherType [] dispatcherType = {};
	            fmapping.setDispatchMode(dispatcherTypes.toArray(dispatcherType));
            }

            // We are adding to the generic list of filter mappings here.
            // When WebAppFilterManager.init is called, it will be added to the
            // uri or servlet based mapping lists.
            if (isMatchAfter) {
                this.webAppConfig.getFilterMappings().add(fmapping);
            } else {
                int previous=this.webAppConfig.getLastIndexBeforeDeclaredFilters();
                this.webAppConfig.getFilterMappings().add(previous, fmapping);
                this.webAppConfig.setLastIndexBeforeDeclaredFilters(++previous);
            } 
        }

    }

    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns)
            throws IllegalStateException, IllegalArgumentException {
        addMappingForUrlPatterns(dispatcherTypes, isMatchAfter, -1, urlPatterns);
    }

    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, int index, String... urlPatterns)
            throws IllegalStateException, IllegalArgumentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addMappingForUrlPatterns", "isMatchAfter -> " + isMatchAfter + " index -> " + index);
        } 
        if (urlPatterns == null) {
            throw new IllegalArgumentException(nls.getString("add.filter.mapping.to.null.url.patterns"));
        } else if (urlPatterns.length == 0) {
            throw new IllegalArgumentException(nls.getString("add.filter.mapping.to.empty.url.patterns"));
        }
        
        if (this.context!=null&&this.context.isInitialized()) {
            throw new IllegalStateException(nls.getString("Not.in.servletContextCreated"));
        }
        
        
        if (this.urlPatternMappings==null){
            this.urlPatternMappings = new ArrayList<String>();
        }

        for (String urlPattern : urlPatterns) {
            this.urlPatternMappings.add(urlPattern);
            IFilterMapping fmapping = new FilterMapping(urlPattern, this, null);
            if (dispatcherTypes!=null){ // will default to REQUEST
	            DispatcherType [] dispatcherType = {};
	            fmapping.setDispatchMode(dispatcherTypes.toArray(dispatcherType));
            }
            if (isMatchAfter) {
                if (index<0){
                    this.webAppConfig.getFilterMappings().add(fmapping);                    
                }
                else{
                    this.webAppConfig.getFilterMappings().add(index, fmapping);
                }
            } else {
                int previous=this.webAppConfig.getLastIndexBeforeDeclaredFilters();
                this.webAppConfig.getFilterMappings().add(previous, fmapping);
                this.webAppConfig.setLastIndexBeforeDeclaredFilters(++previous);
            } 
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addMappingForUrlPatterns", "isMatchAfter -> " + isMatchAfter + " index -> " + index);
        } 
    }

    
    public Collection<String> getServletNameMappings() {
        return this.servletNameMappings;
    }

    
    public Collection<String> getUrlPatternMappings() {
        return this.urlPatternMappings;
    }
    
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    
    public String getClassName() {
    	String className = super.getClassName();
        if (className != null) {
            return className;
        } else if (this.filterClass != null) {
            return this.filterClass.getName();
        } else if (this.filter != null) {
            return this.filter.getClass().getName();
        } else {
            return null;
        }
    }
    
    public Filter getFilter() {
        return filter;
    }

    public Class<? extends Filter> getFilterClass() {
        return filterClass;
    }

    public void setFilterClass(Class<? extends Filter> filterClass) {
        this.filterClass = filterClass;
    }

    @Override
    public String getFilterClassName() {
        return getClassName();
    }

    @Override
    public void setFilterClassName(String className) {
        setClassName(className);
    }
    
    public String toString (){
        StringBuilder strBuilder = new StringBuilder();
        Filter filter = ((FilterConfig)this).getFilter();
        String filterObjectsClassName=null;
        if (filter!=null){
            filterObjectsClassName = filter.getClass().getName();
        }
        strBuilder.append("Filter->"+filterObjectsClassName);
        strBuilder.append(",FilterClass->"+((FilterConfig)this).getFilterClass());
        strBuilder.append(",FilterClassLoader->"+((FilterConfig)this).getFilterClassLoader());
        strBuilder.append(",FilterClassName->"+((FilterConfig)this).getFilterClassName());
        strBuilder.append(",FilterName->"+((FilterConfig)this).getFilterName());
        strBuilder.append(",ServletContext->"+((FilterConfig)this).getServletContext());
        strBuilder.append(",UrlPatternMappings->"+((FilterConfig)this).getUrlPatternMappings());
        strBuilder.append(",ServletNameMappings->"+((FilterConfig)this).getServletNameMappings());
        
        Map<String, String> localInitParams = this.getInitParameters();
        for (Entry<String,String>entry:localInitParams.entrySet()){
            strBuilder.append(",initParams->"+entry.getKey()+","+entry.getValue());
        }
        strBuilder.append(super.toString());
        
        return strBuilder.toString();
    }

}
