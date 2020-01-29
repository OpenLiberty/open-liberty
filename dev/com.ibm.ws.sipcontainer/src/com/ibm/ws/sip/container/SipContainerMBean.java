/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.management.DynamicMBean;
import javax.management.Notification;
import javax.management.RuntimeOperationsException;
import javax.management.StandardMBean;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;

/**
 * @author Amir Perlman, Jan 2, 2005
 * 
 *         JMX for managing SIP container. Allows performing management operations on
 *         the SIP Container and also to get notification events, such as SIP Container
 *         overloaded.
 */
@Component(
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = false,
           service = { SipContainerInterface.class, DynamicMBean.class },
           property = {
                       "service.vendor=IBM",
                       "jmx.objectname=WebSphere:name=com.ibm.ws.sip.container.SipContainerMBean" })
public class SipContainerMBean extends StandardMBean implements SipContainerInterface {

    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SipContainerMBean.class);  

    /**
     * Flag indicating whether MBean has be succesfuly loaded and is
     * operational.
     */
    private final boolean _isJMXEnabled = false;
    
    /**
  	 * This executor is injected with the Liberty default ExecutorServiceImpl. Container tasks invoked with it will be executed on the 
  	 * global threads pool
  	 */
  	private ExecutorService _executorService;
  	
  	/**
  	 * The dump method setting to be used prior running the dumping executor service
  	 * If not set, the dumping utility will print to the SystemOut.
  	 */
  	private String _dumpMethod = null;  	
  	private String _dumpDescription = null;
    
    /**
  	 * @param executorService the executorService to set
  	 */
  	@Reference(policy = ReferencePolicy.STATIC)
  	protected void setExecutorService(ExecutorService service) {
  		if (c_logger.isTraceDebugEnabled()) {
  			c_logger.traceDebug(this, "setExecutorService", "setting the executor service for DumpActivator");
  		}
  		_executorService = service;
  	}

    /**
     * Construct a MBean for the specifed SIP Container.
     * 
     * @param container
     */
    public SipContainerMBean() {
        super(SipContainerInterface.class, false);
    }

    @Activate
    protected void activate(Map<String, Object> properties) {
        System.out.println("SipContainerMBean activated");
    }

    /**
     * Sets the container's weight, overiding the internal calcualation peformed
     * by the container. Use a value of 0 to make the container go into quiesce
     * mode. Use a value of -1 to resume dynamic calculation of weight according
     * to the container's load.
     * 
     * @param weight
     */
    public void setWeight(int weight) {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "setWeight", Integer.toString(weight));
        }

        PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null) {
        	perfMgr.setServerWeight(weight);
		}
    }

    /**
     * Gets the current weight of the SIP Container.
     */
    public int getWeight() {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "getWeight");
        }

        int weight = -1;
        //TODO: Anat implement the call to get the weight

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "getWeight: " + weight);
        }

        return weight;
    }

    /**
     * Changes the container's state between quiesced and un-quiesced. Quiesced
     * mode sets the container weight to QUIESCE_MODE to prevent from new
     * message to be sent to the container
     * 
     */
    public void quiesce(boolean b) {
        if (c_logger.isInfoEnabled()) {
            if (b) {
                // quiesce
                c_logger.info("info.sip.container.quiesce.on", Situation.SITUATION_STOP_INITIATED);
            }
            else {
                // de-quiesce
                c_logger.info("info.sip.container.quiesce.off", Situation.SITUATION_START);
            }
        }
        SipContainer.getInstance().setQuiesceAttribute(b);
    }

    /**
     * Dispatch to Notification Listeners alert when SIP Container is overloaded.
     * 
     */
    public void sendOverloadedNotification() {
        if (!_isJMXEnabled) {
            return;
        }

        Notification notification = new Notification(
                        "sip.container.overloaded", this, 1,
                        "SIP Container is Overloaded. Stops serving new requests");
        sendNotification(notification);
    }

    /**
     * Dispatch to alert notification Listeners when SIP Container is no longer
     * overloaded
     * 
     */
    public void sendOverloadClearedNotification() {
        if (!_isJMXEnabled) {
            return;
        }

        Notification notification = new Notification(
                        "sip.container.overload.cleared", this, 1,
                        "SIP Container is NO longer Overloaded. Resumes serving new requests");
        sendNotification(notification);
    }

    /**
     * @see com.ibm.websphere.management.RuntimeCollaborator# sendNotification(javax.management.Notification)
     */
    public void sendNotification(Notification notification) {
        try {
            //TODO Liberty replace RuntimeCollaborator.sendNotification()
            /* super.sendNotification(notification); */
        } catch (RuntimeOperationsException e) {
            if (c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception", Situation.SITUATION_CREATE, null, e);
            }
        }
        // TODO Liberty replace MBeanException
        catch (Exception e/* MBeanException e */) {
            if (c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception", Situation.SITUATION_CREATE, null, e);
            }
        }
    }
    
    /**
     * To allow configuring an output method. It should be called prior to the "dump..." methods.
     * 
     * Example: setDumpMethod("file", "/opt/IBM/output.log")
     * * "file" indicates that a specific file method will be used for printing
     * * "/opt/IBM/output.log" is a full path to the file for printing
     * 
     * @param method a string parameter for printing method
     * @param description a string parameter to describe how to print
     * 
     * @return a string indication for success or failure
     */
    public String setDumpMethod(String method, String description) {
    	_dumpMethod = method;
    	_dumpDescription = description;
    	
        return "Success";
    }
    
    /**
     * Execute a dump action
     * 
     * @param dumpMethod object
     */
    public boolean dispatchActivateTask(Object dumpMethodString) {    	
    	DumpActivator dumping = new DumpActivator(dumpMethodString);
    	if (_dumpMethod != null) {
    		dumping.setDumpMethod(_dumpMethod, _dumpDescription);
    	}
        _executorService.execute(dumping);
        return true;
    }

    /**
     * Prints the SIP application session IDs.
     * 
     * @return int 0 or -1 as an indication of success or failure
     */
    @Override
    public int dumpAllSASIds() {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "dumpAllSASIds");
        }

        boolean success = dispatchActivateTask(DumpActivator.DUMP_SAS_IDs);

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "dumpAllSASIds: " + success);
        }
        return success == true ? 0 : -1;
    }

    /**
     * Prints transaction users and the SIP session IDs within the transaction user (TU), if one exists.
     * 
     * @return int 0 or -1 as an indication of success or failure
     */
    public int dumpAllTUSipSessionIds() {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "dumpAllTUSipSessionIds");
        }

        boolean success = dispatchActivateTask(DumpActivator.DUMP_TU_IDs);

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "dumpAllTUSipSessionIds: " + success);
        }
        return success == true ? 0 : -1;
    }

    /**
     * Prints all SIP application sessions and the SIP application session details.
     * 
     * @return int 0 or -1 as an indication of success or failure
     */
    @Override
    public int dumpAllSASDetails() {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "dumpAllSASDetails");
        }

        boolean success = dispatchActivateTask(DumpActivator.DUMP_SAS_DETAILs);

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "dumpAllSASDetails: " + success);
        }
        return success == true ? 0 : -1;
    }

    /**
     * Prints transaction users and details of the SIP session IDs within the transaction user (TU), if one exists.
     * 
     * @return int 0 or -1 as an indication of success or failure
     */
    public int dumpAllTUSipSessionDetails() {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "dumpAllTUSipSessionDetails");
        }

        boolean success = dispatchActivateTask(DumpActivator.DUMP_TU_DETAILs);

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "dumpAllTUSipSessionDetails: " + success);
        }
        return success == true ? 0 : -1;
    }

    /**
     * Print details on a SAS which id is provided in the sasId parameter
     * 
     * @param sasId
     */
    public void dumpSASDetails(String sasId) {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "dumpAllSASDetails, sessionId=" + sasId);
        }

        boolean success = dispatchActivateTask(DumpActivator.SAS_PREFIX + sasId);

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "dumpAllSASDetails: " + success);
        }
    }

    /**
     * Print details on a session which id is provided in the sessionId parameter
     * 
     * @param ssId
     */
    public void dumpSipSessionDetails(String ssId) {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "dumpSipSessionDetails, sessionId=" + ssId);
        }

        boolean success = dispatchActivateTask(DumpActivator.SS_PREFIX + ssId);

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "dumpSipSessionDetails: " + success);
        }
    }

}
