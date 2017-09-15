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
package com.ibm.websphere.simplicity.application;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.ibm.websphere.simplicity.Scope;
import com.ibm.websphere.simplicity.application.tasks.ActSpecJNDITask;
import com.ibm.websphere.simplicity.application.tasks.AppDeploymentOptionsTask;
import com.ibm.websphere.simplicity.application.tasks.ApplicationTask;
import com.ibm.websphere.simplicity.application.tasks.BackendIdSelectionTask;
import com.ibm.websphere.simplicity.application.tasks.BindJndiForEJBBusinessTask;
import com.ibm.websphere.simplicity.application.tasks.BindJndiForEJBMessageBindingTask;
import com.ibm.websphere.simplicity.application.tasks.BindJndiForEJBNonMessageBindingTask;
import com.ibm.websphere.simplicity.application.tasks.CorrectOracleIsolationLevelTask;
import com.ibm.websphere.simplicity.application.tasks.CorrectUseSystemIdentityTask;
import com.ibm.websphere.simplicity.application.tasks.CtxRootForWebModTask;
import com.ibm.websphere.simplicity.application.tasks.CustomTask;
import com.ibm.websphere.simplicity.application.tasks.DataSourceFor10CMPBeansTask;
import com.ibm.websphere.simplicity.application.tasks.DataSourceFor10EJBModulesTask;
import com.ibm.websphere.simplicity.application.tasks.DataSourceFor20CMPBeansTask;
import com.ibm.websphere.simplicity.application.tasks.DataSourceFor20EJBModulesTask;
import com.ibm.websphere.simplicity.application.tasks.DefaultBindingTask;
import com.ibm.websphere.simplicity.application.tasks.EJBDeployOptionsTask;
import com.ibm.websphere.simplicity.application.tasks.EmbeddedRarTask;
import com.ibm.websphere.simplicity.application.tasks.EnsureMethodProtectionFor10EJBTask;
import com.ibm.websphere.simplicity.application.tasks.EnsureMethodProtectionFor20EJBTask;
import com.ibm.websphere.simplicity.application.tasks.JSPCompileOptionsTask;
import com.ibm.websphere.simplicity.application.tasks.JSPReloadForWebModTask;
import com.ibm.websphere.simplicity.application.tasks.ListModulesTask;
import com.ibm.websphere.simplicity.application.tasks.MapEJBRefToEJBTask;
import com.ibm.websphere.simplicity.application.tasks.MapMessageDestinationRefToEJBTask;
import com.ibm.websphere.simplicity.application.tasks.MapModulesToServersEntry;
import com.ibm.websphere.simplicity.application.tasks.MapModulesToServersTask;
import com.ibm.websphere.simplicity.application.tasks.MapResEnvRefToResTask;
import com.ibm.websphere.simplicity.application.tasks.MapResRefToEJBTask;
import com.ibm.websphere.simplicity.application.tasks.MapRolesToUsersTask;
import com.ibm.websphere.simplicity.application.tasks.MapRunAsRolesToUsersTask;
import com.ibm.websphere.simplicity.application.tasks.MapWebModToVHTask;
import com.ibm.websphere.simplicity.application.tasks.MetadataCompleteForModulesTask;
import com.ibm.websphere.simplicity.application.tasks.MultiEntryApplicationTask;
import com.ibm.websphere.simplicity.application.tasks.WSDeployOptionsTask;
import com.ibm.websphere.simplicity.log.Log;

public class ApplicationOptions {

    private static Hashtable<String, Class> standardTasks = new Hashtable<String, Class>();
    private static Class c = ApplicationOptions.class;
    private static Field[] fields = c.getDeclaredFields();

    static {
        mapTasksToClasses();
    }

    public static Class getTaskClass(String taskName) {
        if (standardTasks.containsKey(taskName))
            return standardTasks.get(taskName);
        else
            return CustomTask.class;
    }

    public ApplicationOptions(List<ApplicationTask> tasks, Scope scope) throws Exception {
        this.tasks = tasks;
        this.scope = scope;
        if (tasks != null)
            loadTasks(tasks);
    }

    protected Scope scope = null;
    protected List<ApplicationTask> tasks = null;
    protected Vector<CustomTask> customTasks = new Vector<CustomTask>();
    protected Set<AssetModule> modules = null;

