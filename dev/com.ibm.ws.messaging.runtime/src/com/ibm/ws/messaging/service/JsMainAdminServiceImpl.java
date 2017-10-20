/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.service;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.AliasDestination;
import com.ibm.ws.sib.admin.BaseDestination;
import com.ibm.ws.sib.admin.InvalidFileStoreConfigurationException;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMEConfig;
import com.ibm.ws.sib.admin.JsMainAdminService;
import com.ibm.ws.sib.admin.SIBDestination;
import com.ibm.ws.sib.admin.SIBFileStore;
import com.ibm.ws.sib.admin.SIBLocalizationPoint;
import com.ibm.ws.sib.admin.SIBMessagingEngine;
import com.ibm.ws.sib.admin.SIBus;
import com.ibm.ws.sib.admin.internal.AliasDestinationImpl;
import com.ibm.ws.sib.admin.internal.JsAdminConstants;
import com.ibm.ws.sib.admin.internal.JsAdminConstants.ME_STATE;
import com.ibm.ws.sib.admin.internal.JsMainImpl;
import com.ibm.ws.sib.admin.internal.SIBDestinationImpl;
import com.ibm.ws.sib.admin.internal.SIBFileStoreImpl;
import com.ibm.ws.sib.admin.internal.SIBLocalizationPointImpl;
import com.ibm.ws.sib.admin.internal.SIBMessagingEngineImpl;
import com.ibm.ws.sib.admin.internal.SIBusImpl;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.sib.core.DestinationType;

/**
 * A Singleton class to fetch JsAdminServiceImpl
 */
public class JsMainAdminServiceImpl extends JsMainAdminService {

    /** RAS trace variable */
    private static final TraceComponent tc = SibTr.register(
                                                            JsMainAdminServiceImpl.class, JsConstants.TRGRP_AS,
                                                            JsConstants.MSG_BUNDLE);
    private static JsMEConfig jsMEConfig = new JsMEConfig();

    private String _state = ME_STATE.STOPPED.toString();

    private JsMainImpl _jsMainImpl = null;

    private volatile Map<String, Object> properties;

    private final Set<String> pids = new HashSet<String>();
    private String bundleLocation;

