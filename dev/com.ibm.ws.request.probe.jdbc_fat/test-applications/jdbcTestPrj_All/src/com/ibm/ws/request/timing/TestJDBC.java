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
    PreparedStatement pstmt = null;
    ResultSet result = null;

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
      
      for(int i = 11; i <= 13; i++) {
          stmt.executeUpdate((new StringBuilder("insert into cities values ('myHomeCity_")).append(i).append("', ").append(i).append(", 'myHomeCounty_").append(i).append("')").toString());
      }
      
      System.out.println("doGet completed Successfully");
      
      stmt.execute("delete from cities where population=13");
      result = stmt.executeQuery("select population from cities");
      result.close();
       
    } catch (Exception e) {
      e.printStackTrace(); 
    }
   
      try {
          
          String sql ="insert into cities values (?, ?, ?)";
          pstmt = con.prepareStatement(sql);
          
          pstmt.setString(1, "myHomeCity_20");
          pstmt.setInt(2, 20);
          pstmt.setString(3, "myHomeCounty_20");
          pstmt.executeUpdate();
          pstmt.close();
         
         sql = "select name, population from cities where population > 10";
         pstmt = con.prepareStatement(sql);
         result = pstmt.executeQuery();
         result.close();
         pstmt.close();
         
         
         sql = ("delete from cities where population=12");
         pstmt = con.prepareStatement(sql);
          pstmt.execute();
          pstmt.close();
         
         
         stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
         
          result = stmt.executeQuery("select name,population,county from cities");
         if(result.next()) {
             result.moveToInsertRow();
             result.updateString(1, "newCity");
             result.updateInt(2, 200);
             result.updateString(3, "newCity40");
             result.insertRow();
             result.moveToCurrentRow();
             result.updateInt("population", 22);
             result.updateRow();
             result.cancelRowUpdates();
             result.deleteRow();
         }
         result.close();
        
      }
      catch(Exception e ) {
          e.printStackTrace();
      }
      finally {
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
      }
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

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
  }
}