    protected ActSpecJNDITask actSpecJndi = new ActSpecJNDITask();
    protected AppDeploymentOptionsTask appDeploymentOptions = new AppDeploymentOptionsTask();
    protected BackendIdSelectionTask backendIdSelection = new BackendIdSelectionTask();
    protected BindJndiForEJBBusinessTask bindJndiForEJBBusiness = new BindJndiForEJBBusinessTask();
    protected BindJndiForEJBMessageBindingTask bindJndiForEjbMessageBinding = new BindJndiForEJBMessageBindingTask();
    protected BindJndiForEJBNonMessageBindingTask bindJndiForEjbNonMessageBinding = new BindJndiForEJBNonMessageBindingTask();
    protected CorrectOracleIsolationLevelTask correctOracleIsolationLevel = new CorrectOracleIsolationLevelTask();
    protected CorrectUseSystemIdentityTask correctUseSystemIdentity = new CorrectUseSystemIdentityTask();
    protected CtxRootForWebModTask ctxRootForWebMod = new CtxRootForWebModTask();
    protected DataSourceFor10CMPBeansTask dataSourceFor10CmpBeans = new DataSourceFor10CMPBeansTask();
    protected DataSourceFor10EJBModulesTask dataSourceFor10EjbModules = new DataSourceFor10EJBModulesTask();
    protected DataSourceFor20CMPBeansTask dataSourceFor20CmpBeans = new DataSourceFor20CMPBeansTask();
    protected DataSourceFor20EJBModulesTask dataSourceFor20EjbModules = new DataSourceFor20EJBModulesTask();
    protected DefaultBindingTask defaultBinding = new DefaultBindingTask();
    protected EJBDeployOptionsTask ejbDeployOptions = new EJBDeployOptionsTask();
    protected EmbeddedRarTask embeddedRar = new EmbeddedRarTask();
    protected EnsureMethodProtectionFor10EJBTask ensureMethodProtectionFor10Ejb = new EnsureMethodProtectionFor10EJBTask();
    protected EnsureMethodProtectionFor20EJBTask ensureMethodProtectionFor20Ejb = new EnsureMethodProtectionFor20EJBTask();
    protected JSPCompileOptionsTask jspCompileOptions = new JSPCompileOptionsTask();
    protected JSPReloadForWebModTask jspReloadForWebMod = new JSPReloadForWebModTask();
    protected ListModulesTask listModules = new ListModulesTask();
    protected MapEJBRefToEJBTask mapEjbRefToEjb = new MapEJBRefToEJBTask();
    protected MapMessageDestinationRefToEJBTask mapMessageDestinationRefToEJB = new MapMessageDestinationRefToEJBTask();
    protected MapModulesToServersTask mapModulesToServers = new MapModulesToServersTask();
    protected MapResEnvRefToResTask mapResEnvRefToRes = new MapResEnvRefToResTask();
    protected MapResRefToEJBTask mapResRefToEjb = new MapResRefToEJBTask();
    protected MapRolesToUsersTask mapRolesToUsers = new MapRolesToUsersTask();
    protected MapRunAsRolesToUsersTask mapRunAsRolesToUsers = new MapRunAsRolesToUsersTask();
    protected MapWebModToVHTask mapWebModToVh = new MapWebModToVHTask();
    protected MetadataCompleteForModulesTask metadataCompleteForModules = new MetadataCompleteForModulesTask();
    protected WSDeployOptionsTask wsDeployOptions = new WSDeployOptionsTask();

    public List<ApplicationTask> getTasks() {
        return this.tasks;
    }

    public Set<AssetModule> getModules() throws Exception {
        if (modules == null) {
            modules = new HashSet<AssetModule>();
            if (mapModulesToServers != null)
                for (int i = 0; i < mapModulesToServers.size(); i++) {
                    MapModulesToServersEntry entry = mapModulesToServers.get(i);
                    AssetModule module = AssetModule.getModuleInstance(null, entry.getModule(), entry.getUri());
                    modules.add(module);
                }
        }
        return modules;
    }

    /**
     * The ActSpecJNDI option binds Java 2 Connector (J2C) activation specifications
     * to destination Java Naming and Directory Interface (JNDI) names. You can
     * optionally bind J2C activation specifications in your application or module
     * to a destination JNDI name.
     */
    public ActSpecJNDITask getActSpecJndi() {
        return actSpecJndi;
    }

