/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.jsp.compiler;

/**
 * The JspLineId class represents a jsp source line number and its associated generated source details.
 */

public class JspLineId {
    
    protected String filePath = null;
    protected String generatedFilePath = null;
	protected String parentFile = null;
    protected int startSourceLineNum = 0;
    protected int startSourceColNum = 0;
    protected int sourceLineCount = 0;
    protected int endSourceLineNum = 0;
    protected int endSourceColNum = 0;
    protected int startGeneratedLineNum = 0;
    protected int startGeneratedLineCount = 0;
    protected int endGeneratedLineNum = 0;
    protected int endGeneratedLineCount = 0;
    
    public JspLineId(String filePath, 
                     String generatedFilePath,
                     String parentFile,  
                     int startSourceLineNum,
                     int startSourceColNum,
                     int sourceLineCount,
                     int endSourceLineNum,
                     int endSourceColNum,
                     int startGeneratedLineNum,
                     int startGeneratedLineCount,
                     int endGeneratedLineNum,
                     int endGeneratedLineCount) {
        this.filePath = filePath;                         
        this.generatedFilePath = generatedFilePath;                         
		this.parentFile = parentFile;                         
        this.startSourceLineNum = startSourceLineNum; 
        this.startSourceColNum = startSourceColNum; 
        this.sourceLineCount = sourceLineCount; 
        this.endSourceLineNum = endSourceLineNum; 
        this.endSourceColNum = endSourceColNum; 
        this.startGeneratedLineNum = startGeneratedLineNum; 
        this.startGeneratedLineCount = startGeneratedLineCount; 
        this.endGeneratedLineNum = endGeneratedLineNum; 
        this.endGeneratedLineCount = endGeneratedLineCount; 
    }
    
    /**
     * @return int The line count for the 'end' generated section of code
     */
    public int getEndGeneratedLineCount() {
        return endGeneratedLineCount;
    }

    /**
     * @return int The line number in the generated source for the 'end' section of code
     */
    public int getEndGeneratedLineNum() {
        return endGeneratedLineNum;
    }

    /**
     * @return int The line number in the generated source for the 'end' section of code
     */
    public int getEndSourceColNum() {
        return endSourceColNum;
    }

    /**
     * @return int The source line number for the 'end' section of code
     */
    public int getEndSourceLineNum() {
        return endSourceLineNum;
    }

    /**
     * @return String the path of the JSP file
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return int The number of lines for the source
     */
    public int getSourceLineCount() {
        return sourceLineCount;
    }

    /**
     * @return int The number of lines for the 'start' section of code
     */
    public int getStartGeneratedLineCount() {
        return startGeneratedLineCount;
    }

    /**
     * @return int The line number of the 'start' section of generated code
     */
    public int getStartGeneratedLineNum() {
        return startGeneratedLineNum;
    }

    /**
     * @return int the column number of the 'start' section of generated code
     */
    public int getStartSourceColNum() {
        return startSourceColNum;
    }

    /**
     * @return int The line number of the 'start' section of source code 
     */
    public int getStartSourceLineNum() {
        return startSourceLineNum;
    }

    /**
     * @return String The path of the generated source file 
     */
    public String getGeneratedFilePath() {
        return generatedFilePath;
    }
	/**
	 * @return String The path of the parent file 
	 */
	public String getParentFile() {
		return parentFile;
	}

    public String toString() {
        String separatorString = System.getProperty("line.separator");
        
        return new String (""+separatorString+
            "filePath =                    [" + filePath +"]"+separatorString+                         
            "generatedFilePath =           [" +generatedFilePath +"]"+separatorString+            
			"parentFile =                  [" +parentFile +"]"+separatorString+            
            "startSourceLineNum =          [" +startSourceLineNum +"]"+separatorString+
            "startSourceColNum =           [" +startSourceColNum +"]"+separatorString+
            "sourceLineCount =             [" +sourceLineCount +"]"+separatorString+
            "endSourceLineNum =            [" +endSourceLineNum +"]"+separatorString+
            "endSourceColNum =             [" +endSourceColNum +"]"+separatorString+
            "startGeneratedLineNum =       [" +startGeneratedLineNum +"]"+separatorString+
            "startGeneratedLineCount =     [" +startGeneratedLineCount +"]"+separatorString+
            "endGeneratedLineNum =         [" +endGeneratedLineNum +"]"+separatorString+
            "endGeneratedLineCount =       [" +endGeneratedLineCount +"]"+separatorString+
            "");
    }
}
