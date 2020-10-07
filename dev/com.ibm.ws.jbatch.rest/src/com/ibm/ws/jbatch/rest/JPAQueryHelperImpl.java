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
package com.ibm.ws.jbatch.rest;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.TypedQuery;

import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.services.IJPAQueryHelper;
import com.ibm.ws.jbatch.rest.utils.WSSearchConstants;
import com.ibm.ws.jbatch.rest.utils.WSSearchObject;

/**
 * This Class helps construct Dynamic JPA queries given the URL parameters in the WSSearchObject
 */
public class JPAQueryHelperImpl implements IJPAQueryHelper {

    private final String BASE_QUERY = "SELECT x from JobInstanceEntity x";
    private StringBuilder query;
    private StringBuilder whereClause;
    private boolean addAND;
    StringBuilder orderClause;
    private final WSSearchObject wsso;
    private int instanceVersion = 1;

    // Make visible for unit test.
    Map<String, Object> parameterMap = new HashMap<String, Object>();

    /**
     * CTOR
     */
    public JPAQueryHelperImpl(WSSearchObject wsso) {
        this.wsso = wsso;
    }

    /**
     * Processes the different parameters passed via the WSSearchObject
     *
     * @param wsso
     * @return
     */
    private void processParameters() {
    	// Initialize
    	query = new StringBuilder().append(BASE_QUERY);
        whereClause = new StringBuilder();
        addAND = false;
        orderClause = new StringBuilder();
    	
        // Process the parameters
        processInstanceIdParams();
        processCreateTimeParams();
        processInstanceStateParams();
        processExitStatusParams();
        processSubmitterParams();
        processSecurityFilters();
        processLastUpdatedTimeParams();
        processAppNameParams();
        processJobNameParams();
        processJobParameter();
        processSortParams();

        // If we built any snippets add them to the WHERE clause
        if (whereClause.length() != 0) {
            query.append(" WHERE " + whereClause);
        }
        // If there's a sort being done on the results, we'll have an ORDER BY clause
        if (orderClause.length() != 0) {
            query.append(" ORDER BY " + orderClause);
        }

    }

    /**
     * Handles ANDing portions of the WHERE clause
     *
     * @param addAND
     * @param query
     */
    private void handleSQLForAND() {
        if (addAND == true) {
            whereClause.append(" AND ");
        }
        addAND = true;
    }

    /**
     * Processes the sort parameters
     *
     * @param wsso
     */
    private void processSortParams() {

        List<String> sortList = wsso.getSortList();

        if (sortList != null) {

            String delim = "";
            for (String field : sortList) {

                // items prefixed with "-" mean sort descending
                boolean desc = false;
                if (field.startsWith("-")) {
                    field = field.substring(1);
                    desc = true;
                }

                if (WSSearchConstants.VALID_SORT_FIELDS.contains(field)) {

                    orderClause.append(delim);
                    orderClause.append("x." + field);

                    if (desc)
                        orderClause.append(" DESC");

                    delim = ",";

                    if (field.equals("lastUpdatedTime")) {
                        instanceVersion = 2;
                    }
                }
            }
        }
    }

    /**
     * Processes the lastUpdatedTime parameters
     *
     * @param wsso
     */
    private void processLastUpdatedTimeParams() {
        if (wsso.getStartLastUpdatedTime() != null && wsso.getEndLastUpdatedTime() != null) {
            instanceVersion = 2;
            handleSQLForAND();
            whereClause.append("x.lastUpdatedTime BETWEEN :startLastUpdatedTime AND :endLastUpdatedTime");
            parameterMap.put("startLastUpdatedTime", wsso.getStartLastUpdatedTime());
            parameterMap.put("endLastUpdatedTime", wsso.getEndLastUpdatedTime());
        } else if (wsso.getSpecificLastUpdatedTime() != null) {
            instanceVersion = 2;
            handleSQLForAND();
            whereClause.append("x.lastUpdatedTime BETWEEN :specificLastUpdatedTimeStart AND :specificLastUpdatedTimeEnd");
            parameterMap.put("specificLastUpdatedTimeStart", setDayStartForDate(wsso.getSpecificLastUpdatedTime()));
            parameterMap.put("specificLastUpdatedTimeEnd", setDayEndForDate(wsso.getSpecificLastUpdatedTime()));
        } else if (wsso.getLessThanLastUpdatedTime() != null) {
            instanceVersion = 2;
            handleSQLForAND();
            whereClause.append("x.lastUpdatedTime <= :lessThanLastUpdatedTime");
            parameterMap.put("lessThanLastUpdatedTime", setDayEndForDate(subtractDaysFromCurrentDate(new Integer(wsso.getLessThanLastUpdatedTime()))));
        } else if (wsso.getGreaterThanLastUpdatedTime() != null) {
            instanceVersion = 2;
            handleSQLForAND();
            whereClause.append("x.lastUpdatedTime >= :greaterThanLastUpdatedTime");
            parameterMap.put("greaterThanLastUpdatedTime", setDayStartForDate(subtractDaysFromCurrentDate(new Integer(wsso.getGreaterThanLastUpdatedTime()))));
        }
    }