    /**
     * Contains general deployment options. These include file permission
     * specifications, auto-linking of JNDI names for EE5 apps, enabling
     * and disabling distribution of the app, and so on.
     */
    public AppDeploymentOptionsTask getAppDeploymentOptions() {
        return appDeploymentOptions;
    }

    /**
     * The BackendIdSelection option specifies the backend ID for the enterprise
     * bean Java archive (JAR) modules that have container-managed persistence
     * (CMP) beans. An enterprise bean JAR module can support multiple backend
     * configurations as specified using an application assembly tool.
     */
    public BackendIdSelectionTask getBackendIdSelection() {
        return backendIdSelection;
    }

    /**
     * Specify Java Naming and Directory (JNDI) name bindings for each enterprise bean
     * with a business interface in an EJB module. Each enterprise bean with a business
     * interface in an EJB module must be bound to a JNDI name. For any business interface
     * that does not provide a JNDI name, or if its bean does not provide a JNDI name, a
     * default binding name is provided. If its bean provides a JNDI name, the default
     * JNDI name for the business interface is provided on top of its bean JNDI name by
     * appending the package-qualifed class name of the interface.
     * <p>
     * If you specify the JNDI name for a bean in the Provide JNDI names for beans panel,
     * Do not specify both the JNDI name and business interface JNDI name for the same bean.
     * If you do not specify the JNDI name for a bean, you can optionally specify a business
     * interface JNDI name. If you do not specify a business interface JNDI name, the
     * runtime provides a container default.
     */
    public BindJndiForEJBBusinessTask getBindJndiForEJBBusiness() {
        return bindJndiForEJBBusiness;
    }

    /**
     * The BindJndiForEJBMessageBinding option Binds enterprise beans to listener
     * port names or Java Naming and Directory Interface (JNDI) names. Ensure each
     * message-driven enterprise bean in your application or module is bound to a
     * listener port name.
     */
    public BindJndiForEJBMessageBindingTask getBindJndiForEjbMessageBinding() {
        return bindJndiForEjbMessageBinding;
    }

    /**
     * The BindJndiForEJBNonMessageBinding option binds enterprise beans to Java
     * Naming and Directory Interface (JNDI) names. Ensure each non message-driven
     * enterprise bean in your application or module is bound to a JNDI name.
     */
    public BindJndiForEJBNonMessageBindingTask getBindJndiForEjbNonMessageBinding() {
        return bindJndiForEjbNonMessageBinding;
    }

    /**
     * The CorrectOracleIsolationLevel option specifies the isolation level for the
     * Oracle type provider. The last field of each entry specifies the isolation
     * level. Valid isolation level values are 2 or 4.
     */
    public CorrectOracleIsolationLevelTask getCorrectOracleIsolationLevel() {
        return correctOracleIsolationLevel;
    }

    /**
     * The CorrectUseSystemIdentity option replaces RunAs System to RunAs Roles. The
     * enterprise beans that you install contain a RunAs system identity. You can
     * optionally change this identity to a RunAs role.
     */
    public CorrectUseSystemIdentityTask getCorrectUseSystemIdentity() {
        return correctUseSystemIdentity;
    }

    /**
     * The CtxRootForWebMod option edits the context root of the Web module. You can
     * edit a context root that is defined in the application.xml file using this option.
     */
    public CtxRootForWebModTask getCtxRootForWebMod() {
        return ctxRootForWebMod;
    }

    /**
     * The DataSourceFor10CMPBeans option specifies optional data sources for
     * individual 1.x container-managed persistence (CMP) beans. Mapping a
     * specific data source to a CMP bean overrides the default data source
     * for the module that contains the enterprise bean.
     */
    public DataSourceFor10CMPBeansTask getDataSourceFor10CmpBeans() {
        return dataSourceFor10CmpBeans;
    }

    /**
     * The DataSourceFor10EJBModules option specifies the default data source for
     * the enterprise bean module that contains 1.x container-managed persistence
     * (CMP) beans.
     */
    public DataSourceFor10EJBModulesTask getDataSourceFor10EjbModules() {
        return dataSourceFor10EjbModules;
    }

