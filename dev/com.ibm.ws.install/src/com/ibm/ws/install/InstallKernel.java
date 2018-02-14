/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import java.io.File;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import com.ibm.ws.install.InstallConstants.ExistsAction;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnectionProxy;

/**
 * This interface provides the APIs to perform Liberty installation.
 */
public interface InstallKernel {

    /**
     * <p>Install the specified feature including the following tasks</p>
     * <ul>
     * <li>determine to install the feature or not</li>
     * <li>get the installed features and APARS</li>
     * <li>call Resolver to resolve the required features and APARs</li>
     * <li>check licenses</li>
     * <li>call Massive Client to download the resources</li>
     * <li>install the downloaded resources</li>
     * </ul>
     * <p/>In case of error, the successfully installed features or ifixes will not be rolled back.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection&lt;String&gt; installedFeatures = installKernel.installFeature(<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"com.ibm.websphere.appserver.jaxb-2.2",<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InstallConstants.TO_CORE,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ExistsAction.fail,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"myid@ca.ibm.com", <br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"mypassword");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO ... <br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param featureId An identifier of the feature to install. It should be in the form:<br/>
     *            <code>{name}[/{version}]</code><br/>
     * @param toExtension value can be either
     *            <ul>
     *            <li>InstallConstants.TO_CORE - the feature will be installed to as a core feature</li>
     *            <li>InstallConstants.TO_USER - the feature will be installed to as an user feature</li>
     *            <li>or extension - the feature will be installed to as an user extension feature</li>
     *            </ul>
     * @param acceptLicense
     * @param existsAction Specify where to install the feature. see {@link com.ibm.ws.install.InstallConstants.ExistsAction}
     * @param userId the user id to connect the default massive repository. If it is null, anonymous user will be used.
     * @param password the user password
     * @return Collection of the names of installed features.<br/>
     *         <ul>
     *         <li>Installed ifixes will not be included in the collection</li>
     *         <li>Empty collection will be returned if the feature already exists.</li>
     *         </ul>
     * @throws InstallException if there is an error
     */
    public Collection<String> installFeature(String featureId, String toExtension, boolean acceptLicense, ExistsAction existsAction, String userId,
                                             String password) throws InstallException;

    /**
     * <p>Install the specified feature including the following tasks</p>
     * <ul>
     * <li>determine to install the feature or not</li>
     * <li>get the installed features and APARS</li>
     * <li>call Resolver to resolve the required features and APARs</li>
     * <li>check licenses</li>
     * <li>call Massive Client to download the resources</li>
     * <li>install the downloaded resources</li>
     * </ul>
     * <p/>In case of error, the successfully installed features or ifixes will not be rolled back.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection&lt;String&gt; installedFeatures = installKernel.installFeature(<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"com.ibm.websphere.appserver.jaxb-2.2",<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InstallConstants.TO_CORE,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ExistsAction.fail);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO ... <br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param featureId An identifier of the feature to install. It should be in the form:<br/>
     *            <code>{name}[/{version}]</code><br/>
     * @param toExtension value can be either
     *            <ul>
     *            <li>InstallConstants.TO_CORE - the feature will be installed to as a core feature</li>
     *            <li>InstallConstants.TO_USER - the feature will be installed to as an user feature</li>
     *            <li>or extension - the feature will be installed to as an user extension feature</li>
     *            </ul>
     * @param acceptLicense
     * @param existsAction Specify where to install the feature. see {@link com.ibm.ws.install.InstallConstants.ExistsAction}
     * @return Collection of the names of installed features.<br/>
     *         <ul>
     *         <li>Installed ifixes will not be included in the collection</li>
     *         <li>Empty collection will be returned if the feature already exists.</li>
     *         </ul>
     * @throws InstallException if there is an error
     */
    public Collection<String> installFeature(String featureId, String toExtension, boolean acceptLicense, ExistsAction existsAction) throws InstallException;

