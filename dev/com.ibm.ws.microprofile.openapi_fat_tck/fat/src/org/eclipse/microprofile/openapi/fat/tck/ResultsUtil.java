package org.eclipse.microprofile.openapi.fat.tck;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.websphere.simplicity.log.Log;


public class ResultsUtil {

    public void xPathProcessor() throws SAXException, IOException
                  ,  XPathExpressionException, ParserConfigurationException {

      
        //Create DocumentBuilderFactory for reading testng-results file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse("publish/tckRunner/tck/target/surefire-reports/testng-results.xml");
      
        // Create XPathFactory for creating XPath Object
        XPathFactory xPathFactory = XPathFactory.newInstance();

        // Create XPath object from XPathFactory
        XPath xpath = xPathFactory.newXPath();

        // Compile the XPath expression for getting all brands
        XPathExpression xPathExpr = xpath.compile("/testng-results/@total");
      
        // XPath text example : executing xpath expression in java
        Object result = xPathExpr.evaluate(doc, XPathConstants.NODESET);
        Log.info(getClass(), "Result", "Number of tests: " + getXpathResult(result));
        
        xPathExpr = xpath.compile("/testng-results/@passed");
        result = xPathExpr.evaluate(doc, XPathConstants.NODESET);
        Log.info(getClass(), "Result", "Number of passed tests: " + getXpathResult(result));
       
        xPathExpr = xpath.compile("/testng-results/@failed");
        result = xPathExpr.evaluate(doc, XPathConstants.NODESET);
        Log.info(getClass(), "Result", "Number of failed tests: " + getXpathResult(result));
        
        if (Integer.parseInt(getXpathResult(result)) != 0) {
        	doc = builder.parse("publish/tckRunner/tck/target/surefire-reports/testng-failed.xml");
        	xPathExpr = xpath.compile("/suite/test/classes/class/@name");
        	
        	result = xPathExpr.evaluate(doc, XPathConstants.NODESET);
            
        	NodeList nodes = (NodeList) result;
            String testClass;
            for (int i = 0; i < nodes.getLength(); i++) {
                testClass = nodes.item(i).getNodeValue();
                xPathExpr = xpath.compile("/suite/test/classes/class"
                    		+ "[@name='" + testClass + "']"
                			+ "/methods/include[@invocation-numbers='0 1']/@name");
                
                result = xPathExpr.evaluate(doc, XPathConstants.NODESET);
                if (getFailures(result) != "") {
                	Log.info(getClass(), "Result", "Tests failed in " + testClass + ": \n" + getFailures(result));
                }
            }
        }
        
       
    }

    public String getFailures(Object result){
        NodeList nodes = (NodeList) result;
        String finalResult = "";
        for (int i = 0; i < nodes.getLength(); i++) {
            finalResult += nodes.item(i).getNodeValue() + "\n";
        }
        
        return finalResult;
    }

    public String getXpathResult(Object result){
        NodeList nodes = (NodeList) result;
        String finalResult = "";
        for (int i = 0; i < nodes.getLength(); i++) {
            finalResult += nodes.item(i).getNodeValue();
        }
        
        return finalResult;
    }
}