    /**
     * Processes the createTime parameters
     *
     * @param wsso
     * @return
     */
    private void processCreateTimeParams() {
        if (wsso.getStartCreateTime() != null && wsso.getEndCreateTime() != null) {
            handleSQLForAND();
            whereClause.append("x.createTime BETWEEN :startCreateTime AND :endCreateTime");
            parameterMap.put("startCreateTime", wsso.getStartCreateTime());
            parameterMap.put("endCreateTime", wsso.getEndCreateTime());
        } else if (wsso.getSpecificCreateTime() != null) {
            handleSQLForAND();
            whereClause.append("x.createTime BETWEEN :specificCreateTimeStart AND :specificCreateTimeEnd");
            parameterMap.put("specificCreateTimeStart", setDayStartForDate(wsso.getSpecificCreateTime()));
            parameterMap.put("specificCreateTimeEnd", setDayEndForDate(wsso.getSpecificCreateTime()));
        } else if (wsso.getLessThanCreateTime() != null) {
            handleSQLForAND();
            whereClause.append("x.createTime <= :lessThanCreateTime");
            parameterMap.put("lessThanCreateTime", setDayEndForDate(subtractDaysFromCurrentDate(new Integer(wsso.getLessThanCreateTime()))));
        } else if (wsso.getGreaterThanCreateTime() != null) {
            handleSQLForAND();
            whereClause.append("x.createTime >= :greaterThanCreateTime");
            parameterMap.put("greaterThanCreateTime", setDayStartForDate(subtractDaysFromCurrentDate(new Integer(wsso.getGreaterThanCreateTime()))));
        }
    }

    /**
     * Processes the instanceId parameters
     *
     * @param wsso
     * @return
     */
    private void processInstanceIdParams() {
        if (wsso.getStartInstanceId() != -1 && wsso.getEndInstanceId() != -1) {
            handleSQLForAND();
            whereClause.append("x.instanceId BETWEEN :startInstanceId AND :endInstanceId");
            parameterMap.put("startInstanceId", wsso.getStartInstanceId());
            parameterMap.put("endInstanceId", wsso.getEndInstanceId());
        } else if (wsso.getLessThanInstanceId() != -1) {
            handleSQLForAND();
            whereClause.append("x.instanceId <= :lessThanInstanceId");
            parameterMap.put("lessThanInstanceId", wsso.getLessThanInstanceId());
        } else if (wsso.getGreaterThanInstanceId() != -1) {
            handleSQLForAND();
            whereClause.append("x.instanceId >= :greaterThanInstanceId");
            parameterMap.put("greaterThanInstanceId", wsso.getGreaterThanInstanceId());
        } else if (wsso.getInstanceIdList() != null && wsso.getInstanceIdList().size() > 0) {
            handleSQLForAND();
            whereClause.append("x.instanceId IN :instanceIdList");
            parameterMap.put("instanceIdList", wsso.getInstanceIdList());
        }
    }

    /**
     * Processes the instanceState parameters
     *
     * @param wsso
     * @return
     */
    private void processInstanceStateParams() {
        if (wsso.getInstanceState() != null && wsso.getInstanceState().size() > 0) {
            handleSQLForAND();
            whereClause.append("x.instanceState IN :instanceStateList");
            parameterMap.put("instanceStateList", wsso.getInstanceState());
        }
    }

