/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.utils.CollectorManagerPipelineUtils;
import com.ibm.wsspi.logging.LogHandler;

/**
 * This class scans the existing services and registers itself as a ServiceListener,
 * looking/listening for LogHandlers services. As soon as the first LogHandler is
 * discovered, this class creates the MessageRouterImpl and "injects" the LogHandler(s)
 * into it.
 * 
 * The class also scans the existing bundles and registers itself as a BundleListener,
 * to detect when bundles come and go, in order to look for MessageRouter.properties
 * files defined in the bundles. It passes the property data to the MessageRouterImpl
 * (a la a "modified" update).
 * 
 * In other words, this class acts as a pseudo-injector of services and config for
 * the MessageRouterImpl. I can't just register the MessageRouterImpl as a DS service
 * component, because the DS injection stuff happens late in init, and we want the
 * MessageRouter up as early as possible.
 */
public class MessageRouterConfigurator implements BundleListener {

    private final TraceComponent tc = Tr.register(this.getClass(), null, "com.ibm.ws.logging.internal.resources.LoggingMessages");

    /**
     * A reference to the OSGI bundle framework.
     */
    private BundleContext bundleContext = null;

    /**
     * A reference to the actual MessageRouter.
     */
    private WsMessageRouterImpl msgRouter = null;

    /**
     * Map of previous logHandler:msgId mappings to compare to incoming updates.
     */
    private final HashMap<String, ArrayList<String>> previousHandlerMessageMap = new HashMap<String, ArrayList<String>>();

    /**
     * Listener for receiving config updates from <zosLogging>.
     */
    private MessageRouterConfigListenerImpl configListener = new MessageRouterConfigListenerImpl(this);