    /**
     * <p>Install the collection of features including the following tasks</p>
     * <ul>
     * <li>determine to install the features or not</li>
     * <li>get the installed features and APARS</li>
     * <li>call Resolver to resolve the required features and APARs</li>
     * <li>check licenses</li>
     * <li>call Massive Client to download the resources</li>
     * <li>install the downloaded resources</li>
     * </ul>
     * <p/>In case of error, the successfully installed features or ifixes will not be rolled back.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ArrayList&lt;String&gt; features = new ArrayList&lt;String&gt;();<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;features.add("com.ibm.websphere.appserver.jaxb-2.2");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection&lt;String&gt; installedFeatures = installKernel.installFeature(<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;features,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InstallConstants.TO_CORE,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ExistsAction.fail,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"myid@ca.ibm.com", <br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"mypassword");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO ... <br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param featureIds An non-empty collection of the identifiers of the features to install. It should be in the form:<br/>
     *            <code>{name}[/{version}]</code><br/>
     * @param toExtension value can be either
     *            <ul>
     *            <li>InstallConstants.TO_CORE - the feature will be installed to as a core feature</li>
     *            <li>InstallConstants.TO_USER - the feature will be installed to as an user feature</li>
     *            <li>or extension - the features will be installed to as user extension features</li>
     *            </ul>
     * @param acceptLicense
     * @param existsAction Specify where to install the feature. see {@link com.ibm.ws.install.InstallConstants.ExistsAction}
     * @param userId the user id to connect the default massive repository. If it is null, anonymous user will be used.
     * @param password the user password
     * @return Collection of the names of installed features.<br/>
     *         <ul>
     *         <li>Installed ifixes will not be included in the collection</li>
     *         <li>Empty collection will be returned if the features already exist.</li>
     *         </ul>
     * @throws InstallException if there is an error
     */
    public Collection<String> installFeature(Collection<String> featureIds, String toExtension, boolean acceptLicense, ExistsAction existsAction, String userId,
                                             String password) throws InstallException;

    /**
     * <p>Install the collection of features including the following tasks</p>
     * <ul>
     * <li>determine to install the features or not</li>
     * <li>get the installed features and APARS</li>
     * <li>call Resolver to resolve the required features and APARs</li>
     * <li>check licenses</li>
     * <li>call Massive Client to download the resources</li>
     * <li>install the downloaded resources</li>
     * </ul>
     * <p/>In case of error, the successfully installed features or ifixes will not be rolled back.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ArrayList&lt;String&gt; features = new ArrayList&lt;String&gt;();<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;features.add("com.ibm.websphere.appserver.jaxb-2.2");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection&lt;String&gt; installedFeatures = installKernel.installFeature(<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;features,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InstallConstants.TO_CORE,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;true,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ExistsAction.fail);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO ... <br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param featureIds An non-empty collection of the identifiers of the features to install. It should be in the form:<br/>
     *            <code>{name}[/{version}]</code><br/>
     * @param toExtension value can be either
     *            <ul>
     *            <li>InstallConstants.TO_CORE - the feature will be installed to as a core feature</li>
     *            <li>InstallConstants.TO_USER - the feature will be installed to as an user feature</li>
     *            <li>or extension - the features will be installed to as user extension features</li>
     *            </ul>
     * @param acceptLicense
     * @param existsAction Specify where to install the feature. see {@link com.ibm.ws.install.InstallConstants.ExistsAction}
     * @return Collection of the names of installed features.<br/>
     *         <ul>
     *         <li>Installed ifixes will not be included in the collection</li>
     *         <li>Empty collection will be returned if the features already exist.</li>
     *         </ul>
     * @throws InstallException if there is an error
     */
    public Collection<String> installFeature(Collection<String> featureIds, String toExtension, boolean acceptLicense, ExistsAction existsAction) throws InstallException;