    /**
     * The DataSourceFor20CMPBeans option specifies optional data sources for
     * individual 2.x container-managed persistence (CMP) beans. Mapping a
     * specific data source to a CMP bean overrides the default data source
     * for the module that contains the enterprise bean.
     */
    public DataSourceFor20CMPBeansTask getDataSourceFor20CmpBeans() {
        return dataSourceFor20CmpBeans;
    }

    /**
     * The DataSourceFor20EJBModules option specifies the default data source for
     * the enterprise bean 2.x module that contains 2.x container managed
     * persistence (CMP) beans.
     */
    public DataSourceFor20EJBModulesTask getDataSourceFor20EjbModules() {
        return dataSourceFor20EjbModules;
    }

    /**
     * A collection of default setting options.
     */
    public DefaultBindingTask getDefaultBinding() {
        return defaultBinding;
    }

    /**
     * Allows you to specify various options that can be passed when you want to
     * deploy EJB modules during application installation. This task has only 2
     * rows. The first row specifies the option name and the second row has the
     * corresponding option value.
     */
    public EJBDeployOptionsTask getEjbDeployOptions() {
        return ejbDeployOptions;
    }

    /**
     * The EmbeddedRar option binds Java 2 Connector objects to JNDI names. You must
     * bind each Java 2 Connector object in your application or module, such as, J2C
     * connection factories, J2C activation specifications and J2C administrative
     * objects, to a JNDI name.
     */
    public EmbeddedRarTask getEmbeddedRar() {
        return embeddedRar;
    }

    /**
     * The EnsureMethodProtectionFor10EJB option selects method protections for
     * unprotected methods of 1.x enterprise beans. Specify to leave the method as
     * unprotected, or assign protection which denies all access.
     */
    public EnsureMethodProtectionFor10EJBTask getEnsureMethodProtectionFor10Ejb() {
        return ensureMethodProtectionFor10Ejb;
    }

    /**
     * The EnsureMethodProtectionFor20EJB option selects method protections for
     * unprotected methods of 2.x enterprise beans. Specify to assign a security
     * role to the unprotected method, add the method to the exclude list, or mark
     * the method as cleared. You can assign multiple roles for a method by
     * separating role names with commas.
     */
    public EnsureMethodProtectionFor20EJBTask getEnsureMethodProtectionFor20Ejb() {
        return ensureMethodProtectionFor20Ejb;
    }

    /**
     * The JSPCompileOptions option assigns shared libraries to applications or
     * every module. You can associate multiple shared libraries to applications
     * and modules.
     */
    public JSPCompileOptionsTask getJspCompileOptions() {
        return jspCompileOptions;
    }

    /**
     * The JSPReloadForWebMod option edits the JSP reload attributes for the Web
     * module. You can specify the reload attributes of the servlet and JSP for
     * each module.
     */
    public JSPReloadForWebModTask getJspReloadForWebMod() {
        return jspReloadForWebMod;
    }

    /**
     * TODO
     */
    public ListModulesTask getListModules() {
        return listModules;
    }

    /**
     * The MapEJBRefToEJB option maps enterprise Java references to enterprise
     * beans. You must map each enterprise bean reference defined in your application
     * to an enterprise bean.
     */
    public MapEJBRefToEJBTask getMapEjbRefToEjb() {
        return mapEjbRefToEjb;
    }

    /**
     * The MapMessageDestinationRefToEJB option maps message destination references
     * to Java Naming and Directory Interface (JNDI) names of administrative objects
     * from the installed resource adapters. You must map each message destination
     * reference that is defined in your application to an administrative object.
     */
    public MapMessageDestinationRefToEJBTask getMapMessageDestinationRefToEjb() {
        return mapMessageDestinationRefToEJB;
    }

    /**
     * The MapModulesToServers option specifies the application server where you want
     * to install modules that are contained in your application. You can install
     * modules on the same server, or disperse them among several servers.
     */
    public MapModulesToServersTask getMapModulesToServers() {
        return mapModulesToServers;
    }

    /**
     * The MapResEnvRefToRes option maps resource environment references to resources.
     * You must map each resource environment reference that is defined in your
     * application to a resource.
     */
    public MapResEnvRefToResTask getMapResEnvRefToRes() {
        return mapResEnvRefToRes;
    }

