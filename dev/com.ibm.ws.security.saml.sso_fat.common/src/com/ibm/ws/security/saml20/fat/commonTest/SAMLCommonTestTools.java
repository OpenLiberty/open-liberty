/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml20.fat.commonTest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SimpleTimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.joda.time.DateTime;
import org.opensaml.xml.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings.ReplaceVars;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings.UpdateTimeVars;

public class SAMLCommonTestTools {

    private final static Class<?> thisClass = SAMLCommonTestTools.class;
    // Used to aid readability in output.txt
    private static int indentFactor = -1;

    private final String REPLACEMENT_INDICATOR = "@@@TEXTREPLACED@@@";
    //    private ArrayList<String> location;
    public static SAMLMessageTools msgUtils = new SAMLMessageTools();

    //	public ArrayList<ArrayList<String>> samlTokenRecurse(NodeList nodeList) throws Exception {
    //		return samlTokenRecurse(nodeList, 0, null, null, null, null) ;
    //	}
    //
    //	public ArrayList<ArrayList<String>> samlTokenRecurseValidate(NodeList nodeList, ArrayList<String> locationList, String compareValue) throws Exception {
    //		return samlTokenRecurse(nodeList, 0, null, locationList, compareValue, null) ;
    //	}
    //
    //	public ArrayList<ArrayList<String>> samlTokenRecurseReplace(NodeList nodeList, ArrayList<String> locationList, String replaceValue) throws Exception {
    //		return samlTokenRecurse(nodeList, 0, null, locationList, null, replaceValue) ;
    //	}
    //
    //	public ArrayList<ArrayList<String>> samlTokenRecurse(NodeList nodeList, int indent, String theValue, ArrayList<String> locationList, String compareValue, String replaceValue) throws Exception {
    //
    //		String thisMethod = "samlTokenRecurse" ;
    //		ArrayList<ArrayList<String>> tokensList = new ArrayList<ArrayList<String>>();
    //		String indentString ;
    //		if (indent == 0 ) {
    //			indentString = "" ;
    //		} else {
    //			indentString = new String(new char[indent]).replace('\0', ' ');
    //		}
    //
    //		//		ArrayList<String> newLoc = new ArrayList<String>();
    //		//        if (( locString != null) && ( !locString.isEmpty())) {
    //		//        newLoc.addAll(locString) ;
    //		//        }
    //
    //		Log.info(thisClass,  thisMethod, "nodeList: " + nodeList) ;
    //
    //		int iLength = nodeList.getLength();
    //		Log.info(thisClass,  thisMethod, "NextNode length: " + iLength) ;
    //		if (iLength >0 ) {
    //			for (int iI = 0; iI < iLength; iI++) {
    //				Log.info(thisClass, thisMethod, "loop counter iI: " + iI) ;
    //				Node theNode = nodeList.item(iI);
    //
    //				if (theNode.getAttributes() == null) {
    //					Log.info(thisClass, thisMethod, "AttributeList is null" );
    //				} else {
    //					Log.info(thisClass,  thisMethod, "attributes length: " + theNode.getAttributes().getLength()) ;
    //					for (int n=0 ; n< theNode.getAttributes().getLength(); n++) {
    //						Log.info(thisClass, thisMethod, "loop counter n: " + n) ;
    //						Log.info(thisClass, thisMethod, indentString + "attr: " + theNode.getAttributes().item(n).toString());
    //					}
    //				}
    //				Log.info(thisClass, thisMethod, indentString + "theNode: " + theNode.toString());
    //				Log.info(thisClass, thisMethod, indentString + "keyName: " + theNode.getNodeName());
    //				Log.info(thisClass, thisMethod, indentString + "keyType: " + theNode.getNodeType());
    //				Log.info(thisClass, thisMethod, indentString + "keyValue: " + theNode.getNodeValue());
    //				Log.info(thisClass, thisMethod, indentString + "keyLocalName: " + theNode.getLocalName());
    //				Log.info(thisClass, thisMethod, indentString + "keyTextContext: " + theNode.getTextContent());
    //				Log.info(thisClass, thisMethod, indentString + "keyNameSpaceURI: " + theNode.getNamespaceURI());
    //				Log.info(thisClass, thisMethod, indentString + "keyPrefix: " + theNode.getPrefix());
    //				Log.info(thisClass, thisMethod, indentString + "keyBaseURI: " + theNode.getBaseURI());
    //				//            Log.info(thisClass, thisMethod, indentString + "key: " + theNode);
    //				NodeList childList = theNode.getChildNodes();
    //				String theNodeName = theNode.getNodeName() ;
    ////				String theNodeName = theNode.getNodeValue() ;
    //				//				String newLocString ;
    //
    //				//				newLoc.add(theNode.getNodeName()) ;
    //				//          if (! locString.isEmpty()) {
    //				//            newLocString = locString + ":" + theNode.getNodeName() ;
    //				//            } else {
    //				//            	newLocString = theNode.getNodeName();
    //				//            }
    //				//tokensList.addAll(samlTokenRecurse(childList, indent + 1, newLocString, theNode.getNodeValue())) ;
    //				// tokensList.addAll(samlTokenRecurse(childList, indent + 1, newLoc, theNode.getNodeValue())) ;
    //				//  ArrayList<ArrayList<STring>> = samlTokenRecurse(childList, indent + 1, newLoc, theNode.getNodeValue())) ;
    //
    //				ArrayList<String> newLocationList = new ArrayList<String>() ;
    //				String currentNode = null ;
    //				if (locationList != null && ! locationList.isEmpty()) {
    //					Log.info(thisClass,  thisMethod, "locationList: " + locationList.toString()) ;
    //					Log.info(thisClass,  thisMethod, "newLocationList: " + newLocationList.toString()) ;
    //					int a = 0 ;
    //					if (!locationList.isEmpty()) {
    //						for (String e : locationList) {
    //							Log.info(thisClass,  thisMethod, "a: " + a) ;
    //							if (a == 0) {
    //								currentNode = e ;
    //							} else {
    //								newLocationList.add(e) ;
    //							}
    //							a++ ;
    //						}
    //					}
    //					Log.info(thisClass,  thisMethod, "newLocationList: " + newLocationList.toString()) ;
    //				}
    //
    //				Boolean skip = false ;
    //				if ((compareValue != null || replaceValue != null)) {
    //					if (currentNode != null) {
    //						if (theNodeName != null ) {
    //							if (! currentNode.equals(theNodeName)) {
    //								skip = true ;
    //							}
    //						}
    //					}
    //				}
    //				if (!skip) {
    //					ArrayList<ArrayList<String>> xxx = samlTokenRecurse(childList, indent + 1, theNode.getNodeValue(), locationList, compareValue, replaceValue) ;
    //					for (ArrayList<String> entry : xxx ) {
    //						ArrayList<String> temp = new ArrayList<String>() ;
    //						temp.add(theNode.getNodeName()) ;
    //						temp.addAll(entry) ;
    //						tokensList.add(temp) ;
    //					}
    //				}
    //
    //				//       tokensList.addAll(samlTokenRecurse(childList, indent + 1, newLoc, theNode.getNodeValue())) ;
    //			}
    //		} else {
    //			//String finalString = locString + ":" + theValue;
    //			//			newLoc.add(theValue);
    //			//			Log.info(thisClass,  thisMethod, "Final data: " + newLoc.toString()) ;
    //			//			//tokensList.add(finalString);
    //			//			tokensList.add(newLoc) ;
    //
    //			ArrayList<String> temp = new ArrayList<String>() ;
    //			temp.add(theValue) ;
    //			tokensList.add(temp) ;
    //		}
    //		return tokensList;
    //
    //	}
    //
    //public ArrayList<String> getNestedElement(WebRequest request, ArrayList<String> requestedElements) throws Exception {
    public String getNestedElement(Node theNode, ArrayList<String> requestedElements, String type) throws Exception {
        indentFactor++;
        String thisMethod = "getNestedElement";
        Log.info(thisClass, thisMethod, "NODE is: " + theNode);
        if (theNode == null) {
            Log.info(thisClass, thisMethod, getIndent() + "theNode is null. Returning null");
            indentFactor--;
            return null;
        }

        //        Log.info(thisClass, thisMethod, getIndent() + "theNode: " + theNode.toString());
        Log.info(thisClass, thisMethod, "requestedElement: " + requestedElements.get(0));
        Log.info(thisClass, thisMethod, "requestedElement as string: " + requestedElements.get(0).toString());
        Log.info(thisClass, thisMethod, "nodeName: " + theNode.getNodeName().toString());

        Log.info(thisClass, thisMethod,
                 getIndent() + "theNode: " + theNode.getNodeName().toString() + "\tNext request: " + requestedElements.get(0).toString() + "\tType: " + type);
        if (theNode.getNodeName().equals("#text")) {
            Log.info(thisClass, thisMethod, getIndent() + "Node's name denotes one containing text; returning the node's value.");
            indentFactor--;
            return theNode.getNodeValue();
        }

        if (requestedElements == null || requestedElements.size() == 0) {
            Log.info(thisClass, thisMethod, getIndent() + "No more elements to look for; returning the node's value");
            indentFactor--;
            return theNode.getNodeValue();
        }
        if (!theNode.getNodeName().equals(requestedElements.get(0)) && !theNode.getNodeName().equals(removePrefix(requestedElements.get(0)))) {
            //if (!theNode.getNodeName().equals(requestedElements.get(0))) {
            Log.info(thisClass, thisMethod, getIndent() + "Node's name does not equal the next requested element; returning null");
            indentFactor--;
            return null;
        }

        Log.info(thisClass, thisMethod, getIndent() + "Found a match!");
        NodeList childNodeList = theNode.getChildNodes();
        int numChildNodes = childNodeList.getLength();
        String nodeListString = "";
        for (int i = 0; i < numChildNodes; i++) {
            nodeListString = nodeListString + childNodeList.item(i).getNodeName() + " ";
        }
        Log.info(thisClass, thisMethod, getIndent() + "Next set of nodes are (" + numChildNodes + " nodes): [" + nodeListString + "]");

        String theValue = null;
        if (numChildNodes > 0) {
            // Remove the current entry from the requestedElements if there are more elements to look for. Otherwise, stop.
            if (requestedElements.size() <= 1) {
                Log.info(thisClass, thisMethod, getIndent() + "No additional elements to look for; returning the node's text content: " + theNode.getTextContent());
                // Log.info(thisClass, thisMethod,  "theNode: " + theNode.toString());
                // Log.info(thisClass, thisMethod,  "keyName: " + theNode.getNodeName());
                // Log.info(thisClass, thisMethod,  "keyType: " + theNode.getNodeType());
                // Log.info(thisClass, thisMethod,  "keyValue: " + theNode.getNodeValue());
                // Log.info(thisClass, thisMethod,  "keyLocalName: " + theNode.getLocalName());
                // Log.info(thisClass, thisMethod,  "keyTextContext: " + theNode.getTextContent());
                // Log.info(thisClass, thisMethod,  "keyNameSpaceURI: " + theNode.getNamespaceURI());
                // Log.info(thisClass, thisMethod,  "keyPrefix: " + theNode.getPrefix());
                // Log.info(thisClass, thisMethod,  "keyBaseURI: " + theNode.getBaseURI());
                indentFactor--;
                return theNode.getTextContent();
            }

            Log.info(thisClass, thisMethod, getIndent() + "Removing " + requestedElements.get(0) + " from the requested elements list");
            ArrayList<String> newRequestedElements = new ArrayList<String>();
            for (int j = 1; j < requestedElements.size(); j++) {
                newRequestedElements.add(requestedElements.get(j));
            }
            for (int i = 0; i < numChildNodes; i++) {
                theValue = getNestedElement(childNodeList.item(i), newRequestedElements, type);
                if (theValue != null) {
                    Log.info(thisClass, thisMethod, getIndent() + "Recursively found a nested element value: " + theValue);
                    indentFactor--;
                    return theValue;
                }
            }
            Log.info(thisClass, thisMethod, getIndent() + "Matching entry has not been found in any subnodes");
            // we haven't found the entry and have gone through all the subnodes -
            if ((type == SAMLConstants.NODE_TYPE) || (type == SAMLConstants.NODE_EXISTS_TYPE)) {
                // chc
                Log.info(thisClass, thisMethod, getIndent() + "Initial number of requested elements: " + requestedElements.size());
                // chc
                Log.info(thisClass, thisMethod, getIndent() + "Element 1 is: " + requestedElements.get(1));
                Node theAttribute = theNode.getAttributes().getNamedItem(requestedElements.get(1));
                if (theAttribute == null) {
                    Log.info(thisClass, thisMethod, getIndent() + "The node's " + requestedElements.get(1) + " attribute was not found; returning null");
                    indentFactor--;
                    return null;
                }
                Log.info(thisClass, thisMethod, requestedElements.get(1) + ": " + theAttribute.getNodeValue());
                indentFactor--;
                return theAttribute.getNodeValue();
            }

            Log.info(thisClass, thisMethod, getIndent() + "Returning the node's value: " + theNode.getNodeValue());
            indentFactor--;
            return theNode.getNodeValue();
        }

        // no child nodes - return the value
        Log.info(thisClass, thisMethod, getIndent() + "theNode type is: " + theNode.getNodeType());

        Log.info(thisClass, thisMethod, getIndent() + "theNode has no child nodes; returning the node's value");
        if ((type == SAMLConstants.NODE_TYPE) || (type == SAMLConstants.NODE_EXISTS_TYPE)) {
            //	chc
            Log.info(thisClass, thisMethod, getIndent() + "Array size is: " + requestedElements.size());
            //	chc
            Log.info(thisClass, thisMethod, getIndent() + "Element 1 is: " + requestedElements.get(1));
            Node theAttribute = theNode.getAttributes().getNamedItem(requestedElements.get(1));
            if (theAttribute == null) {
                indentFactor--;
                return null;
            }
            Log.info(thisClass, thisMethod, getIndent() + "Returning the " + requestedElements.get(1) + " attribute value: " + theAttribute.getNodeValue());
            indentFactor--;
            return theAttribute.getNodeValue();
        }
        if (type == SAMLConstants.ALG_TYPE) {
            String value = null;
            NamedNodeMap nodeMap = theNode.getAttributes();
            if (nodeMap != null) {
                Node node = nodeMap.getNamedItem("Algorithm");
                if (node != null) {
                    value = node.getNodeValue();
                }
            }
            Log.info(thisClass, thisMethod, getIndent() + "Node attribute value is: " + value);
            indentFactor--;
            return value;
        }
        Log.info(thisClass, thisMethod, getIndent() + "Returning the node's value: " + theNode.getNodeValue());
        indentFactor--;
        return theNode.getNodeValue();
    }

