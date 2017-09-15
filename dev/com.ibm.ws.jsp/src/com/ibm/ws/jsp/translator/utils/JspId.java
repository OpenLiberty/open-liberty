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
package com.ibm.ws.jsp.translator.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class JspId {
    protected List attrNameList = new ArrayList();
    protected String filePath = null;
    protected String fileName = null;
    protected int startSourceLineNum = 0;
    protected int startSourceColNum = 0;
    protected int sourceLineCount = 0;
    protected int endSourceLineNum = 0;
    protected int endSourceColNum = 0;
    protected int startGeneratedLineNum = 0;
    protected int startGeneratedLineCount = 0;
    protected int endGeneratedLineNum = 0;
    protected int endGeneratedLineCount = 0;
    
    public JspId(String jspIdString) {
        StringTokenizer st = null;
        String id = jspIdString;
        if (jspIdString.indexOf('{') != -1 && jspIdString.indexOf('}') != -1) {
            String attrNames = jspIdString.substring(jspIdString.indexOf('{')+1, jspIdString.indexOf('}'));
            st = new StringTokenizer(attrNames, "~");
            while (st.hasMoreTokens()) {
                attrNameList.add(st.nextToken());    
            }
            id = jspIdString.substring(jspIdString.indexOf('}')+1);
        }
        try {
            filePath = URLDecoder.decode(id.substring(0, id.indexOf('[')), "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String mappingInfo = id.substring(id.indexOf('['));
        String sourceInfo = null;
        if (mappingInfo.indexOf(':') != -1) {
            sourceInfo = mappingInfo.substring(0, mappingInfo.indexOf(':'));
            String generatedInfo = mappingInfo.substring(mappingInfo.indexOf(':')+1);
            
            if (generatedInfo.indexOf('[') != generatedInfo.lastIndexOf('[')) {
				st = new StringTokenizer(generatedInfo.substring(1, generatedInfo.indexOf(']')), ",");
            }
            else {
                st = new StringTokenizer(generatedInfo.substring(1, generatedInfo.length()-1), ",");
            }
            
            startGeneratedLineNum = Integer.valueOf(st.nextToken()).intValue();
            startGeneratedLineCount = Integer.valueOf(st.nextToken()).intValue();
            
            if (generatedInfo.indexOf('[') != generatedInfo.lastIndexOf('[')) {
                st = new StringTokenizer(generatedInfo.substring(generatedInfo.lastIndexOf('[')+1, generatedInfo.lastIndexOf(']')), ",");
                endGeneratedLineNum = Integer.valueOf(st.nextToken()).intValue();
                endGeneratedLineCount = Integer.valueOf(st.nextToken()).intValue();
            }
        }
        else {
            sourceInfo = mappingInfo.substring(0, mappingInfo.indexOf(']')+1);
        }
        if (sourceInfo.indexOf('[') != sourceInfo.lastIndexOf('[')) 
            st = new StringTokenizer(sourceInfo.substring(1, sourceInfo.indexOf(']')), ",");
        else 
            st = new StringTokenizer(sourceInfo.substring(1, sourceInfo.length()-1), ",");
        
        startSourceLineNum = Integer.valueOf(st.nextToken()).intValue();
        startSourceColNum = Integer.valueOf(st.nextToken()).intValue();
        sourceLineCount = Integer.valueOf(st.nextToken()).intValue();
            
        if (sourceInfo.indexOf('[') != sourceInfo.lastIndexOf('[')) {
            st = new StringTokenizer(sourceInfo.substring(sourceInfo.lastIndexOf('[')+1, sourceInfo.lastIndexOf(']')), ",");
            endSourceLineNum = Integer.valueOf(st.nextToken()).intValue();
            endSourceColNum = Integer.valueOf(st.nextToken()).intValue();
        }
    }
    
    /**
     * Returns the endGeneratedLineCount.
     * @return int
     */
    public int getEndGeneratedLineCount() {
        return endGeneratedLineCount;
    }

    /**
     * Returns the endGeneratedLineNum.
     * @return int
     */
    public int getEndGeneratedLineNum() {
        return endGeneratedLineNum;
    }

    /**
     * Returns the endSourceColNum.
     * @return int
     */
    public int getEndSourceColNum() {
        return endSourceColNum;
    }

    /**
     * Returns the endSourceLineNum.
     * @return int
     */
    public int getEndSourceLineNum() {
        return endSourceLineNum;
    }

    /**
     * Returns the fileName.
     * @return String
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the filePath.
     * @return String
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Returns the sourceLineCount.
     * @return int
     */
    public int getSourceLineCount() {
        return sourceLineCount;
    }

    /**
     * Returns the startGeneratedLineCount.
     * @return int
     */
    public int getStartGeneratedLineCount() {
        return startGeneratedLineCount;
    }

    /**
     * Returns the startGeneratedLineNum.
     * @return int
     */
    public int getStartGeneratedLineNum() {
        return startGeneratedLineNum;
    }

    /**
     * Returns the startSourceColNum.
     * @return int
     */
    public int getStartSourceColNum() {
        return startSourceColNum;
    }

    /**
     * Returns the startSourceLineNum.
     * @return int
     */
    public int getStartSourceLineNum() {
        return startSourceLineNum;
    }
    
    public List getAttrNameList() {
        return attrNameList;
    }

}