    /**
     * The ServiceListener interface. Invoked by OSGI whenever a ServiceReference changes state.
     * Receives LogHandler events and passes them along to MessageRouterImpl.
     */
    private final ServiceListener logHandlerListener = new ServiceListener() {
        @Override
        @SuppressWarnings("unchecked")
        public void serviceChanged(ServiceEvent event) {
            switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                    setLogHandler((ServiceReference<LogHandler>) event.getServiceReference());
                    break;
                case ServiceEvent.UNREGISTERING:
                    unsetLogHandler((ServiceReference<LogHandler>) event.getServiceReference());
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * The ServiceListener interface. Invoked by OSGI whenever a ServiceReference changes state.
     * Receives WsLogHandler events and passes them along to MessageRouterImpl.
     */
    private final ServiceListener wsLogHandlerListener = new ServiceListener() {
        @Override
        @SuppressWarnings("unchecked")
        public void serviceChanged(ServiceEvent event) {
            switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                    setWsLogHandler((ServiceReference<WsLogHandler>) event.getServiceReference());
                    break;
                case ServiceEvent.UNREGISTERING:
                    unsetWsLogHandler((ServiceReference<WsLogHandler>) event.getServiceReference());
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * First, this guy registers itself as a ServiceListener, listening specifically for
     * LogHandler services. Second, it scans the current set services looking for any LogHandler
     * services that are already active.
     * 
     * @param context The BundleContext.
     */
    public MessageRouterConfigurator(BundleContext context) {
        bundleContext = context;

        try {
            // Register ServiceListeners, to be informed when new LogHandler and WsLogHandler services are registered.
            bundleContext.addServiceListener(logHandlerListener, "(" + Constants.OBJECTCLASS + "=com.ibm.wsspi.logging.LogHandler)");
            bundleContext.addServiceListener(wsLogHandlerListener, "(" + Constants.OBJECTCLASS + "=com.ibm.ws.logging.WsLogHandler)");

            processInitialLogHandlerServices();
            processInitialWsLogHandlerServices();

        } catch (InvalidSyntaxException ise) {
            // This should really never happen.  Blow up if it does.
            throw new RuntimeException(ise);
        }

        // Listen for changes to the <zosLogging> config for new message IDs to route
        configListener = configListener.register(bundleContext);
    }

    /**
     * The BundleListener interface. Invoked by OSGI whenever a Bundle changes state.
     */
    @Override
    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.INSTALLED:
                processBundle(event.getBundle());
                break;
            case BundleEvent.UNINSTALLED:
                // TODO: remove entries?  how do i know if this bundle's entries have been overridden by another bundle?
                break;
        }
    }

    /**
     * Add the LogHandler ref. 1 or more LogHandlers may be set.
     * This method is called from the ServiceListener.
     */
    protected void setLogHandler(ServiceReference<LogHandler> ref) {
        getMessageRouter().setLogHandler((String) ref.getProperty("id"), bundleContext.getService(ref));
    }

    /**
     * Remove the LogHandler ref.
     * This method is called from the ServiceListener.
     */
    protected void unsetLogHandler(ServiceReference<LogHandler> ref) {
        getMessageRouter().unsetLogHandler((String) ref.getProperty("id"), bundleContext.getService(ref));
    }

    /**
     * Add the WsLogHandler ref. 1 or more LogHandlers may be set.
     * This method is called from the ServiceListener.
     */
    protected void setWsLogHandler(ServiceReference<WsLogHandler> ref) {
    	getMessageRouter().setWsLogHandler((String) ref.getProperty("id"), bundleContext.getService(ref));
    }

    /**
     * Remove the WsLogHandler ref.
     * This method is called from the ServiceListener.
     */
    protected void unsetWsLogHandler(ServiceReference<WsLogHandler> ref) {
        getMessageRouter().unsetWsLogHandler((String) ref.getProperty("id"), bundleContext.getService(ref));
    }

    /**
     * Search for and add any LogHandler ServiceReferences that were already started
     * by the time we registered our ServiceListener.
     */
    @SuppressWarnings("unchecked")
    protected void processInitialLogHandlerServices() throws InvalidSyntaxException {
        ServiceReference<LogHandler>[] servRefs = (ServiceReference<LogHandler>[])
                        bundleContext.getServiceReferences(LogHandler.class.getName(), null);

        if (servRefs != null) {
            for (ServiceReference<LogHandler> servRef : servRefs) {
                setLogHandler(servRef);
            }
        }
    }

    /**
     * Search for and add any WsLogHandler ServiceReferences that were already started
     * by the time we registered our ServiceListener.
     */
    @SuppressWarnings("unchecked")
    protected void processInitialWsLogHandlerServices() throws InvalidSyntaxException {
        ServiceReference<WsLogHandler>[] servRefs = (ServiceReference<WsLogHandler>[])
                        bundleContext.getServiceReferences(WsLogHandler.class.getName(), null);

        if (servRefs != null) {
            for (ServiceReference<WsLogHandler> servRef : servRefs) {
                setWsLogHandler(servRef);
            }
        }
    }

    /**
     * Process all Bundles that were already installed by the time we registered
     * our BundleListener.
     */
    protected void processInitialBundles() {
        // Note: by default, the list is returned in the order that the bundles were installed
        //       (they are naturally ordered by bundle_id).
        for (Bundle bundle : bundleContext.getBundles()) {
            processBundle(bundle);
        }
    }

    /**
     * Read and parse the bundle's MessageRouter.properties file, if one exists.
     */
    protected void processBundle(Bundle bundle) {
        Properties props = readMessageRouterProps(bundle);

        if (props != null) {
            msgRouter.modified(props);
        }
    }

    /**
     * Read the MessageRouter.properties file from the given Bundle.
     * 
     * @param bundle The Bundle from which to read the properties file.
     * 
     * @return A Properties object containing the file contents.
     */
    protected Properties readMessageRouterProps(Bundle bundle) {
        Properties props = null;

        // Use getEntry to load the file instead of getResource, since
        // getResource will cause the bundle to resolve itself, which 
        // we don't want to do.
        final String mrpFileName = "/META-INF/MessageRouter.properties";
        URL propFile = bundle.getEntry(mrpFileName);

        if (propFile != null) {
            props = new Properties();

            try {
                props.load(propFile.openStream());
            } catch (IOException ioe) {
                // TODO will this FFDC?  NO! what to do?!
            }
        }
        return props;
    }

    /**
     * Lazy activation and retrieval of the MessageRouter.
     */
    protected WsMessageRouterImpl getMessageRouter() {
        if (msgRouter == null) {
            // First activation.
            msgRouter = MessageRouterSingleton.singleton;

            // Pass the MessageRouter to the TrService via the TrConfigurator.
            TrConfigurator.setMessageRouter(msgRouter);

            // Register this guy as a BundleListener, looking for new Bundles as they
            // are added to the framework.  Then process the already-installed bundles. 
            // We are looking for any /META-INF/MessageRouter.properties files defined
            // in the bundles.  Note: it's possible (tho unlikely) that multiple threads
            // could be running this code at the same time.  This is not a problem.
            // The msgRouter will be informed of duplicate MessageRouter.properties
            // entries, but it can handle that. One thing to note, howerver, is that the 
            // natural ordering of Bundles may get messed up by the BundleListener. The 
            // BundleListener is registered before we process the initial bundle set 
            // (which we must do, otherwise we risk missing the arrival of a bundle between 
            // the time we're processing the initial set and the time we register the 
            // BundleListener). However, if the BundleListener receives a "bundle installed" 
            // event asynchronously while we're still processing the initial set, that newly 
            // installed bundle's MessageRouter properties may get overridden by a bundle 
            // from the initial set, which would violate the policy that the properties are 
            // applied in the order by which the bundles were started. So we're stuck between 
            // a rock and a hard place. Not sure what to do about that. 
            bundleContext.addBundleListener(this);
            processInitialBundles();
        }

        return msgRouter;
    }

    /**
     * http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
     * ... tells me to do it this way.
     */
    private static class MessageRouterSingleton {
        public static final WsMessageRouterImpl singleton = new WsMessageRouterImpl();
    }

    /**
     * The bundle is stopping. Inform the TrConfigurator, who in turn will inform
     * the TrService. Unregister the config listener service.
     */
    public void stop() {
        if (msgRouter != null) {
            TrConfigurator.unsetMessageRouter(msgRouter);
        }
        if (configListener != null) {
            configListener.unregister();
        }
    }

    /**
     * Compare the incoming list of message IDs to be associated with the given handler,
     * adding and removing as needed to match the new config.
     * 
     * @param msgIds
     * @param handlerId
     */
    public void updateMessageListForHandler(String msgIds, String handlerId) {
        if (msgIds == null) {
            return; // Should never happen, but avoid NPEs
        }

        String[] msgStr = msgIds.split(",");
        ArrayList<String> newMsgList = new ArrayList<String>(Arrays.asList(msgStr));
        newMsgList.remove(""); // Ignore blank value

        // Get the list of existing handlers mapped to this message ID.
        ArrayList<String> prevMsgList = previousHandlerMessageMap.get(handlerId);
        {
            if (prevMsgList == null) {
                prevMsgList = new ArrayList<String>();
            }
        }

        if (prevMsgList.equals(newMsgList)) {
            return; // No change = nothing to do here, skip issuing message
        }

        getMessageRouter(); // Ensure router is not null

        // Check for new messages and add them to the list for the handler
        ArrayList<String> addedMsgs = new ArrayList<String>(newMsgList);
        addedMsgs.removeAll(prevMsgList);

        for (String msgId : addedMsgs) {
            msgRouter.addMsgToLogHandler(msgId, handlerId);
        }

        // Check for removed messages and remove them from the list for the handler
        ArrayList<String> removedMsgs = new ArrayList<String>(prevMsgList);
        removedMsgs.removeAll(newMsgList);

        for (String msgId : removedMsgs) {
            msgRouter.removeMsgFromLogHandler(msgId, handlerId);
        }

        // Save the new map for the next config change
        previousHandlerMessageMap.put(handlerId, newMsgList);

        // Notify that there was a change in this handler's messages
        String setOfIds = "";
        for (String msg : newMsgList) {
            if (!setOfIds.equals("")) {
                setOfIds = setOfIds.concat(" ");
            }
            setOfIds = setOfIds.concat(msg);
        }
        Tr.info(tc, "MSG_ROUTER_UPDATED", new Object[] { handlerId, setOfIds });
    }
}