    private Object removePrefix(String element) {
        String[] tmp = element.split(":");
        return tmp.length > 1 ? tmp[1] : tmp[0];
    }

    //		public void foo(WebResponse response) throws Exception {
    //
    //			try {
    //				WebForm form2 = response.getForms()[0];
    //				WebRequest request = form2.getRequest();
    //
    //				String samlResponse = extractSAMLReponse(request); // Constants.SAMLResponse
    //				//			Document x = response.getDOM() ;
    //				//			try {
    //				//				NodeList keyInfoList  = x.getChildNodes() ;
    //				//		        int iLength = keyInfoList.getLength();
    //				//		        for (int iI = 0; iI < iLength; iI++) {
    //				//		            Node keyInfoNode = keyInfoList.item(iI);
    //				//		            Log.info(thisClass, "foo", "key: " + keyInfoNode);
    //				//
    //				//		            NodeList child = keyInfoNode.getChildNodes() ;
    //				//			        int iLength2 = keyInfoList.getLength();
    //				//			        for (int iI2 = 0; iI2 < iLength2; iI2++) {
    //				//			            Node keyInfoNode2 = child.item(iI2);
    //				//			            Log.info(thisClass, "foo", "key2: " + keyInfoNode2);
    //				//			        }
    //				//
    //				//		        }
    //				//				Log.info(thisClass,  "foo", "getElement  :" + x.getElementById("IssueInstant") );
    //				//			} catch ( Exception e ) {
    //				//				Log.info(thisClass,  "foo", "failure with doc builder") ;
    //				//				e.printStackTrace() ;
    //				//			}
    //				Log.info(thisClass,  "foo", "encoded:\n" + samlResponse);
    //				Log.info(thisClass,  "foo", "");
    //
    //
    //				byte[] base64DecodedResponse = Base64.decode(samlResponse); // SamlResponse xml content
    //				Log.info(thisClass, "foo", "decoded: " + base64DecodedResponse.toString());
    //
    //				String xml1 = new String(base64DecodedResponse);
    //				Log.info(thisClass, "foo", "xml1: " + xml1);
    //
    //				String xml2 = xml1.replace("fimuser", "Fimuser") ;
    //				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance() ;
    //				DocumentBuilder builder ;
    //				try {
    //					builder = factory.newDocumentBuilder();
    //					Document doc = builder.parse(new InputSource ( new StringReader(xml1))) ;
    //					Log.info(thisClass, "foo", "/en " + doc.getDocumentElement()) ;
    //					NodeList keyInfoList  = doc.getChildNodes() ;
    ////					doc.get
    ////					DOMStringList domParmList = doc.getDomConfig().getParameterNames() ;
    ////					for (int i = 0 ; i<domParmList.getLength() ; i++)
    ////						Log.info(thisClass, "foo", "dom parm: " + domParmList.item(i).toString()) ;
    ////					}
    ////					for (  item : domParmList) {
    ////						Log.info(thisClass,  "foo", "dom parms: " + domParmList[i].toString());
    ////
    ////					}
    //			//		Log.info(thisClass,  "foo", "getting issuer from dom: " + doc.getDomConfig().getParameter("saml:Issuer") );
    //
    //
    //
    //					ArrayList<ArrayList<String>> fooArray = samlTokenRecurse(keyInfoList) ;
    //					StringBuffer sb = new StringBuffer() ;
    //
    //					Log.info(thisClass,  "foo", "-----------------------------------------------------------------------------------");
    ////					traverse( doc.getDocumentElement(), 0, sb ) ;
    //					//NodeList x = doc.getElementsByTagName("samlp:Status") ;
    //					NodeList x = doc.getElementsByTagName("saml:NameID") ;
    ////					NodeList x = doc.getElementsByTagName("saml:Issuer") ;
    //					//int iLength = keyInfoList.getLength();
    //					int iLength = x.getLength();
    //					Log.info(thisClass,  "foo", "Number of nodes: " + iLength) ;
    //					if (iLength >0 ) {
    //						for (int iI = 0; iI < iLength; iI++) {
    //							Log.info(thisClass, "foo", "loop counter iI: " + iI) ;
    ////							Node theNode = keyInfoList.item(iI);
    //							Node theNode = x.item(iI);
    //							Log.info(thisClass,  "foo", "NodeName: " + theNode.getNodeName() );
    //							Log.info(thisClass,  "foo", "ParentNodeName: " + theNode.getParentNode().getNodeName() );
    //							Log.info(thisClass,  "foo", "GrandParentNodeName: " + theNode.getParentNode().getParentNode().getNodeName() );
    ////							traverse(theNode, 0, sb) ;
    //							sb.append("\n");
    //						}
    //					}
    //					Log.info(thisClass,  "foo", "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    //
    //					String theValue = null ;
    //					iLength = keyInfoList.getLength();
    //					ArrayList<String> play = new ArrayList<String>() ;
    ////					play.add("samlp:Response") ;
    ////					play.add("saml:Assertion") ;
    //////					play.add("saml:Issuer") ;
    ////					play.add("saml:Subject") ;
    ////					play.add("saml:NameID") ;
    //					play.add(SAMLConstants.SAML_TOKEN_RESPONSE) ;
    //					play.add(SAMLConstants.SAML_TOKEN_ASSERTION) ;
    //					play.add(SAMLConstants.SAML_TOKEN_SUBJECT) ;
    //					play.add(SAMLConstants.SAML_TOKEN_NAMEID) ;
    //
    //
    //					if (iLength > 0 ){
    //
    //						for (int i = 0 ; i<iLength; i++) {
    //							theValue = getNestedElement(keyInfoList.item(i), play) ;
    //							if (theValue != null) {
    //								break ;
    //							}
    //						}
    //					}
    //					Log.info(thisClass, "foo", "found the value for the request: " + theValue) ;
    //
    //
    //
    //					Log.info(thisClass,  "foo", sb.toString());
    //					Log.info(thisClass,  "foo", "-----------------------------------------------------------------------------------");
    //
    //					Log.info(thisClass, "foo", "" );
    //					Log.info(thisClass, "foo", "" );
    //					for (ArrayList<String> entry : fooArray ) {
    //						Log.info(thisClass, "foo", "Final Array entry: " + entry.toString() );
    //					}
    //
    //					ArrayList<ArrayList<String>> fooArray2 = samlTokenRecurseValidate(keyInfoList, new ArrayList<String>(Arrays.asList("x", "b")), "x") ;
    //					Log.info(thisClass, "foo", "" );
    //					Log.info(thisClass, "foo", "" );
    //					for (ArrayList<String> entry : fooArray2 ) {
    //						Log.info(thisClass, "foo", "Final Array entry: " + entry.toString() );
    //					}
    //
    //					//				Log.info(thisClass, "foo", "nodeName: " + doc.getNodeName() ) ;
    //					//		        int iLength = keyInfoList.getLength();
    //					//		        for (int iI = 0; iI < iLength; iI++) {
    //					//		            Node keyInfoNode = keyInfoList.item(iI);
    //					//		            Log.info(thisClass, "foo", "keyName: " + keyInfoNode.getNodeName());
    //					//		            Log.info(thisClass, "foo", "keyValue: " + keyInfoNode.getNodeValue());
    //					//		            Log.info(thisClass, "foo", "key: " + keyInfoNode);
    //					//		            NodeList childList = keyInfoNode.getChildNodes();
    //					//			        int iLength2 = childList.getLength();
    //					//			        for (int iI2 = 0; iI2 < iLength2; iI2++) {
    //					//			            Node keyInfoNode2 = childList.item(iI2);
    //					//			            Log.info(thisClass, "foo", "childName: " + keyInfoNode2.getNodeName());
    //					//			            Log.info(thisClass, "foo", "childValue: " + keyInfoNode2.getNodeValue());
    //					//			            Log.info(thisClass, "foo", "child: " + keyInfoNode2);
    //					//			            NodeList grandhildList = keyInfoNode2.getChildNodes();
    //					//				        int iLength3 = grandhildList.getLength();
    //					//				        for (int iI3 = 0; iI3 < iLength3; iI3++) {
    //					//				            Node keyInfoNode3 = grandhildList.item(iI3);
    //					//				            Log.info(thisClass, "foo", "grandchildName: " + keyInfoNode3.getNodeName());
    //					//				            Log.info(thisClass, "foo", "grandchildValue: " + keyInfoNode3.getNodeValue());
    //					//				            Log.info(thisClass, "foo", "grandchild: " + keyInfoNode3);
    //					//				        }
    //					//			        }
    //					//		        }
    //					//				Log.info(thisClass,  "foo", "getElement: " + doc.getElementById("IssueInstant") );
    //				} catch ( Exception e ) {
    //					Log.info(thisClass,  "foo", "failure with doc builder") ;
    //					e.printStackTrace() ;
    //					throw e;
    //				}
    //				Log.info(thisClass,  "foo", "getXML: " + new String(base64DecodedResponse));
    //				Log.info(thisClass,  "foo", "" );
    //				// Mess it up here
    //
    //
    //				String newSamlResponse= Base64.encodeBytes(base64DecodedResponse, Base64.DONT_BREAK_LINES);
    //				Log.info(thisClass,  "foo", "new encoded:\n" + newSamlResponse);
    //				Log.info(thisClass,  "foo", "" );
    //
    //				Log.info(thisClass,  "foo", "Old equals new? " + newSamlResponse.equals( samlResponse));
    //
    //				byte[] newBase64DecodedResponse = xml2.getBytes();
    //				String newerSamlResponse = Base64.encodeBytes(newBase64DecodedResponse, Base64.DONT_BREAK_LINES) ;
    //				Log.info(thisClass,  "foo", "newer encoded:\n" + newerSamlResponse);
    //				Log.info(thisClass,  "foo", "" );
    //
    //				Log.info(thisClass,  "foo", "Old equals newer? " + newerSamlResponse.equals(samlResponse));
    //
    //
    //			} catch (Exception e) {
    //				e.printStackTrace();
    //				throw e;
    //			}
    //
    //		}

