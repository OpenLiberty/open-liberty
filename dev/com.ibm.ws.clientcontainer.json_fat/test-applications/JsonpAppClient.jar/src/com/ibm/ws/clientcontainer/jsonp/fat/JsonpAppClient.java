/*******************************************************************************
 * Copyright (c) 2015,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.jsonp.fat;

public class JsonpAppClient {

    public static void main(String[] args) {
        System.out.println("\nEntering JSON-P Application Client.");
        
        /**
         * Ensure that JsonObjectBuilder is functioning.
         */
        BuildJSONP buildClient = new BuildJSONP();
        buildClient.testJsonBuild();
        System.out.println("Completed test BuildJSONP.testJsonBuild\n");

        /**
         * Ensure that JsonReader is functioning.
         */
        ReadJSONP readClient = new ReadJSONP();
        readClient.testJsonRead();
        System.out.println("Completed test ReadJSONP.testJsonRead\n");

        /**
         * Ensure that JsonGenerator is functioning.
         */
        StreamJSONP streamClient = new StreamJSONP();
        streamClient.testJsonStream();
        System.out.println("Completed test StreamJSONP.testJsonStream\n");

        /**
         * Ensure that JsonWriter is functioning.
         */
        WriteJSONP writeClient = new WriteJSONP();
        writeClient.testJsonWrite();
        System.out.println("Completed test WriteJSONP.testJsonWrite\n");
        
        System.out.println("\nJSON-P Application Client Completed.");
    }

}