    /**
     * Constructs the JsMEConfig object based upon the Map and Activates various
     * ME components
     */
    @Override
    public void activate(ComponentContext context,
                         Map<String, Object> properties, ConfigurationAdmin configAdmin) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "activate", new Object[] { context, properties,
                                                      configAdmin });
        }
        try {
            // set ME state to starting
            _state = ME_STATE.STARTING.toString();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Starting the JMS server.");
            }

            // initilize config object
            initialize(context, properties, configAdmin);

            _jsMainImpl = new JsMainImpl(context.getBundleContext());
            _jsMainImpl.initialize(jsMEConfig);
            _jsMainImpl.start();

            // If its here it means all the components have started hence set
            // the state to STARTED
            _state = ME_STATE.STARTED.toString();
            SibTr.info(tc, "ME_STARTED_SIAS0108");
        } catch (InvalidFileStoreConfigurationException ifs) {
            // since there is exception in starting ME the state is set to
            // STOPPED
            _state = ME_STATE.STOPPED.toString();

            SibTr.error(tc, "ME_STOPPED_SIAS0109");
            SibTr.exception(tc, ifs);
            FFDCFilter.processException(ifs,
                                        "com.ibm.ws.messaging.service.JsMainAdminServiceImpl",
                                        "132", this);
        } catch (Exception e) {
            // since there is exception in starting ME the state is set to
            // STOPPED
            _state = ME_STATE.STOPPED.toString();

            SibTr.error(tc, "ME_STOPPED_SIAS0109");
            SibTr.exception(tc, e);
            FFDCFilter.processException(e,
                                        "com.ibm.ws.messaging.service.JsMainAdminServiceImpl",
                                        "139", this);

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "activate");
        }
    }

    /**
     * Initializes the JsMEConfig Object Creates a defaultQueue and defaultTopic
     * as well.If filestore is not mentioned in server.xml an default filestore
     * is created
     * 
     * @param context
     * @param properties
     * @throws InvalidFileStoreConfigurationException
     */
    private void initialize(ComponentContext context,
                            Map<String, Object> properties, ConfigurationAdmin configAdmin)
                    throws InvalidFileStoreConfigurationException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "initialize", new Object[] { context, properties });
        }
        this.properties = properties;
        this.bundleLocation = context.getBundleContext().getBundle().getLocation();
        
        // populate filestore
        SIBFileStore filestore = new SIBFileStoreImpl();
        populateFileStore(properties, filestore, configAdmin);

        // set bus properties
        // nothing to set for bus as only defaults are considered
        // uuid will be set by the processor
        SIBus bus = new SIBusImpl();

        // set the properties for mesagingengine object.first the queues or
        // topic is got from
        // Administration Config and list of SIBBestination is created
        // and then the localization list is created for those SIBdestination
        SIBMessagingEngine messagingEngine = new SIBMessagingEngineImpl();

        messagingEngine.setHighMessageThreshold((Long) properties
                        .get(JsAdminConstants.HIGHMESSAGETHRESHOLD));

        HashMap<String, BaseDestination> destinationList = new HashMap<String, BaseDestination>();
        HashMap<String, SIBLocalizationPoint> destinationLocalizationList = new HashMap<String, SIBLocalizationPoint>();

        // populate destinations of type QUEUE
        populateDestinations(properties, destinationList, destinationLocalizationList, messagingEngine.getName(), JsAdminConstants.QUEUE, configAdmin,
                             false);
        // populate destinations of type TOPICSPACE
        populateDestinations(properties, destinationList, destinationLocalizationList, messagingEngine.getName(), JsAdminConstants.TOPICSPACE, configAdmin,
                             false);
        //populate destinations of type ALIAS
        populateAliasDestinations(properties, destinationList, configAdmin);

        // processor will update the UUID via Admin.Since we dont get UUID info from server.xml
        messagingEngine.setDestinationList(destinationList);
        messagingEngine.setSibLocalizationPointList(destinationLocalizationList);

        // set all the constructed artifact to jsMEConfig
        jsMEConfig.setMessagingEngine(messagingEngine);
        jsMEConfig.setSIBFilestore(filestore);
        jsMEConfig.setSIBus(bus);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "initialize");
        }

    }

    /**
     * Creates SIBDestination objects and is added to destination list.
     * Destination is constructed from the queue or topic tags in server.xml
     * Creates default queue or topic if not provided by user in server.xml
     * Deals with destinations only of type Queue or Topic
     * 
     * Some properties even though not there in config it is set
     * to maintain backward compatibility with the runtime code.
     * 
     * @param properties
     * @param destinationList
     * @param destinationType
     * @param configAdmin
     */
    private void populateDestinations(Map<String, Object> properties,
                                      HashMap<String, BaseDestination> destinationList, HashMap<String, SIBLocalizationPoint> destinationLocalizationList,
                                      String meName, String destinationType, ConfigurationAdmin configAdmin,
                                      boolean modified) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "populateDestinations", new Object[] { properties,
                                                                  destinationList, destinationLocalizationList, meName, destinationType, configAdmin });
        }
        String[] destinations = (String[]) properties.get(destinationType);
        // flag indicating if user have provided default queue or topic.
        // If user have overridden then we need to consider that instead of
        // using defaults
        boolean defaultQueueTopicProvided = false;
        // flag indicating if user have provided the _SYSTEM.Exception.Destination
        boolean exceptionDestinationProvided = false;

        if (destinations != null) {

            for (String destinationPid : destinations) {
                pids.add(destinationPid);
                Configuration config = null;
                try {
                    config = configAdmin.getConfiguration(destinationPid, bundleLocation);
                } catch (IOException e) {
                    SibTr.exception(tc, e);
                    FFDCFilter.processException(e, this.getClass().getName(), "369", this);
                }
                Dictionary destinationProperties = config.getProperties();
                SIBDestination destination = new SIBDestinationImpl();
                SIBLocalizationPoint destinationLocalization = new SIBLocalizationPointImpl();

                if (destinationProperties != null) {
                    if (destinationProperties.get(JsAdminConstants.ID) != null
                        && !destinationProperties.get(JsAdminConstants.ID).toString().trim()
                                        .isEmpty()) {
                        if (TraceComponent.isAnyTracingEnabled()
                            && tc.isEntryEnabled()) {
                            SibTr.debug(this, tc, "Destination ID :  "
                                                  + destinationProperties.get(JsAdminConstants.ID));
                        }

                        String destinationName = (String) destinationProperties.get(JsAdminConstants.ID);

                        // we hit this scenario when use have given same id for <queue> and <topic>
                        // tags.We always consider queue first while constructing the destinations.
                        // Hence we ignore the destinaiton
                        if (destinationList.containsKey(destinationName)) {
                            SibTr.warning(tc, "SAME_DEST_ID_SIAS0123", new Object[] { destinationName });
                            continue;
                        }

                        // set the name of the queue.Here ID is considered as the name
                        destination.setName(destinationName);
                        // set the destiantion type as Queue
                        if (destinationType.equals(JsAdminConstants.QUEUE)) {
                            destination.setDestinationType(DestinationType.QUEUE);

                            if (destinationProperties.get(JsAdminConstants.ID).equals(JsAdminConstants.DEFAULTQUEUE))
                                defaultQueueTopicProvided = true;
                            if (destinationProperties.get(JsAdminConstants.ID).equals(JsAdminConstants.EXCEPTION_DESTINATION))
                                exceptionDestinationProvided = true;
                        } else { //TOPICSPACE
                            destination
                                            .setDestinationType(DestinationType.TOPICSPACE);

                            if (destinationProperties.get(JsAdminConstants.ID).equals(JsAdminConstants.DEFAULTTOPIC))
                                defaultQueueTopicProvided = true;
                        }
                        //here local is true and alias is false as we are negotiating the destination 
                        //of type Queue or Topic and not Alias or Foreign 
                        destination.setLocal(true);
                        destination.setAlias(false);

                        // The max and default have been replaced with forceReliability in the config
                        // overrideOfQOSByProducerAllowed is removed from config but we have to pass 
                        // to admin hence it is set as true by default in SIBDestinationImpl
                        String forceReliability = (String) destinationProperties
                                        .get(JsAdminConstants.FORCERELIABILITY);
                        String defaultReliability = forceReliability;
                        String maxReliability = forceReliability;

                        //set the defaultReliability and maxReliability
                        //if defaultReliability > maxReliability then consider the defaults for both
                        //During modified if the condition is not satisfied, the old values are retained
                        destination.setDefaultAndMaxReliability(defaultReliability, maxReliability, jsMEConfig, modified);

                        //Set the exception destination
                        String exceptionDest = (String) destinationProperties
                                        .get(JsAdminConstants.EXCEPTIONDESTINATION);
                        if (exceptionDest.trim().isEmpty())
                            destination.setExceptionDestination(JsAdminConstants.EXCEPTION_DESTINATION);
                        else
                            destination.setExceptionDestination(exceptionDest);
                        String failedDeliveryPolicy = (String) destinationProperties.get(JsAdminConstants.FAILEDDELIVERYPOLICY);
                        Long redeliveryInterval = (Long) destinationProperties
                                        .get(JsAdminConstants.REDELIVERYINTERVAL);
                        if (redeliveryInterval < 1) {
                            redeliveryInterval = JsAdminConstants.DEFAULT_REDELIVERYINTERVAL_VALUE;
                        }
                        destination.setBlockedRetryTimeout(redeliveryInterval);

                        if (failedDeliveryPolicy.equals(JsAdminConstants.KEEP_TRYING)) {
                            destination.setFailedDeliveryPolicy(failedDeliveryPolicy);
                            destination.setExceptionDestination(null);
                        } else if (failedDeliveryPolicy.equals(JsAdminConstants.DISCARD)) {
                            destination.setExceptionDestination(null);
                            // Exception Discard Reliability is the property which makes the messages (having 
                            // reliability level less than whatever specified) get discarded after trying
                            // for a maximum number of attempts as specified in maxRedelivery count
                            // Setting it to ASSUREDPERSISTENT, makes sure that all the messages will get discarded
                            // once it reaches the maxRedelivery Count
                            destination.setExceptionDiscardReliability(JsAdminConstants.ASSUREDPERSISTENT);
                        }
                        Integer maxRedeliveryCount = (Integer) destinationProperties
                                        .get(JsAdminConstants.MAXREDELIVERYCOUNT);
                        destination
                                        .setMaxFailedDeliveries(maxRedeliveryCount);

                        destination.setSendAllowed((Boolean) destinationProperties
                                        .get(JsAdminConstants.SENDALLOWED));
                        destination
                                        .setReceiveAllowed((Boolean) destinationProperties
                                                        .get(JsAdminConstants.RECEIVEALLOWED));

                        // in liberty release the maintainstrictorder and receiveexclusive are 
                        // having the same value.This is because to get strict order 
                        // it is required to have a single consumer consuming the message
                        Boolean maintainStrictOrder = (Boolean) destinationProperties
                                        .get(JsAdminConstants.MAINTAINSTRICTORDER);
                        destination
                                        .setMaintainStrictOrder(maintainStrictOrder);
                        destination
                                        .setReceiveExclusive(maintainStrictOrder);

                        Long maxQueueDepth = (Long) destinationProperties
                                        .get(JsAdminConstants.MAXMESSAGEDEPTH);
                        destination.setHighMessageThreshold(maxQueueDepth);

                    } else {
                        SibTr.error(tc, "NO_ID_PROVIDED_SIAS0102", new Object[] {
                                    destinationType });
                        continue;

                    }
                    // destination localization identifier is always in the format "destinationname@mename"
                    destinationLocalization.setIdentifier(destination.getName() + "@" + meName);
                    destinationLocalization.setSendAllowed(destination.isSendAllowed());
                    destinationLocalization.setHighMsgThreshold(destination.getHighMessageThreshold());

                    destinationList.put(destination.getName(), destination);
                    destinationLocalizationList.put(destination.getName() + "@" + meName, destinationLocalization);

                } else {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        SibTr.debug(this, tc, "destinationProperties is null");
                    }
                }

            }// end of for loop
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                SibTr.debug(this, tc, "No " + destinationType
                                      + "defined in server.xml");
            }
        }

        //create the default exception destiantion
        if (destinationType.equals(JsAdminConstants.QUEUE) && !exceptionDestinationProvided) {
            if (!modified) {
                SIBDestination exceptionDest = new SIBDestinationImpl(JsAdminConstants.EXCEPTION_DESTINATION, DestinationType.QUEUE);
                exceptionDest.setExceptionDestination(null);//set the exceptiondestination to empty as it itself is the exceptiondestiantion

                SIBLocalizationPoint exceptionDestLocalization = new SIBLocalizationPointImpl();

                exceptionDestLocalization.setIdentifier(exceptionDest.getName() + "@" + meName);
                exceptionDestLocalization.setSendAllowed(exceptionDest.isSendAllowed());
                exceptionDestLocalization.setHighMsgThreshold(exceptionDest.getHighMessageThreshold());

                destinationList.put(exceptionDest.getName(), exceptionDest);
                destinationLocalizationList.put(exceptionDest.getName() + "@" + meName, exceptionDestLocalization);
            }

        }

        // create defaults if not provided - during activate()
        // if during modified the defaultQueue or defaultTopic is deleted, we
        // should not create the new defaults as the old has to be retained
        if (destinationType.equals(JsAdminConstants.QUEUE) && !defaultQueueTopicProvided) {
            if (!modified) {
                SIBDestination defaultQueue = new SIBDestinationImpl(JsAdminConstants.DEFAULTQUEUE, DestinationType.QUEUE);
                SIBLocalizationPoint defaultQueueLocalization = new SIBLocalizationPointImpl();

                defaultQueueLocalization.setIdentifier(defaultQueue.getName() + "@" + meName);
                defaultQueueLocalization.setSendAllowed(defaultQueue.isSendAllowed());
                defaultQueueLocalization.setHighMsgThreshold(defaultQueue.getHighMessageThreshold());

                destinationList.put(defaultQueue.getName(), defaultQueue);
                destinationLocalizationList.put(defaultQueue.getName() + "@" + meName, defaultQueueLocalization);
            }

        } else if (destinationType.equals(JsAdminConstants.TOPICSPACE) && !defaultQueueTopicProvided) {
            if (!modified) {
                SIBDestination defaultTopic = new SIBDestinationImpl(
                                JsAdminConstants.DEFAULTTOPIC, DestinationType.TOPICSPACE);
                SIBLocalizationPoint defaultTopicLocalization = new SIBLocalizationPointImpl();

                defaultTopicLocalization.setIdentifier(defaultTopic.getName() + "@" + meName);
                defaultTopicLocalization.setSendAllowed(defaultTopic.isSendAllowed());
                defaultTopicLocalization.setHighMsgThreshold(defaultTopic.getHighMessageThreshold());

                destinationList.put(defaultTopic.getName(), defaultTopic);
                destinationLocalizationList.put(defaultTopic.getName() + "@" + meName, defaultTopicLocalization);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "populateDestiantions", new Object[] { destinationList });
        }

    }

    /**
     * This method is only used to populate destination of type Alias
     * 
     * 
     * Some properties even though not there in config it is set
     * to maintain backward compatibility with the runtime code.
     * 
     * @param properties
     * @param destinationList
     * @param meName
     * @param configAdmin
     * @param modified
     */
    private void populateAliasDestinations(Map<String, Object> properties,
                                           HashMap<String, BaseDestination> destinationList, ConfigurationAdmin configAdmin) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "populateAliasDestinations", new Object[] { properties, destinationList, configAdmin });
        }

        String[] aliasDestinations = (String[]) properties.get(JsAdminConstants.ALIAS);

        if (aliasDestinations != null) {

            for (String aliasDestinationPid : aliasDestinations) {
                pids.add(aliasDestinationPid);
                Configuration config = null;
                try {
                    config = configAdmin.getConfiguration(aliasDestinationPid, bundleLocation);
                } catch (IOException e) {
                    SibTr.exception(tc, e);
                    FFDCFilter.processException(e, this.getClass().getName(), "561", this);
                }
                Dictionary aliasDestinationProperties = config.getProperties();
                AliasDestination aliasDest = new AliasDestinationImpl();

                String aliasDestinationName = (String) aliasDestinationProperties.get(JsAdminConstants.ID);
                String targetDestinationName = (String) aliasDestinationProperties.get(JsAdminConstants.TARGETDESTINATION);

                if (destinationList.containsKey(aliasDestinationName)) {
                    SibTr.error(tc, "ALIAS_SAME_DEST_ID_SIAS0125", new Object[] { aliasDestinationName });
                    continue;
                }

                if (aliasDestinationName != null && !aliasDestinationName.toString().trim()
                                .isEmpty()) {
                    if (targetDestinationName == null || targetDestinationName.toString().trim()
                                    .isEmpty()) {
                        SibTr.error(tc, "INVALID_TARGET_DEST_SIAS0110", new Object[] { aliasDestinationProperties.get(JsAdminConstants.ID) });
                        continue;
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        SibTr.debug(this, tc, "Destination ID :  " + aliasDestinationProperties.get(JsAdminConstants.ID));
                    }

                    // set the name of the queue.Here ID is considered as the name
                    aliasDest.setName(aliasDestinationName);

                    // set the target destination
                    aliasDest.setTargetDestination(targetDestinationName);

                    //here local is false and alias is true as we are negotiating the destination 
                    //of type Alias and not Queue or Topic
                    aliasDest.setLocal(false);
                    aliasDest.setAlias(true);

                    // set overrideOfQOSByProducerAllowed
                    String forceReliablility = (String) aliasDestinationProperties.get(JsAdminConstants.FORCERELIABILITY);
                    aliasDest.setDefaultReliability(forceReliablility);
                    aliasDest.setMaximumReliability(forceReliablility);

                    String sendAllowed = "false";
                    String receiveAllowed = "false";
                    if (destinationList.get(targetDestinationName) instanceof SIBDestination) {
                        SIBDestination targetDestination = (SIBDestination) destinationList.get(targetDestinationName);
                        if (targetDestination.isSendAllowed()) {
                            sendAllowed = ((String) aliasDestinationProperties
                                            .get(JsAdminConstants.SENDALLOWED));
                        }
                        receiveAllowed = String.valueOf(targetDestination.isReceiveAllowed());
                    }
                    aliasDest.setSendAllowed(sendAllowed);
                    aliasDest.setReceiveAllowed(receiveAllowed);

                } else {
                    SibTr.error(tc, "NO_ID_PROVIDED_SIAS0102", new Object[] {
                                JsAdminConstants.ALIAS });
                    continue;
                }

                destinationList.put(aliasDest.getName(), aliasDest);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "populateAliasDestinations", new Object[] { destinationList });
        }
    }

    /**
     * A SIBFilestore object is created using the filestore tag mentioned in the
     * server.xml If nothing is mentioned in the server.xml the defaults are
     * considered
     * 
     * @param properties
     * @param filestore
     * @param configAdmin
     * @throws InvalidFileStoreConfigurationException
     */
    private void populateFileStore(Map<String, Object> properties,
                                   SIBFileStore filestore, ConfigurationAdmin configAdmin)
                    throws InvalidFileStoreConfigurationException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "populateFileStore", new Object[] { properties,
                                                               filestore, configAdmin });
        }
        String[] sFileStore = (String[]) properties.get(JsAdminConstants.FILESTORE);

        if (sFileStore == null) { // filstore tag is not mentioned in server.xml
            // filestore object is already having the defaults
            // set the default filestore path
            String path = null;
            path = resolveFileStorePath(filestore.getPath());
            filestore.setPath(path);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "The filestore path: " + path);
                SibTr.debug(this, tc, "FileStore tag has not been defined in the server.xml file, hence defaults will be considered.");
            }

        } else {

            pids.add(sFileStore[0]);
            Configuration config = null;
            try {
                config = configAdmin.getConfiguration(sFileStore[0], bundleLocation);
            } catch (IOException e) {
                SibTr.exception(tc, e);
                FFDCFilter.processException(e, this.getClass().getName(), "671", this);
            }
            Dictionary fsProp = config.getProperties();

            // get and set filestore properties
            // uuid will be set by the processor
            String fsPath = (String) fsProp.get(JsAdminConstants.PATH);
            // Create a File to check if the path specified is absolute or relative
            File dummyFile = new File(fsPath);
            if (dummyFile.isAbsolute()) {
                filestore.setPath(fsPath);
            }
            else {
                // If the path is relative then we will create the filestore in the directory stucture
                // <server_home>/messaging/<relative_path>
                String completePath = WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR
                                      + "/messaging/" + fsPath;
                filestore.setPath(resolveFileStorePath(completePath));
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "The filestore path: " + filestore.getPath());
            }
            // size is given is MB in server.xml.Hence have to convert to Bytes

            filestore.setFileStoreSize((Long) fsProp.get(JsAdminConstants.FILESTORESIZE) * 1024 * 1024);

            filestore
                            .setLogFileSize((Long) fsProp.get(JsAdminConstants.LOGFILESIZE) * 1024 * 1024);

            //validate the filestore settings 
            filestore.validateFileStoreSettings();

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "populateFileStore");
        }
    }

    /**
     * Using com.ibm.wsspi.kernel.service.location.WsLocationAdmin to resolve
     * ${wlp.server.name}/messaging/messageStore
     * 
     * @param fileStorePath
     * @return
     */
    private String resolveFileStorePath(String fileStorePath) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "resolveFileStorePath", new Object[] { fileStorePath });
        }

        String filePath = null;

        try {
            BundleContext bundleContext = FrameworkUtil.getBundle(
                                                                  WsLocationAdmin.class).getBundleContext();
            ServiceReference<WsLocationAdmin> locationAdminRef = bundleContext
                            .getServiceReference(WsLocationAdmin.class);
            WsLocationAdmin locationAdmin = bundleContext
                            .getService(locationAdminRef);
            filePath = locationAdmin.resolveString(fileStorePath);
        } catch (Exception e) {
            SibTr.exception(tc, e);
            FFDCFilter.processException(e, this.getClass().getName(), "720", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "resolveFileStorePath", new Object[] { filePath });
        }
        return filePath;

    }

    /**
     * {@inheritDoc}
     * 
     * Constructs the new config object based on the server.xml changes.There
     * are few rules while constructing 1) If the defaultQueue or defaultTopic
     * is deleted it will not be taken into consideration.Old values are
     * retained 2) Filestore changes are not honoured hence old values will be
     * considered
     * 
     * */
    @Override
    public void modified(ComponentContext context,
                         Map<String, Object> properties, ConfigurationAdmin configAdmin) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "modified", new Object[] { context, properties,
                                                      configAdmin });
        }

        this.properties = properties;
        internalModify(configAdmin);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "modified");
        }

    }

    /**
     * @param configAdmin
     */
    private synchronized void internalModify(ConfigurationAdmin configAdmin) {
        pids.clear();
        try {

            // Create the new config to accept the new changes in server.xml
            JsMEConfig newConfig = new JsMEConfig();

            // set the properties for mesagingengine object.first the queues or
            // topic is got from
            // Administration Config and list of SIBBestination is created
            // and then the localization list is created for those SIBdestination
            SIBMessagingEngine messagingEngine = new SIBMessagingEngineImpl();
            messagingEngine.setHighMessageThreshold((Long) properties
                            .get(JsAdminConstants.HIGHMESSAGETHRESHOLD));

            // map containing the all types of destiniation. i.e Queue, Topic or Alias
            HashMap<String, BaseDestination> destinationList = new HashMap<String, BaseDestination>();
            HashMap<String, SIBLocalizationPoint> destinationLocalizationList = new HashMap<String, SIBLocalizationPoint>();
            // populate destinations of type QUEUE
            populateDestinations(properties, destinationList, destinationLocalizationList, messagingEngine.getName(), JsAdminConstants.QUEUE, configAdmin,
                                 true);
            // populate destinations of type TOPICSPACE
            populateDestinations(properties, destinationList, destinationLocalizationList, messagingEngine.getName(), JsAdminConstants.TOPICSPACE, configAdmin,
                                 true);

            if (!destinationList.containsKey(JsAdminConstants.DEFAULTQUEUE)) {// user have deleted
                // the default queue or might be that user have not defined at all in server.xml.
                //In whichever case we need to retain the old values

                destinationList.put(JsAdminConstants.DEFAULTQUEUE, jsMEConfig.getMessagingEngine()
                                .getDestinationList().get(JsAdminConstants.DEFAULTQUEUE));
                String localizationName = JsAdminConstants.DEFAULTQUEUE + "@" + messagingEngine.getName();
                destinationLocalizationList.put(localizationName, jsMEConfig.getMessagingEngine().getSibLocalizationPointList().get(localizationName));
            }
            if (!destinationList.containsKey(JsAdminConstants.DEFAULTTOPIC)) {// user have deleted
                // the default topic or might be that user have not defined at all in server.xml.
                //In whichever case we need to retain the old values

                destinationList.put(JsAdminConstants.DEFAULTTOPIC, jsMEConfig.getMessagingEngine()
                                .getDestinationList().get(JsAdminConstants.DEFAULTTOPIC));
                String localizationName = JsAdminConstants.DEFAULTTOPIC + "@" + messagingEngine.getName();
                destinationLocalizationList.put(localizationName, jsMEConfig.getMessagingEngine().getSibLocalizationPointList().get(localizationName));
            }
            if (!destinationList.containsKey(JsAdminConstants.EXCEPTION_DESTINATION)) {// user have deleted
                // the default exception destination or might be that user have not defined at all in server.xml.
                //In whichever case we need to retain the old values

                destinationList.put(JsAdminConstants.EXCEPTION_DESTINATION, jsMEConfig.getMessagingEngine()
                                .getDestinationList().get(JsAdminConstants.EXCEPTION_DESTINATION));
                String localizationName = JsAdminConstants.EXCEPTION_DESTINATION + "@" + messagingEngine.getName();
                destinationLocalizationList.put(localizationName, jsMEConfig.getMessagingEngine().getSibLocalizationPointList().get(localizationName));
            }

            //populate destinations of type ALIAS
            populateAliasDestinations(properties, destinationList, configAdmin);

            messagingEngine.setDestinationList(destinationList);
            messagingEngine.setSibLocalizationPointList(destinationLocalizationList);

            // set all the constructed artifact to jsMEConfig
            newConfig.setMessagingEngine(messagingEngine);
            // old values are retained
            newConfig.setSIBFilestore(jsMEConfig.getSibFilestore());
            newConfig.setSIBus(jsMEConfig.getSIBus());

            compareAndTakeAction(newConfig);

            // set the new config
            jsMEConfig = newConfig;

        } catch (Exception e) {

            SibTr.error(tc, "MODIFICATION_UNSUCCESSFUL_SIAS0117");
            SibTr.exception(tc, e);
            FFDCFilter.processException(e,
                                        "com.ibm.ws.messaging.service.JsMainAdminServiceImpl",
                                        "852", this);
        }
    }

    /**
     * Compares the old and new config object to identify the changes.
     * Appropriate actions are taken on the changes i) First 2 objects are
     * compared to identify the addition and deletion of destinations ii) Second
     * 2 objects are comapred to identify the retained destination. It can mean
     * two things either the destinations are not at all changed or the
     * attributes are changed
     * 
     * @param newConfig
     */
    private void compareAndTakeAction(JsMEConfig newConfig) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "compareAndTakeAction", newConfig);
        }

        // newDestinationSet is a sorted set containing the destination id of
        // new config object
        TreeSet<String> newDestinationSet = new TreeSet<String>(newConfig
                        .getMessagingEngine().getDestinationList().keySet());
        // oldDestinationSet is a sorted set containing the destination id of
        // old config object
        TreeSet<String> oldDestinationSet = new TreeSet<String>(jsMEConfig
                        .getMessagingEngine().getDestinationList().keySet());
        // completeDestinationSet is union of newDestinationSet and
        // oldDestinationSet
        TreeSet<String> completeDestinationSet = new TreeSet<String>();
        // continas the destinations which are deleted
        TreeSet<String> deletedDestinationSet = new TreeSet<String>();
        // contains the destinations which are added
        TreeSet<String> addedDestinationSet = new TreeSet<String>();
        // contains the modified destinations.It will first have the destinations which is modified or the unalteed destiantion
        //Then via a logic some of the same destinations will be removed so that it will have only the modified destinations
        TreeSet<String> modifiedDestinationSet = new TreeSet<String>();

        completeDestinationSet.addAll(oldDestinationSet);
        completeDestinationSet.addAll(newDestinationSet);

        Iterator it = completeDestinationSet.iterator();

        //identify the destinations which has to be added, deleted or modified and create a set 
        while (it.hasNext()) {
            String key = (String) it.next();

            if (jsMEConfig.getMessagingEngine().getDestinationList()
                            .containsKey(key)) {// check if the key exists in old set
                if (newConfig.getMessagingEngine().getDestinationList()
                                .containsKey(key)) {// check if the key exists in new
                    // set.if yes then destination might
                    // have been modified or it might have not been altered
                    modifiedDestinationSet.add(key);
                } else {// destination have been deleted
                    deletedDestinationSet.add(key);
                }
            } else {// destination does not exist in old set.Implies a new
                // destiantion has been added
                addedDestinationSet.add(key);
            }
        }

        //iterate through the modifiedDestinationSet to set the UUID
        //This is done because when modified is called a new jsMeConfiG Object is created
        //and the UUID information is lost.Hence we try to restore the UUID for both destination and the localization
        Iterator mit = modifiedDestinationSet.iterator();
        while (mit.hasNext()) {
            String meName = newConfig.getMessagingEngine().getName();
            String destinationName = (String) mit.next();

            // set the UUID of the destination
            newConfig.getMessagingEngine().getDestinationList().get(destinationName)
                            .setUUID(
                                     jsMEConfig.getMessagingEngine()
                                                     .getDestinationList().get(destinationName).getUUID());

            BaseDestination bd = newConfig.getMessagingEngine().getDestinationList().get(destinationName);

            //Localization is not applicable for alias type.Hence the check is made to see if its local
            if (bd.isLocal()) {

                String uuid = jsMEConfig.getMessagingEngine().getSibLocalizationPointList().get(destinationName + "@" + meName).getUuid();
                String targetUuid = jsMEConfig.getMessagingEngine().getSibLocalizationPointList().get(destinationName + "@" + meName).getTargetUuid();

                //set the uuid and targetuuid of the localization
                newConfig.getMessagingEngine().getSibLocalizationPointList().get(destinationName + "@" + meName).setUuid(uuid);
                newConfig.getMessagingEngine().getSibLocalizationPointList().get(destinationName + "@" + meName).setTargetUuid(targetUuid);

            }

        }

        // now check in modifiedDestinationSet to see if at all anything have
        // changed. If nothing is changed we just ignore it
        mit = modifiedDestinationSet.iterator();
        while (mit.hasNext()) {
            String key = (String) mit.next();
            BaseDestination modifiedDestination = newConfig.getMessagingEngine()
                            .getDestinationList().get(key);
            BaseDestination oldDestination = jsMEConfig.getMessagingEngine()
                            .getDestinationList().get(key);

            // if the properties are different then remove destination from modifiedDestinationSet, else do nothing(i.e retain it in the set)
            // IMP : here equals() used is overridden method
            if (modifiedDestination.isLocal()) {
                if (((SIBDestination) modifiedDestination).equals(oldDestination))
                    mit.remove(); // remove the destination from the modifiedDestinationSet as both the objects are equal

            } else if (modifiedDestination.isAlias()) {
                if (((AliasDestination) modifiedDestination).equals(oldDestination))
                    mit.remove(); // remove the destination from the modifiedDestinationSet as both the objects are equal
            }
        }

        // Check if the highMessageThreshold is different.If yes then invoke reloadEngine() 
        if (jsMEConfig.getMessagingEngine().getHighMessageThreshold() != newConfig.getMessagingEngine().getHighMessageThreshold()) {
            try {
                _jsMainImpl.reloadEngine(newConfig.getMessagingEngine().getHighMessageThreshold());
            } catch (Exception e) {
                SibTr.exception(tc, e);
                FFDCFilter.processException(e, this.getClass().getName(), "972", this);
            }
        }
        Iterator dit = deletedDestinationSet.iterator();
        while (dit.hasNext()) {
            String key = (String) dit.next();
            // deleted SIBDestination can be got from the old jsMEConfig
            try {
                _jsMainImpl.deleteDestinationLocalization(jsMEConfig
                                .getMessagingEngine().getDestinationList().get(
                                                                               (key)));
            } catch (Exception e) {
                SibTr.exception(tc, e);
                FFDCFilter.processException(e, this.getClass().getName(), "974", this);
            }
        }
        Iterator nit = addedDestinationSet.iterator();
        while (nit.hasNext()) {
            String key = (String) nit.next();
            // New SIBDestination can be got from the new jsMEConfig
            try {
                _jsMainImpl.createDestinationLocalization(newConfig
                                .getMessagingEngine().getDestinationList().get(
                                                                               (key)));
            } catch (Exception e) {
                SibTr.exception(tc, e);
                FFDCFilter.processException(e, this.getClass().getName(), "992", this);
            }
        }
        Iterator mmit = modifiedDestinationSet.iterator();
        while (mmit.hasNext()) {
            String key = (String) mmit.next();
            // Modified SIBDestination can be got from the new jsMEConfig
            try {
                _jsMainImpl.alterDestinationLocalization(newConfig
                                .getMessagingEngine().getDestinationList().get(
                                                                               (key)));
            } catch (Exception e) {
                SibTr.exception(tc, e);
                FFDCFilter.processException(e, this.getClass().getName(), "1010", this);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "compareAndTakeAction");
        }

    }

    /** {@inheritDoc} */
    @Override
    public void deactivate(ComponentContext context,
                           Map<String, Object> properties) {

        try {
            _jsMainImpl.stop();
            _jsMainImpl.destroy();
            SibTr.info(tc, "ME_STOPPED_SIAS0121");
        } catch (Exception e) {
            SibTr.exception(tc, e);
        }

    }

    /**
     * gets the messagingengine state
     * 
     * @return the _state
     */
    @Override
    public String getMeState() {
        return _state;
    }

    /**
     * Sets the messagingengine state
     * 
     * @param state
     *            the _state to set
     */
    public void setMeState(String state) {
        _state = state;
    }

    @Override
    public void configurationEvent(ConfigurationEvent event, ConfigurationAdmin configAdmin) {
        if (event.getType() == ConfigurationEvent.CM_UPDATED && pids.contains(event.getPid())) {
            internalModify(configAdmin);
        }
    }

}
