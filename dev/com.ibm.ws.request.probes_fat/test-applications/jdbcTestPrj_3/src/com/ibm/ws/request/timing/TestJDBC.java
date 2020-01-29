package com.ibm.ws.request.timing;


import java.io.*;
import java.sql.*;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.sql.DataSource;

@WebServlet("/*")
public class TestJDBC extends HttpServlet
{
  private static final long serialVersionUID = 1L;

  @Resource(name="jdbc/exampleDS")
  DataSource ds1;

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    Statement stmt = null;

    Connection con = null;
    int sleepTimeInMilliSecs = 12000;
    
    try {
        
        if (request.getParameter("sleepTime") != null) {
                sleepTimeInMilliSecs = Integer.parseInt(request
                                .getParameter("sleepTime"));
        }
      PrintWriter pw = response.getWriter();
      System.out.println("Create DataSource connection");
      con = this.ds1.getConnection();

      stmt = con.createStatement();

      stmt.executeUpdate("create table cities (name varchar(50) not null, population int, county varchar(30))");
      
      for(int i = 11; i <= 15; i++) {
          stmt.executeUpdate((new StringBuilder("insert into cities values ('myHomeCity_ ")).append(i).append("', ").append(i).append(", 'myHomeCounty_").append(i).append("')").toString());
      }
      
      System.out.println("doGet completed Successfully");
    } catch (Exception e) {
      e.printStackTrace();
      try
      {
        if (stmt != null)
          stmt.executeUpdate("drop table cities");
        else
          System.out.println("stmt is null");
      }
      catch (SQLException e1)
      {
        e1.printStackTrace();
      }
      try {
        if (con != null)
          con.close();
      } catch (SQLException e2) {
        e2.printStackTrace();
      }
    }
    finally
    {
      try
      {
        if (stmt != null)
          stmt.executeUpdate("drop table cities");
        else
          System.out.println("stmt is null");
      }
      catch (SQLException e)
      {
        e.printStackTrace();
      }
      try {
        if (con != null)
          con.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }

	  Stock myFavStock = new Stock("Stock1", new Double(100D));
        HttpSession cSession = request.getSession(true);
        cSession.setAttribute("FavStockC", myFavStock);
        Stock s = (Stock)cSession.getAttribute("FavStockC");


        System.out.println((new StringBuilder(" Session value is ")).append(s.getValue()).toString());

        try
        {
            System.out.println("Thread sleeping for:" + sleepTimeInMilliSecs);
            Thread.sleep(sleepTimeInMilliSecs);
            System.out.println("Thread woke up");
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
        System.out.println("%%%%%%%%%%% Completed session set");
        return;
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
  }
}
