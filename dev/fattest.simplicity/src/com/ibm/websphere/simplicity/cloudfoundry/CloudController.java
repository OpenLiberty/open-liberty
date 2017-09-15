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
package com.ibm.websphere.simplicity.cloudfoundry;

import com.ibm.websphere.simplicity.cloudfoundry.util.CloudProperties;

public final class CloudController {

    private final CloudProperties cloudProperties;
    private final String FAILED_STRING = "FAILED";

    public CloudController() {
        cloudProperties = new CloudProperties();
        setCFTarget();
        setCFLogin();
    }

    private void setCFTarget() {
        //String[] command = { cloudProperties.getCfCommand(), "api", "http://api." + cloudProperties.getHost() };
        String[] command = { "cf", "api", "http://api.stage1.ng.bluemix.net" };
        //System.out.println("Targeting CloudFoundry: " + cloudProperties.getHost());
        System.out.println("Targeting CloudFoundry: stage1.ng.bluemix.net");
        ExecuteCommandUtil.executeCommand(command);
    }

    private void setCFLogin() {
        //String[] command = { cloudProperties.getCfCommand(), "login", "-u", cloudProperties.getUserName(), "-p", cloudProperties.getPassword() };
        String[] command = { "cf", "login", "-u", "charcoch@uk.ibm.com", "-p", "charlie1" };
        //System.out.println("Login using username: " + cloudProperties.getUserName() + " and password: " + cloudProperties.getPassword());
        System.out.println("Login using username:charcoch@uk.ibm.com");
        ExecuteCommandUtil.executeCommand(command);
    }

    //public String pushApplication(Application app) {
    public void pushApplication() {
        //String failureOutput = "";
        //String[] command = { cloudProperties.getCfCommand(), "push", app.getName(), "-b", cloudProperties.getBuildpack() };
        String[] command = { "cf", "push", "cccctest", "-b", "https://github.com/cloudfoundry/ibm-websphere-liberty-buildpack" };//, "-p", "ServletSample.jar" };//jaxws_fat_server.zip
        //System.out.println("Pushing application: " + app.getName() + " from working directory: " + app.getApplicationPath().getAbsolutePath());
        ExecuteCommandUtil.executeCommand(command, "/home/charlie/testapp/env-javaweb");
        //if (ExecuteCommandUtil.executeCommand(command, app.getApplicationPath().getAbsolutePath()).contains(FAILED_STRING)) {
        //    failureOutput = getLogs(app.getName());
        //}
        //return failureOutput;
    }

    public String deleteAndPushApplication(Application app) {
        String failureOutput = "";
        deleteApplication(app.getName());
        String[] command = { cloudProperties.getCfCommand(), "push", app.getName(), "-b", cloudProperties.getBuildpack() };
        System.out.println("Pushing application: " + app.getName() + " from working directory: " + app.getApplicationPath().getAbsolutePath());
        if (ExecuteCommandUtil.executeCommand(command, app.getApplicationPath().getAbsolutePath()).contains(FAILED_STRING)) {
            failureOutput = getLogs(app.getName());
        }
        return failureOutput;
    }

    public void deleteApplication(String appName) {
        String[] command = { cloudProperties.getCfCommand(), "delete", "-f", appName };
        System.out.println("Deleting application: " + appName);
        ExecuteCommandUtil.executeCommand(command);
    }

    private String getLogs(String appName) {
        String[] command = { cloudProperties.getCfCommand(), "logs", appName, "--recent" };
        System.out.println("Getting logs for application: " + appName);
        return ExecuteCommandUtil.executeCommand(command);
    }

    public void CreateService(String serviceType, String servicePlan, String serviceName) {
        String[] command = { cloudProperties.getCfCommand(), "create-service", serviceType, servicePlan, serviceName };
        System.out.println("Creating service: " + serviceName);
        ExecuteCommandUtil.executeCommand(command);
    }

    public void deleteAndCreateService(String serviceType, String servicePlan, String serviceName) {
        deleteService(serviceName);
        String[] command = { cloudProperties.getCfCommand(), "create-service", serviceType, servicePlan, serviceName };
        System.out.println("Creating service: " + serviceName);
        ExecuteCommandUtil.executeCommand(command);
    }

    public void deleteService(String serviceName) {
        String[] command = { cloudProperties.getCfCommand(), "delete-service", "-f", serviceName };
        System.out.println("Deleting service: " + serviceName);
        ExecuteCommandUtil.executeCommand(command);
    }

    public void bindService(String appName, String serviceName) {
        String[] command = { cloudProperties.getCfCommand(), "bind-service", appName, serviceName };
        System.out.println("Binding service: " + serviceName + " to application: " + appName);
        ExecuteCommandUtil.executeCommand(command);
    }

    public void restageApplication(String appName) {
        String[] command = { cloudProperties.getCfCommand(), "restage", appName };
        System.out.println("Restaging application: " + appName);
        ExecuteCommandUtil.executeCommand(command);
    }

}
