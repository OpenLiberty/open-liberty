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
package com.ibm.ws.jbatch.utility.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import com.ibm.ws.jbatch.utility.http.HttpUtils;
import com.ibm.ws.jbatch.utility.http.Response;
import com.ibm.ws.jbatch.utility.rest.BatchRestClient;
import com.ibm.ws.jbatch.utility.utils.ArgumentRequiredException;
import com.ibm.ws.jbatch.utility.utils.ConsoleWrapper;
import com.ibm.ws.jbatch.utility.utils.ObjectUtils;
import com.ibm.ws.jbatch.utility.utils.StringUtils;
import com.ibm.ws.jbatch.utility.utils.TaskIO;

/**
 * jbatch utility task that downloads a joblog.
 * 
 */
public class GetJobLogTask extends BaseBatchRestTask<GetJobLogTask> {

    /**
     * CTOR.
     */
    public GetJobLogTask(String scriptName) {
        super("getJobLog", scriptName);
    }

    /**
     * 
     */
    @Override
    public int handleTask(ConsoleWrapper stdin, 
                           PrintStream stdout,
                           PrintStream stderr, 
                           String[] args) throws Exception {

        setTaskIO( new TaskIO(stdin, stdout, stderr) );
        
        setTaskArgs(args);
        
        // Make sure either --jobInstanceId or --jobExecutionId is specified.
        verifyRequiredArgs();
        
        BatchRestClient batchRestClient = buildBatchRestClient();
        
        Response response = (getJobInstanceId() != null) 
                                ? batchRestClient.getJobLogsForJobInstance( getJobInstanceId(), getType() )
                                : batchRestClient.getJobLogsForJobExecution( getJobExecutionId(), getType() );
        
        // If --outputfile is specified, use that.
        // Otherwise if a filename= came back in the Content-Disposition header (meaning
        // the response payload is a zip file), then use that filename. 
        File outputFile = ObjectUtils.firstNonNull( getOutputFile(),
                                                    parseFileFromDisposition( response.getHeader( "Content-Disposition" ) ) );
        
        // If neither --outputFile was specified, nor did the response contain a Content-Disposition
        // header (meaning the response payload is plain text), then write to STDOUT.
        OutputStream outputStream = (outputFile != null) 
                                        ? new FileOutputStream(outputFile)                
                                        : getTaskIO().getStdout() ;
                
        // Write the response to the output stream.
        response.copyToStream(outputStream);
        
        if (outputFile != null) {
            outputStream.close();

            issueJobLogToFileMessage( outputFile );
        }
        
        return 0;
    }

    /**
     * @return the --outputFile arg, or null if not specified
     */
    protected File getOutputFile() {
        return getTaskArgs().getFileValue("--outputFile");
    }
    
    /**
     * @return the --jobExecutionId arg.
     */
    protected Long getJobExecutionId() {
        return getTaskArgs().getLongValue("--jobExecutionId", null);
    }
    
    /**
     * @return the --jobInstanceId arg.
     */
    protected Long getJobInstanceId() {
        return getTaskArgs().getLongValue("--jobInstanceId", null);
    }
    
    /**
     * @return the --type=text|zip arg. If not specified the default is "text"
     */
    protected String getType() {
        return getTaskArgs().verifyStringValue("--type",
                                               Arrays.asList("text", "zip"),
                                               "text");
    }
    
    /**
     * @throws ArgumentRequiredException if neither --jobInstanceId or --jobExecutionId is specified.
     */
    protected void verifyRequiredArgs() throws IOException {
        if (getJobInstanceId() == null && getJobExecutionId() == null) {
            throw new ArgumentRequiredException("--jobInstanceId or --jobExecutionId");
        }
    }
    
    /**
     * @return The File named by the given Content-Disposition header.
     */
    protected File parseFileFromDisposition(String contentDisposition) {
        String filename = HttpUtils.parseHeaderParameter(contentDisposition, "filename");
        
        if (StringUtils.isEmpty(filename)) {
            return null;
        } else {
            return new File(StringUtils.dequote( filename ) );
        }
    }
    
    /**
     * print out the given jobinstance record.
     */
    protected void issueJobLogToFileMessage(File outputFile) throws IOException {
        getTaskIO().info( getMessage("joblog.to.file", outputFile.getCanonicalPath() ) );
    }
    
}

