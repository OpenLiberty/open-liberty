/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.annotation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SipXmlUtil {
	/**
	 * Add sip app tag to sip.xml
	 * @param sipXmlDocument
	 * @param applicationName
	 */
	public static void addSipAppToSipXml(Document  sipXmlDocument, String applicationName){
		
  	  Element appNameE = sipXmlDocument.createElement("app-name");
	  appNameE.setTextContent(applicationName);
  	  
  	  Node sipAppE = sipXmlDocument.getElementsByTagName("sip-app").item(0);
  	  
  	  Node pivotNode = getNodeToInsertBefore(sipAppE, appNameE);

  	  sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\t "), pivotNode);
  	  sipAppE.insertBefore(appNameE, pivotNode);
      sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\t "), pivotNode);
	}
	
	/**
	 * Add display name tag to web.xml
	 * @param webXmlDocument
	 * @param applicationName
	 */
	public static void addDisplayNameToWebXml(Document  webXmlDocument, 
			String applicationName){
		
  	  Element appNameE = webXmlDocument.createElement("display-name");
	  appNameE.setTextContent(applicationName);
  	  
  	  Node sipAppE = webXmlDocument.getElementsByTagName("web-app").item(0);
  	  
  	  Node pivotNode = getNodeToInsertBefore(sipAppE, appNameE);

  	  sipAppE.insertBefore(webXmlDocument.createTextNode("\n\t "), pivotNode);
  	  sipAppE.insertBefore(appNameE, pivotNode);
      sipAppE.insertBefore(webXmlDocument.createTextNode("\n\t "), pivotNode);
	}

	/**
	 * Add description tag to sip.xml
	 * @param sipXmlDocument
	 * @param desc
	 */
	public static void addDescriptionToSipXml(Document  sipXmlDocument, 
			String desc){
		
	  if (desc == null || desc.equals("")) return;	
		
  	  Element descE = sipXmlDocument.createElement("javaee:description");
	  descE.setTextContent(desc);
  	  
  	  Node sipAppE = sipXmlDocument.getElementsByTagName("sip-app").item(0);
  	  
  	  Node pivotNode = getNodeToInsertBefore(sipAppE, descE);

  	  sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\t "), pivotNode);
  	  sipAppE.insertBefore(descE, pivotNode);
      sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\t "), pivotNode);
	}
	
	/**
	 * Add display name tag to sip.xml
	 * @param sipXmlDocument
	 * @param displayName
	 */
	public static void addDisplayNameToSipXml(Document  sipXmlDocument, 
			String displayName){
	
	  if (displayName == null || displayName.equals("")) return;
		  
  	  Element descE = sipXmlDocument.createElement("javaee:display-name");
	  descE.setTextContent(displayName);
  	  
  	  Node sipAppE = sipXmlDocument.getElementsByTagName("sip-app").item(0);
  	  
  	  Node pivotNode = getNodeToInsertBefore(sipAppE, descE);

  	  sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\t "), pivotNode);
  	  sipAppE.insertBefore(descE, pivotNode);
      sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\t "), pivotNode);
	}
	
	/**
	 * Add icon tag to sip.xml
	 * @param sipXmlDocument
	 * @param smallIcon
	 * @param largeIcon
	 */
	public static void addIconToSipXml(Document  sipXmlDocument, String smallIcon, 
			String largeIcon){

		// Validate the content is not empty
		if (smallIcon == null && largeIcon == null) return ;
		if (smallIcon != null && smallIcon.equals("") &&
		    largeIcon != null && largeIcon.equals("")) return ;

		Element iconE        = sipXmlDocument.createElement("javaee:icon");
		Element smallIconE = sipXmlDocument.createElement("javaee:small-icon");
		Element largeIconE = sipXmlDocument.createElement("javaee:large-icon");

		if (smallIcon != null && !smallIcon.equals("")) {
			iconE.appendChild(sipXmlDocument.createTextNode("\n "));
			iconE.appendChild(sipXmlDocument.createTextNode("\t\t "));
			smallIconE.setTextContent(smallIcon);
			iconE.appendChild(smallIconE);
			iconE.appendChild(sipXmlDocument.createTextNode("\n"));
		}

		if (largeIcon != null && !largeIcon.equals("")) {
			iconE.appendChild(sipXmlDocument.createTextNode("\n "));
			iconE.appendChild(sipXmlDocument.createTextNode("\t\t "));
			largeIconE.setTextContent(largeIcon);
			iconE.appendChild(largeIconE);
			iconE.appendChild(sipXmlDocument.createTextNode("\n"));
		}
		
		
		Node sipAppE = sipXmlDocument.getElementsByTagName("sip-app").item(0);
		Node pivotNode = getNodeToInsertBefore(sipAppE, iconE);

		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
		sipAppE.insertBefore(iconE, pivotNode);
		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
	}	
	
	/**
	 * Add icon tag to web.xml
	 * @param webXmlDocument
	 * @param smallIcon
	 * @param largeIcon
	 */
	public static void addIconToWebXml(Document  webXmlDocument, String smallIcon, 
			String largeIcon){

		// Validate the content is not empty
		if (smallIcon == null && largeIcon == null) {
			return ;
		}
		
		if (smallIcon != null && smallIcon.equals("") &&
		    largeIcon != null && largeIcon.equals("")) {
			return ;
		}

		Element iconE        = webXmlDocument.createElement("icon");
		Element smallIconE = webXmlDocument.createElement("small-icon");
		Element largeIconE = webXmlDocument.createElement("large-icon");

		if (smallIcon != null && !smallIcon.equals("")) {
			iconE.appendChild(webXmlDocument.createTextNode("\n "));
			iconE.appendChild(webXmlDocument.createTextNode("\t\t "));
			smallIconE.setTextContent(smallIcon);
			iconE.appendChild(smallIconE);
			iconE.appendChild(webXmlDocument.createTextNode("\n"));
		}

		if (largeIcon != null && !largeIcon.equals("")) {
			iconE.appendChild(webXmlDocument.createTextNode("\n "));
			iconE.appendChild(webXmlDocument.createTextNode("\t\t "));
			largeIconE.setTextContent(largeIcon);
			iconE.appendChild(largeIconE);
			iconE.appendChild(webXmlDocument.createTextNode("\n"));
		}
		
		
		Node sipAppE = webXmlDocument.getElementsByTagName("web-app").item(0);
		Node pivotNode = getNodeToInsertBefore(sipAppE, iconE);

		sipAppE.insertBefore(webXmlDocument.createTextNode("\n\n "), pivotNode);
		sipAppE.insertBefore(iconE, pivotNode);
		sipAppE.insertBefore(webXmlDocument.createTextNode("\n\n "), pivotNode);
	}	
	
	/**
	 * Add servlet tag to sip.xml 
	 * @param sipXmlDocument
	 * @param servletName
	 * @param servletClassName
	 * @param loadOnStartUp
	 * @param desc
	 */
	public static void addServletToSipXml(Document  sipXmlDocument, 
		  String servletName, String servletClassName, Integer loadOnStartUp, String desc){
		
		if (servletClassName == null || servletName == null) return;
		
		Element servletE          = sipXmlDocument.createElement("servlet");
		Element servletNameE      = sipXmlDocument.createElement("javaee:servlet-name");
		Element servletClassNameE = sipXmlDocument.createElement("javaee:servlet-class");
		Element loadOnStartupE    = sipXmlDocument.createElement("javaee:load-on-startup");
		Element descE             = sipXmlDocument.createElement("javaee:description");


		servletNameE.setTextContent(servletName);
		servletClassNameE.setTextContent(servletClassName);

		if (desc != null && !desc.equals("")){
			descE.appendChild(sipXmlDocument.createTextNode(desc));
			servletE.appendChild(sipXmlDocument.createTextNode("\n "));
			servletE.appendChild(sipXmlDocument.createTextNode("\t\t "));
			servletE.appendChild(descE);
		}
		
		servletE.appendChild(sipXmlDocument.createTextNode("\n "));
		servletE.appendChild(sipXmlDocument.createTextNode("\t\t "));
		servletE.appendChild(servletNameE);
		servletE.appendChild(sipXmlDocument.createTextNode("\n "));
		servletE.appendChild(sipXmlDocument.createTextNode("\t\t "));
		servletE.appendChild(servletClassNameE);

		if (loadOnStartUp != null && loadOnStartUp >= 0){
			loadOnStartupE.appendChild(sipXmlDocument.createTextNode(loadOnStartUp.toString()));
			servletE.appendChild(sipXmlDocument.createTextNode("\n "));
			servletE.appendChild(sipXmlDocument.createTextNode("\t\t "));
			servletE.appendChild(loadOnStartupE);
		}
		
		servletE.appendChild(sipXmlDocument.createTextNode("\n\t "));
		Node sipAppE = sipXmlDocument.getElementsByTagName("sip-app").item(0);

		Node pivotNode = getNodeToInsertBefore(sipAppE, servletE);

		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
		sipAppE.insertBefore(servletE, pivotNode);
		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
	}
	
	/**
	 * Add servlet tag to web.xml 
	 * @param webXmlDocument
	 * @param servletName
	 * @param servletClassName
	 * @param loadOnStartUp
	 * @param desc
	 */
	public static void addServletToWebXml(Document  webXmlDocument, 
		  String servletName, String servletClassName, Integer loadOnStartUp, String desc){
		
		if (servletClassName == null || servletName == null){
			return;
		}
		
		Element servletE          = webXmlDocument.createElement("servlet");
		Element servletNameE      = webXmlDocument.createElement("servlet-name");
		Element servletClassNameE = webXmlDocument.createElement("servlet-class");
		Element descE             = webXmlDocument.createElement("description");
		
		Element servletMapping          = webXmlDocument.createElement("servlet-mapping");
		Element servletMappingName          = webXmlDocument.createElement("servlet-name");
		Element servletMappingURL          = webXmlDocument.createElement("url-pattern");

		servletNameE.setTextContent(servletName);
		servletClassNameE.setTextContent(servletClassName);

		if (desc != null && !desc.equals("")){
			descE.appendChild(webXmlDocument.createTextNode(desc));
			servletE.appendChild(webXmlDocument.createTextNode("\n "));
			servletE.appendChild(webXmlDocument.createTextNode("\t\t "));
			servletE.appendChild(descE);
		}
		
		servletE.appendChild(webXmlDocument.createTextNode("\n "));
		servletE.appendChild(webXmlDocument.createTextNode("\t\t "));
		servletE.appendChild(servletNameE);
		servletE.appendChild(webXmlDocument.createTextNode("\n "));
		servletE.appendChild(webXmlDocument.createTextNode("\t\t "));
		servletE.appendChild(servletClassNameE);
		
		servletE.appendChild(webXmlDocument.createTextNode("\n\t "));
		Node webAppE = webXmlDocument.getElementsByTagName("web-app").item(0);

		Node pivotNode = getNodeToInsertBefore(webAppE, servletE);

		webAppE.insertBefore(webXmlDocument.createTextNode("\n\n "), pivotNode);
		webAppE.insertBefore(servletE, pivotNode);
		webAppE.insertBefore(webXmlDocument.createTextNode("\n\n "), pivotNode);
		
		//insert the servlet mapping 
		servletMappingName.setTextContent(servletName);
		servletMappingURL.setTextContent("/" + servletName);
		
		servletMapping.appendChild(webXmlDocument.createTextNode("\n "));
		servletMapping.appendChild(webXmlDocument.createTextNode("\t\t "));
		servletMapping.appendChild(servletMappingName);
		servletMapping.appendChild(webXmlDocument.createTextNode("\n "));
		servletMapping.appendChild(webXmlDocument.createTextNode("\t\t "));
		servletMapping.appendChild(servletMappingURL);
		servletMapping.appendChild(webXmlDocument.createTextNode("\n\t "));
		
		//add the mapping component after the last servlet component.
		pivotNode = getLastServletNode(webAppE);
		webAppE.insertBefore(webXmlDocument.createTextNode("\n\n "), pivotNode);
		webAppE.insertBefore(servletMapping, pivotNode);
		webAppE.insertBefore(webXmlDocument.createTextNode("\n\n "), pivotNode);
	}
	
	/**
	 * return the node element just after the last servlet Node from the web xml document,
	 * the servlet mapping elements will be added just after the last servelt element
	 * 
	 * @param webAppE
	 * @return
	 */
	private static Node getLastServletNode(Node webAppE) {
		NodeList list = webAppE.getOwnerDocument().getElementsByTagName("servlet");
		if (list != null){
			return list.item(list.getLength() - 1).getNextSibling();
		}
		return null;
	}

	/**
	 * Add listener to sip.xml
	 * @param sipXmlDocument
	 * @param listenerClassName
	 * @param desc
	 */
	public static void addListenerToSipXml(Document  sipXmlDocument, String listenerClassName, 
			String desc){
		
		  if (listenerClassName == null) return;
		
    	  Element listenerE          = sipXmlDocument.createElement("listener");
      	  Element listenerClassNameE = sipXmlDocument.createElement("javaee:listener-class");
      	  Element descE              = sipXmlDocument.createElement("javaee:description"); 

      	  if (desc != null && !desc.equals("")){
      		  descE.appendChild(sipXmlDocument.createTextNode(desc));
      		  listenerE.appendChild(sipXmlDocument.createTextNode("\n "));
      		  listenerE.appendChild(sipXmlDocument.createTextNode("\t\t "));
      		  listenerE.appendChild(descE);
      	  }

      	  listenerE.appendChild(sipXmlDocument.createTextNode("\n "));
      	  listenerE.appendChild(sipXmlDocument.createTextNode("\t\t "));
      	  listenerClassNameE.setTextContent(listenerClassName);
      	  listenerE.appendChild(listenerClassNameE);
      	  listenerE.appendChild(sipXmlDocument.createTextNode("\n "));
      	  
      	  
      	  Node sipAppE = sipXmlDocument.getElementsByTagName("sip-app").item(0);
      	  
      	  Node pivotNode = getNodeToInsertBefore(sipAppE, listenerE);
      	  
      	  sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
      	  sipAppE.insertBefore(listenerE, pivotNode);
      	  sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
	}	
	
	/**
	 * Add main servlet to sip.xml
	 * @param sipXmlDocument
	 * @param mainServlet
	 */
	public static void addMainServletToSipXml(Document  sipXmlDocument, String mainServlet){
		if (mainServlet == null || mainServlet.equals("")) {
			return;
		}

		Element servletSelE        = sipXmlDocument.createElement("servlet-selection");
		Element mainServletE       = sipXmlDocument.createElement("main-servlet");

		servletSelE.appendChild(sipXmlDocument.createTextNode("\n "));
		servletSelE.appendChild(sipXmlDocument.createTextNode("\t\t "));
		mainServletE.setTextContent(mainServlet);
		servletSelE.appendChild(mainServletE);
		servletSelE.appendChild(sipXmlDocument.createTextNode("\n"));


		Node sipAppE = sipXmlDocument.getElementsByTagName("sip-app").item(0);

		Node pivotNode = getNodeToInsertBefore(sipAppE, servletSelE);

		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
		sipAppE.insertBefore(servletSelE, pivotNode);
		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
	}	

	/**
	 * Add distibutable tag to sip.xml
	 * @param sipXmlDocument
	 */
	public static void addDistributableToSipXml(Document  sipXmlDocument){
		Element distibutableE = sipXmlDocument.createElement("distributable");

		Node sipAppE = sipXmlDocument.getElementsByTagName("sip-app").item(0);

		Node pivotNode = getNodeToInsertBefore(sipAppE, distibutableE);

		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
		sipAppE.insertBefore(distibutableE, pivotNode);
		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
	}	
	
	/**
	 * Add distibutable tag to web.xml
	 * @param webXmlDocument
	 */
	public static void addDistributableToWebXml(Document webXmlDocument){
		Element distibutableE = webXmlDocument.createElement("distributable");

		Node sipAppE = webXmlDocument.getElementsByTagName("web-app").item(0);

		Node pivotNode = getNodeToInsertBefore(sipAppE, distibutableE);

		sipAppE.insertBefore(webXmlDocument.createTextNode("\n\n "), pivotNode);
		sipAppE.insertBefore(distibutableE, pivotNode);
		sipAppE.insertBefore(webXmlDocument.createTextNode("\n\n "), pivotNode);
	}	
	
	/**
	 * Add proxy timeout tag to sip.xml
	 * @param sipXmlDocument
	 * @param proxyTimeout
	 */
	public static void addProxyTimeoutToSipXml(Document  sipXmlDocument, int proxyTimeout){

		Element proxyConfigE        = sipXmlDocument.createElement("proxy-config");
		Element proxyTimeoutE       = sipXmlDocument.createElement("proxy-timeout");

		proxyConfigE.appendChild(sipXmlDocument.createTextNode("\n "));
		proxyConfigE.appendChild(sipXmlDocument.createTextNode("\t\t "));
		proxyTimeoutE.setTextContent("" + proxyTimeout);
		proxyConfigE.appendChild(proxyTimeoutE);
		proxyConfigE.appendChild(sipXmlDocument.createTextNode("\n"));

		Node sipAppE = sipXmlDocument.getElementsByTagName("sip-app").item(0);

		Node pivotNode = getNodeToInsertBefore(sipAppE, proxyConfigE);

		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
		sipAppE.insertBefore(proxyConfigE, pivotNode);
		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
	}	
	

	/**
	 * Add session timeout tag to the sip.xml
	 * @param sipXmlDocument
	 * @param sessionTimeout
	 */
	public static void addSessionTimeoutToSipXml(Document  sipXmlDocument, int sessionTimeout){

		Element sessionConfigE        = sipXmlDocument.createElement("session-config");
		Element sessionTimeoutE       = sipXmlDocument.createElement("javaee:session-timeout");

		sessionConfigE.appendChild(sipXmlDocument.createTextNode("\n "));
		sessionConfigE.appendChild(sipXmlDocument.createTextNode("\t\t "));
		sessionTimeoutE.setTextContent("" + sessionTimeout);
		sessionConfigE.appendChild(sessionTimeoutE);
		sessionConfigE.appendChild(sipXmlDocument.createTextNode("\n"));


		Node sipAppE = sipXmlDocument.getElementsByTagName("sip-app").item(0);

		Node pivotNode = getNodeToInsertBefore(sipAppE, sessionConfigE);

		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
		sipAppE.insertBefore(sessionConfigE, pivotNode);
		sipAppE.insertBefore(sipXmlDocument.createTextNode("\n\n "), pivotNode);
	}	
	
	
	/**
	 * This method is used to keep the right order of elements inside the sip.xml DD
	 * @param rootNode 		- root node of sip.xml
	 * @param nodeToInsert  - node that want to insert
	 * @return              - node that nodeToInsert should be inserted before
	 */
	private static Node getNodeToInsertBefore(Node  rootNode, Node nodeToInsert){
		
		String[] elementNameList = 
			{"app-name",
			 "javaee:description",
			 "javaee:display-name",
			 "display-name",
			 "javaee:icon",
			 "icon",
			 "distributable", 
			 "context-param", 
			 "listener", 
			 "servlet-selection", 
			 "servlet",
			 "proxy-config",
			 "session-config",
			 "security-constraint",
			 "login-config", 
			 "security-role",
			 "env-entry",
			 "ejb-ref",
			 "ejb-local-ref",
			 "resource-ref",
			 "resource-env-ref",
			 "message-destination-ref",
			 "persistence-context-ref",
			 "persistence-unit-ref",
			 "post-construct",
			 "pre-destroy",
			 "message-destination",
			 "locale-encoding-mapping-list"};
		
		String nodeName = nodeToInsert.getNodeName();
		
		boolean started = false;
		for (String elementName : elementNameList ){
			
			if (elementName.equals(nodeName)){
				started = true;
			}
			
			if (started){
				Node pivotNode = rootNode.getOwnerDocument().getElementsByTagName(elementName).item(0);
				if (pivotNode != null) return  pivotNode;
			}
		}
		

		return null;
	}
}