/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.rest.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.jbatch.container.ws.InstanceState;

/**
 * Contains the different parameters and the parsing for the pre-purge search
 *
 */
public class WSSearchObject {

    // Search/Purge related variables
    long startInstanceId = -1;
    long endInstanceId = -1;
    long lessThanInstanceId = -1;
    long greaterThanInstanceId = -1;
    List<Long> instanceIdList;
    List<InstanceState> instanceState;
    List<String> exitStatusList;
    Date startCreateTime = null;
    Date endCreateTime = null;
    String lessThanCreateTime = null;
    String greaterThanCreateTime = null;
    Date specificCreateTime = null;
    Date startLastUpdatedTime = null;
    Date endLastUpdatedTime = null;
    String lessThanLastUpdatedTime = null;
    String greaterThanLastUpdatedTime = null;
    Date specificLastUpdatedTime = null;
    boolean ignoreCase = false;
    Map<String, String> jobParams = null;
    List<String> submitterList = null;
    List<String> appNameList = null;
    List<String> jobNameList = null;
    Set<String> groupSecuritySubjectGroupsList = null;

    // Non-Search/Purge related variables
    String queryIssuer = null;

    List<String> sortList;
    String sort = null;

    // Constructor
    public WSSearchObject(String instanceIdParams, String createTimeParams,
                          String instanceStateParams, String exitStatusParams) throws Exception {

        this(instanceIdParams, createTimeParams, instanceStateParams, new String[] { exitStatusParams }, null, null, null, null, null, null, null);
    }

    // Constructor v3
    public WSSearchObject(String instanceIdParams, String createTimeParams,
                          String instanceStateParams, String exitStatusParams, String lastUpdatedTimeParams,
                          String sortParams, Map<String, String> jobParams) throws Exception {

        this(instanceIdParams, createTimeParams, instanceStateParams, new String[] { exitStatusParams }, lastUpdatedTimeParams, sortParams, jobParams, null, null, null, null);
    }

    // Constructor v4
    public WSSearchObject(String instanceIdParams, String createTimeParams,
                          String instanceStateParams, String[] exitStatusParams, String lastUpdatedTimeParams,
                          String sortParams, Map<String, String> jobParams,
                          String[] submitterParams, String[] appNameParams, String[] jobNameParams, String ignoreCaseParams) throws Exception {

        if (instanceIdParams != null)
            processInstanceIdParams(instanceIdParams);

        if (createTimeParams != null)
            processCreateTimeParams(createTimeParams);

        if (instanceStateParams != null)
            processInstanceStateParams(instanceStateParams);

        if (exitStatusParams != null)
            processExitStatusParams(exitStatusParams);

        if (lastUpdatedTimeParams != null)
            processLastUpdatedTimeParams(lastUpdatedTimeParams);

        if (jobParams != null)
            processJobParameter(jobParams);

        if (sortParams != null)
            processSortParams(sortParams);

        if (submitterParams != null)
            processSubmitterParams(submitterParams);

        if (appNameParams != null)
            processAppNameParams(appNameParams);

        if (jobNameParams != null)
            processJobNameParams(jobNameParams);

        if (ignoreCaseParams != null)
            processIgnoreCaseParams(ignoreCaseParams);
    }

    /**
     * Processes the sort parameters
     *
     * @param params
     * @throws Exception
     */
    private void processSortParams(String params) throws Exception {

        if (params != null)
            this.sortList = Arrays.asList(params.split(","));
    }

    /**
     * Processes the lastUpdatedTime parameters
     *
     * @param params
     * @throws Exception
     */
    private void processLastUpdatedTimeParams(String params) throws Exception {
        DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (params.contains(":")) {
            String[] parts = params.split(":");
            this.startLastUpdatedTime = setDayStartForDate(dFormat.parse(parts[0]));
            this.endLastUpdatedTime = setDayEndForDate(dFormat.parse(parts[1]));
        } else if (params.contains("<")) {
            this.lessThanLastUpdatedTime = params.substring(1, params.indexOf("d"));
        } else if (params.contains(">")) {
            this.greaterThanLastUpdatedTime = params.substring(1,
                                                               params.indexOf("d"));
        } else { // This handles a single date
            dFormat = new SimpleDateFormat("yyyy-MM-dd");
            this.specificLastUpdatedTime = dFormat.parse(params);
        }
    }