    /**
     * <p>Install the collection of features from a directory</p>
     *
     * @param featureIds An non-empty collection of the identifiers of the features to install. It should be in the form:<br/>
     *            <code>{name}[/{version}]</code><br/>
     * @param fromDir the directory which contains esa files
     * @param toExtension value can be either
     *            <ul>
     *            <li>InstallConstants.TO_CORE - the feature will be installed to as a core feature</li>
     *            <li>InstallConstants.TO_USER - the feature will be installed to as an user feature</li>
     *            <li>or extension - the features will be installed to as user extension features</li>
     *            </ul>
     * @param acceptLicense
     * @param existAction Specify where to install the feature. see {@link com.ibm.ws.install.InstallConstants.ExistsAction}
     * @param offlineOnly If true, do not search esa from massive repository
     * @return Collection of the names of installed features.<br/>
     *         <ul>
     *         <li>Installed ifixes will not be included in the collection</li>
     *         <li>Empty collection will be returned if the features already exist.</li>
     *         </ul>
     * @throws InstallException if there is an error
     */
    public Collection<String> installFeature(Collection<String> featureIds, File fromDir, String toExtension, boolean acceptLicense, ExistsAction existsAction,
                                             boolean offlineOnly) throws InstallException;

    /**
     * <p>Install the specified ifix including the following tasks</p>
     * <ul>
     * <li>determine to install the ifix or not</li>
     * <li>call Resolver to resolve the required APARs</li>
     * <li>check licenses</li>
     * <li>call Massive Client to download the resources</li>
     * <li>install the downloaded resources</li>
     * </ul>
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;installKernel.installFix("8550-wlp-archive-IFPM89999", "myid@ca.ibm.com", "mypassword");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param fixId An identifier of the ifix to install.
     * @param userId the user id to connect the default massive repository. If it is null, anonymous user will be used.
     * @param password the user password
     * @throws InstallException if there is an error
     */
    public void installFix(String fixId, String userId, String password) throws InstallException;

    /**
     * <p>Install the specified ifix including the following tasks</p>
     * <ul>
     * <li>determine to install the ifix or not</li>
     * <li>call Resolver to resolve the required APARs</li>
     * <li>check licenses</li>
     * <li>call Massive Client to download the resources</li>
     * <li>install the downloaded resources</li>
     * </ul>
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;installKernel.installFix("8550-wlp-archive-IFPM89999");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param fixId An identifier of the ifix to install.
     * @throws InstallException if there is an error
     */
    public void installFix(String fixId) throws InstallException;

    /**
     * Uninstall the specified feature.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;installKernel.uninstallFeature("com.ibm.websphere.appserver.jaxb-2.2");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param featureId - the feature id to be uninstalled
     */
    public void uninstallFeature(String featureId, boolean force) throws InstallException;

    public void uninstallFeaturePrereqChecking(String featureId, boolean allowUserFeatureUninstall, boolean force) throws InstallException;

    public void uninstallFeaturePrereqChecking(Collection<String> featureIds) throws InstallException;

    public void uninstallCoreFeaturePrereqChecking(Collection<String> featureIds) throws InstallException;

    /**
     * Uninstall the collection of features including to check that
     * that there is no other features still require the uninstalling features.
     * The uninstalling features will be uninstalled according to the order which is determined by
     * the dependency checking.
     *
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ArrayList&lt;String&gt; features = new ArrayList&lt;String&gt;();<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;features.add("com.ibm.websphere.appserver.jaxb-2.2");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;installKernel.uninstallFeature(features);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param featureIds an non-empty collection of the feature ids to be uninstalled
     * @throws InstallException if there is an error
     */
    public void uninstallFeature(Collection<String> featureIds) throws InstallException;

    /**
     * Uninstall the specified ifix.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;installKernel.uninstallFix("9000-wlp-archive-ifts10001");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param fixId - the ifix id to be uninstalled
     */
    public void uninstallFix(String fixId) throws InstallException;

    /**
     * This api is not ready yet.
     * Uninstall the collection of ifixes.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ArrayList&lt;String&gt; ifixes = new ArrayList&lt;String&gt;();<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;features.add("9000-wlp-archive-ifts10001");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;installKernel.uninstallFix(ifixes);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param fixIds a collection of ifix ids to be uninstalled
     */
    public void uninstallFix(Collection<String> fixIds) throws InstallException;

    /**
     * Uninstall the specified product.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;installKernel.uninstallFeaturesByProductId("com.ibm.websphere.appserver");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param productId - the product id to be uninstalled
     */
    public void uninstallFeaturesByProductId(String productId) throws InstallException;

