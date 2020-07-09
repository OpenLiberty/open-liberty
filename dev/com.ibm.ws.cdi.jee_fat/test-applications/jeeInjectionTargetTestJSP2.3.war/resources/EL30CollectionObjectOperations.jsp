<%@ page import="java.util.Collection"%>
<%@ page import="com.ibm.ws.cdi.vistest.masked.beans.EL30MapCollectionObjectBean"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.Set"%>
<%@ page import="java.util.HashSet"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Iterator" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>JSP to test the EL 3.0 Operations on Collection Objects</title>
</head>
<body>

	<%! 
		// List to be used as a collection object
		List<Integer> list = new ArrayList<Integer>() {
        	{
            	add(1);
            	add(4);
           		add(3);
            	add(2);
            	add(5);
            	add(3);
            	add(1);
        	}
    	}; 
    	
    	// Set to be used as a collection object
    	Set<Integer> set = new HashSet<Integer>() {
            {
                add(1);
                add(4);
                add(3);
                add(5);
                add(2);
            }
        };
        
        // Nested set to be used as a collection object for the flatMap operation
        Set<Set<String>> nestedSet = new HashSet<Set<String>>() {
        	{
        		add(new HashSet<String>() {
        			{
        				add("1");
        				add("4");
        			}
        		});
                add(new HashSet<String>(){
                	{
                		add("3");
                	}
                });
                add(new HashSet<String>(){
                	{
                		add("2");
                	}
                });
                add(new HashSet<String>(){
                	{
                		add("5");
                		add("3");
                		add("1");
                	}
                });
        	}
        };
        
     	// Map to be used as a collection object
        Map<Integer, String> map = new HashMap<Integer, String>() {
            {
                put(1, "1");
                put(2, "4");
                put(3, "3");
                put(4, "2");
                put(5, "5");
                put(6, "3");
                put(7, "1");
            }
        };
	 %>
     <%
        final EL30MapCollectionObjectBean[] cObjectOp = new EL30MapCollectionObjectBean[] { new EL30MapCollectionObjectBean(),
                                                                                           new EL30MapCollectionObjectBean(),
                                                                                           new EL30MapCollectionObjectBean() }; 
        cObjectOp[0].setMap(map);
        cObjectOp[1].setMap(map);
        cObjectOp[2].setMap(map);
	 	
        // Nested map to be used as a collection object for the flatMap operation
	    Map<Integer, EL30MapCollectionObjectBean> nestedMap = new HashMap<Integer, EL30MapCollectionObjectBean>();
	    nestedMap.put(1, cObjectOp[0]);
	    nestedMap.put(2, cObjectOp[1]);
	    nestedMap.put(3, cObjectOp[2]);
	    
 		// Obtain the stream by getting a collection view of the map
    	Collection<String> collectionMap = map.values();
    	Collection<EL30MapCollectionObjectBean> collectionNestedMap = nestedMap.values();
    	
    	// Add each object to be used to the request
    	request.setAttribute("list", list); 
    	request.setAttribute("set", set);
    	request.setAttribute("nestedSet", nestedSet);
    	request.setAttribute("map", collectionMap);
        request.setAttribute("nestedMap", collectionNestedMap);
        request.setAttribute("out", out);
    %>
	
	<%-- Test the EL 3.0 operations on collection objects depending on the parameter that it is being passed. --%>
	<% if (request.getParameterMap().containsKey("testListCollectionOperations")) { %>
		Original List: ${list}
		<br/>
	<%	try { 
			if(request.getParameter("testListCollectionOperations").equals("filter")) { %>
				<%-- Get all the elements greater or  equal than 3 --%>
				Filter: ${list.stream().filter(e -> e >= 3).toList()}
	<% 		}
			else if(request.getParameter("testListCollectionOperations").equals("map")) { %>
			    <%-- Add 2 to each element of the list --%>
				Map: ${list.stream().map(e -> e + 2).toList()}
	<% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("flatMap")) { %>
				<%-- Flat the list --%>
				FlatMap: ${[[1, 4], [3], [2], [5, 3, 1]].stream().flatMap(e -> e.stream()).toList()}
    <% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("distinct")) { %>
				<%-- Get distinct values from the list --%>
				Distinct: ${list.stream().distinct().toList()}
	<% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("sorted")) { %>
				<%-- Sort the list in decreasing order --%>
				Sorted in Decreasing: ${list.stream().sorted((i, j) -> j-i).toList()}
	<% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("forEach")) { %>
				<%-- To print a list of integers --%>
				ForEach: ${list.stream().forEach(e -> out.print(e))}
	<% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("peek")) { %>
				<%-- To print the a list of integer before and after a filter. It filters even values. --%>
				Debug Peek: ${list.stream().peek(e -> out.print(e)).filter(e -> e%2 == 0).peek(e -> out.print(e)).toList()}
	<% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("iterator")) { %>
				<%-- This method returns an iterator for the source stream, suitable for use in Java codes --%>
				<c:set var="iter" value="${list.stream().iterator()}" scope="request"/> 
				<%
                	Iterator<Integer> iterator = (Iterator<Integer>) request.getAttribute("iter");
                	out.print("Iterator: ");
                	while (iterator.hasNext()) {
                    	out.print(iterator.next() + " ");
                	}
				%>
	<% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("limit")) { %>
				<%-- Get the first 3 elements in the list --%>
				Limit: ${list.stream().limit(3).toList()}
	<% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("substream")) { %>
				<%-- Get a sub list out of the entire list --%>
				Substream: ${list.stream().substream(2, 4).toList()}
	<% 		 }
			else if (request.getParameter("testListCollectionOperations").equals("toArray")) { %>
				<%-- Get an array containing the elements of the source stream. --%>
				<c:set var="obj" value="${list.stream().toArray()}" scope="request"/> 
				<%
					out.print("ToArray: " + Arrays.toString((Object[]) request.getAttribute("obj")));
				%>
	<% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("toList")) { %>
				<%-- Get a list containing the elements of the source stream --%>
				ToList: ${list.stream().toList()}
	<% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("reduce")) { %>
				<%-- Find the largest number in the list --%>
				Reduce: ${list.stream().reduce((p, q) -> p > q ? p:q).get()}
	<% 		}
			else if (request.getParameter("testListCollectionOperations").equals("max")) { %>
				<%-- Find the maximum number in the list --%>
				Max: ${list.stream().max().get()}
	<% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("min")) { %>
				<%-- Find the minimum number in the list --%>
				Min: ${list.stream().min().get()}
	<% 		} 
			else if (request.getParameter("testListCollectionOperations").equals("average")) { %>
				<%-- Find the average value of the list --%>
				Average: ${list.stream().average().get()}
	<%		} 
			else if (request.getParameter("testListCollectionOperations").equals("sum")) { %>
				<%-- Find the sum of the list --%>
				Sum: ${list.stream().sum()}
	<%		} 
			else if (request.getParameter("testListCollectionOperations").equals("count")) { %>
				<%-- Find the amount of element in the list --%>
				Count: ${list.stream().count()}
	<%		} 
			else if (request.getParameter("testListCollectionOperations").equals("anyMatch")) { %>
				<%-- Check if the list contains any element smaller than 2 --%>
				AnyMatch: ${list.stream().anyMatch(e -> e < 2).orElse(false)}
	<%		} 
			else if (request.getParameter("testListCollectionOperations").equals("allMatch")) { %>
				<%-- Check if the all the elements in the list are smaller than 4  --%>
				AllMatch: ${list.stream().allMatch(e -> e < 4).orElse(false)}
	<%		} 
			else if (request.getParameter("testListCollectionOperations").equals("noneMatch")) { %>
				<%-- Check if the none of the elements are greater than 5 --%>
				NoneMatch: ${list.stream().noneMatch(e -> e > 5).orElse(false)}
	<%		} 
			else if (request.getParameter("testListCollectionOperations").equals("findFirst")) { %>
				<%-- Get the first element of the list --%>
				FindFirst: ${list.stream().findFirst().get()}
	<%		} 
			else {
				out.print("Invalid parameter");
	   		} 
		} catch(Exception e) {
			out.println("Exception caught: " + e.getMessage() + "<br/>");
            out.println("Test Failed. An exception was thrown: " + e.toString() + "<br/>");
		}
	} else if (request.getParameterMap().containsKey("testSetCollectionOperations")) { %>
	<%	try { 
			if(request.getParameter("testSetCollectionOperations").equals("filter")) { %>
				<%-- Get all the elements greater or  equal than 3 --%>
				Filter: ${set.stream().filter(e -> e >= 3).toList()}
	<% 		}
			else if(request.getParameter("testSetCollectionOperations").equals("map")) { %>
			    <%-- Add 2 to each element of the list --%>
				Map: ${set.stream().map(e -> e + 2).toList()}
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("flatMap")) { %>
				<%-- Flat the list --%>
				FlatMap: ${nestedSet.stream().flatMap(e -> e.stream()).toList()}
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("distinct")) { %>
				<%-- Get distinct values from the list --%>
				Distinct: ${set.stream().distinct().toList()}
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("sorted")) { %>
				<%-- Sort the list in decreasing order --%>
				Sorted in Decreasing: ${set.stream().sorted((i, j) -> j-i).toList()}
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("forEach")) { %>
				<%-- To print a list of integers --%>
				ForEach: ${set.stream().forEach(e -> out.print(e))}
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("peek")) { %>
				<%-- To print the a list of integer before and after a filter. It filters even values. --%>
				Debug Peek: ${set.stream().peek(e -> out.print(e)).filter(e -> e%2 == 0).peek(e -> out.print(e)).toList()}
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("iterator")) { %>
				<%-- This method returns an iterator for the source stream, suitable for use in Java codes --%>
				<c:set var="iter" value="${set.stream().iterator()}" scope="request"/> 
				<%
	            	Iterator<Integer> iterator = (Iterator<Integer>) request.getAttribute("iter");
	            	out.print("Iterator: ");
	            	while (iterator.hasNext()) {
	                	out.print(iterator.next() + " ");
	            	}
				%>
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("limit")) { %>
				<%-- Get the first 3 elements in the list --%>
				Limit: ${set.stream().limit(3).toList()}
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("substream")) { %>
				<%-- Get a sub list out of the entire list --%>
				Substream: ${set.stream().substream(2, 4).toList()}
	<% 		 }
			else if (request.getParameter("testSetCollectionOperations").equals("toArray")) { %>
				<%-- Get an array containing the elements of the source stream. --%>
				<c:set var="obj" value="${set.stream().toArray()}" scope="request"/> 
				<%
					out.print("ToArray: " + Arrays.toString((Object[]) request.getAttribute("obj")));
				%>
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("toList")) { %>
				<%-- Get a list containing the elements of the source stream --%>
				ToList: ${set.stream().toList()}
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("reduce")) { %>
				<%-- Find the largest number in the list --%>
				Reduce: ${set.stream().reduce((p, q) -> p > q ? p:q).get()}
	<% 		}
			else if (request.getParameter("testSetCollectionOperations").equals("max")) { %>
				<%-- Find the maximum number in the list --%>
				Max: ${set.stream().max().get()}
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("min")) { %>
				<%-- Find the minimum number in the list --%>
				Min: ${set.stream().min().get()}
	<% 		} 
			else if (request.getParameter("testSetCollectionOperations").equals("average")) { %>
				<%-- Find the average value of the list --%>
				Average: ${set.stream().average().get()}
	<%		} 
			else if (request.getParameter("testSetCollectionOperations").equals("sum")) { %>
				<%-- Find the sum of the list --%>
				Sum: ${set.stream().sum()}
	<%		} 
			else if (request.getParameter("testSetCollectionOperations").equals("count")) { %>
				<%-- Find the amount of element in the list --%>
				Count: ${set.stream().count()}
	<%		} 
			else if (request.getParameter("testSetCollectionOperations").equals("anyMatch")) { %>
				<%-- Check if the list contains any element smaller than 2 --%>
				AnyMatch: ${set.stream().anyMatch(e -> e < 2).orElse(false)}
	<%		} 
			else if (request.getParameter("testSetCollectionOperations").equals("allMatch")) { %>
				<%-- Check if the all the elements in the list are smaller than 4  --%>
				AllMatch: ${set.stream().allMatch(e -> e < 4).orElse(false)}
	<%		} 
			else if (request.getParameter("testSetCollectionOperations").equals("noneMatch")) { %>
				<%-- Check if the none of the elements are greater than 5 --%>
				NoneMatch: ${set.stream().noneMatch(e -> e > 5).orElse(false)}
	<%		} 
			else if (request.getParameter("testSetCollectionOperations").equals("findFirst")) { %>
				<%-- Get the first element of the list --%>
				FindFirst: ${set.stream().findFirst().get()}
	<%		} 
			else {
				out.print("Invalid parameter");
	   		} 
		} catch(Exception e) {
			out.println("Exception caught: " + e.getMessage() + "<br/>");
	        out.println("Test Failed. An exception was thrown: " + e.toString() + "<br/>");
		}
	} else if (request.getParameterMap().containsKey("testMapCollectionOperations")) { %>
		Original Map: ${map}
		<br/>
	<%	try { 
			if(request.getParameter("testMapCollectionOperations").equals("filter")) { %>
				<%-- Get all the elements greater or  equal than 3 --%>
				Filter: ${map.stream().filter(e -> e >= 3).toList()}
	<% 		}
			else if(request.getParameter("testMapCollectionOperations").equals("map")) { %>
			    <%-- Add 2 to each element of the list --%>
				Map: ${map.stream().map(e -> e + 2).toList()}
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("flatMap")) { %>
				<%-- Flat the list --%>
				FlatMap: ${nestedMap.stream().flatMap(e -> e.map.stream()).toList()}
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("distinct")) { %>
				<%-- Get distinct values from the list --%>
				Distinct: ${map.stream().distinct().toList()}
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("sorted")) { %>
				<%-- Sort the list in decreasing order --%>
				Sorted in Decreasing: ${map.stream().sorted((i, j) -> j-i).toList()}
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("forEach")) { %>
				<%-- To print a list of integers --%>
				ForEach: ${map.stream().forEach(e -> out.print(e))}
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("peek")) { %>
				<%-- To print the a list of integer before and after a filter. It filters even values. --%>
				Debug Peek: ${map.stream().peek(e -> out.print(e)).filter(e -> e%2 == 0).peek(e -> out.print(e)).toList()}
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("iterator")) { %>
				<%-- This method returns an iterator for the source stream, suitable for use in Java codes --%>
				<c:set var="iter" value="${map.stream().iterator()}" scope="request"/> 
				<%
	            	Iterator<Integer> iterator = (Iterator<Integer>) request.getAttribute("iter");
	            	out.print("Iterator: ");
	            	while (iterator.hasNext()) {
	                	out.print(iterator.next() + " ");
	            	}
				%>
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("limit")) { %>
				<%-- Get the first 3 elements in the list --%>
				Limit: ${map.stream().limit(3).toList()}
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("substream")) { %>
				<%-- Get a sub list out of the entire list --%>
				Substream: ${map.stream().substream(2, 4).toList()}
	<% 		 }
			else if (request.getParameter("testMapCollectionOperations").equals("toArray")) { %>
				<%-- Get an array containing the elements of the source stream. --%>
				<c:set var="obj" value="${map.stream().toArray()}" scope="request"/> 
				<%
					out.print("ToArray: " + Arrays.toString((Object[]) request.getAttribute("obj")));
				%>
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("toList")) { %>
				<%-- Get a list containing the elements of the source stream --%>
				ToList: ${map.stream().toList()}
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("reduce")) { %>
				<%-- Find the largest number in the list --%>
				Reduce: ${map.stream().reduce((p, q) -> p > q ? p:q).get()}
	<% 		}
			else if (request.getParameter("testMapCollectionOperations").equals("max")) { %>
				<%-- Find the maximum number in the list --%>
				Max: ${map.stream().max().get()}
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("min")) { %>
				<%-- Find the minimum number in the list --%>
				Min: ${map.stream().min().get()}
	<% 		} 
			else if (request.getParameter("testMapCollectionOperations").equals("average")) { %>
				<%-- Find the average value of the list --%>
				Average: ${map.stream().average().get()}
	<%		} 
			else if (request.getParameter("testMapCollectionOperations").equals("sum")) { %>
				<%-- Find the sum of the list --%>
				Sum: ${map.stream().sum()}
	<%		} 
			else if (request.getParameter("testMapCollectionOperations").equals("count")) { %>
				<%-- Find the amount of element in the list --%>
				Count: ${map.stream().count()}
	<%		} 
			else if (request.getParameter("testMapCollectionOperations").equals("anyMatch")) { %>
				<%-- Check if the list contains any element smaller than 2 --%>
				AnyMatch: ${map.stream().anyMatch(e -> e < 2).orElse(false)}
	<%		} 
			else if (request.getParameter("testMapCollectionOperations").equals("allMatch")) { %>
				<%-- Check if the all the elements in the list are smaller than 4  --%>
				AllMatch: ${map.stream().allMatch(e -> e < 4).orElse(false)}
	<%		} 
			else if (request.getParameter("testMapCollectionOperations").equals("noneMatch")) { %>
				<%-- Check if the none of the elements are greater than 5 --%>
				NoneMatch: ${map.stream().noneMatch(e -> e > 5).orElse(false)}
	<%		} 
			else if (request.getParameter("testMapCollectionOperations").equals("findFirst")) { %>
				<%-- Get the first element of the list --%>
				FindFirst: ${map.stream().findFirst().get()}
	<%		} 
			else {
				out.print("Invalid parameter");
	   		} 
		} catch(Exception e) {
			out.println("Exception caught: " + e.getMessage() + "<br/>");
	        out.println("Test Failed. An exception was thrown: " + e.toString() + "<br/>");
		}
	} 
	%>
	
</body>
</html>