    /**
     * The MapResRefToEJB option maps resource references to resources. You must map
     * each resource reference that is defined in your application to a resource.
     */
    public MapResRefToEJBTask getMapResRefToEjb() {
        return mapResRefToEjb;
    }

    /**
     * The MapRolesToUsers option maps users to roles. You must map each role that is
     * defined in the application or module to a user or group from the domain user
     * registry. You can specify multiple users or groups for a single role by
     * separating them with a pipe (|).
     */
    public MapRolesToUsersTask getMapRolesToUsers() {
        return mapRolesToUsers;
    }

    /**
     * The MapRunAsRolesToUsers option maps RunAs roles to users. The enterprise beans
     * you that install contain predefined RunAs roles. Enterprise beans that need to
     * run as a particular role for recognition while interacting with another
     * enterprise bean use RunAs roles.
     */
    public MapRunAsRolesToUsersTask getMapRunAsRolesToUsers() {
        return mapRunAsRolesToUsers;
    }

    /**
     * The MapWebModToVH option selects virtual hosts for Web modules. Specify the
     * virtual host where you want to install the Web modules that are contained in
     * your application. You can install Web modules on the same virtual host, or
     * disperse them among several hosts.
     */
    public MapWebModToVHTask getMapWebModToVh() {
        return mapWebModToVh;
    }

    /**
     * If your application contains EJB 3.0 or Web 2.5 modules, you can optionally lock the
     * deployment descriptor of one or more of the EJB 3.0 or Web 2.5 modules. If you set
     * the metadata-complete attribute to true and lock deployment descriptors, the product
     * writes the complete module deployment descriptor, including deployment information
     * from annotations, to XML format.
     * <p>
     * Annotations are a standard mechanism of adding metadata to Java classes. You can use
     * metadata to simplify development and deployment of Java EE 5 artifacts. Prior to the
     * introduction of Java language annotations, deployment descriptors were the standard
     * mechanism used by Java EE components. These deployment descriptors were mapped to XML
     * format, which facilitated their persistence. If you select to lock deployment
     * descriptors, the product merges Java EE 5 annotation-based metadata with the XML-based
     * existing deployment descriptor metadata and persists the result.
     * 
     * @return
     */
    public MetadataCompleteForModulesTask getMetadataCompleteForModules() {
        return metadataCompleteForModules;
    }

    /**
     * To deploy Java-based Web services, you need an enterprise application, also known
     * as an EAR file that is configured and enabled for Web services.
     * <p>
     * A Java? API for XML-Based Web Services (JAX-WS) application does not require
     * additional bindings and deployment descriptors for deployment whereas a Java API
     * for XML-based RPC (JAX-RPC) Web services application requires you to add
     * additional bindings and deployment descriptors for application deployment. JAX-WS
     * is much more dynamic, and does not require any of the static data generated by
     * the deployment step required for deploying JAX-RPC applications.
     * <p>
     * For JAX-WS Web services, the use of the webservices.xml deployment descriptor is
     * optional because you can use annotations to specify all of the information that
     * is contained within the deployment descriptor file. You can use the deployment
     * descriptor file to augment or override existing JAX-WS annotations. Any information
     * that you define in the webservices.xml deployment descriptor overrides any
     * corresponding information that is specified by annotations.
     */
    public WSDeployOptionsTask getWSDeployOptions() {
        return wsDeployOptions;
    }

    public ApplicationTask getTaskByName(String taskName) throws Exception {
        ApplicationTask ret = null;
        if (isCustomTask(taskName)) {
            for (CustomTask task : customTasks)
                if (task.getTaskName().equalsIgnoreCase(taskName)) {
                    ret = task;
                    break;
                }
        } else {
            Class clazz = standardTasks.get(taskName);
            for (Field field : fields) {
                if (field.getType().equals(clazz)) {
                    ret = (ApplicationTask) field.get(this);
                    break;
                }
            }
        }
        return ret;
    }

    protected boolean isCustomTask(String taskName) {
        return !standardTasks.containsKey(taskName);
    }

    public String toTaskString() throws Exception {
        StringBuilder ret = new StringBuilder();
        for (Field field : fields) {
            Class superclass = field.getClass().getSuperclass();
            if (superclass.equals(ApplicationTask.class) ||
                    superclass.equals(MultiEntryApplicationTask.class)) {
                ApplicationTask task = (ApplicationTask) field.get(this);
                ret.append(task.toString());
            }
        }
        return ret.toString();
    }