    /**
     * Uninstall the specified product and delete the specified files.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection&lt;File&gt; toBeDeleted = new ArrayList&lt;File&gt;();<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;toBeDeleted.add(new File(wlpInstallRoot, "lafiles"));<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;//toBeDeleted.add(new File(...));<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;installKernel.uninstallFeaturesByProductId("com.ibm.websphere.appserver", toBeDeleted);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param productId - the product id to be uninstalled
     * @param toBeDeleted - the collection of files to be deleted
     */
    public void uninstallFeaturesByProductId(String productId, Collection<File> toBeDeleted) throws InstallException;

    /**
     * Uninstall the specified product features and delete the specified files.
     * Platform features will not be uninstalled.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection&lt;File&gt; toBeDeleted = new ArrayList&lt;File&gt;();<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;toBeDeleted.add(new File(wlpInstallRoot, "lafiles"));<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;//toBeDeleted.add(new File(...));<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;installKernel.uninstallProductFeatures("com.ibm.websphere.appserver", toBeDeleted);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param productId - the product id to be uninstalled
     * @param toBeDeleted - the collection of files to be deleted
     */
    public void uninstallProductFeatures(String productId, Collection<File> toBeDeleted) throws InstallException;

    /**
     * Add the listener to get the progress from the install kernel.
     * <p/>
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallEventListener listener = new InstallEventListener() {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;public void handleInstallEvent(InstallProgressEvent event) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;System.out.println(event.progress + "% completed");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;});<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;installKernel.addListener(listener);<br/>
     * </code>
     *
     * @param listener An instance implements the interface InstallEventListener
     * @param notificationType The type of listener. Currently supports
     *            <ul>
     *            <li>InstallConstants.EVENT_TYPE_PROGRESS = "PROGRESS" </li>
     *            </ul>
     */
    public void addListener(InstallEventListener listener, String notificationType);

    /**
     * Remove the listener from the install kernel.
     * <p/>
     *
     * @param listener The instance which implements the interface InstallEventListener to be removed
     */
    public void removeListener(InstallEventListener listener);

    /**
     * Determine the licenses of the feature are required.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Set&lt;InstallLicense&gt; licenses = installKernel.getFeatureLicense(<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"com.ibm.websphere.appserver.jaxb-2.2",<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"myid@ca.ibm.com",<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"mypassword");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO ...<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param featureId
     * @param locale
     * @param userId the user id to connect the default massive repository. If it is null, anonymous user will be used.
     * @param password the user password
     * @return a set of licenses
     * @throws InstallException if there is an error,
     *             e.g. the feature is not available in the massive repository
     */
    public Set<InstallLicense> getFeatureLicense(String featureId, Locale locale, String userId, String password) throws InstallException;

    /**
     * Determine the licenses of the feature are required.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Set&lt;InstallLicense&gt; licenses = installKernel.getFeatureLicense(<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"com.ibm.websphere.appserver.jaxb-2.2");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO ...<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param featureId
     * @param locale
     * @return a set of licenses
     * @throws InstallException if there is an error,
     *             e.g. the feature is not available in the massive repository
     */
    public Set<InstallLicense> getFeatureLicense(String featureId, Locale locale) throws InstallException;

    /**
     * Determine the licenses of a collection of features are required.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ArrayList&lt;String&gt; features = new ArrayList&lt;String&gt;();<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;features.add("com.ibm.websphere.appserver.jaxb-2.2");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Set&lt;InstallLicense&gt; licenses = installKernel.getFeatureLicense(<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;features,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"myid@ca.ibm.com",<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"mypassword");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO ...<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param featureIds An non-empty collection of the identifiers of the features to install. It should be in the form:<br/>
     *            <code>{name}[/{version}]</code><br/>
     * @param locale
     * @param userId the user id to connect the default massive repository. If it is null, anonymous user will be used.
     * @param password the user password
     * @return a set of licenses
     * @throws InstallException if there is an error,
     *             e.g. the feature is not available in the massive repository
     */
    public Set<InstallLicense> getFeatureLicense(Collection<String> featureIds, Locale locale, String userId, String password) throws InstallException;

