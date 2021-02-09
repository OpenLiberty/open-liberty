<%@ page language="java" session="true" contentType="text/html;charset=UTF-8"%>
<!--
    Copyright (c) 2013 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<html>
<head>
<title></title>

</head>

<body>

<h2>FileUpload for Servlet 3.0</h2>

Using multipart/form-data annotation in the servlet
<p>
File size need to be less than maxfilesize=50000
<p>

<form action="/TestServlet31/FileUploadGetSubmittedFileName" enctype="multipart/form-data" method="POST" >
 
<!--Hidden Variables-->
<input TYPE="hidden" NAME="ID1" VALUE="1"/>
<input TYPE="hidden" NAME="ID2" VALUE="WIN"/>
<input TYPE="hidden" size="35" name="ID3" value="333333333333333"/>
<input TYPE="hidden" size="55" name="ID4"  value="55555555555555555"/>
<P>UploadFile Name<p> <input TYPE="file" size="55" NAME="files"><BR>
</P>
<input TYPE="SUBMIT" name="SubmitButton" value="Submit">


</form>


</body>
</html>