    /**
     * Processes the exitStatus parameters
     *
     * @param wsso
     * @return
     */
    private void processExitStatusParams() {
        String wildcard = null;
        if (wsso.getExitStatusList() != null && !wsso.getExitStatusList().isEmpty()) {
            handleSQLForAND();
            whereClause.append("(");
            int i = 1;
            for (String status : wsso.getExitStatusList()) {
                if (i > 1)
                    whereClause.append(" OR ");
                wildcard = status.replaceAll("\\*", "%");
                whereClause.append(doIgnoreCase("x.exitStatus") + " like " + doIgnoreCase(":exitStatus" + i));
                parameterMap.put("exitStatus" + i, wildcard);
                i++;
            }
            whereClause.append(")");
        }
    }

    /**
     * Processes the submitter parameter
     *
     * @param wsso
     * @return
     */
    private void processSubmitterParams() {
        if (wsso.getSubmitterList() != null && !wsso.getSubmitterList().isEmpty()) {
            handleSQLForAND();
            whereClause.append("(");
            int i = 1;
            for (String submitter : wsso.getSubmitterList()) {
                if (i > 1)
                    whereClause.append(" OR ");
                String wildcard = submitter.replaceAll("\\*", "%");
                whereClause.append(doIgnoreCase("x.submitter") + " like " + doIgnoreCase(":submitter" + i));
                parameterMap.put("submitter" + i, wildcard);
                i++;
            }
            whereClause.append(")");
        }
    }

    /**
     * Processes the submitter variable added by the auth service
     *
     * @param wsso
     * @return
     */
    private void processSecurityFilters() {

        if (wsso.getQueryIssuer() != null && wsso.getGroupsForGroupSecurity() != null) {
            handleSQLForAND();
            whereClause.append( " (( ");
            whereClause.append("x.submitter = :queryIssuer");
            whereClause.append(" ) OR (( ");
            whereClause.append("x.instanceId IN (SELECT DISTINCT v3.instanceId FROM JobInstanceEntityV3 v3 JOIN v3.groupNames g WHERE g IN :groups");
            whereClause.append("))))");
            parameterMap.put("queryIssuer", wsso.getQueryIssuer());
            parameterMap.put("groups", wsso.getGroupsForGroupSecurity());
        } else if (wsso.getQueryIssuer() != null) {
        	handleSQLForAND();
            whereClause.append("x.submitter = :queryIssuer");
            parameterMap.put("queryIssuer", wsso.getQueryIssuer());
        }
        
    }

    /**
     * Processes the appName parameter
     *
     * @param wsso
     * @return
     */
    private void processAppNameParams() {
        /**
         * The app name field in the repository is stored in the format [app name]#[ear/war filename]
         * We assume the user is giving us one of two things:
         * 1) The full repository entry including the # separator and all terms
         * 2) No # separator, in which case we assume they are matching on the first term (app name)
         */

        if (wsso.getAppNameList() != null && !wsso.getAppNameList().isEmpty()) {
            handleSQLForAND();
            whereClause.append("(");
            int i = 1;
            for (String appName : wsso.getAppNameList()) {
                if (i > 1)
                    whereClause.append(" OR ");
                String wildcard = appName.replaceAll("\\*", "%");

                // If the input does not include the # separator, assume they want to match the first term (app name).
                if (!wildcard.contains("#")) {
                    wildcard = wildcard.concat("#%");
                }

                whereClause.append(doIgnoreCase("x.amcName") + " like " + doIgnoreCase(":appName" + i));
                parameterMap.put("appName" + i, wildcard);
                i++;
            }
            whereClause.append(")");
        }
    }

    /**
     * Processes the jobName parameter
     *
     * @param wsso
     * @return
     */
    private void processJobNameParams() {
        if (wsso.getJobNameList() != null && !wsso.getJobNameList().isEmpty()) {
            handleSQLForAND();
            whereClause.append("(");
            int i = 1;
            for (String jobName : wsso.getJobNameList()) {
                if (i > 1)
                    whereClause.append(" OR ");
                String wildcard = jobName.replaceAll("\\*", "%");
                whereClause.append(doIgnoreCase("x.jobName") + " like " + doIgnoreCase(":jobName" + i));
                parameterMap.put("jobName" + i, wildcard);
                i++;
            }
            whereClause.append(")");
        }
    }

