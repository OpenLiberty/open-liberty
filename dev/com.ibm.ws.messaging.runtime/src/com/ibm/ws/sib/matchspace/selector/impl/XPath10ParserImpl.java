/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.matchspace.selector.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.MatchSpace;
import com.ibm.ws.sib.matchspace.impl.Matching;
import com.ibm.ws.sib.matchspace.QuerySyntaxException;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.tools.MatchParser;
import com.ibm.ws.sib.matchspace.tools.XPath10Parser;
import com.ibm.ws.sib.matchspace.utils.FFDC;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceComponent;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;

public class XPath10ParserImpl implements XPath10Parser
{
  // Standard trace boilerplate
  private static final Class cclass = XPath10ParserImpl.class;
  private static Trace tc = TraceUtils.getTrace(XPath10ParserImpl.class,
    MatchSpaceConstants.MSG_GROUP_LISTS);	
  // The Matching instance
  private static Matching matching = null;  
  // List of operands isolated from expression
  List selOperands = null;
  
  MatchParser predicateParser = null;
  
  // Name space Context for selectors
  NamespaceContext namespaceContext = null;
  int locationStep = 0;

  /**
   * This class encapsulates the properties of a specific location step.
   *
   */
  private class StepProperties
  {
    // The position of the end of a location step
    int endOfStep;
    // True if the entire step should be wrapped in an Identifier 
    boolean wrapStep;
    
    public String toString() 
    {
      String asString = "End of step is " + endOfStep +
                        " and wrapStep is " + wrapStep;
      return asString;
    }    
  }  
  
  /**
   * This method retains the behaviour of the first implementation of XPath support,
   * where the entire expression is wrapped in a single identifier.
   * 
   * @param selector
   * @return
   */
  public Selector parseWholeSelector(String selector) 
  {
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	    tc.entry(
	      cclass,
	      "parseWholeSelector",
	      "selector: " + selector);
	
    // Need to set the domain to XPATH1.0
    Identifier ident = new IdentifierImpl(selector);
    // Set the full name into the identifier. The full name is used in position assignment when
    // determining uniqueness of names. Only a full name will do.
    ident.setFullName(selector);    
    ident.setSelectorDomain(2);
        
    // Call XPath to compile the XPath1.0 expression and store the
    // resultant XPathExpression in the Identifier.
    XPathExpression xpexp = null;
    try
    {
      // Parse an expression up-front
      XPath xpath0 = XPathFactory.newInstance().newXPath();
      
      // If a namespace context has been set then set it into the XPath env      
      if(namespaceContext != null)
        xpath0.setNamespaceContext(namespaceContext);
      
      xpexp = xpath0.compile(selector);
    }
    catch (Exception ex)
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.
      // This signals that the XPath compiler encountered a problem
      FFDC.processException(cclass,
        "com.ibm.ws.sib.matchspace.selector.impl.XPath10ParserImpl",
        ex,
        "1:143:1.16");
 
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        tc.debug(this,cclass, "parseWholeSelector", ex);   
      
      // Set the identifier object to "invalid"
      ident.setType(Selector.INVALID);
    }