    /**
     * Processes the exitStatus parameters
     *
     * @param params
     * @throws Exception
     */
    private void processExitStatusParams(String[] params) throws Exception {
        // In the this() call from the earlier constructors, if exitStatus is null, we end up with a String[]
        // with a null element that we need to check for here.
        if (params != null && params[0] != null)
            this.exitStatusList = Arrays.asList(params);
    }

    public void processJobParameter(Map<String, String> jobParams) {
        this.jobParams = jobParams;
    }

    /**
     * Processes the instanceState parameters
     *
     * @param params
     * @throws Exception
     */
    private void processInstanceStateParams(String params) throws Exception {
        List<String> tempList = Arrays.asList(params.split(","));
        List<InstanceState> stateList = new ArrayList<InstanceState>(tempList.size());

        for (String value : tempList) {
            try {
                //Ensuring the params are all caps since InstanceState is an Enum (case sensitive)
                stateList.add(InstanceState.valueOf(value.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("An invalid instanceState parameter was used: " + value + ". Valid instanceState parameters are: " +
                                                   Arrays.toString(InstanceState.values()).replace("[", "").replace("]", ""));
            }
        }

        this.instanceState = stateList;
    }

    /**
     * Processes the instanceId parameters
     *
     * @param params
     * @throws Exception
     */
    private void processInstanceIdParams(String params) throws Exception {
        if (params.contains(":")) {
            String[] parts = params.split(":");
            this.startInstanceId = new Long(parts[0]);
            this.endInstanceId = new Long(parts[1]);
            if (isNegative(this.startInstanceId))
                throw new IllegalArgumentException("A negative startInstanceId value was entered: " + this.startInstanceId);
            if (isNegative(this.endInstanceId))
                throw new IllegalArgumentException("A negative endInstanceId value was entered: " + this.endInstanceId);
        } else if (params.contains("<")) {
            String part = params.substring(1, params.length());
            this.lessThanInstanceId = new Long(part);
            if (isNegative(this.lessThanInstanceId))
                throw new IllegalArgumentException("A negative lessThanInstanceId value was entered: " + this.lessThanInstanceId);
        } else if (params.contains(">")) {
            String part = params.substring(1, params.length());
            this.greaterThanInstanceId = new Long(part);
            if (isNegative(this.greaterThanInstanceId))
                throw new IllegalArgumentException("A negative greaterThanInstanceId value was entered: " + this.greaterThanInstanceId);
        } else if (params.contains(",")) {
            List<String> tempList = Arrays.asList(params.split(","));
            this.instanceIdList = new ArrayList<Long>(tempList.size());
            Long longValue;
            for (String value : tempList) {
                longValue = Long.parseLong(value);
                if (isNegative(longValue))
                    throw new IllegalArgumentException("A negative jobInstanceId value was entered: " + longValue);

                this.instanceIdList.add(longValue);
            }
        } else if (params != null) {
            List<String> tempList = Arrays.asList(params);
            this.instanceIdList = new ArrayList<Long>(tempList.size());
            Long longValue;
            for (String value : tempList) {
                longValue = Long.parseLong(value);
                if (isNegative(longValue))
                    throw new IllegalArgumentException("A negative jobInstanceId value was entered: " + longValue);

                this.instanceIdList.add(Long.parseLong(value));
            }
        }
    }

    /**
     * Processes the createTime parameters
     *
     * @param params
     * @throws Exception
     */
    private void processCreateTimeParams(String params) throws Exception {
        DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (params.contains(":")) {
            String[] parts = params.split(":");
            this.startCreateTime = setDayStartForDate(dFormat.parse(parts[0]));
            this.endCreateTime = setDayEndForDate(dFormat.parse(parts[1]));
        } else if (params.contains("<")) {
            this.lessThanCreateTime = params.substring(1, params.indexOf("d"));
        } else if (params.contains(">")) {
            this.greaterThanCreateTime = params.substring(1,
                                                          params.indexOf("d"));
        } else { // This handles a single date
            dFormat = new SimpleDateFormat("yyyy-MM-dd");
            this.specificCreateTime = dFormat.parse(params);
        }
    }

    private void processSubmitterParams(String[] params) {
        this.submitterList = Arrays.asList(params);
    }

    private void processAppNameParams(String[] params) {
        this.appNameList = Arrays.asList(params);
    }

    private void processJobNameParams(String[] params) {
        this.jobNameList = Arrays.asList(params);
    }

    private void processIgnoreCaseParams(String params) {
        if ("true".equalsIgnoreCase(params)) {
            this.ignoreCase = true;
        }
    }

    // Getters and Setters

    public long getStartInstanceId() {
        return startInstanceId;
    }

    public void setStartInstanceId(long startInstanceId) {
        this.startInstanceId = startInstanceId;
    }

    public long getEndInstanceId() {
        return endInstanceId;
    }

    public void setEndInstanceId(long endInstanceId) {
        this.endInstanceId = endInstanceId;
    }

    public long getLessThanInstanceId() {
        return lessThanInstanceId;
    }

    public void setLessThanInstanceId(long lessThanInstanceId) {
        this.lessThanInstanceId = lessThanInstanceId;
    }

    public long getGreaterThanInstanceId() {
        return greaterThanInstanceId;
    }

    public void setGreaterThanInstanceId(long greaterThanInstanceId) {
        this.greaterThanInstanceId = greaterThanInstanceId;
    }

    public List<InstanceState> getInstanceState() {
        return instanceState;
    }

    public void setInstanceState(List<InstanceState> instanceState) {
        this.instanceState = instanceState;
    }

    public List<String> getExitStatusList() {
        return exitStatusList;
    }

    public void setExitStatusList(List<String> exitStatus) {
        this.exitStatusList = exitStatus;
    }

    public Date getStartCreateTime() {
        return startCreateTime;
    }

    public void setStartCreateTime(Date startCreateTime) {
        this.startCreateTime = startCreateTime;
    }

    public Date getEndCreateTime() {
        return endCreateTime;
    }

    public void setEndCreateTime(Date endCreateTime) {
        this.endCreateTime = endCreateTime;
    }

    public String getLessThanCreateTime() {
        return lessThanCreateTime;
    }

    public void setLessThanCreateTime(String lessThanCreateTime) {
        this.lessThanCreateTime = lessThanCreateTime;
    }

    public String getGreaterThanCreateTime() {
        return greaterThanCreateTime;
    }

    public void setGreaterThanCreateTime(String greaterThanCreateTime) {
        this.greaterThanCreateTime = greaterThanCreateTime;
    }

    public List<Long> getInstanceIdList() {
        return instanceIdList;
    }

    public void setInstanceIdList(List<Long> instanceIdList) {
        this.instanceIdList = instanceIdList;
    }

    public Date getSpecificCreateTime() {
        return specificCreateTime;
    }

    public void setSpecificCreateTime(Date specificCreateTime) {
        this.specificCreateTime = specificCreateTime;
    }

    public Date getStartLastUpdatedTime() {
        return startLastUpdatedTime;
    }

    public void setStartLastUpdatedTime(Date startLastUpdatedTime) {
        this.startLastUpdatedTime = startLastUpdatedTime;
    }

    public Date getEndLastUpdatedTime() {
        return endLastUpdatedTime;
    }

    public void setEndLastUpdatedTime(Date endLastUpdatedTime) {
        this.endLastUpdatedTime = endLastUpdatedTime;
    }

    public String getLessThanLastUpdatedTime() {
        return lessThanLastUpdatedTime;
    }

    public void setLessThanLastUpdatedTime(String lessThanLastUpdatedTime) {
        this.lessThanLastUpdatedTime = lessThanLastUpdatedTime;
    }

    public String getGreaterThanLastUpdatedTime() {
        return greaterThanLastUpdatedTime;
    }

    public void setGreaterThanLastUpdatedTime(String greaterThanLastUpdatedTime) {
        this.greaterThanLastUpdatedTime = greaterThanLastUpdatedTime;
    }

    public Date getSpecificLastUpdatedTime() {
        return specificLastUpdatedTime;
    }

    public void setSpecificLastUpdatedTime(Date specificLastUpdatedTime) {
        this.specificLastUpdatedTime = specificLastUpdatedTime;
    }

    public List<String> getSubmitterList() {
        return submitterList;
    }

    public void setSubmitterList(List<String> submitter) {
        this.submitterList = submitter;
    }

    public String getQueryIssuer() {
        return queryIssuer;
    }

    public void setQueryIssuer(String issuer) {
        this.queryIssuer = issuer;
    }

    public List<String> getAppNameList() {
        return appNameList;
    }

    public void setAppNameList(List<String> appNames) {
        this.appNameList = appNames;
    }

    public List<String> getJobNameList() {
        return jobNameList;
    }

    public void setJobNameList(List<String> jobNames) {
        this.jobNameList = jobNames;
    }

    public boolean getIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public Map<String, String> getJobParameters() {
        return jobParams;
    }

    public List<String> getSortList() {
        return sortList;
    }

    public void setSortList(List<String> sortList) {
        this.sortList = sortList;
    }

    /**
     * Helper method to print the content of the WSSearchObject
     */
    @Override
    public String toString() {

        StringBuffer sb = new StringBuffer();
        sb.append("WSSearchObject content :: \n");
        sb.append("startInstanceId = " + this.startInstanceId + "\n");
        sb.append("endInstanceId = " + this.endInstanceId + "\n");
        sb.append("lessThanInstanceId = " + this.lessThanInstanceId + "\n");
        sb.append("greaterThanInstanceId = " + this.greaterThanInstanceId + "\n");
        sb.append("instanceState = " + this.instanceState + "\n");
        sb.append("exitStatus = " + this.exitStatusList + "\n");
        sb.append("startCreateTime = " + this.startCreateTime + "\n");
        sb.append("endCreateTime = " + this.endCreateTime + "\n");
        sb.append("lessThanCreateTime = " + this.lessThanCreateTime + "\n");
        sb.append("greaterThanCreateTime = " + this.greaterThanCreateTime + "\n");
        sb.append("instanceIdList = " + this.instanceIdList + "\n");
        sb.append("specificCreateTime = " + this.specificCreateTime + "\n");
        sb.append("startLastUpdatedTime = " + this.startLastUpdatedTime + "\n");
        sb.append("endLastUpdatedTime = " + this.endLastUpdatedTime + "\n");
        sb.append("lessThanLastUpdatedTime = " + this.lessThanLastUpdatedTime + "\n");
        sb.append("greaterThanLastUpdatedTime = " + this.greaterThanLastUpdatedTime + "\n");
        sb.append("specificLastUpdatedTime = " + this.specificLastUpdatedTime + "\n");
        sb.append("sortList = " + this.sortList + "\n");
        sb.append("submitter = " + this.submitterList + "\n");
        sb.append("queryIssuer = " + this.queryIssuer + "\n");
        sb.append("appName = " + this.appNameList + "\n");
        sb.append("jobName = " + this.jobNameList + "\n");
        sb.append("ignoreCase = " + this.ignoreCase + "\n");
        if (jobParams != null) {
            for (Map.Entry<String, String> e : this.jobParams.entrySet()) {
                sb.append("jobParameter." + e.getKey() + "=" + e.getValue() + "\n");
            }
        }

        return sb.toString();
    }

    /**
     *
     * @param date
     * @return
     */
    private Date setDayEndForDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());
        cal.set(Calendar.HOUR_OF_DAY, 24);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return (new Date(cal.getTimeInMillis() - 1L));
    }

    /**
     *
     * @param date
     * @return
     */
    private Date setDayStartForDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis());
    }

    /**
     *
     * @param value
     * @return
     */
    private boolean isNegative(long value) {
        return (value < 0) ? true : false;
    }

	public void setGroupsForGroupSecurity(Set<String> groupsForSubject) {
		groupSecuritySubjectGroupsList = groupsForSubject;		
	}

	public Set<String> getGroupsForGroupSecurity() {
		
		return groupSecuritySubjectGroupsList;
	}
}
