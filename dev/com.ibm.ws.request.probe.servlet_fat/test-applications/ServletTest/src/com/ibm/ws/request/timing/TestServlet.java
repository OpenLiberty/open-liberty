package com.ibm.ws.request.timing;


import java.io.*;
import java.sql.*;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.sql.DataSource;

@WebServlet("/*")
public class TestServlet extends HttpServlet
{
  private static final long serialVersionUID = 1L;

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {    
      int sleepTimeInMilliSecs = 12000;
      
      try {
          
          if (request.getParameter("sleepTime") != null) {
                  sleepTimeInMilliSecs = Integer.parseInt(request.getParameter("sleepTime"));
           }
          
          Stock myFavStock = new Stock("Stock1", new Double(100D));
          HttpSession cSession = request.getSession(true);
          cSession.setAttribute("FavStockC", myFavStock);
          Stock s = (Stock)cSession.getAttribute("FavStockC");

          System.out.println((new StringBuilder(" Session value is ")).append(s.getValue()).toString());
          

          
          System.out.println("Thread sleeping for:" + sleepTimeInMilliSecs);
          Thread.sleep(sleepTimeInMilliSecs);
          System.out.println("Thread woke up");
          
        }catch(Exception e) {
           e.printStackTrace();
       }
          
          System.out.println("%%%%%%%%%%% Completed session set");
       
    
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
  }
}