    private void processJobParameter() {
        Map<String, String> jobParams = wsso.getJobParameters();
        if (jobParams == null || jobParams.isEmpty())
            return;

        int i = 0;
        StringBuilder subquery1 = new StringBuilder();
        StringBuilder subquery2 = new StringBuilder();

        // Iterate over the list of parameters to search on.
        for (String paramName : jobParams.keySet()) {
            String paramValue = jobParams.get(paramName);
            if (paramName != null) {
                paramName = paramName.replaceAll("\\*", "%");

                if (paramValue != null) {
                    paramValue = paramValue.replaceAll("\\*", "%");
                } else {
                    // If no value was specified, match any value
                    // This shouldn't happen now, but still good as a null check
                    paramValue = "%";
                }

                String queryParam = "jobParamName" + ((i == 0) ? "" : i);
                String queryValue = "jobParamValue" + ((i == 0) ? "" : i);

                // Matching a parameter to an instance requires querying a join of the execution and parameter tables
                if (i == 0) {
                    subquery1.append("(SELECT e from JobExecutionEntityV2 e"
                                     + " JOIN e.jobParameterElements p");
                    subquery2.append(" WHERE " + doIgnoreCase("p.name") + " like " + doIgnoreCase(":" + queryParam)
                                     + " AND " + doIgnoreCase("p.value") + " like " + doIgnoreCase(":" + queryValue)
                                     + " AND e.jobInstance = x");
                } else {
                    // Additional parameters require inner joins on the parameter table
                    String pnum = "p" + i;
                    subquery1.append(" JOIN e.jobParameterElements " + pnum);
                    subquery2.append(" AND " + doIgnoreCase(pnum + ".name") + " like " + doIgnoreCase(":" + queryParam)
                                     + " AND " + doIgnoreCase(pnum + ".value") + " like " + doIgnoreCase(":" + queryValue));

                }

                parameterMap.put(queryParam, paramName);
                parameterMap.put(queryValue, paramValue);
                i++;
            }
        }
        handleSQLForAND();
        whereClause.append("EXISTS " + subquery1.toString() + subquery2.toString() + ")");
    }

    /**
     * Helper method to subtract days from the current date
     *
     * @param days
     * @return
     */
    private static Date subtractDaysFromCurrentDate(int days) {
        Calendar cal = Calendar.getInstance();
        Date date = new Date();
        cal.setTime(date);
        cal.add(Calendar.DATE, -days);

        return cal.getTime();
    }

    /**
     *
     * @param date
     * @return
     */
    private static Date setDayEndForDate(Date date) {
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
    private static Date setDayStartForDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis());
    }

    /**
     * Getter for the query parameter
     *
     * @return
     */
    @Override
    public String getQuery() {
    	processParameters();
        if (instanceVersion == 2) {
            return query.toString().replace("JobInstanceEntity", "JobInstanceEntityV2");
        } else {
            return query.toString();
        }
    }

    /**
     * @param query
     */
    @Override
    public void setQueryParameters(TypedQuery<JobInstanceEntity> query) {
        Iterator<String> iterator = parameterMap.keySet().iterator();
        while (iterator.hasNext()) {
            String name = iterator.next();
            Object value = parameterMap.get(name);
            query.setParameter(name, value);
        }
    }
    
    public void setAuthSubmitter(String submitter) {
    	wsso.setQueryIssuer(submitter);
    }
    
    // no @Override annotation here because the interface is in open but this closed impl of the iface
    // must be built first - changed the name of this method from the original 'setAuthSubmitter'
    public void setQueryIssuer(String queryIssuer) {
    	wsso.setQueryIssuer(queryIssuer);
    }

    /**
     * Wrap the text with UPPER if we're ignoring case
     */
    private String doIgnoreCase(String input) {
        if (wsso.getIgnoreCase() == true) {
            return "UPPER(" + input + ")";
        } else {
            return input;
        }
    }

    // no @Override annotation here because the interface is in open but this closed impl of the iface
    // must be built first
	public void setGroups(Set<String> groupsForSubject) {
		wsso.setGroupsForGroupSecurity(groupsForSubject);	
	}
}