    // Store xpexp in the Identifier    
    ident.setCompiledExpression(xpexp);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "parseWholeSelector", ident);
    
    return ident;
  } 
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.matchspace.tools.MatchParser#getSelector(java.lang.String)
   */
  public Selector getSelector(String selector) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "getSelector",
               "selector: " + selector);	  
  	// Start by parsing the selector expression (in-line for the moment)
  	selOperands = new ArrayList();
    // We'll Construct a selector tree
    Selector selectorTree = null;
    
    try
    {
      parseSelector(selector);

      Selector[] selectorArray = new Selector[selOperands.size()];
      selOperands.toArray(selectorArray);
 	
      // Iterate over the nodes that were passed into us
      for(int i = selectorArray.length - 1 ; i>=0; i--)
      {
        Selector parsedSelector = (Selector)selectorArray[i];
        if(selectorTree == null)
          selectorTree = parsedSelector;
        else
          selectorTree = matching.createExtensionOperator(Selector.AND, parsedSelector, selectorTree);
      }
    }
    catch(InvalidXPathSyntaxException iex)
    {
      // No FFDC Code Needed.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        tc.debug(this,cclass, "getSelector", iex);   

      // Attempt to parse the entire selector expression. If that fails, then
      // we really can despair of handling this expression.
      selectorTree = parseWholeSelector(selector);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "getSelector", selectorTree);  
    
    return selectorTree;
  }	

  /**
   * Parse the XPath Selector expression.
   * 
   * @param selector
   * @throws InvalidXPathSyntaxException
   */
  private void parseSelector(String selector) throws InvalidXPathSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "parseSelector",
               "selector: " + selector);   
    // Set the locationStep to -1
    locationStep = -1;
    int start = 0;
    // find number of path separators

    // Pre-check selector for multi-level wildcards. We can't handle these in
    // the optimised code. Simply wrap the entire expression
    int posWildCard = selector.indexOf("//", start);
    if(posWildCard >= 0)
    {
      // We have a multi-level wildcard, process the whole thing
      locationStep = 0;
      selOperands.add(createIdentifierForWildExpression(selector));
      return;
    }
    
    // Locate the first path separator
    int posSeparator = selector.indexOf("/", start);

    // Handle an initial separator character
    if(posSeparator == 0)
    {
      // An initial slash
      String step = selector.substring(start,start+1);
      // Add an IdentifierImpl for the location step to the array list
      selOperands.add(createIdentifierForSubExpression(step, step, true, false));   
      // Bump the start counter      
      start++;
     }

    // Now iterate over the rest of the selector string attempting to isolate
    // Individual location path steps.
    int stepEnd = 0;
    while (stepEnd >= 0)
    {
      StepProperties stepProperties = new StepProperties();
      
      // Drive the method that isolates the next step
      isolateNextStep(selector,start, stepProperties);

      stepEnd = stepProperties.endOfStep;
      
      // Useful debug
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        if(stepEnd < 0)
        {
          tc.debug(this,
                   cclass, 
                   "parsePredicate", 
                   "isolated Step: " + selector.substring(start) +
                   " and wrapStep is " + stepProperties.wrapStep);
        }
        else
        {
          tc.debug(this,
                   cclass, 
                   "parsePredicate", 
                   "isolated Step: " + selector.substring(start,stepEnd) +
                   " and wrapStep is " + stepProperties.wrapStep);
        }
      }
     
      // Should the entire location step be wrapped in an Identifier
      if(stepProperties.wrapStep)
      {
        if(stepEnd <0)
        {
          // Wrap entire location step in an Identifier
          wrapLastLocationStep(selector, start, true);
        }
        else
        {
          // Wrap intermediate location step in an Identifier
          wrapLocationStep(selector, start,stepEnd);          
        }
      }
      else
      {
        // We can attempt to parse the step using the MatchParser
        if(stepEnd <0)
        {
          // This is the final location step
          parseLastLocationStep(selector, start);
        }        
        else
        {
          // An intermediate location step
          parseLocationStep(selector, start, stepEnd);
        }
      }
      
      // Bump the start parameter and then look for next step
      if(stepEnd >= 0)
        start = stepEnd + 1;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "parseSelector");    
  }
  

  /**
   * Break a location step into predicates that can be driven against the MatchParser.
   * 
   * @param selector
   * @param start
   * @param stepEnd
   * @throws InvalidXPathSyntaxException
   */
  private void parseLocationStep(String selector,
                                 int start,
                                 int stepEnd)
    throws InvalidXPathSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "parseLocationStep",
               "selector: " + selector + ", start: " + start + ", end: " + stepEnd);

    int stepStart = start;
    int posOpenBracket = selector.indexOf("[", start);
    if(posOpenBracket > stepEnd || posOpenBracket == -1)
    {
      // Set posOpenBracket to avoid further processing
      posOpenBracket = -1;
      // No brackets so process whole of the location step
      String step = selector.substring(start,stepEnd);
      // Set the full name into the identifier. The full name is used in position assignment when
      // determining uniqueness of names. Only a full name will do.
      String full = selector.substring(0,stepEnd);
      // Add an IdentifierImpl for the location step to the array list
      selOperands.add(createIdentifierForSubExpression(step, full, true, false));      
    }
    
    int posCloseBracket = selector.indexOf("]", start); 
 
    boolean wrapWholeStep = false;
    boolean foundPredicates = false;
    ArrayList tempSelOperands = new ArrayList();
    while (posOpenBracket >= 0)
    {
      foundPredicates = true;
      if(posCloseBracket < posOpenBracket)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(this,cclass, "parseLocationStep", "bracket error"); 
        
        InvalidXPathSyntaxException iex = new InvalidXPathSyntaxException(selector);
        FFDC.processException(cclass,
            "com.ibm.ws.sib.matchspace.selector.impl.XPath10ParserImpl",
            iex,
            "1:372:1.16"); 
        
        throw iex;
      }
      else
      {
        // The full selector path up to this point
        String full = selector.substring(0,posOpenBracket);
        
        // Factor out the location step first but be careful we may have a predicate
        if(start != posOpenBracket)
        {
          // Go ahead and deal with the location step
          String step = selector.substring(start,posOpenBracket);
          // Add an IdentifierImpl for the location step to the array list
          tempSelOperands.add(createIdentifierForSubExpression(step, full, true, false));    
        }
        
        // Now parse the predicate
        String predicate = selector.substring(posOpenBracket + 1,posCloseBracket);
        Selector parsedPredicate = parsePredicate(predicate, full);
        
        // Check whether we were able to parse
        if(parsedPredicate == null)
        {
          // Unable to parse the expression, so we need to wrap the entire step
//          tempSelOperands.add(createIdentifierForSubExpression(predicate, false));
          wrapWholeStep = true;
          break;
        }
        else
        {
          // we were able to parse the expression with the MatchParser
          // Check that the predicate has Simple tests only
          if(!Matching.isSimple(parsedPredicate))
          {
            wrapWholeStep = true;
            break;
          }
          parsedPredicate.setExtended();
          tempSelOperands.add(parsedPredicate);
        }       
      }
        
      // Move to beyond the close bracket
      start = posCloseBracket+1;
      //Are there any more brackets?
      posOpenBracket = selector.indexOf("[", start);
      posCloseBracket = selector.indexOf("]", start); 
      
      // Have we found the last bracket in this location step?
      if(posOpenBracket > stepEnd || posOpenBracket == -1)
      {
        // Set posOpenBracket to avoid further processing
        posOpenBracket = -1; 
      }
    } // eof while
    
    if(foundPredicates)
    {
      // If we determined that we cannot parse this location step, then wrap it
      if(wrapWholeStep)
        wrapLocationStep(selector, stepStart,stepEnd);
      else
      {
        // PostProcessing here
        selOperands.addAll(tempSelOperands);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "parseLocationStep");      
  }  

  /**
   * Break the final location step into predicates that can be driven against the MatchParser.
   * @param selector
   * @param start
   * @throws InvalidXPathSyntaxException
   */
  private void parseLastLocationStep(String selector,
                                     int start )
    throws InvalidXPathSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "parseLastLocationStep",
               "selector: " + selector + ", start: " + start); 
    int stepStart = start;
    int stepEnd = selector.length();
    int posOpenBracket = selector.indexOf("[", start);
    
    String remnant = null;
    String fullRemnant = null;

    // This is the final location step, bump the counter
    locationStep++;
    
    if(posOpenBracket > stepEnd || posOpenBracket == -1)
    {
      // Set posOpenBracket to avoid further processing
      posOpenBracket = -1;
      // No brackets so the remnant is the whole of the location step
      remnant = selector.substring(start,stepEnd); 
      fullRemnant = selector.substring(0, stepEnd);
         
    }

    // Now we'll work with the brackets
    int posCloseBracket = selector.indexOf("]", start); 
    boolean wrapWholeStep = false;
    boolean foundPredicates = false;
    ArrayList<Selector> tempSelOperands = new ArrayList<Selector>();  
    
    while (posOpenBracket >= 0)
    {
      foundPredicates = true;
      if(posCloseBracket < posOpenBracket)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          tc.exit(this,cclass, "parseLastLocationStep", "bracket error");   
       
        InvalidXPathSyntaxException iex = new InvalidXPathSyntaxException(selector);
        FFDC.processException(cclass,
            "com.ibm.ws.sib.matchspace.selector.impl.XPath10ParserImpl",
            iex,
            "1:497:1.16"); 
        
        throw iex;
      }
      else
      {
        // The full name of the selector so far. The full name is used in position assignment when
        // determining uniqueness of names. Only a full name will do.
        String full = selector.substring(0,posOpenBracket);
        // Factor out the location step first but be careful we may have a predicate
        if(start != posOpenBracket)
        {
          // Go ahead and deal with the location step
          String step = selector.substring(start,posOpenBracket);
          // Add an IdentifierImpl for the location step to the array list
          // N.B. "false" cos we've bumped the location step already
          tempSelOperands.add(createIdentifierForSubExpression(step, full, false, false)); 
        }
        
        // Now parse the predicate
        String predicate = selector.substring(posOpenBracket + 1,posCloseBracket);
        Selector parsedPredicate = parsePredicate(predicate, full);
        
        // Check whether we were able to parse
        if(parsedPredicate == null)
        {
          // Unable to parse the expression, so we'll wrap the whole step
//          tempSelOperands.add(createIdentifierForSubExpression(predicate, false));
          wrapWholeStep = true;
          break;
        }
        else
        {
          // we were able to parse the expression with the MatchParser
          parsedPredicate.setExtended();
          tempSelOperands.add(parsedPredicate);
        }       
      }
        
      // Move to beyond the close bracket
      start = posCloseBracket+1;
      //Are there any more brackets?
      posOpenBracket = selector.indexOf("[", start);
      posCloseBracket = selector.indexOf("]", start); 
      
      // Have we found the last bracket in this location step?
      if(posOpenBracket > stepEnd || posOpenBracket == -1)
      {
        // Set posOpenBracket to avoid further processing
        posOpenBracket = -1;
        
        // Set up the remnant
        if(start < stepEnd)
        {
          remnant = selector.substring(start,stepEnd);
          fullRemnant = selector.substring(0, stepEnd);
        }
      }
    } // eof while

    if(foundPredicates)
    {
      // If we determined that we cannot parse this location step, then wrap it
      if(wrapWholeStep)
      {
        remnant = null;
        wrapLastLocationStep(selector, stepStart, false);
      }
      else
      {
        // PostProcessing here
        selOperands.addAll(tempSelOperands);
      }
    }
    
    // Process the remnant
    if(remnant != null)
    {
      Selector parsedPredicate = parsePredicate(remnant, fullRemnant);
      // Check whether we were able to parse
      if(parsedPredicate == null)
      {
        // Unable to parse the expression, so we'll wrap it
        selOperands.add(createIdentifierForSubExpression(remnant, fullRemnant, false, true));
      }
      else
      {
        // we were able to parse the expression with the MatchParser
        parsedPredicate.setExtended();
        selOperands.add(parsedPredicate);
      }                
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "parseLastLocationStep");     
  }    
  
  /**
   * When we've isolated a subexpression we wrap it in an Identifier.
   * 
   * @param subExpression
   * @param isLocationStep
   * @param isLastStep
   * @return
   * @throws InvalidXPathSyntaxException
   */
  private IdentifierImpl createIdentifierForSubExpression(String subExpression,
                                                          String fullExpression,
                                                          boolean isLocationStep,
                                                          boolean isLastStep) 
    throws InvalidXPathSyntaxException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "createIdentifierForSubExpression",
               "subExpression: " + subExpression + ", isLocStep: " + 
               new Boolean(isLocationStep) + ", isLastStep: " + 
               new Boolean(isLocationStep));
    
    IdentifierImpl stepIdentifier = new IdentifierImpl(subExpression); 
    // Set the full name into the identifier
    stepIdentifier.setFullName(fullExpression);
    
    // bump the locationStep if we're not dealing with a predicate
    if(isLocationStep)
      locationStep++;
    
    // Set the appropriate XPath parameters for this Identifier
    setXPathCharacteristics(stepIdentifier);
    
    // Set the type to child if not the last step
    if(!isLastStep)
      stepIdentifier.setType(Selector.CHILD);
    else
      stepIdentifier.setType(Selector.BOOLEAN);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "createIdentifierForSubExpression", stepIdentifier);   
    return stepIdentifier;
  }   

  /**
   * Wrap a selector with a multilevel wildcard in an Identifier.
   * 
   * @param selector
   * @return
   * @throws InvalidXPathSyntaxException
   */
  private IdentifierImpl createIdentifierForWildExpression(String selector) 
    throws InvalidXPathSyntaxException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "createIdentifierForWildExpression",
               "selector: " + selector);
    
    IdentifierImpl wildIdentifier = new IdentifierImpl(selector); 

    // Set the appropriate XPath parameters for this Identifier
    setXPathCharacteristics(wildIdentifier); 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "createIdentifierForSubExpression", wildIdentifier); 
    
    return wildIdentifier;
  }   
  
  /**
   * Configure the Identifier with appropriate XPath parameters.
   * 
   * @param identifier
   * @throws InvalidXPathSyntaxException
   */
  private void setXPathCharacteristics(IdentifierImpl identifier)
    throws InvalidXPathSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "setXPathCharacteristics",
               "identifier: " + identifier);
    
    // Need to set the domain to XPATH1.0
    identifier.setSelectorDomain(2);

    // Set the locationStep also
    identifier.setStep(locationStep);
    
    // Call XPath to compile the XPath1.0 expression and store the
    // resultant XPathExpression in the Identifier.
    XPathExpression xpexp = null;
    
    try
    {
      // Parse an expression up-front
      Node node = null;
      NodeList ns = null;
      XPath xpath0 = XPathFactory.newInstance().newXPath();
      
      // If a namespace context has been set then set it into the XPath env
      if(namespaceContext != null)
        xpath0.setNamespaceContext(namespaceContext);
      
      xpexp = xpath0.compile(identifier.getName());
    }
    catch (Exception ex)
    {
      // No FFDC Code Needed.
      // We don't FFDC because we'll catch this exception and then attempt
      // to parse the entire expression. If that fails, then we FFDC.
 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        tc.exit(this,cclass, "setXPathCharacteristics", ex);   
     
      throw new InvalidXPathSyntaxException(identifier.getName());
    }

    // Store xpexp in the Identifier    
    identifier.setCompiledExpression(xpexp);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "setXPathCharacteristics");     
  }     
  
  /**
   * Attempt to parse an isolated predicate using the MatchParser.
   * 
   * @param predicate
   * @return
   */
  private Selector parsePredicate(String predicate, String fullPath) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "parsePredicate",
               "predicate: " + predicate); 

    Selector parsed = null;
    try
    {
      // Preprocess predicate for special characters
      String parserInput = preProcessForSpecials(predicate);
      
      // Attempt to parse predicate with "JMS" parser
      predicateParser =  MatchParserImpl.prime(predicateParser, parserInput, true);     
      parsed = predicateParser.getSelector(parserInput);
      if (parsed.getType() == Selector.INVALID)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(this,cclass, "parsePredicate", "Unable to parse predicate");           
        // reset the output to null
        parsed = null;
      }
      else
      {
        postProcessSelectorTree(parsed, fullPath);
      }
    }
    catch (Exception ex)
    {
      // No FFDC Code Needed.
      // In this case we will only trace the exception that we encountered.
      // It could be that the XPath parser is able to process this predicate
      // and that we've encountered a discrepancy between the syntax of an
      // expression supported by JMS and by XPath.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        tc.debug(this,cclass, "parsePredicate", ex);   
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "parsePredicate", parsed);     
    return parsed;
  }       

  /**
   * Locate and replace any special characters that are going to cause problems
   * fr the MatchParser.
   * 
   * @param predicate
   * @return
   */
  private String preProcessForSpecials(String predicate) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "preProcessForSpecials",
               "predicate: " + predicate);     
    String processed = predicate;
   
    String replace = replaceSpecialsWithSub(predicate);

    if (replace != null)
    {
      processed = replace;      
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "preProcessForSpecials", processed);    
    return processed; 
  }         

  /**
   * Handle special character substitution.
   * 
   * @param parsed
   * @throws InvalidXPathSyntaxException
   */
  private void postProcessSelectorTree(Selector parsed, String fullPath)
   throws InvalidXPathSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "postProcessSelectorTree",
               "parsed: " + parsed + ", fullPath: " + fullPath);      
    // Walk the selector tree looking for Identifiers
    if(parsed instanceof IdentifierImpl)
    {
      IdentifierImpl parsedIdentifier = (IdentifierImpl) parsed;
    
      String identName = parsedIdentifier.getName();
      
      // Reinstate any ampersands and slashes that were removed prior to
      // parsing
      String newIdentName = replaceSubForSpecials(identName);

      if (newIdentName != null)
      {
        parsedIdentifier.setName(newIdentName);
        parsedIdentifier.setFullName(fullPath+newIdentName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          tc.debug(this,
                   cclass, 
                   "postProcessSelectorTree", 
                   "Identifier name has been reset to: " + ((IdentifierImpl) parsed).getName());
      }
      else
      {
        // Set the full path name into the identifier
        parsedIdentifier.setFullName(fullPath+identName);
      }
      
      // Set the appropriate XPath parameters for this Identifier
      setXPathCharacteristics((IdentifierImpl) parsed);      
    }
    else if(parsed instanceof OperatorImpl)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        tc.debug(this,
                 cclass, 
                 "postProcessSelectorTree", 
                 parsed + " is an OperatorImpl");            
      
      // Set the extended flag in the Operator
      parsed.setExtended();
      postProcessOperands((OperatorImpl) parsed, fullPath);
    }
    else if(parsed instanceof LiteralImpl)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        tc.debug(this,
                 cclass, 
                 "postProcessSelectorTree", 
                 parsed + " is an LiteralImpl");          
      LiteralImpl literal = (LiteralImpl) parsed;
      
      if(literal.getType() == Selector.STRING)
      {
        // Reinstate any ampersands and slashes that were removed prior to
        // parsing        
        String literalString = (String) literal.getValue();
        String newLiteralString = replaceSubForSpecials(literalString);

        if (newLiteralString != null)
        {
          ((LiteralImpl) parsed).value = newLiteralString;

          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            tc.debug(this,
                     cclass, 
                     "postProcessSelectorTree", 
                     "Literal Value has been reset to: " + ((LiteralImpl) parsed).getValue());          
        }
      }
    }    
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        tc.debug(this,
                 cclass, 
                 "postProcessSelectorTree", 
                 "No post processing can be done on: " + parsed);           
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "postProcessSelectorTree");     
  }           

  /**
   * Traverse an Operator tree handling special character substitution.
   *  
   * @param opImpl
   * @throws InvalidXPathSyntaxException
   */
  private void postProcessOperands(OperatorImpl opImpl, String fullPath)
    throws InvalidXPathSyntaxException  
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "postProcessOperands",
               "opImpl: " + opImpl + ", fullPath: " + fullPath);        
    // Check for Identifiers with ampersand strings
    for(int i = 0; i< opImpl.operands.length; i++)
    {
      Selector sel = opImpl.operands[i];
      postProcessSelectorTree(sel, fullPath);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "postProcessOperands");      
  }             

  /**
   * Replace special characters in the string with substitutions.
   * 
   * @param inString
   * @return
   */
  private String replaceSpecialsWithSub(String inString) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "replaceSpecialsWithSub",
               "inString: " + inString);    
    
    String outString = null;
    StringBuffer sb = null;
    
    // First of all look for ampersands
    int posAmpersand = inString.indexOf("@");
    if(posAmpersand >=0)
    {
      // Need to do some work, the predicate has an attribute char
      sb = new StringBuffer(inString);
    }

    // Locate and replace the ampersands
    while(posAmpersand >=0)
    {
      // Replace the ampersand with "_$AMP$_"
      sb.replace(posAmpersand, posAmpersand + 1, "_$AMP$_");
      int start = posAmpersand + 1;
      posAmpersand = sb.indexOf("@", start);
    }

    // Now look for slashes
    int posSlash = inString.indexOf("/");
    if(posSlash >=0 && sb == null)
    {
      // Need to do some work, the predicate has an attribute char
      sb = new StringBuffer(inString);
    }

    // Locate and replace the slashes
    while(posSlash >=0)
    {
      // Replace the slash with "_$SL$_"
      sb.replace(posSlash, posSlash + 1, "_$SL$_");
      int start = posSlash + 1;
      posSlash = sb.indexOf("/", start);
    }    

    // Now look for doubledots
    int posDD = inString.indexOf("..");
    if(posDD >=0 && sb == null)
    {
      // Need to do some work, the predicate has an attribute char
      sb = new StringBuffer(inString);
    }

    // Locate and replace the doubledots
    while(posDD >=0)
    {
      // Replace the slash with "_$DD$_"
      sb.replace(posDD, posDD + 2, "_$DD$_");
      int start = posDD + 1;
      posDD = sb.indexOf("..", start);
    }        

    // Now look for singledots
    int posSD = -1;
    if(sb != null)
      posSD = sb.indexOf(".");
    else
    {
      posSD = inString.indexOf(".");

      if(posSD >=0)
      {
        // Need to do some work, the predicate has an attribute char
        sb = new StringBuffer(inString);
      }      
    }

    // Locate and replace the singledots
    while(posSD >=0)
    {
      // Move start position to beyond the current dot
      int start = posSD + 1;
      // Need to be a bit careful here, we don't want to replace
      // decimal points
      if( start < sb.length() && Character.isDigit(sb.charAt(posSD+1)))
      {
        // Subsequent char is a digit
        posSD = -1;
      }
      else
      {
        // Test previous char
        if(posSD > 0)
        {
          if( Character.isDigit(sb.charAt(posSD-1)))
          {
            // Prev char is a digit
            posSD = -1;
          }
        }
      }
      
      // Replace the dot with "_$SD$_"      
      if(posSD >= 0)
      {
        sb.replace(posSD, posSD + 1, "_$SD$_");
      }
 
      // Any more dots?
      posSD = sb.indexOf(".", start);
    }            
    
    // We've finished
    if (sb != null)
    {
      outString = sb.toString();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "replaceSpecialsWithSub", outString);     
    return outString; 
  }             

  /**
   * Replace substitutions in the string with the original special characters.
   * 
   * @param inString
   * @return
   */  
  private String replaceSubForSpecials(String inString) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "replaceSubForSpecials",
               "inString: " + inString); 
    
    String outString = null;
    StringBuffer sb = null;

    // First deal with ampersands
    int posAmpersand = inString.indexOf("_$AMP$_");
    
    if(posAmpersand >=0)
    {
      // Need to do some work, the string has an attribute char
      sb = new StringBuffer(inString);
    }

    // Locate and replace the ampersand substitute
    while(posAmpersand >=0)
    {
      // Replace the "_$AMP$_" with an ampersand
      sb.replace(posAmpersand, posAmpersand + 7, "@");
      int start = posAmpersand + 1;
      posAmpersand = sb.indexOf("_$AMP$_", start);      
    }

    // Now deal with slashes
    int posSlash = inString.indexOf("_$SL$_");
    
    if(posSlash >=0 && sb == null)
    {
      // Need to do some work, the string has a slash char
      sb = new StringBuffer(inString);
    }

    // Locate and replace the slash substitute
    while(posSlash >=0)
    {
      // Replace the "_$SL$_" with a slash
      sb.replace(posSlash, posSlash + 6, "/");
      int start = posSlash + 1;
      posSlash = sb.indexOf("_$SL$_", start);      
    }    

    // Now deal with doubledots
    int posDD = inString.indexOf("_$DD$_");
    
    if(posDD >=0 && sb == null)
    {
      // Need to do some work, the string has a slash char
      sb = new StringBuffer(inString);
    }

    // Locate and replace the doubledot substitute
    while(posDD >=0)
    {
      // Replace the "_$DD$_" with a doubledot
      sb.replace(posDD, posDD + 6, "..");
      int start = posDD + 2;
      posDD = sb.indexOf("_$DD$_", start);      
    }        

    // Now deal with singledots
    int posSD = inString.indexOf("_$SD$_");
    
    if(posSD >=0 && sb == null)
    {
      // Need to do some work, the string has a slash char
      sb = new StringBuffer(inString);
    }

    // Locate and replace the singledot substitute
    while(posSD >=0)
    {
      // Replace the "_$SD$_" with a dot
      sb.replace(posSD, posSD + 6, ".");
      int start = posSD + 1;
      posSD = sb.indexOf("_$SD$_", start);      
    }            
    
    // We're finished
    if (sb != null)
    {
      outString = sb.toString();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "replaceSubForSpecials", outString); 
    return outString;
  }      
  
  /**
   * Work through a selector string character by character in order to isolate a
   * location step.
   * 
   * If it is determined that the current location step cannot be successfully parsed
   * by the MatchParser, then the wrapWholeStep boolean is set in the stepProperties
   * parameter.
   * 
   * @param selector
   * @param start
   * @param stepProperties
   */
  private void isolateNextStep(String selector,
                               int start,
                               StepProperties stepProperties) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "isolateNextStep",
               "selector: " + selector + ", start: " + start);    
    int end = -1;

    boolean wrapWholeStep = false;

    String subSelector = selector.substring(start);
    char[] chars = subSelector.toCharArray();

    // Is a full stop acceptable - at this stage in processing we should only
    // have decimal points left. Might want to stop testing this.
    boolean acceptDot = false;
    // If we locate a * we assume that we'll need to wrap the whole location step. We
    // cannot currently distinguish between single levelwildcards and multiplication signs.
    boolean acceptStar = true;
    // becomes false on non-separator, true on separator
    boolean acceptOrdinary = true;
    // becomes true on separator
    boolean prevSeparator = true;

    // Square bracket related
    boolean acceptOpenBracket = true;
    boolean acceptCloseBracket = false;
    int openBracketCount = 0;
    
    boolean acceptOpenRoundBracket = false;
    boolean acceptCloseRoundBracket = false;
    int openRoundBracketCount = 0;
    //boolean withinPredicate = false;
    boolean maybeIntegerPredicate = false;
    
    // Process the string character by character
    try
    {
      int subSelectorLength = chars.length;
      for (int i = 0; i < subSelectorLength; i++)
      {
        char cand = chars[i];
 
        // First lets attempt to catch the case where we have a position predicate 
        if(openBracketCount > 0 && maybeIntegerPredicate)
        {
          // If we're in a predicate check to see if its integral
          if(!Character.isDigit(cand) &&  cand != ']')
          {
            maybeIntegerPredicate = false;
          }
        }
      
        if (cand == MatchSpace.SUBTOPIC_MATCHONE_CHAR)
        {
          if (acceptStar)
          {
            wrapWholeStep = true;
            acceptStar = false;
            //acceptOrdinary = false;
            acceptDot = false;
            prevSeparator = false;
          }
          else
          {
            throw new QuerySyntaxException("Wilcard error: " + subSelector + ", at: " + (i+1));
          }
        }
        else if (cand == MatchSpace.SUBTOPIC_STOP_CHAR)
        {
          if (acceptDot)
          {
            if(prevSeparator)
            {
              acceptOrdinary = false;
            }
            else
            {
              acceptOrdinary = true;
            }
            acceptStar = prevSeparator = false;
          }
          else
          {
            throw new QuerySyntaxException("Dot error: " + subSelector + ", at: " + (i+1));
          }
        }
        else if (cand == MatchSpace.SUBTOPIC_SEPARATOR_CHAR)
        {
          // Key bit of code here
          // Is the separator within brackets? Is it part of a wildcard? or have
          // we determined the  end of a location step?
          if(openBracketCount == 0 && openRoundBracketCount == 0)
          {
            // This is the end of the location step
            end = i;
            break;
          }
          acceptStar=true;
          acceptOrdinary = true;
          acceptDot = true;
          prevSeparator = true;
        }
        else if( cand == '[')
        {
          if (acceptOpenBracket)
          {
            // Start of a predicate
            acceptStar = true;
            acceptOrdinary = true;
            prevSeparator = false;
            acceptDot = true;
            openBracketCount++;
            // Check for nested predicates
            if(openBracketCount>1)
            {
              wrapWholeStep = true;
            }
            //acceptOpenBracket = false;
            acceptCloseBracket = true;
            acceptOpenRoundBracket = true;
            maybeIntegerPredicate = true;
          }
          else
          {
            throw new QuerySyntaxException("Open Bracket error: " + subSelector + ", at: " + (i+1));
          }
        }
        else if( cand == ']')
        {
          if (acceptCloseBracket)
          {
            // End of a predicate
            acceptStar = true;
            acceptOrdinary = true;
            prevSeparator = false;
            acceptDot = true;
            acceptOpenBracket = true;
            openBracketCount--;
            if(openBracketCount < 0)
            {
              throw new QuerySyntaxException("Bracket error: " + subSelector + ", at: " + (i+1));
            }            
            //acceptCloseBracket = false;
            // Test to see whether the predicate was entirely numeric
            // Looking for department[2], etc
            if(maybeIntegerPredicate)
            {
              maybeIntegerPredicate = false;
              wrapWholeStep = true;
            }
          }
          else
          {
            throw new QuerySyntaxException("Close Bracket error: " + subSelector + ", at: " + (i+1));
          }
        }
        else if( cand == '(')
        {
          if (acceptOpenRoundBracket)
          {
            // A function
            wrapWholeStep = true;
            acceptStar = true;
            acceptOrdinary = true;
            prevSeparator = false;
            acceptDot = true;
            acceptOpenRoundBracket = false;
            acceptCloseRoundBracket = true;
            openRoundBracketCount++;
          }
          else
          {
            throw new QuerySyntaxException("Open Round Bracket error: " + subSelector + ", at: " + (i+1));
          }
        }
        else if( cand == ')')
        {
          if (acceptCloseRoundBracket)
          {
            // End of a predicate
            acceptStar = true;
            acceptOrdinary = true;
            prevSeparator = false;
            acceptDot = true;
            acceptOpenRoundBracket = true;
            acceptCloseRoundBracket = false;
            openRoundBracketCount--;
          }
          else
          {
            throw new QuerySyntaxException("Close Round Bracket error: " + subSelector + ", at: " + (i+1));
          }
        }      
        else // Ordinary character
        {
          if (acceptOrdinary)
          {
            acceptStar = true;
            prevSeparator = false;
            acceptDot = true;
            acceptOpenRoundBracket = true;
          }
          else
          {
            throw new QuerySyntaxException("Character error: " + subSelector + ", at: " + (i+1));
          }
        }
      }
      
      // Finished processing char by char, did the brackets pair up ok?
      if(openBracketCount != 0)
      {
        throw new QuerySyntaxException("Matching Bracket error: " + subSelector + ", non-matching square brackets");
      }      
    }
    catch (QuerySyntaxException qex)
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.
      // This signals that the character parsing encountered a problem, we FFDC
      // but continue processing
      FFDC.processException(cclass,
        "com.ibm.ws.sib.matchspace.selector.impl.XPath10ParserImpl",
        qex,
        "1:1377:1.16");
 
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        tc.debug(this,cclass, "isolateNextStep", qex);         
    }
    
    // Set the StepProperties
    if(wrapWholeStep)
      stepProperties.wrapStep = true;
    else
      stepProperties.wrapStep = false;
    
    // Set the end of step value
    if(end == -1)
      stepProperties.endOfStep = -1;
    else
    {
      stepProperties.endOfStep = end+start;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "isolateNextStep");     
  }

  /**
   * Wrap a location step in an Identifier.
   * 
   * @param selector
   * @param start
   * @param stepEnd
   * @throws InvalidXPathSyntaxException
   */
  private void wrapLocationStep(String selector,
                                int start,
                                int stepEnd)
    throws InvalidXPathSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "wrapLocationStep",
               "selector: " + selector + ", start: " + start + ", stepEnd: " + stepEnd);
    
    String step = selector.substring(start,stepEnd);
    String full =  selector.substring(0,stepEnd);
    // Add an IdentifierImpl for the location step to the array list
    selOperands.add(createIdentifierForSubExpression(step, full, true, false)); 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "wrapLocationStep");    
  }  

  /**
   * Wrap the last location step in an Identifier.
   * 
   * @param selector
   * @param start
   * @param bumpLocationStep
   * @throws InvalidXPathSyntaxException
   */
  private void wrapLastLocationStep(String selector,
                                    int start,
                                    boolean bumpLocationStep)
    throws InvalidXPathSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.entry(cclass,
               "wrapLastLocationStep",
               "selector: " + selector + ", start: " + start + 
               ", bumpLocationStep: " + new Boolean(bumpLocationStep));
    
    int stepEnd = selector.length();
    String step =  selector.substring(start,stepEnd);
    String full =  selector.substring(0,stepEnd);
    // Add an IdentifierImpl for the location step to the array list
    selOperands.add(createIdentifierForSubExpression(step, full, bumpLocationStep, true));
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      tc.exit(this,cclass, "wrapLastLocationStep");    
  }


  public void setMatching(Matching matching) 
  {
    this.matching = matching;
  }

  public void setNamespaceMappings(Map namespaceMappings)
  {
    if(namespaceMappings != null)
    {
      namespaceContext = new XPathNamespaceContext(namespaceMappings);
    }
    else
      namespaceContext = null;
   
  }
  
  public static class XPathNamespaceContext implements NamespaceContext
  {
    Map prefixMap = null;
    
    public XPathNamespaceContext(Map namespaceMappings)
    {
      prefixMap = namespaceMappings;     
    }
      
    public String getNamespaceURI(String prefix)
    {
      if(prefixMap != null)
      {
        String namespace = (String)prefixMap.get(prefix);
        if(namespace != null)
          return namespace;
        else
          return XMLConstants.NULL_NS_URI;
      }
      else
        return XMLConstants.NULL_NS_URI;
    }
      
    public String getPrefix(String namespace)
    {
      return null;
    }

    public Iterator getPrefixes(String namespace)
    {
      return null;
    }
  }
}
