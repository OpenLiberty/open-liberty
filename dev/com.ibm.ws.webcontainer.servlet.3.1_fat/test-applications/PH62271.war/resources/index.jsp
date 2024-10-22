<%@ page language="java" session="true" contentType="text/html;charset=UTF-8" %>
    <!--
    Copyright (c) 2013 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
    <html>
    <head>
        <title></title>
    </head>
    <body>
        <h2>Testing Part#write Servlet 3.0</h2>
        Using multipart/form-data annotation in the servlet to make sure getPart() works
        <p>File size need to be less than maxfilesize=50000<p>
        <form action="/PH62271/PH62271Servlet" enctype="multipart/form-data" method="POST">
            <P>UploadFile Name<p>
            <input TYPE="file" size="55" NAME="files"><BR></P>
            <input TYPE="text" NAME="location" value=''>
            <input TYPE="SUBMIT" name="SubmitButton" value="Submit">
        </form>
    </body>
    </html>
