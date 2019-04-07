/*******************************************************************************
 * Copyright (c) 1997, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.ws.webcontainer.util.ArrayEnumeration;
import com.ibm.ws.webcontainer.util.EmptyEnumeration;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

@SuppressWarnings("unchecked")
public class ServletConfig extends TargetConfig implements IServletConfig {
        protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.servlet");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.servlet.ServletConfig";

    protected static final TraceNLS nls = TraceNLS.getTraceNLS(ServletConfig.class, "com.ibm.ws.webcontainer.resources.Messages");
    protected static final TraceNLS liberty_nls = TraceNLS.getTraceNLS(ServletConfig.class, "com.ibm.ws.webcontainer.resources.LShimMessages");
    private boolean enabled=true;
    public boolean isEnabled() {
                return enabled;
        }

        private String servletName;
    private String className;
    private List<String> mappings;
    // PK03770
    // private int startUpWeight = -1;
    private Integer startUpWeight = null;
    private static final Integer DEFAULT_STARTUP = Integer.valueOf(-1);
    // PK03770

    private boolean isCachingEnabled = true; // LIDB3477-17, cache setting for
                                             // dynamically added servlets
    private boolean isStatisticsEnabled = true; // 304662

    private boolean isInternal = false;

    protected WebComponentMetaData metaData;
    private boolean singleThreadModelServlet;
    private boolean isJsp;
    private WebAppConfig webAppConfig;
    private IServletWrapper servletWrapper;
    private Class<? extends Servlet> servletClass;
    private Servlet servlet;
    private boolean addedToLoadOnStartup;
    private Integer previousWeight=null;
    private MultipartConfigElement multipartConfig=null;
    private File multipartConfigBaseLocation=null;
        private String runAsRole;
        private ServletSecurityElement servletSecurityElement;
        private DeclareRoles declareRolesAnnotation;

    /**
     * @param id
     * @param webAppConfig
     */
    public ServletConfig(String id, WebAppConfig webAppConfig) {
        super(id);
        this.webAppConfig = webAppConfig;
    }

    // Begin f269714, LI3477 - ServletConfig creation for Security
    /**
     * Add an attributes from a map
     * 
     * @param key
     * @param value
     */
    public void setAttributes(Map map) {
        _attributes = map;
    }

    // End f269714, LI3477 - ServletConfig creation for Security

    /**
     * @return String
     */
    public String getClassName() {
        if (this.className != null) {
            return this.className;
        } else if (this.servletClass != null) {
            return this.servletClass.getName();
        } else if (this.servlet != null) {
            return this.servlet.getClass().getName();
        } else {
            return null;
        }
    }

    /**
     * @return String
     */
    public String getServletName() {
        return servletName;
    }

    /**
     * @return java.util.Enumeration
     */
    public Enumeration getInitParameterNames() {
        Map<String,String> map;

        // if (metaData != null)
        // map = metaData.getWebComponentInitParameters();
        // else
        map = initParams;

        if (map != null && map.size() > 0)
            return new ArrayEnumeration(map.keySet().toArray());
        else
            return EmptyEnumeration.instance();
    }

    public boolean isLoadOnStartup() {
        // PK03770
        if ((startUpWeight == null) || (startUpWeight.intValue() == DEFAULT_STARTUP.intValue())) {
            return false;
        }
        return true;
        // PK03770
    }

    /**
     * @return int
     */
    public int getStartUpWeight() {
        // PK03770
        return (startUpWeight == null) ? DEFAULT_STARTUP.intValue() : startUpWeight.intValue();
        // PK03770

    }

    /**
     * Sets the startUpWeight. A null value is translated to the default weight.
     * 
     * @param weight
     */
    public void setStartUpWeight(Integer weight) {
        //save previous weight to see if we need to try to remove and add it again to the list
        this.previousWeight = this.startUpWeight;
        
        if (weight != null) {
            if (weight.intValue() >= 0)
                this.startUpWeight = weight;
            else
                this.startUpWeight = DEFAULT_STARTUP;
        } else {
            this.startUpWeight = DEFAULT_STARTUP;
        }
        if (this.context != null)
            this.context.addToStartWeightList(this);
    }

    /**
     * Sets the className.
     * 
     * @param className
     *            The className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Sets the servletName.
     * 
     * @param servletName
     *            The servletName to set
     */
    public void setServletName(String servletName) {
        this.setName(servletName);
        this.servletName = servletName;
    }

    public void setMappings(List<String> list) {
        this.mappings = list;
    }

    public List<String> getMappings() {
        // start PI23529
        if(mappings == null && (WCCustomProperties.EMPTY_SERVLET_MAPPINGS || com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= WebContainer.SPEC_LEVEL_31 )){
                return new ArrayList();
        }
        //end PI23529
        return mappings;
    }

    //WARNING!!! Don't change this method unless you plan on breaking test cases.
    public String toString() {
        StringWriter strWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(strWriter);
        
        Servlet servlet = this.getServlet();
        String servletObjectsClassName=null;
        if (servlet!=null){
            servletObjectsClassName = servlet.getClass().getName();
        }
        printWriter.println("Servlet->"+servletObjectsClassName);
        printWriter.println("\tgetClassName->"+this.getClassName());
        printWriter.println("\tgetName->"+this.getName());
        
        List<String> mappings = this.getMappings();
        if (mappings!=null){
            for (String mapping:mappings){
                printWriter.println("\tmapping->"+mapping);
            }
        }
        else {
            printWriter.println("\tno mappings\n");
        }
        printWriter.println("\tloadOnStartupWeight->"+this.getStartUpWeight());
        
        if (this.getRunAsRole()!=null)
                printWriter.println("\trunAsRole->"+this.getRunAsRole());
        
        if (this.getServletSecurity()!=null){
                ServletSecurityElement servletSecurity = this.getServletSecurity();
                printWriter.println("\tServletSecurity->");
                Collection<String> methodNames = servletSecurity.getMethodNames();
                if (methodNames!=null&&!methodNames.isEmpty()){
                        printWriter.print("\t\tMethodNames->");
                        for (String methodName:methodNames){
                                printWriter.print(methodName+",");
                        }
                        printWriter.println();
                }
                
                writeHttpConstraintElementString(printWriter, servletSecurity,"\t\t");
                
                
                Collection<HttpMethodConstraintElement> httpMethodConstraints = servletSecurity.getHttpMethodConstraints();
                if (httpMethodConstraints!=null&&httpMethodConstraints.size()>0){
                        for (HttpMethodConstraintElement httpMethodConstraint:httpMethodConstraints){
                                printWriter.println("\t\tHttpMethodConstraint:"+httpMethodConstraint.getMethodName());
                                writeHttpConstraintElementString(printWriter, httpMethodConstraint,"\t\t\t");
                        }
                }
                
        }
        
        printWriter.append("\t"+super.toString());
        printWriter.flush();
        
        return strWriter.toString();
    }

        private void writeHttpConstraintElementString(PrintWriter printWriter,
                        HttpConstraintElement httpConstraintElement, String string) {
                printWriter.println(string+"EmptyRoleSemantic->"+httpConstraintElement.getEmptyRoleSemantic());
                
                String[] rolesAllowed = httpConstraintElement.getRolesAllowed();
                if (rolesAllowed!=null&&rolesAllowed.length>0)
                {
                        printWriter.print(string+"RolesAllowed->");
                        for (String roleAllowed:rolesAllowed)
                        {
                                printWriter.print(roleAllowed+",");
                        }
                        printWriter.println();
                }
                
                
                TransportGuarantee transportGuarantee = httpConstraintElement.getTransportGuarantee();
                if (transportGuarantee!=null){
                        printWriter.println(string+"transportGuarantee->"+transportGuarantee);
                }
        }

    // Begin LIDB3477-17, cache setting for dynamically added servlets

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#isCachingEnabled()
     */
    public boolean isCachingEnabled() {
        return isCachingEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.webcontainer.servlet.IServletConfig#setIsCachingEnabled
     * (boolean)
     */
    public void setIsCachingEnabled(boolean isEnabled) {
        isCachingEnabled = isEnabled;
    }

    // End LIDB3477-17

    public boolean isInternal() {
        return isInternal;
    }

    public void setInternal(boolean isInternal) {
        this.isInternal = isInternal;
    }

    public void setStatisticsEnabled(boolean value) { // 304662, 304662.1
        isStatisticsEnabled = value;
    }

    public boolean isStatisticsEnabled() { // 304662, 304662.1
        return isStatisticsEnabled;
    }

    public void setMetaData(WebComponentMetaData metaData) {
        this.metaData = metaData;
    }

    /**
     * Returns the metaData.
     * 
     * @return WebComponentMetaData
     */
    public WebComponentMetaData getMetaData() {
        return metaData;
    }

    public boolean isSingleThreadModelServlet() {
        return singleThreadModelServlet;
    }

    public void setSingleThreadModelServlet(boolean singleThreadModelServlet) {
        this.singleThreadModelServlet = singleThreadModelServlet;
    }
    
    
    public Set<String> addMapping(CheckContextInitialized checkContextInitialized,String... urlPatterns) {
        Set<String> mappingConflicts = null;
        
        if (checkContextInitialized==CheckContextInitialized.TRUE&&this.context.isInitialized()) {
            throw new IllegalStateException(liberty_nls.getString("Not.in.servletContextCreated"));
        }

        if (urlPatterns == null) {
            throw new IllegalArgumentException(nls.getString("add.servlet.mapping.to.null.url.patterns"));
        } else if (urlPatterns.length == 0) {
            throw new IllegalArgumentException(nls.getString("add.servlet.mapping.to.empty.url.patterns"));
        }

        String path = null;                                     //709390

        for (String urlPattern : urlPatterns) {
            //709390 - urlPattern of / is being mapped as /* in the URI mapper table
            path = urlPattern;
            if (path.equals("/")){
                      path = "/*";
            }
            //709390
            
            if (this.context.containsTargetMapping(path)) {     //709390
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "addMapping", "found existing mapping for urlPattern->" + path);
                if (mappingConflicts == null) {
                    mappingConflicts = new HashSet<String>();
                }
                mappingConflicts.add(urlPattern);
            }
        }

        if (mappingConflicts == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "addMapping", "no mapping conflicts");

            if (mappings == null) {
                mappings = new ArrayList();
            }
            if (webAppConfig != null) {
                for (String urlPattern : urlPatterns) {
                    this.webAppConfig.addServletMapping(servletName, urlPattern);
                    try {
                        if (this.servletWrapper != null) {
                            //709390
                            if (urlPattern.equals("/")){
                                      urlPattern = "/*";
                            }
                            //709390
                            this.context.addMappingTarget(urlPattern, this.servletWrapper);
                        }
                    } catch (Exception e) {
                        FFDCFilter.processException(e, this.getClass().getName() + ".addMapping", "423");
                    }
                }
            }
        }

        if (mappingConflicts == null) {
            mappingConflicts = Collections.EMPTY_SET;
        }
        return mappingConflicts;
    }
    
    public Set<String> addMapping(String... urlPatterns) {
        return addMapping(CheckContextInitialized.TRUE,urlPatterns);
    }

    /**
     * Returns the isJsp.
     * 
     * @return boolean
     */
    public boolean isJsp() {
        return isJsp;
    }

    /**
     * Sets the isJsp.
     * 
     * @param isJsp
     *            The isJsp to set
     */
    public void setIsJsp(boolean isJsp) {
        this.isJsp = isJsp;
    }

    public IServletWrapper getServletWrapper() {
        return servletWrapper;
    }

    public void setServletWrapper(IServletWrapper servletWrapper) {
        this.servletWrapper = servletWrapper;
    }

    public void setServletClass(Class<? extends Servlet> servletClass) {
        this.servletClass = servletClass;
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }

    public Class<? extends Servlet> getServletClass() {
        return servletClass;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public void setLoadOnStartup(int loadOnStartup) {
        this.setStartUpWeight(loadOnStartup);
    }

    @Override
    public boolean isAddedToLoadOnStartup() {
        return addedToLoadOnStartup;
    }

    @Override
    public void setAddedToLoadOnStartup(boolean addedToLoadOnStartup) {
        this.addedToLoadOnStartup = addedToLoadOnStartup;
    }

    @Override
    public boolean isWeightChanged() {
        return (this.startUpWeight!=this.previousWeight&&!this.startUpWeight.equals(this.previousWeight));
    }

        @Override
    public void setMultipartConfig(MultipartConfigElement multipartConfig) {
                
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "setMultipartConfig", 
                        "[location, maxFileSize, maxRequestSize, fileSizeThreshold],["+
                        multipartConfig.getLocation()+","+multipartConfig.getMaxFileSize()+","+
                        multipartConfig.getMaxRequestSize()+","+multipartConfig.getFileSizeThreshold());
       
                this.multipartConfig=multipartConfig;
        }
        
    @Override
    public MultipartConfigElement getMultipartConfig() {
        return multipartConfig;
    }
    
    @Override
    public File getMultipartBaseLocation() {
        return multipartConfigBaseLocation;
    }
    
    @Override
    public void setMultipartBaseLocation(File arg0) {
        multipartConfigBaseLocation=arg0;
    }

        @Override
        public void setRunAsRole(String runAsRole) {
                this.runAsRole = runAsRole;
        }

        @Override
        public Set<String> setServletSecurity(ServletSecurityElement servletSecurityElement) {
            if (servletSecurityElement==null)
                throw new IllegalArgumentException();

            if (this.context.isInitialized()) {
                throw new IllegalStateException(liberty_nls.getString("Not.in.servletContextCreated"));
            }

            final WebApp webApp = ((WebAppConfiguration)webAppConfig).getWebApp();
            IWebAppSecurityCollaborator securityCollab = AccessController.doPrivileged(new PrivilegedAction<IWebAppSecurityCollaborator>(){

                @Override
                public IWebAppSecurityCollaborator run() {
                    return webApp.getCollaboratorHelper().getSecurityCollaborator();
                }
            });
            String appName = webApp.getApplicationName();
            String contextRoot = webAppConfig.getContextRoot();
            String vHost = webAppConfig.getVirtualHostName();
            this.servletSecurityElement = servletSecurityElement;
            if (mappings!=null) {
                // calculate the list of url patterns that will not have constraints applied from the incoming servletSecurityElement
                // because those mappings occur in security constraints in the DD which per spec is authoritative for those patterns.
                List<String> listOfConflicts = securityCollab.getURIsInSecurityConstraints(appName, contextRoot, vHost, mappings);
                if (listOfConflicts!=null){
                    Set<String> setOfConflicts = new HashSet<String>();
                    setOfConflicts.addAll(listOfConflicts);
                    return setOfConflicts;
                } else {                    
                    return new HashSet<String>();
                }
            } else {
                return new HashSet<String>();
            }
        }

        @Override
        public String getRunAsRole() {
                return this.runAsRole;
        }

        @Override
        public ServletSecurityElement getServletSecurity() {
                // TODO Auto-generated method stub
                return servletSecurityElement;
        }

        @Override
        public boolean isClassDefined() {
                // TODO Auto-generated method stub
                return (this.getClassName()!=null||this.getServletClass()!=null||this.getServlet()!=null);
        }

        public void setEnabled(boolean enabled) {
                this.enabled = enabled;
        }


}