    protected void loadTasks(List<ApplicationTask> tasks) throws Exception {
        for (ApplicationTask task : tasks) {
            if (task instanceof CustomTask) {
                customTasks.add((CustomTask) task);
            } else {
                // Avoid endless lists of if..then by using reflection
                boolean found = false;
                for (Field field : fields) {
                    if (field.getType().equals(task.getClass())) {
                        field.set(this, task);
                        found = true;
                        break;
                    }
                }
                if (!found)
                    Log.warning(c, "Task not found: " + task.getTaskName() + "; make sure it's in the standardTasks map");
            }
        }
    }

    private static void mapTasksToClasses() {
        standardTasks.put(AppConstants.ActSpecJNDITask, ActSpecJNDITask.class);
        standardTasks.put(AppConstants.AppDeploymentOptionsTask, AppDeploymentOptionsTask.class);
        standardTasks.put(AppConstants.BackendIdSelectionTask, BackendIdSelectionTask.class);
        standardTasks.put(AppConstants.BindJndiForEJBBusinessTask, BindJndiForEJBBusinessTask.class);
        standardTasks.put(AppConstants.BindJndiForEJBMessageBindingTask, BindJndiForEJBMessageBindingTask.class);
        standardTasks.put(AppConstants.BindJndiForEJBNonMessageBindingTask, BindJndiForEJBNonMessageBindingTask.class);
        standardTasks.put(AppConstants.CorrectOracleIsolationLevelTask, CorrectOracleIsolationLevelTask.class);
        standardTasks.put(AppConstants.CorrectUseSystemIdentityTask, CorrectUseSystemIdentityTask.class);
        standardTasks.put(AppConstants.CtxRootForWebMethodTask, CtxRootForWebModTask.class);
        standardTasks.put(AppConstants.DataSourceFor10CMPBeansTask, DataSourceFor10CMPBeansTask.class);
        standardTasks.put(AppConstants.DataSourceFor10EJBModulesTask, DataSourceFor10EJBModulesTask.class);
        standardTasks.put(AppConstants.DataSourceFor20CMPBeansTask, DataSourceFor20CMPBeansTask.class);
        standardTasks.put(AppConstants.DataSourceFor20EJBModulesTask, DataSourceFor20EJBModulesTask.class);
        standardTasks.put(AppConstants.DefaultBindingTask, DefaultBindingTask.class);
        standardTasks.put(AppConstants.EJBDeployOptionsTask, EJBDeployOptionsTask.class);
        standardTasks.put(AppConstants.EmbeddedRarTask, EmbeddedRarTask.class);
        standardTasks.put(AppConstants.EnsureMethodProtectionFor10EJBTask, EnsureMethodProtectionFor10EJBTask.class);
        standardTasks.put(AppConstants.EnsureMethodProtectionFor20EJBTask, EnsureMethodProtectionFor20EJBTask.class);
        standardTasks.put(AppConstants.JSPCompileOptionsTask, JSPCompileOptionsTask.class);
        standardTasks.put(AppConstants.JSPReloadForWebModTask, JSPReloadForWebModTask.class);
        standardTasks.put(AppConstants.ListModulesTaskName, ListModulesTask.class);
        standardTasks.put(AppConstants.MapEJBRefToEJBTask, MapEJBRefToEJBTask.class);
        standardTasks.put(AppConstants.MapModulesToServersTask, MapModulesToServersTask.class);
        standardTasks.put(AppConstants.MapResEnvRefToResTask, MapResEnvRefToResTask.class);
        standardTasks.put(AppConstants.MapResRefToEJBTask, MapResRefToEJBTask.class);
        standardTasks.put(AppConstants.MapRolesToUsersTask, MapRolesToUsersTask.class);
        standardTasks.put(AppConstants.MapRunAsRolesToUsersTask, MapRunAsRolesToUsersTask.class);
        standardTasks.put(AppConstants.MapWebModToVHTask, MapWebModToVHTask.class);
        standardTasks.put(AppConstants.MetadataCompleteForModulesTask, MetadataCompleteForModulesTask.class);
        standardTasks.put(AppConstants.WSDeployOptionsTask, WSDeployOptionsTask.class);
    }

}