    /**
     * Determine the licenses of a collection of features are required.
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ArrayList&lt;String&gt; features = new ArrayList&lt;String&gt;();<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;features.add("com.ibm.websphere.appserver.jaxb-2.2");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Set&lt;InstallLicense&gt; licenses = installKernel.getFeatureLicense(<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;features);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO ...<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param featureIds An non-empty collection of the identifiers of the features to install. It should be in the form:<br/>
     *            <code>{name}[/{version}]</code><br/>
     * @param locale
     * @return a set of licenses
     * @throws InstallException if there is an error,
     *             e.g. the feature is not available in the massive repository
     */
    public Set<InstallLicense> getFeatureLicense(Collection<String> featureIds, Locale locale) throws InstallException;

    /**
     * This api is not ready.
     * Returns a set of the installed license types.
     *
     * @return Set&lt;String&gt;
     */
    public Set<String> getInstalledLicense();

    /**
     * Returns a set of symbolic name of the installed features.
     *
     * @param installedBy value can be either
     *            <ul>
     *            <li>InstallConstants.TO_CORE - to get the core features</li>
     *            <li>InstallConstants.TO_USER - to get the features in default usr product extension location</li>
     *            <li>or extension - to get the features in product extension locations defined in etc/extensions</li>
     *            </ul>
     * @return Set&lt;String&gt;
     */
    public Set<String> getInstalledFeatures(String installedBy);

    /**
     * Returns the installed core features at wlp/lib/platforms or wlp/lib/features
     *
     * @return Map&lt;String,InstalledFeature&gt; or null if the installation directory is not valid.
     */
    public Map<String, InstalledFeature> getInstalledFeatures();

    /**
     * Returns the installed feature collections
     *
     * @return Map&lt;String,InstalledFeatureCollection&gt; or null if the installation directory is not valid.
     */
    public Map<String, InstalledFeatureCollection> getInstalledFeatureCollections();

    /**
     * By default, InstallKernel does not use the logger parent's handlers to publish
     * log records. Call this api to enable InstallKernel to publish the log records with INFO, WARNING,
     * or FINE level to the System.out and the logs with SEVERE level to the System.err.
     * Note: InstallException will be logged as FINE level without message.
     *
     * @param level the log level for the logger
     */
    public void enableConsoleLog(Level level);

    /**
     * Set the userAgent with kernel user info to distinguish installUtility and featureManager users
     *
     * @param a string indication either featureManager or installUtility
     */
    public void setUserAgent(String kernelUser);

    /**
     * Set the repository properties instance, which can be used to access and manage repository configurations
     *
     * @param repoProperties the properties instance for the repository properties.
     */
    public void setRepositoryProperties(Properties repoProperties);

    /**
     * <p>Install the collection of assets.</p>
     * <ul>
     * <li>Core features will be installed to wlp/lib</li>
     * <li>User features will be installed to wlp/usr</li>
     * <li>For the sample, the server will be deployed to wlp/usr and the required feature will be downloaded and installed.</li>
     * <li>Assumed that the licenses are already accepted.</li>
     * <li>If a file already exists, it will be replaced.</li>
     * </ul>
     *
     * <p/><b>Example:</b><br/>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;File wlpInstallRoot = new File("./wlp");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;InstallKernel installKernel = InstallKernelFactory.getInstance(wlpInstallRoot);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;try {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ArrayList&lt;String&gt; assets = new ArrayList&lt;String&gt;();<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;assets.add("com.ibm.websphere.appserver.jaxb-2.2");<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection&lt;String&gt; installedFeatures = installKernel.installAsset(<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;assets,<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;loginInfo);<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO ... <br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;} catch (InstallException e) {<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// TODO: handle the exception<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
     * </code>
     *
     * @param featureIds An non-empty collection of the identifiers of the assets to install. It should be in the form:<br/>
     *            <code>{name}</code><br/>
     * @param loginInfo Specify the massive LoginInfo object which can be defined multiple LoginInfoEntries and the proxy information.
     * @param proxyHost Specify the proxy server hostname.
     * @param proxyPort Specify the proxy server port.
     * @param proxyUser Specify the user if proxy server requires authenication.
     * @param proxyPwd Specify the user password if proxy server requires authenication.
     * @return Map of the names of installed assets.<br/>
     * @throws InstallException if there is an error
     */
    public Map<String, Collection<String>> installAsset(Collection<String> assetIds, RepositoryConnectionList loginInfo, String proxyHost, String proxyPort, String proxyUser,
                                                        String proxyPwd) throws InstallException;

