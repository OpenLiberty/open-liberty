<%--
    Copyright (c) 2017, 2022 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0

    Contributors:
        IBM Corporation - initial API and implementation
 --%>
<%@ page session="false" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html style="height: 100%; width: 100%; overflow: hidden; position: relative;">

<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="DC.Rights" content="© Copyright IBM Corp. 2017" />
<meta name="viewport" content="width=device-width,initial-scale=1" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />

<link rel="stylesheet" href="dojo/resources/dojo.css" />
<link rel="stylesheet" href="dijit/themes/dijit.css" />
<link rel="stylesheet" href="idx/themes/oneui/oneui.css" />
<link rel="stylesheet" href="gridx/resources/claro/Gridx.css" />
<link rel="stylesheet" href="css/javabatch.css" />


<%@ include file="jsShared/bidiConfig.jsp"%>
<script src="jsShared/utils/globalization.js"></script>
<script type="text/javascript">
  var languageLocale = globalization.getLanguageCode();
</script>
<%
    if (hasBidi.length() == 0) {
%>
<script type="text/javascript">
        var dojoConfig = {
            locale: languageLocale
        };
</script>
<%
    } else {
%>
<script type="text/javascript">
        var dojoConfig = {
            locale: languageLocale,
            has: {
                'adminCenter-bidi': true,
                'dojo-bidi': true
            }
        };
</script>
<%
    }
%>
<script src="dojo/dojo.js"></script>
<script>
  require([ "jsBatch/main" ], function(dashboard) {
    document.documentElement.setAttribute("lang", languageLocale);
  });
</script>
<title>Java Batch Tool</title>
<%@ include file="imagesShared/Liberty-Common-Icon-Stack.svg"%>
<%@ include file="images/JavaBatch-Icon-Stack.svg"%>
</head>

<style type="text/css">
	body{
		margin:0px;
	}
</style>

<body class="oneui" style="height: 100%; width: 100%">
    <noscript role="region" aria-label="JavaScript required">
        <div>
            <h2>Java Batch Tool requires JavaScript. JavaScript is currently disabled.</h2>
            <h2>Enable JavaScript or use a browser which supports JavaScript.</h2>
        </div>
    </noscript>
    
    <div id="mainContainer" role="main" style="height: 100%; width: 100%;">
  	  <div style="width: 100%; height: 100%;" id="javaBatchRootBorderContainer">
  	  
 	    <!-- The breadcrumb bar -->
      	<div data-dojo-type="dijit.layout.StackContainer" id="breadcrumbStackContainer-id">
             <div data-dojo-type="dijit.layout.ContentPane" id="breadcrumbAndSearchDiv" label="Breadcrumb">
             
                 <!-- The area where the breadcrumb buttons are attached.  The dashboard button is always first starting from left. -->
                 <div data-dojo-type="dijit.layout.ContentPane" id="breadcrumbDiv">
                 </div>
                 <!-- The breadcrumb search button -->
                 <div id="searchDiv" class="searchDiv" region="top">
                     <button id="searchButton" type="button"></button>
                 </div>
             </div>
        </div>
         
        <!-- The rest of the java batch tool -->
        
        <!-- Any views are attached here -->
  	  	<div data-dojo-type="dijit.layout.StackContainer" id="breadcrumbContainer-id" doLayout="false" style="overflow: auto;">
			<div id="loadingView" data-dojo-type="dijit.layout.ContentPane" style="height: 100%; width: 100%;">
				<div id="loadingViewStandBy" style="height: 100%; width: 100%;"></div>
			</div>
		</div>
        
     </div>
   </div>

</body>


</html>