    public String extractSAMLReponse(com.gargoylesoftware.htmlunit.WebRequest request) throws Exception {

        String samlResponse = null;
        String value = null;
        try {
            List<NameValuePair> parms = request.getRequestParameters();
            for (NameValuePair parm : parms) {
                if (SAMLConstants.SAML_RESPONSE.equals(parm.getName())) {
                    value = parm.getValue();
                }
            }
            if (value != null) {
                samlResponse = value.replace(" ", "");
            } else {
                samlResponse = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return samlResponse;
    }

    public void printSAMLTokenFromResponse(com.gargoylesoftware.htmlunit.WebRequest request) throws Exception {

        String thisMethod = "printSAMLTokenFromResponse";
        String samlResponse = extractSAMLReponse(request);
        Log.info(thisClass, thisMethod, "encoded:\n" + samlResponse);
        //			Log.info(thisClass,  thisMethod, "");

        byte[] base64DecodedResponse = Base64.decode(samlResponse);
        //			Log.info(thisClass, thisMethod, "decoded: " + base64DecodedResponse.toString());

        String xml1 = new String(base64DecodedResponse);

        //	xml1 = "fimuser123456789fimuser1234567fimuser12345fimuser123456789" ;
        Log.info(thisClass, thisMethod, "SAML Token: " + xml1);
    }

    public com.gargoylesoftware.htmlunit.WebRequest getRequestWithSamlToken(Object thePage, SAMLTestSettings settings) throws Exception {

        String thisMethod = "getRequestWithSamlToken";
        msgUtils.printMethodName(thisMethod);

        List<HtmlForm> forms = ((HtmlPage) thePage).getForms();
        if (forms == null || forms.isEmpty()) {
            throw new Exception("Response did not contain any forms but was expected to. Full response was: " + AutomationTools.getResponseText(thePage));
        }
        HtmlForm form = forms.get(0);
        // need to get a request that won't validate the values that we set (as sometimes, we're hacking up the token)
        //        WebRequest request = form.newUnvalidatedRequest();
        com.gargoylesoftware.htmlunit.WebRequest request = form.getWebRequest(null);

        List<NameValuePair> parms = request.getRequestParameters();

        boolean bPrintedYet = false;
        if (settings.getSamlTokenReplaceVars() != null) {
            String samlResponse = replaceValueInSAMLToken(request, settings);
            parms = updateParm(parms, SAMLConstants.SAML_RESPONSE, samlResponse);
            bPrintedYet = true;
        }
        if (settings.getRemoveTagInResponse() != null) {
            String samlResponse = removeTagInSAMLToken(request, settings, settings.getRemoveTagInResponse());
            parms = updateParm(parms, SAMLConstants.SAML_RESPONSE, samlResponse);
            bPrintedYet = true;
        }
        if (settings.getRemoveTagInResponse2() != null) {
            String samlResponse = removeTagInSAMLToken(request, settings, settings.getRemoveTagInResponse2());
            parms = updateParm(parms, SAMLConstants.SAML_RESPONSE, samlResponse);
            bPrintedYet = true;
        }
        if (settings.getRemoveAttribAll() != null) {
            String samlResponse = removeAllAttributeInSAMLToken(request, settings);
            parms = updateParm(parms, SAMLConstants.SAML_RESPONSE, samlResponse);
            bPrintedYet = true;
        }
        if (settings.getSamlTokenUpdateTimeVars() != null) {
            String samlResponse = updateTimesInSAMLToken(request, settings);
            parms = updateParm(parms, SAMLConstants.SAML_RESPONSE, samlResponse);
            bPrintedYet = true;
        }
        //		if (settings.getRSSettings() != null) {
        //			Log.info(thisClass,  thisMethod, "Setting RS Settings parms");
        //			request.setParameter("targetApp", settings.getSpDefaultApp());
        //			request.setParameter("header", settings.getRSSettings().getHeaderName());
        //			request.setParameter("formatType", settings.getRSSettings().getSamlTokenFormat());
        //		}
        if (!bPrintedYet) {
            printSAMLTokenFromResponse(request);
            bPrintedYet = true;
        }
        request.setRequestParameters(parms);
        msgUtils.printRequestParts(request, thisMethod, "Updated Request");
        return request;
    }

    private List<NameValuePair> updateParm(List<NameValuePair> parms, String parmName, String newValue) throws Exception {

        List<NameValuePair> toRemove = new ArrayList<NameValuePair>();
        for (NameValuePair parm : parms) {
            if (parm.getName().equals(parmName)) {
                toRemove.add(parm);
            }
        }
        parms.removeAll(toRemove);
        NameValuePair e = new NameValuePair(parmName, newValue);
        parms.add(e);
        return parms;
    }

    public String removeTagInSAMLToken(com.gargoylesoftware.htmlunit.WebRequest request, SAMLTestSettings settings, String tagToBeRemoved) throws Exception {

        String thisMethod = "removeTagInSAMLToken";
        String samlResponse = extractSAMLReponse(request);
        Log.info(thisClass, thisMethod, "encoded:\n" + samlResponse);
        //          Log.info(thisClass,  thisMethod, "");
        Log.info(thisClass, thisMethod, "Tag to be removed: " + tagToBeRemoved);

        byte[] base64DecodedResponse = Base64.decode(samlResponse);
        //          Log.info(thisClass, thisMethod, "decoded: " + base64DecodedResponse.toString());

        String xml1 = new String(base64DecodedResponse);

        // remove all instances
        while (xml1.contains("<" + tagToBeRemoved + " ") || xml1.contains("<" + tagToBeRemoved + ">")) {
            xml1 = removeTagInSAMLTokenWorker(xml1, settings, tagToBeRemoved);
        }

        byte[] newBase64DecodedResponse = xml1.getBytes();
        String updatedResponse = Base64.encodeBytes(newBase64DecodedResponse, Base64.DONT_BREAK_LINES);
        Log.info(thisClass, thisMethod, "newer encoded:\n" + updatedResponse);
        //          Log.info(thisClass,  thisMethod, "" );

        Log.info(thisClass, thisMethod, "Old equals updated? " + updatedResponse.equals(samlResponse));

        return updatedResponse;

    }

    /**
     * remove the specific tag from saml response, such as: ds:Signature
     *
     * @param request
     *                     - the WebRequest to be updated
     * @param settings
     *                     - the SAMLTestSettings containing the list of things to update
     * @return - the updated "request"
     * @throws Exception
     */
    public static String removeTagInSAMLTokenWorker(String xml1, SAMLTestSettings settings, String tagToBeRemoved) throws Exception {
        try {
            String thisMethod = "removeTagInSAMLTokenWorker";

            recordMsg(thisMethod, "xml Before: " + xml1);
            int indexBegin = -1;
            int indexEnd = -1;
            String beginOfTag = "<" + tagToBeRemoved + " "; // such as: "<ds:Signature "
            String endOfTag = "</" + tagToBeRemoved + ">";

            indexBegin = xml1.indexOf(beginOfTag);
            if (indexBegin < 0) { //
                beginOfTag = "<" + tagToBeRemoved + ">"; // simple tag such as: "<ds:Signature>"
                indexBegin = xml1.indexOf(beginOfTag); // has to be found
            }
            if (indexBegin >= 0) {
                //                Log.info(thisClass,  thisMethod,  "found start of the tagged feild");
                indexEnd = xml1.indexOf(endOfTag, indexBegin); // such as: "</ds:Signature>
                if (indexEnd < 0) {// simple end such as: "/>"
                    endOfTag = "/>";
                    indexEnd = xml1.indexOf(endOfTag, indexBegin);
                }
            }
            if (indexBegin >= 0 && indexEnd >= 0) {
                //                Log.info(thisClass,  thisMethod,  "found end of the tagged feild");
                int indexEndNextSpot = indexEnd + endOfTag.length();
                recordMsg(thisMethod, "Removing: " + xml1.substring(indexBegin, indexEndNextSpot));
                // found the whole tag in the xml1
                String preStr = xml1.substring(0, indexBegin);
                String postStr = xml1.substring(indexEndNextSpot);
                xml1 = preStr + postStr;
            } else {
                // TODO Error handling
                recordMsg(thisMethod, "error can not find the whole tag of '" + tagToBeRemoved + "'");
            }
            recordMsg(thisMethod, "xml After : " + xml1);

            return xml1;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static void recordMsg(String theMethod, String msg) throws Exception {

        Boolean calledFromApp = false;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement anElement : stackTrace) {
            if ("com.ibm.ws.jaxrs.fat.jaxrsclient.JaxRSClient".equals(anElement.getClassName())) {
                calledFromApp = true;
                break;
            }
        }
        if (calledFromApp) {
            System.out.println(msg);
        } else { // called from test case
            Log.info(thisClass, theMethod, msg);
        }
        //            System.out.println("In " + thisMethod + " the caller file is: " + f.getFileName());
        //            System.out.println("In " + thisMethod + " the caller Class is: " + f.getClassName());
        //            System.out.println("In " + thisMethod + " the caller method is: " + f.getMethodName());

    }

    /**
     * remove the specific attribute from saml response everywhere, such as: inResponse
     *
     * @param request
     *                     - the WebRequest to be updated
     * @param settings
     *                     - the SAMLTestSettings containing the list of things to update
     * @return - the updated "request"
     * @throws Exception
     */
    public String removeAllAttributeInSAMLToken(com.gargoylesoftware.htmlunit.WebRequest request, SAMLTestSettings settings) throws Exception {
        try {
            String thisMethod = "removeAllAttributeFromSAMLToken";
            String samlResponse = extractSAMLReponse(request);
            Log.info(thisClass, thisMethod, "encoded:\n" + samlResponse);
            //			Log.info(thisClass,  thisMethod, "");

            byte[] base64DecodedResponse = Base64.decode(samlResponse);
            //			Log.info(thisClass, thisMethod, "decoded: " + base64DecodedResponse.toString());

            String removeAttribAll = settings.getRemoveAttribAll();
            String xml1 = new String(base64DecodedResponse);
            Log.info(thisClass, thisMethod, "xml before: " + xml1);

            String xml2 = xml1;
            while (true) {
                xml1 = xml2;
                // in the end, it has to return the original string instance
                xml2 = removeAttribute(xml1, removeAttribAll);
                if (xml1 == xml2)
                    break;
            }

            Log.info(thisClass, thisMethod, "xml After : " + xml1);
            byte[] newBase64DecodedResponse = xml1.getBytes();
            String updatedResponse = Base64.encodeBytes(newBase64DecodedResponse, Base64.DONT_BREAK_LINES);
            Log.info(thisClass, thisMethod, "newer encoded:\n" + updatedResponse);
            //			Log.info(thisClass,  thisMethod, "" );

            Log.info(thisClass, thisMethod, "Old equals updated? " + updatedResponse.equals(samlResponse));

            return updatedResponse;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    static // in the end, it has to return the original string instance
    String removeAttribute(String xml1, String removeAttribAll) {
        int indexBegin = -1;
        int indexEnd = -1;
        String beginOfTag = " " + removeAttribAll + "="; // such as: " inResponse="
        indexBegin = xml1.indexOf(beginOfTag);
        if (indexBegin < 0) { //
            return xml1;
        }
        int index1 = indexBegin + beginOfTag.length();
        int index2 = index1 + 1;
        String quote = xml1.substring(index1, index2);
        indexEnd = xml1.indexOf(quote, index2);
        if (indexBegin >= 0 && indexEnd >= 0) {
            // found the whole tag in the xml1
            int indexEndNextSpot = indexEnd + quote.length();
            String preStr = xml1.substring(0, indexBegin);
            String postStr = xml1.substring(indexEndNextSpot);
            xml1 = preStr + postStr;
        }
        return xml1;
    }

    /**
     * update the value(s) in the SAMLToken
     * Specify he before/after strings (what to replace) and also the location
     * Valid locations are "all", "first", "last", "<num>"
     * There are constants for the first 3, SAMLConstants.LOCATION_ALL, SAMLConstants.LOCATION_FIRST,
     * SAMLConstants.LOCATION_LAST. Use "2" or "3", ... for other locations.
     * If you want to update first and last, or 2 and 3 only, you can add multiple entries to the
     * samlTokenReplaceVars list in SAMLTestSettings (by invoking setSamlTokenReplaceVars for
     * each location that you want updated. NOTE:::: special care should be taken when updating
     * the same string in multiple locations. Add the last instance that you want updated first
     * when using setSamlTkenReplaceVars - an example would explain best: Let's say the SAML token string is:
     * "fimuser123456789fimuser1234567fimuser12345fimuser123456789"
     * you use:
     * setSamlTokenReplaceVars("fimuser", "Fimuser", "2")
     * setSamlTokenReplaceVars("fimuser", "Fimuser", "3")
     * The result will be:
     * "fimuser123456789Fimuser1234567fimuser12345Fimuser123456789"
     * (once the second occurance is updated, the new thrid occurance
     * of fimuser is actually the original strings forth occurance
     * setSamlTokenReplaceVars("fimuser", "Fimuser", "3")
     * setSamlTokenReplaceVars("fimuser", "Fimuser", "2")
     * The result will be:
     * "fimuser123456789Fimuser1234567Fimuser12345fimuser123456789"
     * The logic needed to make this work correctly (including handling strings of variable
     * lenght is just not warrented at this time (I'm not sure how much this routine will be
     * used) That decision can be revisited if needed.
     *
     * @param request
     *                     - the WebRequest to be updated
     * @param settings
     *                     - the SAMLTestSettings containing the list of things to update
     * @return - the updated "request"
     * @throws Exception
     */
    public String replaceValueInSAMLToken(com.gargoylesoftware.htmlunit.WebRequest request, SAMLTestSettings settings) throws Exception {

        String thisMethod = "replaceValueInSAMLToken";
        try {

            String xml1 = convertSamlResponseToXML(request);

            //			String samlResponse = extractSAMLReponse(request);
            //			Log.info(thisClass,  thisMethod, "encoded:\n" + samlResponse);
            //			//			Log.info(thisClass,  thisMethod, "");
            //
            //			byte[] base64DecodedResponse = Base64.decode(samlResponse);
            //			//			Log.info(thisClass, thisMethod, "decoded: " + base64DecodedResponse.toString());
            //
            //			String xml1 = new String(base64DecodedResponse);

            //			xml1 = "fimuser123456789fimuser1234567fimuser12345fimuser123456789" ;
            Log.info(thisClass, thisMethod, "xml Before: " + xml1);
            for (ReplaceVars entry : settings.getSamlTokenReplaceVars()) {
                xml1 = replaceValueInSAMLTokenWorker(xml1, entry, settings);
            }

            String updatedResponse = convertXmlToSamlResponse(xml1);

            //		Log.info(thisClass,  thisMethod, "Old equals updated? " + updatedResponse.equals(samlResponse));

            return updatedResponse;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public String replaceValueInSAMLTokenWorker(String xml1, ReplaceVars entry, SAMLTestSettings settings) throws Exception {

        String thisMethod = "replaceValueInSAMLTokenWorker";

        try {
            String loc = entry.getLocation();

            if (loc == null) { // replace all
                xml1 = xml1.replaceAll(entry.getOld(), entry.getNew());
            } else {
                if (loc.equals(SAMLConstants.LOCATION_ALL)) {
                    if (entry.getOld().equals("*")) {
                        xml1 = hackedSAMLToken(settings, entry.getNew());
                    } else {
                        xml1 = xml1.replaceAll(entry.getOld(), entry.getNew());
                    }
                } else {
                    if (loc.equals(SAMLConstants.LOCATION_FIRST)) {
                        xml1 = xml1.replaceFirst(entry.getOld(), entry.getNew());
                    } else {
                        //specific location
                        int count = (xml1.length() - xml1.replace(entry.getOld(), "").length()) / entry.getOld().length();
                        int locNum = 0;
                        if (loc.equals(SAMLConstants.LOCATION_LAST)) {
                            locNum = count;
                        } else {
                            locNum = Integer.parseInt(loc);
                        }
                        Log.info(thisClass, thisMethod, "loc: " + loc + " locNum: " + locNum);

                        Log.info(thisClass, thisMethod, "count: " + count);
                        Log.info(thisClass, thisMethod, "length of xml1: " + xml1.length());
                        if (count >= locNum) {
                            int curIndex = 0;
                            count = 0;
                            while (curIndex < xml1.length()) {
                                int newIndex = 0;
                                if (count == 0 && curIndex == 0) {
                                    newIndex = xml1.indexOf(entry.getOld(), curIndex);
                                } else {
                                    newIndex = xml1.indexOf(entry.getOld(), curIndex + 1);
                                }
                                //									Log.info(thisClass, thisMethod, "newIndex: " + newIndex );
                                if (newIndex == -1) {
                                    Log.info(thisClass, thisMethod,
                                             "Can not update requested instance in the SAML token as there are too few occurances.  Requested: " + loc + " " + count + " found");
                                    throw new Exception("(Test Code) SAML Token update failure");
                                }
                                count++;
                                //									Log.info(thisClass, thisMethod, "new count: " + count );
                                curIndex = newIndex;
                                //									Log.info(thisClass, thisMethod, "curIndex: " + curIndex );
                                if (count == locNum) {
                                    String part1 = xml1.substring(0, curIndex);
                                    String part2 = xml1.substring(curIndex, xml1.length()).replaceFirst(entry.getOld(), entry.getNew());
                                    //										Log.info(thisClass, thisMethod, "part1: " + part1 );
                                    //										Log.info(thisClass, thisMethod, "part2: " + part2 );

                                    xml1 = part1 + part2;
                                    Log.info(thisClass, thisMethod, "Found instance");
                                    break;
                                }

                            }
                        } else {
                            Log.info(thisClass, thisMethod,
                                     "Can not update requested instance in the SAML token as there are too few occurances.  Requested: " + loc + " " + count + " found");
                            throw new Exception("(Test Code) SAML Token update failure");
                        }
                        //							}
                    }
                }
            }

            return xml1;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public String hackedSAMLToken(SAMLTestSettings settings, String newValue) throws Exception {

        if (newValue.equals(SAMLConstants.HARDCODED_TOKEN_1)) {
            String hackedToken = "<samlp:Response xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" Destination=\"https://localhost:8020/ibm/saml20/sp1/acs\" ID=\"FIMRSP_3b9e8507-014b-18bd-9967-d3f12b273b2b\" IssueInstant=\"2015-01-30T16:13:39Z\" Version=\"2.0\"><saml:Issuer Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:entity\">https://wlp-tfimidp1.austin.ibm.com:9443/sps/WlpTfimIdp1/saml20</saml:Issuer><samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"></samlp:StatusCode></samlp:Status><saml:Assertion ID=\"Assertion-uuid3b9e84fd-014b-1301-a665-d3f12b273b2b\" IssueInstant=\"2015-01-30T16:13:39Z\" Version=\"2.0\"><saml:Issuer Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:entity\">https://wlp-tfimidp1.austin.ibm.com:9443/sps/WlpTfimIdp1/saml20</saml:Issuer><saml:Subject><saml:NameID Format=\"urn:ibm:names:ITFIM:5.1:accessmanager\">fimuser</saml:NameID><saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml:SubjectConfirmationData NotOnOrAfter=\"2030-01-30T16:14:39Z\" Recipient=\"https://localhost:8020/ibm/saml20/sp1/acs\"></saml:SubjectConfirmationData></saml:SubjectConfirmation></saml:Subject><saml:Conditions NotBefore=\"2015-01-30T16:12:39Z\" NotOnOrAfter=\"2030-01-30T16:14:39Z\"><saml:AudienceRestriction><saml:Audience>https://localhost:8020/ibm/saml20/sp1/acs</saml:Audience></saml:AudienceRestriction></saml:Conditions><saml:AuthnStatement AuthnInstant=\"2015-01-30T16:13:39Z\" SessionIndex=\"uuid3b9e7ed1-014b-1c53-a789-d3f12b273b2b\" SessionNotOnOrAfter=\"2015-01-30T17:13:39Z\"><saml:AuthnContext><saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml:AuthnContextClassRef></saml:AuthnContext></saml:AuthnStatement><saml:AttributeStatement><saml:Attribute Name=\"process.serverName\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">wlp-tfimidp1Node01Cell:wlp-tfimidp1Node01:server1</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.longSecurityName\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">139</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.primaryGroupId\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">group:customRealm/users</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"AZN_CRED_AUTH_METHOD\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">itfim</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"AZN_CRED_AUTHNMECH_INFO\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">local</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"AZN_CRED_VERSION\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">0x00000600</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.securityName\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">fimuser</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.oid\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">oid:1.3.18.0.2.30.2</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"AZN_CRED_PRINCIPAL_UUID\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">3b9e84f6-014b-172f-bab4-d3f12b273b2b</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"u\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">user:customRealm/139</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.expiration\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">1422641640000</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"security.authMechOID\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">oid:1.3.18.0.2.30.2</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.forwardable\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">true</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"host\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">wlp-tfimidp1.austin.ibm.com</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"java.naming.provider.url\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">corbaloc:iiop:wlp-tfimidp1.austin.ibm.com:2809/WsnAdminNameService</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"expire\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">1422641640000</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"port\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">8880</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.uniqueId\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">user:customRealm/139</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"AZN_CRED_PRINCIPAL_NAME\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">fimuser</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.realm\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">customRealm</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"type\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">SOAP</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.groups\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">group:customRealm/users</saml:AttributeValue></saml:Attribute></saml:AttributeStatement></saml:Assertion></samlp:Response>";
            return hackedToken;
        } else {
            if (newValue.equals(SAMLConstants.HARDCODED_TOKEN_MISSING_NAMEID)) {
                String hackedToken = "<samlp:Response xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" Destination=\"https://localhost:8020/ibm/saml20/sp1/acs\" ID=\"FIMRSP_3b9e8507-014b-18bd-9967-d3f12b273b2b\" IssueInstant=\"2015-01-30T16:13:39Z\" Version=\"2.0\"><saml:Issuer Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:entity\">https://wlp-tfimidp1.austin.ibm.com:9443/sps/WlpTfimIdp1/saml20</saml:Issuer><samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"></samlp:StatusCode></samlp:Status><saml:Assertion ID=\"Assertion-uuid3b9e84fd-014b-1301-a665-d3f12b273b2b\" IssueInstant=\"2015-01-30T16:13:39Z\" Version=\"2.0\"><saml:Issuer Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:entity\">https://wlp-tfimidp1.austin.ibm.com:9443/sps/WlpTfimIdp1/saml20</saml:Issuer><saml:Subject><saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml:SubjectConfirmationData NotOnOrAfter=\"2030-01-30T16:14:39Z\" Recipient=\"https://localhost:8020/ibm/saml20/sp1/acs\"></saml:SubjectConfirmationData></saml:SubjectConfirmation></saml:Subject><saml:Conditions NotBefore=\"2015-01-30T16:12:39Z\" NotOnOrAfter=\"2030-01-30T16:14:39Z\"><saml:AudienceRestriction><saml:Audience>https://localhost:8020/ibm/saml20/sp1/acs</saml:Audience></saml:AudienceRestriction></saml:Conditions><saml:AuthnStatement AuthnInstant=\"2015-01-30T16:13:39Z\" SessionIndex=\"uuid3b9e7ed1-014b-1c53-a789-d3f12b273b2b\" SessionNotOnOrAfter=\"2015-01-30T17:13:39Z\"><saml:AuthnContext><saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml:AuthnContextClassRef></saml:AuthnContext></saml:AuthnStatement><saml:AttributeStatement><saml:Attribute Name=\"process.serverName\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">wlp-tfimidp1Node01Cell:wlp-tfimidp1Node01:server1</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.longSecurityName\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">139</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.primaryGroupId\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">group:customRealm/users</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"AZN_CRED_AUTH_METHOD\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">itfim</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"AZN_CRED_AUTHNMECH_INFO\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">local</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"AZN_CRED_VERSION\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">0x00000600</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.securityName\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">fimuser</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.oid\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">oid:1.3.18.0.2.30.2</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"AZN_CRED_PRINCIPAL_UUID\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">3b9e84f6-014b-172f-bab4-d3f12b273b2b</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"u\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">user:customRealm/139</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.expiration\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">1422641640000</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"security.authMechOID\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">oid:1.3.18.0.2.30.2</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.forwardable\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">true</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"host\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">wlp-tfimidp1.austin.ibm.com</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"java.naming.provider.url\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">corbaloc:iiop:wlp-tfimidp1.austin.ibm.com:2809/WsnAdminNameService</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"expire\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">1422641640000</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"port\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">8880</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.uniqueId\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">user:customRealm/139</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"AZN_CRED_PRINCIPAL_NAME\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">fimuser</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.realm\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">customRealm</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"type\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">SOAP</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"com.ibm.wsspi.security.cred.groups\" NameFormat=\"urn:ibm:names:ITFIM:5.1:accessmanager\"><saml:AttributeValue xsi:type=\"xs:string\">group:customRealm/users</saml:AttributeValue></saml:Attribute></saml:AttributeStatement></saml:Assertion></samlp:Response>";
                return hackedToken;
            } else {
                return newValue;
            }
        }

    }

    public String updateTimesInSAMLToken(com.gargoylesoftware.htmlunit.WebRequest request, SAMLTestSettings settings) throws Exception {

        String thisMethod = "updateTimesInSAMLToken";

        String xml1 = convertSamlResponseToXML(request);

        for (UpdateTimeVars timeVar : settings.getSamlTokenUpdateTimeVars()) {
            xml1 = updateTimeInSAMLToken(xml1, timeVar, settings);
            Log.info(thisClass, thisMethod, "updated xml: " + xml1);
        }

        return convertXmlToSamlResponse(xml1);
    }

    public String updateTimeInSAMLToken(String xml1, UpdateTimeVars timeVar, SAMLTestSettings settings) throws Exception {

        String thisMethod = "updateTimeInSAMLToken";
        SimpleDateFormat formatter;
        SimpleTimeZone _gmtTimeZoneInternal = new SimpleTimeZone(0, "GMT");
        if (isIDPTFIM(settings.getIdpRoot())) {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        } else {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        }
        formatter.setTimeZone(_gmtTimeZoneInternal);

        // IssueInstant="2015-06-13T03:32:16Z"
        String attribute = SAMLConstants.SAML_ISSUE_INSTANT;
        if (!isIDPADFS(settings.getIdpRoot())) {
            attribute = " " + timeVar.getAttribute();
        }

        // There could be several instances of the specified attribute, be sure to modify the values for each
        Map<String, DateTime> dateUpdates = populateAttributeOccurrences(xml1, attribute, timeVar, formatter);
        if (dateUpdates == null) {
            Log.info(thisClass, thisMethod, "Did not find attribute \"" + attribute + "\" in provided string; returning as-is");
            return xml1;
        }

        Map<String, String> dateReplaceStrings = populateReplacementStrings(dateUpdates, timeVar, formatter);

        for (String valueFromRequest : dateReplaceStrings.keySet()) {
            String newTime = dateReplaceStrings.get(valueFromRequest);
            Log.info(thisClass, thisMethod, "Replacing: " + attribute + "=\"" + valueFromRequest + "\"" + " with: " + attribute + "=\"" + newTime + "\"");
            //            xml1 = xml1.replaceAll(valueFromRequest, newTime);
            xml1 = xml1.replaceAll(attribute + "=\"" + valueFromRequest + "\"", attribute + "=\"" + newTime + "\"");
        }

        // Clean up all splicing indicators
        xml1 = xml1.replaceAll(REPLACEMENT_INDICATOR, "");

        return xml1;

    }

    private Map<String, DateTime> populateAttributeOccurrences(String text, String attribute, UpdateTimeVars timeVar, SimpleDateFormat formatter) throws ParseException {
        String thisMethod = "populateAttributeOccurrences";

        Log.info(thisClass, thisMethod, "Updating value of attribute: " + attribute);
        int indexOfAttribute = text.indexOf(attribute);
        if (indexOfAttribute < 0) {
            Log.info(thisClass, thisMethod, "Did not find attribute " + attribute + " in the text");
            return null;
        }

        // There could be several instances of the specified attribute - make we sure we record the values for all of them
        Map<String, DateTime> dateUpdates = new HashMap<String, DateTime>();

        while (indexOfAttribute >= 0) {
            int indexOfOpenQuote = text.indexOf("\"", indexOfAttribute);
            int indexOfCloseQuote = text.indexOf("\"", indexOfOpenQuote + 1);
            String valueFromRequest = text.substring(indexOfOpenQuote + 1, indexOfCloseQuote);
            Log.info(thisClass, thisMethod, "Time valueFromRequest: " + valueFromRequest);

            DateTime dateTime;
            if (timeVar != null && !timeVar.getUseCurrentTIme()) {
                // Example value: 2015-06-15T22:10:45Z

                Date date = formatter.parse(valueFromRequest);
                System.out.println(date);
                System.out.println(formatter.format(date));

                dateTime = new DateTime(date);

            } else {
                dateTime = new DateTime();
            }

            dateUpdates.put(valueFromRequest, dateTime);

            // Check for additional occurrences of the specified attribute. If already at the end of the string, set index to -1
            indexOfAttribute = (indexOfCloseQuote >= text.length()) ? -1 : text.indexOf(attribute, indexOfCloseQuote);
        }

        return dateUpdates;
    }

    private Map<String, String> populateReplacementStrings(Map<String, DateTime> dateUpdates, UpdateTimeVars timeVar, SimpleDateFormat formatter) {
        String thisMethod = "populateReplacementStrings";

        Map<String, String> map = new HashMap<String, String>();

        for (String valueFromRequest : dateUpdates.keySet()) {
            DateTime dateTime = dateUpdates.get(valueFromRequest);
            DateTime newDateTime;

            int[] time = timeVar.getTime();
            if (timeVar.getAddTime()) {
                Log.info(thisClass, thisMethod, "Adding time " + time[0] + " " + time[1] + " " + time[2] + " " + time[3]);
                newDateTime = dateTime.plusDays(time[0]).plusHours(time[1]).plusMinutes(time[2]).plusSeconds(time[3]);
            } else {
                Log.info(thisClass, thisMethod, "Subtracting time " + time[0] + " " + time[1] + " " + time[2] + " " + time[3]);
                newDateTime = dateTime.minusDays(time[0]).minusHours(time[1]).minusMinutes(time[2]).minusSeconds(time[3]);
            }
            Log.info(thisClass, thisMethod, "New time: " + newDateTime);

            String newTime = formatter.format(newDateTime.toDate());

            // Splice in an indicator so that strings that have already been replaced don't inadvertently get updated again later.
            // e.g. I want to replace "my_text" with "new_text" but also want to replace "new_text" with "some_other_test". This makes
            // sure the original "my_text" instances being replaced with "new_text" don't subsequently get replaced with "some_other_test".
            String splicedTime = newTime.substring(0, newTime.length() / 2) + REPLACEMENT_INDICATOR + newTime.substring(newTime.length() / 2);
            Log.info(thisClass, thisMethod, "Spliced time: " + splicedTime);

            map.put(valueFromRequest, splicedTime);
        }
        return map;
    }

    //
    //
    //
    //
    //
    //    /**
    //     * Gets the SSO token from the subject.
    //     *
    //     * @param subject {@code null} is not supported.
    //     * @return
    //     */
    //    private Saml20Token getSSOToken(Subject subject) {
    //        Saml20Token ssoToken = null;
    //        Set<Saml20Token> ssoTokens = subject.getPrivateCredentials(Saml20Token.class);
    //        Iterator<Saml20Token> ssoTokensIterator = ssoTokens.iterator();
    //        if (ssoTokensIterator.hasNext()) {
    //            ssoToken = ssoTokensIterator.next();
    //        }
    //        return ssoToken;
    //    }
    //
    //    /**
    //     * Print the various programmatic API values we care about.
    //     *
    //     * @param req
    //     * @param writer
    //     */
    //    protected void printProgrammaticApiValues(WebRequest req,
    ////    	    protected void printProgrammaticApiValues(HttpServletRequest req,
    //                                              StringBuffer sb) {
    //
    ////    	HttpServletRequest req = (HttpServletRequest)reqw ;
    ////        writeLine(sb, "getAuthType: " + req.getAuthType());
    ////        writeLine(sb, "getRemoteUser: " + req.getRemoteUser());
    ////        writeLine(sb, "getUserPrincipal: " + req.getUserPrincipal());
    ////
    ////        if (req.getUserPrincipal() != null) {
    ////            writeLine(sb, "getUserPrincipal().getName(): "
    ////                          + req.getUserPrincipal().getName());
    ////        }
    ////        writeLine(sb, "isUserInRole(Employee): "
    ////                      + req.isUserInRole("Employee"));
    ////        writeLine(sb, "isUserInRole(Manager): " + req.isUserInRole("Manager"));
    ////        String role = req.getParameter("role");
    ////        if (role == null) {
    ////            writeLine(sb, "You can customize the isUserInRole call with the follow paramter: ?role=name");
    ////        }
    ////        writeLine(sb, "isUserInRole(" + role + "): " + req.isUserInRole(role));
    ////
    ////        Cookie[] cookies = req.getCookies();
    ////        writeLine(sb, "Getting cookies");
    ////        if (cookies != null && cookies.length > 0) {
    ////            for (int i = 0; i < cookies.length; i++) {
    ////                writeLine(sb, "cookie: " + cookies[i].getName() + " value: "
    ////                              + cookies[i].getValue());
    ////            }
    ////        }
    ////        writeLine(sb, "getRequestURL: " + req.getRequestURL().toString());
    //
    //
    //        try {
    //            // Get the CallerSubject
    //            Subject callerSubject = WSSubject.getCallerSubject();
    //            writeLine(sb, "callerSubject: " + callerSubject);
    //
    //            // Get the public credential from the CallerSubject
    //            if (callerSubject != null) {
    //                WSCredential callerCredential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
    //                if (callerCredential != null) {
    //                    writeLine(sb, "callerCredential: " + callerCredential);
    //                } else {
    //                    writeLine(sb, "callerCredential: null");
    //                }
    //            } else {
    //                writeLine(sb, "callerCredential: null");
    //            }
    //
    //            // getInvocationSubject for RunAs tests
    //            Subject runAsSubject = WSSubject.getRunAsSubject();
    //            writeLine(sb, "RunAs subject: " + runAsSubject);
    //
    //            // Check for cache key for hashtable login test. Will return null otherwise
    //            String customCacheKey = null;
    //            if (callerSubject != null) {
    //                //String[] properties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };
    //                //SubjectHelper subjectHelper = new SubjectHelper();
    //                //Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(callerSubject, properties);
    //                //if (customProperties != null) {
    //                //    customCacheKey = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
    //                //}
    //                if (customCacheKey == null) {
    //                    Saml20Token ssoToken = getSSOToken(callerSubject);
    //                    if (ssoToken != null) {
    //                        //String[] attrs = ssoToken.getAttributes(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
    //                        //if (attrs != null && attrs.length > 0) {
    //                            customCacheKey = "Saml20Token";
    //                        //}
    //                        writeDom(sb, "SamlAssertion: ",  ssoToken.getDOM() );
    //                    }
    //                }
    //            }
    //
    //
    //        } catch (NoClassDefFoundError ne) {
    //            // For OSGI App testing (EBA file), we expect this exception for all packages that are not public
    //            writeLine(sb, "NoClassDefFoundError for SubjectManager: " + ne);
    //        } catch (Throwable t) {
    //            t.printStackTrace();
    //        }
    //    }
    //
    //    /**
    //     * "Writes" the msg out to the client. This actually appends the msg
    //     * and a line delimiters to the running StringBuffer. This is necessary
    //     * because if too much data is written to the PrintWriter before the
    //     * logic is done, a flush() may get called and lock out changes to the
    //     * response.
    //     *
    //     * @param sb Running StringBuffer
    //     * @param msg Message to write
    //     */
    //    void writeLine(StringBuffer sb, String msg) {
    //        sb.append(msg + "\n");
    //    }
    //
    //    void writeDom(StringBuffer sb, String name,  Element dom ){
    //    	sb.append(name);
    //    	sb.append("\n");
    //    	traverse( dom, 0, sb);
    //    	sb.append("\n");
    //    }
    //
    //	   static char[] _cIndent = new char[ 100 ];
    //	   static int    _iIndent = 3;
    //    private static void traverse( Node node, int iLevel, StringBuffer sb )
    //	   {
    //	      int iIndent = iLevel * _iIndent;
    //	      printIndent( iIndent, sb );
    //
    //	      String strN = node.getNodeName();
    //	      sb.append( "<" + strN );
    //
    //	      int iAttribIndent = iIndent + strN.length() + 2;
    //	      // Then print the Attribute
    //	      NamedNodeMap attrlist = node.getAttributes();
    //	      int iLength;
    //	      if( attrlist != null )
    //	      {
    //	         iLength = attrlist.getLength();
    //	         int iI = 0;
    //	         while( iI < iLength )
    //	         {
    //	            Node attr = attrlist.item( iI );
    //	            if( attr instanceof Attr )
    //	            {
    //	               String str = ((Attr)attr).getName();
    //	               sb.append( " " +  str +
    //	                                 "=\"" + ((Attr)attr).getValue() +"\"" );
    //	               iI ++;
    //	               break;
    //	            }
    //	            iI ++;
    //	         }
    //
    //	         for( ; iI < iLength; iI++ )
    //	         {
    //	            Node attr = attrlist.item( iI );
    //	            if( attr instanceof Attr )
    //	            {
    //	     //          sb.append("\n");
    //	               printIndent( iAttribIndent, sb );
    //	               String str = ((Attr)attr).getName();
    //	               sb.append( str +
    //	                                 "=\"" + ((Attr)attr).getValue() + "\"" );
    //	            }
    //	         }
    //	      }
    //
    //
    //	      boolean bClose = false;
    //	      boolean bEnd   = false;
    //
    //	      NodeList nodelist = node.getChildNodes();
    //	      // Traverse all the children of the root element
    //	      iLength = nodelist.getLength();
    //	      Log.info(thisClass, "traverse", "number of nodes: " + iLength) ;
    //	      for (int iI = 0; iI < iLength; iI ++ )
    //	      {
    //	         Node item = nodelist.item( iI );
    //
    //	         // When child is an element
    //	         if( item instanceof Element)
    //	         {
    //	            if( bClose == false && bEnd == false)
    //	            {
    //	               bClose = true;
    //	               sb.append( ">" );
    ////	               sb.append( ">\n" );
    //	            }
    //	            traverse( item, iLevel + 1, sb );
    //				sb.append("\n");
    //	         }
    //	         else
    //	         {
    //	            if( item instanceof Attr )
    //	            {
    //	               Attr attr = (Attr) item;
    //	               sb.append( "???ERR " + attr.getName() + "=\"" + attr.getValue() + "\"" );
    //	            }
    //	            else
    //	            if( item instanceof Text )
    //	            {
    //	               if( !bClose)
    //	               {
    //	                  bClose = true;
    //	                  sb.append( ">" );
    ////	                  sb.append( ">\n" );
    //	               }
    //	               printText( (Text)item, iIndent, sb );
    //	            }
    //	            else
    //	            if( item instanceof Comment )
    //	            {
    //	               if( !bEnd && ! bClose)
    //	               {
    //	                  boolean bNext = false;
    //	                  for( int iJ = iI + 1; iJ < iLength; iJ ++ )
    //	                  {
    //	                     if( nodelist.item( iJ ) instanceof Element ||
    //	                         nodelist.item( iJ ) instanceof Attr     )
    //	                     {
    //	                        bNext = true;
    //	                        break;
    //	                     }
    //	                  }
    //	                  if( bNext == false  )
    //	                  {
    //	                     bEnd   = true;
    //	                     sb.append( ">" );
    ////	                     sb.append( ">\n" );
    //	                  }
    //	                  else
    //	                  {
    //	                     bClose = true;
    //	                     sb.append( ">" );
    ////	                     sb.append( ">\n" );
    //	                  }
    //	               }
    //	               // Skip all the text
    //	               printComment( (Comment)item, iIndent, sb );
    //	            }
    //	            else
    //	            {
    //	               sb.append( "???  " + item.getClass().getName() +
    //	                                 "?" + item.getNodeName() +
    //	                                 "=" + item.getNodeValue()
    //	                                 );
    //	            }
    //	         }
    //	      }
    //	      if( ! bEnd )
    //	      {
    //	         bEnd = true;
    //	         if( bClose )
    //	         {
    //	            printIndent( iIndent, sb );
    //	            sb.append( "</" + strN + ">" );
    ////	            sb.append( "</" + strN + ">\n" );
    //	         }
    //	         else
    //	         {
    //		            sb.append( "/>\n" );
    ////		            sb.append( "/>" );
    //	         }
    //	      }
    //	   }
    //
    //
    //	   private static void printIndent( int iIndent, StringBuffer sb )
    //	   {
    //	      for( int iI = 0; iI < iIndent; iI ++ )
    //	      {
    //	         sb.append( " " );
    //	      }
    //
    //	   }
    //
    //	   private static void printComment( Comment comment, int iIndent, StringBuffer sb )
    //	   {
    //	      iIndent += _iIndent;
    //	      printIndent( iIndent, sb );
    //	      sb.append( "<!--\n" );
    //
    //	      String str =null;
    //	      try
    //	      {
    //	         str = comment.getData();
    //	      }
    //	      catch( Exception e )
    //	      {
    //	         e.printStackTrace();
    //	         return;
    //	      }
    //
    //
    //	      StringTokenizer st = new StringTokenizer( str, "\n\r", false );
    //	      while( st.hasMoreTokens() )
    //	      {
    //	         String token = st.nextToken();
    //	         printIndent( iIndent + 4, sb );
    //	         sb.append( token + "\n" );
    //	      }
    //
    //	      printIndent( iIndent, sb );
    //	      sb.append( "-->\n" );
    //	   }
    //
    //	   private static void printText( Text comment, int iIndent, StringBuffer sb )
    //	   {
    //	      iIndent += _iIndent;
    //
    //	      String str =null;
    //	      try
    //	      {
    //	         str = comment.getData();
    //	      }
    //	      catch( Exception e )
    //	      {
    //	         e.printStackTrace();
    //	         return;
    //	      }
    //
    //
    //	      StringTokenizer st = new StringTokenizer( str, "\n\r", false );
    //	      while( st.hasMoreTokens() )
    //	      {
    //	         String token = st.nextToken();
    //	         printIndent( iIndent, sb );
    ////	         sb.append( token + "\n");
    //	         sb.append( token);
    //	      }
    //	   }

    public Boolean checkSAMLTokenValue(Object response, String expectedValue, ArrayList<String> location, String type, String checkType) throws Exception {

        String thisMethod = "checkSAMLTokenValue";
        String theValue = getSAMLTokenValue(response, location, type);

        if (theValue == null && expectedValue == null) {
            return true;
        }
        if (theValue == null) {
            return false;
        }
        Log.info(thisClass, thisMethod, "Expected Value is: " + expectedValue);
        Log.info(thisClass, thisMethod, "checkType is: " + checkType);
        if ((checkType.equals(SAMLConstants.STRING_EQUALS) && theValue.equals(expectedValue)) ||
            (checkType.equals(SAMLConstants.STRING_CONTAINS) && theValue.contains(expectedValue))) {
            return true;
        }
        // the value in the response is non-null, but the request
        // is just to make sure that there is some value, so, as
        // long as it has a value, we should be ok
        if (type == SAMLConstants.NODE_EXISTS_TYPE) {
            return true;
        } else {
            return false;
        }
    }

    public String getSAMLTokenValue(Object thePage, ArrayList<String> location, String type) throws Exception {
        List<HtmlForm> forms = ((HtmlPage) thePage).getForms();
        if (forms == null || forms.isEmpty()) {
            throw new Exception("Response did not contain any forms but was expected to. Full response was: " + AutomationTools.getResponseText(thePage));
        }
        HtmlForm form = forms.get(0);

        com.gargoylesoftware.htmlunit.WebRequest request = form.getWebRequest(null);

        return getSAMLTokenValueFromRequest(request, location, type);
    }

    public void printXMLDoc(Document doc) throws Exception {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            Log.info(thisClass, "printXMLDoc", "Full document: " + writer.toString());
        } catch (TransformerException ex) {
            ex.printStackTrace();
        }
    }

    public String getSAMLTokenValueFromRequest(com.gargoylesoftware.htmlunit.WebRequest request, ArrayList<String> location, String type) throws Exception {

        String thisMethod = "getSAMLTokenValueFromRequest";

        //		String samlResponse = extractSAMLReponse(request);
        //		Log.info(thisClass,  thisMethod, "encoded:\n" + samlResponse);
        //		//			Log.info(thisClass,  thisMethod, "");
        //
        //
        //		byte[] base64DecodedResponse = Base64.decode(samlResponse); // SamlResponse xml content
        //		Log.info(thisClass, thisMethod, "decoded: " + base64DecodedResponse.toString());
        //
        //		String xml1 = new String(base64DecodedResponse);
        //		Log.info(thisClass, thisMethod, "xml: " + xml1);

        String xml1 = convertSamlResponseToXML(request);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        String theValue = null;

        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml1)));
            // TODO - will this work fof TFIM???
            doc.getDocumentElement().normalize();
            //            printXMLDoc(doc) ;
            Log.info(thisClass, thisMethod, "Document root element: " + doc.getDocumentElement());
            Log.info(thisClass, thisMethod, "Document elements: " + doc.getElementsByTagName("*"));
            NodeList childNodeList = doc.getChildNodes();
            int numChildNodes = childNodeList.getLength();
            Log.info(thisClass, thisMethod, "Number of child nodes: " + numChildNodes);

            if (numChildNodes > 0) {
                for (int i = 0; i < numChildNodes; i++) {
                    Log.info(thisClass, thisMethod, "Child node " + i + ": " + childNodeList.item(i));
                    theValue = getNestedElement(childNodeList.item(i), location, type);
                    if (theValue != null) {
                        break;
                    }
                }
            }
            Log.info(thisClass, thisMethod, "found the value for the request: " + theValue);

        } catch (Exception e) {
            Log.info(thisClass, thisMethod, "failure with doc builder");
            e.printStackTrace();
            throw e;
        }
        return theValue;

    }

    public String getSAMLTokenAssertionFromRequest(Object thePage, SAMLTestSettings settings, Boolean fullTextToken) throws Exception {

        String thisMethod = "getSAMLTokenAssertionFromRequest";

        boolean isEncryptedAssertion = settings.getIsEncryptedAssertion();

        String assertionStart = "<saml2:" + (isEncryptedAssertion ? "Encrypted" : "") + "Assertion";
        String assertionEnd = "</saml2:" + (isEncryptedAssertion ? "Encrypted" : "") + "Assertion>";

        String samlResponse = new String(Base64.decode(extractSAMLReponse(getRequestWithSamlToken(thePage, settings))));
        if (fullTextToken) {
            return samlResponse;
        }
        Log.info(thisClass, thisMethod, "samlResponse: " + samlResponse);

        //        Log.info(thisClass,  thisMethod, "##############******************" + getSAMLTokenValue(thePage, location, SAMLConstants.ELEMENT_TYPE));

        int startIndex = samlResponse.indexOf(assertionStart);
        int endIndex = samlResponse.indexOf(assertionEnd);
        Log.info(thisClass, thisMethod, "start: " + startIndex + "end: " + endIndex);
        String newString = samlResponse;
        Map<String, String> parentNamespaces = new HashMap<String, String>();
        Log.info(thisClass, thisMethod, "About to process parent name space");
        if ((startIndex != -1) && (endIndex != -1)) {
            newString = samlResponse.substring(startIndex, endIndex + assertionEnd.length());
            Log.info(thisClass, thisMethod, "The seed of the assertion is: " + newString);
            parentNamespaces = extractNamespaces(samlResponse.substring(0, startIndex));
        }

        for (String x : parentNamespaces.keySet()) {
            Log.info(thisClass, thisMethod, "Parent ns entry: " + x);
        }

        Log.info(thisClass, thisMethod, "About to process assertion name space");
        //        if (isIDPSHIBBOLETH(settings.getIdpRoot())) {
        //            return newString ;
        //        }
        //
        // Extract and add all namespaces defined by the parent to the Assertion element
        Map<String, String> assertionNamespaces = extractNamespaces(newString);
        // Identical namespaces in the Assertion should override the parent's values for those namespaces
        for (String x : assertionNamespaces.keySet()) {
            Log.info(thisClass, thisMethod, "Assertion ns entry: " + x);
        }
        parentNamespaces.putAll(assertionNamespaces);
        for (String x : parentNamespaces.keySet()) {
            Log.info(thisClass, thisMethod, "Merged ns entry: " + x);
        }
        String newNamespaces = "";
        for (String ns : parentNamespaces.keySet()) {
            // if entry from parentNamespaces is NOT in the assertion, add it
            Log.info(thisClass, thisMethod, "Processing parent ns entry: " + ns);
            if (assertionNamespaces.get(ns) == null) {
                newNamespaces += " " + ns + "=\"" + parentNamespaces.get(ns) + "\"";
            } else {
                Log.info(thisClass, thisMethod, "Don't add parent namespace");
            }
        }
        newNamespaces += " xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"";

        String prefix = newString.substring(0, assertionStart.length());
        String postfix = newString.substring(assertionStart.length());
        newString = prefix + newNamespaces + postfix;

        Log.info(thisClass, thisMethod, "Raw SAML Assertion/Token: " + newString);
        return newString;

    }

    /**
     * Extracts all of the namespace names and values defined in the given string. Namespaces are expected to be in the format:
     * xmlns:namespace-name="namespace-value".
     *
     * @param samlResponse
     * @return
     */
    private Map<String, String> extractNamespaces(String samlResponse) {
        String method = "extractNamespaces";

        Map<String, String> namespaces = new HashMap<String, String>();

        Pattern nsPattern = Pattern.compile("xmlns:([^=]+?)=\"([^\"]+?)\"");
        Matcher m = nsPattern.matcher(samlResponse);
        while (m.find()) {
            String match = m.group();
            Log.info(thisClass, method, "Found namespace: [" + match + "]");
            namespaces.put("xmlns:" + m.group(1), m.group(2));
        }

        return namespaces;
    }

    //SC added
    public String decodeSamlResponseString(String res) throws Exception {
        String thisMethod = "decodeSamlResponseString";
        byte[] base64DecodedResponse = Base64.decode(res); // SamlResponse xml content
        //Log.info(thisClass, thisMethod, "decoded: " + base64DecodedResponse.toString());

        String decoded = new String(base64DecodedResponse);
        Log.info(thisClass, thisMethod, "decoded: " + decoded);

        return decoded;
    }

    public String convertSamlResponseToXML(com.gargoylesoftware.htmlunit.WebRequest request) throws Exception {

        String thisMethod = "convertSamlResponseToXML";

        String samlResponse = extractSAMLReponse(request);

        Log.info(thisClass, thisMethod, "encoded:\n" + samlResponse);
        //			Log.info(thisClass,  thisMethod, "");

        byte[] base64DecodedResponse = Base64.decode(samlResponse); // SamlResponse xml content
        Log.info(thisClass, thisMethod, "decoded: " + base64DecodedResponse.toString());

        String xml1 = new String(base64DecodedResponse);
        Log.info(thisClass, thisMethod, "xml: " + xml1);

        return xml1;
    }

    // Compress the input string, using Gzip

    public static byte[] compressString(byte[] str) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str);
        gzip.close();

        return out.toByteArray();

    }

    public String convertXmlToSamlResponse(String xml1) throws Exception {
        return convertXmlToSamlResponse(xml1, SAMLConstants.ASSERTION_ENCODED);
    }

    public String convertXmlToSamlResponse(String xml1, String samlResponseFormat) throws Exception {

        String thisMethod = "convertSamlResponseToXML";
        Log.info(thisClass, thisMethod, "xml After: " + xml1);

        if (samlResponseFormat.equals(SAMLConstants.ASSERTION_TEXT_ONLY) || samlResponseFormat.equals(SAMLConstants.TOKEN_TEXT_ONLY)) {
            // no further handling required
            //            return xml1;
            return trimXML(xml1);
        }

        // encode or compress and encode as requested
        byte[] newBase64DecodedResponse = xml1.getBytes();
        Log.info(thisClass, thisMethod, "converted from xml: " + newBase64DecodedResponse.toString());

        String updatedResponse = null;
        if (samlResponseFormat.equals(SAMLConstants.ASSERTION_COMPRESSED_ENCODED)) {
            Log.info(thisClass, thisMethod, "Compress and Encode");
            byte[] compressedResponse = compressString(newBase64DecodedResponse);
            updatedResponse = Base64.encodeBytes(compressedResponse, Base64.DONT_BREAK_LINES);
        } else {
            Log.info(thisClass, thisMethod, "Compress Only");
            updatedResponse = Base64.encodeBytes(newBase64DecodedResponse, Base64.DONT_BREAK_LINES);
        }
        Log.info(thisClass, thisMethod, "newer encoded:\n" + updatedResponse);

        return updatedResponse;

    }

    // trim newlines from Xml
    // also have to remove signature
    public static String trimXML(String input) {
        return trimXML(input, SAMLConstants.TEST_CASE);
    }

    public static String trimXML(String input, String Caller) {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringBuffer result = new StringBuffer();
        try {
            String line;
            while ((line = reader.readLine()) != null)
                //                result.append(line.trim());
                result.append(line);
            String samlAssertion = result.toString();
            return removeTagInSAMLTokenWorker(samlAssertion, null, "ds:Signature");
            //            return removeAttribute(samlAssertion, "ds:Signature");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getIndent() {
        String indent = "";
        for (int i = 0; i < indentFactor; i++) {
            indent = indent + "\t";
        }
        return indent;
    }

    public String getLoginTitle(String idpServer) {

        if (isIDPADFS(idpServer)) {
            return SAMLConstants.SAML_ADFS_LOGIN_HEADER;
        } else {
            if (isIDPSHIBBOLETH(idpServer)) {
                return SAMLConstants.SAML_SHIBBOLETH_LOGIN_HEADER;
            } else {
                return SAMLConstants.SAML_TFIM_LOGIN_HEADER;
            }
        }
    }

    public String getResponseTitle(String idpServer) {

        if (isIDPADFS(idpServer)) {
            return SAMLConstants.SAML_ADFS_POST_RESPONSE;
        } else {
            if (isIDPSHIBBOLETH(idpServer)) {
                return SAMLConstants.SAML_SHIBBOLETH_POST_RESPONSE;
            } else {
                return SAMLConstants.SAML_TFIM_POST_RESPONSE;
            }
        }
    }

    public Boolean isIDPTFIM(String idpServer) {

        return !(isInList(SAMLConstants.ADFS_SERVERS, idpServer) || isInList(SAMLConstants.SHIBBOLETH_SERVERS, idpServer));
    }

    public Boolean isIDPADFS(String idpServer) {

        return isInList(SAMLConstants.ADFS_SERVERS, idpServer);
    }

    public Boolean isIDPSHIBBOLETH(String idpServer) {

        return isInList(SAMLConstants.SHIBBOLETH_SERVERS, idpServer);
    }

    /**
     * Determines if specified string is in the list of strings
     *
     * @param theList
     *                         - the list to search in
     * @param searchString
     *                         - the string to search for in the list
     * @return true/false - true - in list, false - not in list
     */
    public Boolean isInList(String[] theList, String searchString) {
        if (theList == null) {
            return false;
        }
        for (String entry : theList) {
            if (entry.equals(searchString)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Choose a random entry from the provided String array.
     *
     * @param entryArray
     * @return
     */
    public String chooseRandomEntry(String... entryArray) {
        String thisMethod = "chooseRandomEntry";
        Log.info(thisClass, thisMethod, "Determining which entry to select");
        Random rand = new Random();
        Integer num = rand.nextInt(1000);
        int div = num % entryArray.length;
        Log.info(thisClass, thisMethod, "Choosing entry from index: " + div);

        String entry = entryArray[div];
        Log.info(thisClass, thisMethod, "Entry chosen: " + entry);
        return entry;
    }

}
