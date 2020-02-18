<%--
    Copyright (c) 2014 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html style="height: 100%; width: 100%; overflow: hidden; position: relative;">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="DC.Rights" content="© Copyright IBM Corp. 2014" />
<meta name="viewport" content="width=device-width,initial-scale=1" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />

<link rel="stylesheet" href="dojo/resources/dojo.css" />
<link rel="stylesheet" href="dijit/themes/dijit.css" />
<link rel="stylesheet" href="idx/themes/oneui/oneui.css" />
<link rel="stylesheet" href="gridx/resources/claro/Gridx.css" />
<link rel="stylesheet" href="dojox/form/resources/CheckedMultiSelect.css" />

<link rel="stylesheet" href="css/explore.css" />
<script src="svg4everybody/svg4everybody.min.js"></script>
<script>svg4everybody(); // run it now or whenever you are ready</script>

<%@ include file="jsShared/bidiConfig.jsp"%>
<script src="dojo/dojo.js" data-dojo-config="<%=dojoConfigString%>"></script>

<script type="text/javascript">
  // On IE and old versions of FF (like 17), location.origin doesn't exist so create it. Consider something like 'Modernizer' instead.
  if (!window.location.origin) {
    window.location.origin, window.top.location.origin = window.location.protocol + "//" + window.location.hostname
        + (window.location.port ? ':' + window.location.port : '');
  }
  require([ "jsExplore/mainDashboard-init", "dojo/i18n!jsExplore/nls/explorerMessages", "js/common/platform", "dojo/domReady!" ], function(
      dashboard, i18n, platform) {
    var breadcrumbImg = 'images/breadcrumb-dashboard-T.png';
    if (platform.isPhone()) {
      breadcrumbImg = "images/breadcrumb-dashboard-S.png";
    }
    document.getElementById("explore_tab_title").innerHTML = i18n.EXPLORE;
    document.documentElement.setAttribute("lang", dojo.locale);
  });
</script>
<script>
  function closeIframe() {
    require([ "dijit/registry", "dojo/domReady!" ], function(registry) {
      var proxyPage = registry.byId("hostProxyPage");
      // If we don't have a hostProxyPage widgit id, it might be that we're deploying the
      // server package from the host page rather than the allHosts Page. So check for the
      // hostHostProxyPage widgit id.
      if (!proxyPage)
        proxyPage = registry.byId("hostHostProxyPage");

      if (proxyPage) {
        registry.byId("breadcrumbContainer-id").removeChild(proxyPage);
        proxyPage.destroy();
      }
      
      registry.byId("breadcrumbContainer-id").set('style', 'overflow: auto;');
    });
  }
</script>


<style type="text/css">
a:link {
    color: #00649D;
    text-decoration: underline;
}

a:visited {
    color: #00649D;
    text-decoration: underline;
}
</style>

<title id="explore_tab_title"></title>
<%@ include file="images/Liberty-Icon-Stack.svg"%>
<%@ include file="imagesShared/Liberty-Common-Icon-Stack.svg"%>
</head>
<body class="oneui" style="height: 100%; width: 100%">
    <noscript>
        <div role="region" aria-label="JavaScript required">
            <h2>Explore requires JavaScript. JavaScript is currently disabled.</h2>
            <h2>Enable JavaScript or use a browser which supports JavaScript.</h2>
        </div>
    </noscript>

    <div id="mainContainer" class="mainContainer" role="main" style="height: 100%; width: 100%">
        <!-- end of header -->
        <div style="width: 100%; height: 100%;" id="exploreRootBorderContainer">
            <div data-dojo-type="dijit.layout.StackContainer" id="breadcrumbStackContainer-id">
                <div data-dojo-type="dijit.layout.ContentPane" id="breadcrumbAndSearchDiv" label="Breadcrumb">
                    <!--  breadcrumb div -->
                    <div data-dojo-type="dijit.layout.ContentPane" id="breadcrumbDiv">
                        <div data-dojo-type="js.layouts.BreadcrumbMRUController" id="breadcrumbController" role="presentation"
                            data-dojo-props='containerId: "breadcrumbContainer-id"'></div>
                    </div>
                    <!--  search button -->
                    <div id="searchDiv" class="searchDiv" region="top">
                        <button id="searchButton" type="button"></button>
                    </div>
                </div>
            </div>
            <!--  50px is the height of the breadcrumb (50) -->
            <div data-dojo-type="dijit.layout.StackContainer" id="breadcrumbContainer-id" doLayout="false" style="overflow: auto;">
                <div data-dojo-type="dijit.layout.ContentPane" aria-label="Dashboard" id="mainDashboard"></div>
            </div>
        </div>
    </div>
</body>
</html>