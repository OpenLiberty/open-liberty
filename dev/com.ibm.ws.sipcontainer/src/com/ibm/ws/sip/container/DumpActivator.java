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
package com.ibm.ws.sip.container;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * DumpActivator is a class that represents a low priority thread that actually performs the dumping operations.
 * 
 * All dump requests will be put in a queue and then handled in the run() method
 * which continually takes the next request off the queue and prints the requested info.
 * 
 */
public class DumpActivator implements Runnable {
	
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(DumpActivator.class);

    public File _file = null;

    /**
     * Methods used for dumping:
     * 
     * - "FILE" means that all dumps will be sent to a predefined file
     * - TODO: add other methods
     */
    public final static String FILE_METHOD = "FILE";

    /**
     * Delimiters
     */
    public final static String TAB_SEPARATOR = "\t";
    public final static String SEMICOLON = "; ";
    public final static String NEW_LINE = "\n";

    /**
     * Constants used to invoke dump methods
     */
    public static final Integer DUMP_SAS_IDs = new Integer(0);
    public static final Integer DUMP_SAS_DETAILs = new Integer(1);
    public static final Integer DUMP_TU_IDs = new Integer(2);
    public static final Integer DUMP_TU_DETAILs = new Integer(3);

    /**
     * Current dump action to perform by the executor service
     * The value can be represented as a String (session id to dump) or Integer (dumping all)
     */
    private Object currentDumpAction;
    
    /**
     * Prefixes used for specific session:
     * 
     * - "SAS_" added to SAS_Id when dumpSASDetails invoked
     * - "SS_" added to SS Id when dumpSipSessionDetails invoked
     */
    public final static String SAS_PREFIX = "SAS_";
    public final static String SS_PREFIX = "SS_";

    /**
     * Format for printing the SIP session creation time
     */
    public static SimpleDateFormat _sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");

    public DumpActivator() {
		super();
	}
    /**
     * Constructor
     * @param currentDumpAction
     */
    public DumpActivator(Object currentDumpAction) {
		super();
		this.currentDumpAction = currentDumpAction;
	}