    /**
     * <p>Install the collection of assets.</p>
     * <ul>
     * <li>Core features will be installed to wlp/lib</li>
     * <li>User features will be installed to wlp/usr</li>
     * <li>For the sample, the server will be deployed to wlp/usr and the required feature will be downloaded and installed.</li>
     * <li>Assumed that the licenses are already accepted.</li>
     * <li>If a file already exists, it will be replaced.</li>
     * </ul>
     *
     * @param featureIds An non-empty collection of the identifiers of the assets to install. It should be in the form:<br/>
     *            <code>{name}</code><br/>
     * @param fromDir the directory which contains esa files
     * @param loginInfo Specify the massive LoginInfo object which can be defined multiple LoginInfoEntries and the proxy information.
     * @param proxyHost Specify the proxy server hostname.
     * @param proxyPort Specify the proxy server port.
     * @param proxyUser Specify the user if proxy server requires authenication.
     * @param proxyPwd Specify the user password if proxy server requires authenication.
     * @return Map of the names of installed assets.<br/>
     * @throws InstallException if there is an error
     */
    public Map<String, Collection<String>> installAsset(Collection<String> assetIds, File fromDir, RepositoryConnectionList loginInfo, String proxyHost, String proxyPort,
                                                        String proxyUser,
                                                        String proxyPwd) throws InstallException;

    /**
     * Returns the LoginInfo Object.
     *
     * @return The object list of loginInfo, which contains all the information of the configured repositories
     * @throws InstallException if there is an error
     */
    public RepositoryConnectionList getLoginInfo() throws InstallException;

    /**
     * Sets the LoginInfo Object.
     *
     * @param loginInfo Specify the massive LoginInfo object which can be defined multiple LoginInfoEntries and the proxy information.
     */
    public void setLoginInfo(RepositoryConnectionList loginInfo);

    /**
     * Sets the LoginInfoProxy Object.
     *
     * @param proxy Specify the LoginInfoProxy object in the install kernel.
     */
    public void setProxy(RestRepositoryConnectionProxy proxy);

    /**
     * <p>Install the collection of assets.</p>
     * <ul>
     * <li>Core features will be installed to wlp/lib</li>
     * <li>User features will be installed to wlp/usr</li>
     * <li>For the sample, the server will be deployed to wlp/usr and the required feature will be downloaded and installed.</li>
     * <li>Assumed that the licenses are already accepted.</li>
     * <li>If a file already exists, it will be replaced.</li>
     * </ul>
     *
     * @param featureIds An non-empty collection of the identifiers of the assets to install. It should be in the form:<br/>
     *            <code>{name}</code><br/>
     * @param fromDir the directory which contains esa files
     * @param loginInfo Specify the massive LoginInfo object which can be defined multiple LoginInfoEntries and the proxy information.
     * @param proxyHost Specify the proxy server hostname.
     * @param proxyPort Specify the proxy server port.
     * @param proxyUser Specify the user if proxy server requires authenication.
     * @param proxyPwd Specify the user password if proxy server requires authenication.
     * @param downloadDependencies Specify to download external dependencies or not
     * @return Map of the names of installed assets.<br/>
     * @throws InstallException if there is an error
     */
    public Map<String, Collection<String>> installAsset(Collection<String> assetIds, File fromDir, RepositoryConnectionList loginInfo, String proxyHost, String proxyPort,
                                                        String proxyUser,
                                                        String proxyPwd, boolean downloadDependencies) throws InstallException;
}
