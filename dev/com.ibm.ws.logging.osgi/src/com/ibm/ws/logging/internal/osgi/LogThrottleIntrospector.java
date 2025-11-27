/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/


package com.ibm.ws.logging.internal.osgi;


import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.wsspi.logging.Introspector;

import com.ibm.ws.logging.utils.LogThrottlingUtils;
import com.ibm.ws.logging.utils.ThrottleState;


public class LogThrottleIntrospector implements Introspector {


    @Override
    public String getIntrospectorName() {
        return "LogThrottleIntrospector";
    }

    @Override
    public String getIntrospectorDescription() {
        return "List of logs being throttled.";
    }
    
    @Override
    public void introspect(PrintWriter out) throws Exception {
        Map <String, ThrottleState> throttleStates = LogThrottlingUtils.getThrottleStates();

        List <String> keyList = new ArrayList <>();
        int index = 1;
        int throttleMaxMessagesPerWindow = LogThrottlingUtils.getThrottleMaxMessages();
        String throttleType = LogThrottlingUtils.getThrottleType();
        String keyHeader = "KEY";
        
        out.println("Config:");
        out.println("throttleMaxMessagesPerWindow: " + throttleMaxMessagesPerWindow);
        out.println("throttleType: " + throttleType);
        out.println("throttleMapSize: " + LogThrottlingUtils.getThrottleMapSize() + "\n");

        
        if(throttleType.equals("message"))
        	keyHeader = "INDEX";
        
        out.printf(
            "%-15s %-12s %-25s %-25s %-15s%n",
            keyHeader, "COUNT", "LAST OCCURRENCE", "AGE", "THROTTLED"
        );
        out.printf(
                "%-12s %-12s %-25s %-25s %-15s%n",
                "", "(Last 5 min)", "", "", ""
            );

        out.println("------------------------------------------------------------------------------------------");

        for (Map.Entry < String, ThrottleState > s: throttleStates.entrySet()) {
            Instant last = Instant.ofEpochMilli(s.getValue().getLastAccessTime());
            long secondsAgo = Duration.between(last, Instant.now()).getSeconds();

            keyList.add(s.getKey());
            
            boolean throttled = s.getValue().getRunningTotal() > throttleMaxMessagesPerWindow ? true : false;
            
            if(throttleType.equals("message")) {
            	 out.printf(
                         "%-15s %-12s %-25s %-25s %-15s%n",
                         "#" + index,
                         s.getValue().getRunningTotal(),
                         DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                         .withZone(ZoneId.systemDefault())
                         .format(last),
                         formatDuration(secondsAgo),
                         throttled
                     );

                     index++;
            }
            else {
            	out.printf(
                        "%-15s %-12s %-25s %-25s %-15s%n",
                        s.getKey(),
                        s.getValue().getRunningTotal(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault())
                        .format(last),
                        formatDuration(secondsAgo),
                        throttled
                    );
            }
           
        }


        if(throttleType.equals("message")) {
	        out.println("------------------------------------------------------------------------------------------");
	        out.println("\nMessage References:");
	        out.println("------------------------------------------------------------------------------------------");
	        for (int i = 0; i < keyList.size(); i++) {
	            out.println("#" + (i + 1) + ":" + keyList.get(i) + "\n");
	        }
        }
        
        /*
        Sample output:
        
        Config:
        throttleMaxMessagesPerWindow: 3
        throttleType: messageID
        throttleMapSize: 500

		KEY             COUNT        LAST OCCURRENCE           AGE                       THROTTLED
             		 (Last 5 min)
		------------------------------------------------------------------------------------------
		TEST15005W      1            2025-11-27 13:57:44       00m 04s ago               false

        */

    }
    
    private static String formatDuration(long seconds) {
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02dm %02ds ago", minutes, secs);
    }
}