    /**
     * Here the dump operation is being executed
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        StringBuilder strToPrint = new StringBuilder();

        // dump operation for a list of objects
        if (currentDumpAction instanceof Integer) {
            Integer method = (Integer) currentDumpAction;
            startDumpingListOfSessions(method.intValue(), strToPrint);
        }
        // dump operation for a particular object by id
        else if (currentDumpAction instanceof String) {
            String id = (String) currentDumpAction;
            startDumpingParticularSession(id, strToPrint);
        }
        else {
            throw new RuntimeException("Got unknown object running in DumpActivator thread");
        }

        // Finally print the string
        print(strToPrint.toString());
    }

    /**
     * Sets a dumping method
     * 
     * @param method
     *            - use "FILE" to dump to a file
     * @param description
     *            - full path to the file for dumping for "FILE" method
     * @return
     */
    public String setDumpMethod(String method, String description) {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "setDumpMethod for " + method);
        }
        if (FILE_METHOD.equalsIgnoreCase(method)) {
            try {
                _file = new File(description);

                if (!_file.getParentFile().exists()) {
                    _file.getParentFile().mkdirs();
                }
                if (!_file.exists()) {
                    _file.createNewFile();
                }
            } catch (IOException e) {
                if (c_logger.isErrorEnabled()) {
                    c_logger.error("error.exception", Situation.SITUATION_CREATE, null, e);
                }
                return "Failed";
            }
        }
        else {
            return method + " is not supported, check syntax.";
        }
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "setDumpMethod");
        }
        return "Success";
    }

    /////////////////// Private methods /////////////////////////////

    /**
     * Returns first level header for SAS printing
     */
    private StringBuilder getFirstLevelHeader(String appName, int count) {
        StringBuilder header = new StringBuilder();
        header.append(appName);
        header.append(TAB_SEPARATOR);
        header.append(count);
        header.append(NEW_LINE);
        return header;
    }

    /**
     * Print a String to a file
     * 
     * @param s String
     */
    private void printToFile(String s) {
        try {
            FileWriter fw = new FileWriter(_file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(s);
            bw.close();
        } catch (IOException e) {
            if (c_logger.isErrorEnabled()) {
                c_logger.error("error.exception", Situation.SITUATION_CREATE, null, e);
            }
        }
    }

    /**
     * Checks is the method for SAS printing
     * 
     * @param method
     * @return boolean
     */
    private boolean isDumpSASMethod(int method) {
        if (method == DUMP_SAS_IDs || method == DUMP_SAS_DETAILs) {
            return true;
        }
        return false;
    }

    /**
     * Checks is the method for TU printing
     * 
     * @param method
     * @return boolean
     */
    private boolean isDumpTUMethod(int method) {
        if (method == DUMP_TU_IDs || method == DUMP_TU_DETAILs) {
            return true;
        }
        return false;
    }

    /**
     * Checks is the method for printing details
     * 
     * @param method
     * @return boolean
     */
    private boolean isDumpDetails(int method) {
        if (method == DUMP_SAS_DETAILs || method == DUMP_TU_DETAILs) {
            return true;
        }
        return false;
    }

    /**
     * Prints the string
     * 
     * @param s String
     */
    private void print(String s) {
        if (_file != null) {
            printToFile(s);
        }
        else {
            System.out.println(s);
        }
    }

    /**
     * Generates Sip application session details for printing
     * 
     * @param dumpMethod
     * @param appSession
     * @return String
     */
    private void generateSASDetailsToPrint(int dumpMethod, SipApplicationSession appSession, StringBuilder str) {
        str.append(appSession.getId());

        if (isDumpDetails(dumpMethod) == false) {
            return;
        }

        StringBuffer attrNames = new StringBuffer();
        for (Iterator itr = appSession.getAttributeNames(); itr.hasNext();) {
            attrNames.append(itr.next()).append(SEMICOLON);
        }
        str.append(TAB_SEPARATOR).append(_sdf.format(new Date(appSession.getCreationTime()))).append(TAB_SEPARATOR).append(attrNames);
    }

    /**
     * Generates transaction user details for printing
     * 
     * @param dumpMethod
     * @param tu
     * @return String
     */
    private void generateTUDetailsToPrint(int dumpMethod, TransactionUserWrapper tu, StringBuilder str) {
        str.append(tu.getId()).append(TAB_SEPARATOR);
        str.append(tu.hasSipSession() ? "  true  " : "  false ");
        str.append(TAB_SEPARATOR).append(tu.getSipSessionId());

        if (isDumpDetails(dumpMethod)) {
            str.append(TAB_SEPARATOR).append(tu.getCallId()).append(TAB_SEPARATOR).append(tu.getDialogState());
            str.append(TAB_SEPARATOR).append(tu.hasOngoingTransactions()).append(TAB_SEPARATOR).append(tu.getInitialDialogMethod());
        }
    }

    /**
     * Generates sip session details for printing
     * 
     * @param sipSession
     * @return String
     */
    private void generateSSDetailsToPrint(SipSession sipSession, StringBuilder str, boolean printSSId) {
        // first check the session is valid
        if (!sipSession.isValid()) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "generateSSDetailsToPrint", "SS <" + sipSession.getId() + "> is not valid, returning");
            }
            return;
        }
        StringBuffer attrNames = new StringBuffer();
        Enumeration attrs = sipSession.getAttributeNames();
        while (attrs.hasMoreElements()) {
            attrNames.append((String) attrs.nextElement());
            attrNames.append(SEMICOLON);
        }

        SipApplicationSession appSession = sipSession.getApplicationSession();

        // the sessionId is printed as part of the all SS IDs, so print it only for a particular SS by ID
        if (printSSId) {
            str.append(sipSession.getId()).append(TAB_SEPARATOR);
        }
        str.append(appSession.getId()).append(TAB_SEPARATOR);
        str.append(_sdf.format(new Date(sipSession.getCreationTime()))).append(TAB_SEPARATOR).append(attrNames);
    }

    /**
     * Generates error message for printing
     * 
     * @param dumpMethod
     * @param appSession
     * @return String
     */
    private void generateErrorMessageToPrint(String id, StringBuilder str) {
        str.append("ERROR: Requested session <" + id + "> does not exist.");
    }

    /**
     * Prepare print for SASs per application
     */
    private void preparePrintForSASs(int dumpMethod, Map<String, List<String>> sasPerApp, StringBuilder strToPrint) {
        for (Iterator iterator = sasPerApp.keySet().iterator(); iterator.hasNext();) {
            String appName = (String) iterator.next();
            List<String> sasIds = sasPerApp.get(appName);

            strToPrint.append(getFirstLevelHeader(appName, sasIds.size()));

            for (String sasId : sasIds) {
                SipApplicationSessionImpl appSession = (SipApplicationSessionImpl) SessionRepository.getInstance().getAppSession(sasId);
                if (appSession == null) {
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug(this, "preparePrintForSASs", "SAS is null");
                    }
                    strToPrint.append("<<< " + sasId + " >>> has no SipApplicationSession");
                    strToPrint.append(NEW_LINE);
                    continue;
                }
                synchronized (appSession.getSynchronizer()) {
                    if (!appSession.isValid() || appSession.isDuringInvalidate()) {
                        if (c_logger.isTraceDebugEnabled()) {
                            c_logger.traceDebug(this, "preparePrintForSASs", "SAS <" + appSession.getId() + "> is not valid or during invalidation");
                        }
                        continue;
                    }
                    generateSASDetailsToPrint(dumpMethod, appSession, strToPrint);
                }
                strToPrint.append(NEW_LINE);
            }
        }
    }

    /**
     * Prepare print for TUs per application
     */
    private void preparePrintForTUs(int dumpMethod, Map<String, List<String>> tuPerApp, StringBuilder strToPrint) {
        for (Iterator iterator = tuPerApp.keySet().iterator(); iterator.hasNext();) {
            String appName = (String) iterator.next();
            List<String> tuIds = tuPerApp.get(appName);

            strToPrint.append(getFirstLevelHeader(appName, tuIds.size()));

            for (String tuId : tuIds) {
                TransactionUserWrapper tu = SessionRepository.getInstance().getTuWrapper(tuId);
                if (tu == null) {
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug(this, "preparePrintForTUs", "TU is null");
                    }
                    strToPrint.append("<<< " + tuId + " >>> has no transaction");
                    strToPrint.append(NEW_LINE);
                    continue;
                }
                synchronized (tu.getSynchronizer()) {
                    generateTUDetailsToPrint(dumpMethod, tu, strToPrint);
                    if (!tu.isValid() || tu.isInvalidating()) {
                        if (c_logger.isTraceDebugEnabled()) {
                            c_logger.traceDebug(this, "preparePrintForTUs", "TU <" + tuId + "> is not valid or during invalidation");
                        }
                    }
                    else if (isDumpDetails(dumpMethod) && tu.hasSipSession()) {
                        strToPrint.append(TAB_SEPARATOR);
                        generateSSDetailsToPrint(SessionRepository.getInstance().getSipSession(tu.getSipSessionId()), strToPrint, false);
                    }
                }
                strToPrint.append(NEW_LINE);
            }
        }
    }

    /**
     * Start dumping for a list
     * 
     * @param dumpMethod
     * @param strToPrint
     */
    public void startDumpingListOfSessions(int dumpMethod, StringBuilder strToPrint) {
        Map<String, List<String>> objPerApp = sortObjectsPerApplication(dumpMethod);
        prepareForPrint(dumpMethod, objPerApp, strToPrint);
    }

    /**
     * Sorts the objects per application
     * 
     * @param dumpMethod
     * @return Map
     */
    private Map<String, List<String>> sortObjectsPerApplication(int dumpMethod) {
        // ********************************************
        // ************** sort SAS objects  ***********
        // ********************************************         
        if (isDumpSASMethod(dumpMethod)) {
            return sortSASPerApp();
        }
        // *******************************************
        // ************** sort TU objects ************
        // *******************************************  
        else if (isDumpTUMethod(dumpMethod)) {
            return sortTUPerApp();
        }
        return null;
    }

    /**
     * Prepares string for printing
     * 
     * @param dumpMethod
     * @param map
     * @param strToPrint
     */
    private void prepareForPrint(int dumpMethod, Map<String, List<String>> map, StringBuilder strToPrint) {
        // ***********************************************
        // ************** dump for SAS methods ***********
        // ***********************************************              
        if (isDumpSASMethod(dumpMethod)) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(null, "run ", "DumpingActivator got request to dump SAS");
            }
            preparePrintForSASs(dumpMethod, map, strToPrint);
        }
        // ***********************************************
        // ************** dump for TU methods ************
        // ***********************************************                                      
        else if (isDumpTUMethod(dumpMethod)) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "run ", "DumpingActivator got request to dump all TUImpl Ids");
            }
            preparePrintForTUs(dumpMethod, map, strToPrint);
        }
    }

    /**
     * Start dumping by sessionId
     * 
     * @param id
     * @param strToPrint
     */
    private void startDumpingParticularSession(String id, StringBuilder strToPrint) {
        // ***********************************************
        // *********** dump a particular SAS  ************
        // ***********************************************      
        if (id.startsWith(SAS_PREFIX)) {
            String sasId = id.substring(4);
            SipApplicationSession sas = SessionRepository.getInstance().getAppSession(sasId);
            if (sas == null) {
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "run", "didnt find any SipApplicationSession on:" + id);
                }
                generateErrorMessageToPrint(sasId, strToPrint);
                return;
            }
            generateSASDetailsToPrint(DUMP_SAS_DETAILs, sas, strToPrint);
        }
        // ***********************************************
        // *********** dump a particular SS  *************
        // ***********************************************
        else if (id.startsWith(SS_PREFIX)) {
            String sId = id.substring(3);
            SipSession s = SessionRepository.getInstance().getSipSession(sId);
            if (s == null) {
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "run", "didnt find any SipSession on:" + id);
                }
                generateErrorMessageToPrint(sId, strToPrint);
                return;
            }
            generateSSDetailsToPrint(s, strToPrint, true);
        }
    }

    /**
     * Sorts the SAS list by appName
     * 
     * @return Map
     */
    private Map sortSASPerApp() {
        // Get the list of all sip application sessions from the session repository
        List<SipApplicationSessionImpl> snapshot = SessionRepository.getInstance().getAllAppSessions();

        // Run over the list and sort it by application name 
        Map<String, List<String>> sasPerApp = new HashMap<String, List<String>>();
        for (Iterator<SipApplicationSessionImpl> itr = snapshot.iterator(); itr.hasNext();) {
            SipApplicationSessionImpl saSession = itr.next();
            if (saSession == null) {
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "sortSASPerApp", "SAS is null");
                }
                continue;
            }

            synchronized (saSession.getSynchronizer()) {
                if (!saSession.isValid() || saSession.isDuringInvalidate()) {
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug(this, "sortSASPerApp", "SAS <" + saSession.getId() + "> is not valid, not sorted for dumping.");
                    }
                    continue;
                }

                String appName = saSession.getAppDescriptor().getApplicationName();
                List<String> sasIds = sasPerApp.get(appName);
                if (sasIds == null) {
                    sasIds = new ArrayList<String>();
                    sasPerApp.put(appName, sasIds);
                }
                sasIds.add(saSession.getId());
            }
        }
        return sasPerApp;
    }

    /**
     * Sorts the TU list by appName
     * 
     * @return Map
     */
    private Map sortTUPerApp() {
        // Get the list of all TUWrappers from the session repository
        List snapshot = SessionRepository.getInstance().getAllTuWrappers();

        // Run over the list and sort it by application name 
        Map<String, List<String>> tuPerApp = new HashMap<String, List<String>>();
        for (Iterator itr = snapshot.iterator(); itr.hasNext();) {
            TransactionUserWrapper tuWrapper = (TransactionUserWrapper) itr.next();
            if (tuWrapper == null) {
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "sortTUPerApp", "TU is null");
                }
                continue;
            }

            synchronized (tuWrapper.getSynchronizer()) {
                if (!tuWrapper.isValid() || tuWrapper.isInvalidating()) {
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug(this, "sortTUPerApp", "TU <" + tuWrapper.getId() + "> is not valid, not sorted for dumping.");
                    }
                    continue;
                }

                String appName = tuWrapper.getSipServletDesc().getSipApp().getApplicationName();
                List<String> tuIds = tuPerApp.get(appName);
                if (tuIds == null) {
                    tuIds = new ArrayList<String>();
                    tuPerApp.put(appName, tuIds);
                }
                tuIds.add(tuWrapper.getId());
            }
        }
        return tuPerApp;
    }
}
