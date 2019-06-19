/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

public class ZipCachingIntrospectorOutput{
    
    public String introspectorDescription, entryCacheSettings, zipReaperSettings, handleIntrospection, zipEntryCache, zipReaperValues;
    public String activeAndPendingIntrospection;
    public String pendingQuickIntrospection;
    public String pendingSlowIntrospection;
    public String completedIntrospection;

    /*
    private PropertyIntrospection parsePropertyLine(String description, String line){
        String[] splitLine = line.split("\\s+[\\[\\]\\s]+\\s*");
        return new PropertyIntrospection(description, splitLine[1], splitLine[2], splitLine[3]);
    }
    */

    public ZipCachingIntrospectorOutput(InputStream in) throws IOException{
        BufferedReader zipCachingReader = new BufferedReader(new InputStreamReader(in));
        String currentLine, aggregateLine;

        zipCachingReader.readLine();//="The description of this introspector:"

        introspectorDescription = zipCachingReader.readLine();//="Liberty zip file caching diagnostics"

        do{
            currentLine = zipCachingReader.readLine();
        }while(!(currentLine.equals("No ZipCachingServiceImpl configured") || currentLine.equals("Entry Cache Settings:")));

        //if there was no ZipCachingService to introspect then return early
        if(currentLine.equals("No ZipCachingServiceImpl configured")){
            zipCachingReader.close();
            return;
        }

        aggregateLine = "";
        currentLine = zipCachingReader.readLine();
        while(!currentLine.equals("Zip Reaper Settings:")){
            if(currentLine.equals("") == false){
                aggregateLine = aggregateLine.concat(currentLine.concat("\n"));
            }

            currentLine = zipCachingReader.readLine();
        }

        entryCacheSettings = aggregateLine;
        aggregateLine = "";

        currentLine = zipCachingReader.readLine();
        while(currentLine.equals("") == false){
            aggregateLine = aggregateLine.concat(currentLine.concat("\n"));

            currentLine = zipCachingReader.readLine();
        }

        zipReaperSettings = aggregateLine;
        aggregateLine = "";

        zipCachingReader.readLine();//="The entry cache is a cache of small zip file entries."
        zipCachingReader.readLine();//="The entry cache is disabled if either setting is 0."
        zipCachingReader.readLine();//=""
        zipCachingReader.readLine();//="The zip reaper is a service which delays closes of zip files."
        zipCachingReader.readLine();//="The zip reaper is disabled if the maximum pending closes setting is 0."
        zipCachingReader.readLine();//=""
        zipCachingReader.readLine();//="Active and Cached ZipFile Handles:"

        currentLine = zipCachingReader.readLine();
        if(currentLine.equals("  ** NONE **") == false){
            while(currentLine.equals("") == false){
                aggregateLine = aggregateLine.concat(currentLine.concat("\n"));
    
                currentLine = zipCachingReader.readLine();
            }
        }
        else{
            aggregateLine = null;
            zipCachingReader.readLine();
        }
        
        handleIntrospection = aggregateLine;
        aggregateLine = "";

        zipCachingReader.readLine();//="Zip Entry Cache:"

        currentLine = zipCachingReader.readLine();
        if(currentLine.equals("  ** DISABLED **") == false){
            while(currentLine.equals("") == false){
                aggregateLine = aggregateLine.concat(currentLine.concat("\n"));
    
                currentLine = zipCachingReader.readLine();
            }
        }
        else{
            aggregateLine = null;
            zipCachingReader.readLine();
        }

        zipEntryCache = aggregateLine;
        aggregateLine = "";

        zipCachingReader.readLine();//="Zip Reaper:"

        currentLine = zipCachingReader.readLine();
        if(currentLine.equals("  ** DISABLED **") == false){
            while(currentLine.equals("Active and Pending Data:") == false){
                aggregateLine = aggregateLine.concat(currentLine.concat("\n"));
    
                currentLine = zipCachingReader.readLine();
            }
        }
        else{
            //nothing after Zip Reaper: if it is disabled
            zipCachingReader.close();
            return;
        }

        zipReaperValues = aggregateLine;
        aggregateLine = "";

        currentLine = zipCachingReader.readLine();
        if(currentLine.equals("  ** NONE **") == false){
            while(currentLine.equals("Zip File Data [ pendingQuick ]") == false){
                aggregateLine = aggregateLine.concat(currentLine.concat("\n"));
    
                currentLine = zipCachingReader.readLine();
            }
        }
        else{
            aggregateLine = null;
            zipCachingReader.readLine();
            currentLine = zipCachingReader.readLine();//="Zip File Data [ pendingQuick ]"
        }

        activeAndPendingIntrospection = aggregateLine;
        aggregateLine = "";

        currentLine = zipCachingReader.readLine();
        if(currentLine.equals("  ** NONE **") == false){
            while(currentLine.equals("Zip File Data [ pendingSlow ]") == false){
                aggregateLine = aggregateLine.concat(currentLine.concat("\n"));
    
                currentLine = zipCachingReader.readLine();
            }
        }
        else{
            aggregateLine = null;
            zipCachingReader.readLine();
            currentLine = zipCachingReader.readLine();//="Zip File Data [ pendingSlow ]"
        }

        pendingQuickIntrospection = aggregateLine;
        aggregateLine = "";

        currentLine = zipCachingReader.readLine();
        if(currentLine.equals("  ** NONE **") == false){
            while(!(currentLine.equals("Zip File Data [ completed ]") || currentLine.equals("Completed zip file data is not being tracked"))) {
                aggregateLine = aggregateLine.concat(currentLine.concat("\n"));
    
                currentLine = zipCachingReader.readLine();
            }
        }
        else{
            aggregateLine = null;
            zipCachingReader.readLine();
            currentLine = zipCachingReader.readLine();//="Zip File Data [ completed ]"
        }

        pendingSlowIntrospection = aggregateLine;
        aggregateLine = "";

        if(currentLine.equals("Completed zip file data is not being tracked")){
            zipCachingReader.close();
            return;
        }

        zipCachingReader.readLine();//=""
        currentLine = zipCachingReader.readLine();
        while(currentLine != null){
            aggregateLine = aggregateLine.concat(currentLine.concat("\n"));

            currentLine = zipCachingReader.readLine();
        }

        completedIntrospection = aggregateLine;

        zipCachingReader.close();
    }



}