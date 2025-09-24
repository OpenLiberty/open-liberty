/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.version20.test.http.war.app;

import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

@RestController
@RequestMapping("/mbean")
public class MBeanGetter {

    public static MBeanServer mbeanConn = ManagementFactory.getPlatformMBeanServer();

	@RequestMapping(value = "query", method = RequestMethod.GET)
	public String qeryTest(@RequestParam("objectName") String objectName) throws Exception{

        if (objectName == null || objectName.isEmpty()) {
            System.out.println("Was not provided an objectName");
			return "Was not provided an objectName";
        } else {
            try {
                //returns/writes  boolean
                boolean isRegistered = mbeanConn.isRegistered(new ObjectName(objectName));

                //perhaps too slow. Give it some time/
                if (!isRegistered) {
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        //don't really need to care for exception
                    }
                }

                StringBuilder sb = new StringBuilder();

                if (isRegistered) {
                    sb.append(isRegistered);
                } else { //lets print out the existing HTTP MBeans to help debug
                    Set<ObjectInstance> set;
                    set = mbeanConn.queryMBeans(new ObjectName("WebSphere:type=HttpServerStats,name=*"), null);
                    isRegistered = mbeanConn.isRegistered(new ObjectName(objectName));
                    //one more time.
                    if (isRegistered) {
                        sb.append(isRegistered);
                    } else {
                        set.stream().forEach(oj -> sb.append(oj + "\n"));
                    }

                }
                return sb.toString();

            } catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            }
        }
	}

}