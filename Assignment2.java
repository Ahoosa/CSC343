/* 
 * This code is provided solely for the personal and private use of students 
 * taking the CSC343H course at the University of Toronto. Copying for purposes 
 * other than this use is expressly prohibited. All forms of distribution of 
 * this code, including but not limited to public repositories on GitHub, 
 * GitLab, Bitbucket, or any other online platform, whether as given or with 
 * any changes, are expressly prohibited. 
*/ 

import java.sql.*;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math;

public class Assignment2 {
   /////////
   // DO NOT MODIFY THE VARIABLE NAMES BELOW.
   
   // A connection to the database
   Connection con;

   // Can use if you wish: seat letters
   List<String> seatLetters = Arrays.asList("A", "B", "C", "D", "E", "F");

   Assignment2() throws SQLException {
      try {
         Class.forName("org.postgresql.Driver");
      } catch (ClassNotFoundException e) {
         e.printStackTrace();
      }
   }

  /**
   * Connects and sets the search path.
   *
   * Establishes a connection to be used for this session, assigning it to
   * the instance variable 'connection'.  In addition, sets the search
   * path to 'air_travel, public'.
   *
   * @param  url       the url for the database
   * @param  username  the username to connect to the database
   * @param  password  the password to connect to the database
   * @return           true if connecting is successful, false otherwise
   */
   public boolean connectDB(String URL, String username, String password) {

    try{
      con = DriverManager.getConnection(URL, username, password);
      String setPath = "set search_path to air_travel,public ";
      PreparedStatement pstat = con.prepareStatement(setPath);
      pstat.executeUpdate();
      return true;
    } 
    catch (SQLException se){

      System.err.println("SQL Exception." + "<Message>: " + se.getMessage()); 
    }
    return false;

   }

  /**
   * Closes the database connection.
   *
   * @return true if the closing was successful, false otherwise
   */
  public boolean disconnectDB() {
      // Implement this method!
    try{
      con.close();
      return true;
    }
    catch(SQLException se){
    System.err.println("SQL Exception." + "<Message>: " + se.getMessage());
    }
    return false;
  }
   
   /* ======================= Airline-related methods ======================= */

   /**
    * Attempts to book a flight for a passenger in a particular seat class. 
    * Does so by inserting a row into the Booking table.
    *
    * Read handout for information on how seats are booked.
    * Returns false if seat can't be booked, or if passenger or flight cannot be found.
    *
    * 
    * @param  passID     id of the passenger
    * @param  flightID   id of the flight
    * @param  seatClass  the class of the seat (economy, business, or first) 
    * @return            true if the booking was successful, false otherwise. 
    */
   public boolean bookSeat(int passID, int flightID, String seatClass) {

      // Implement this method!
    
    try{
      String insert = "INSERT INTO booking VALUES(?,?,?, ?, ?, ?::seat_class, ?, ?)";
      String capacity = "select flight.id, plane,capacity_first,capacity_business, capacity_economy from flight join plane on flight.plane=plane.tail_number where flight.id= ?";
      PreparedStatement ps=con.prepareStatement(capacity);
      ps.setInt(1,flightID);     
      ResultSet cap = ps.executeQuery();
      cap.next();

      
      String passCount = "select count(*) as count from booking where seat_class= ?::seat_class and flight_id= ?";
      ps=con.prepareStatement(passCount);
      ps.setString(1,seatClass); 
      ps.setInt(2,flightID); 
      ResultSet pCount = ps.executeQuery();
      pCount.next();

      String maxBookID="SELECT id FROM booking WHERE id=(SELECT max(id) FROM booking)";
      ps =con.prepareStatement(maxBookID);
      ResultSet lastBooking = ps.executeQuery();
      lastBooking.next();

      String priceRow="SELECT * FROM price where flight_id= ?";
      ps =con.prepareStatement(priceRow);
      ps.setInt(1,flightID); 
      ResultSet priceRes = ps.executeQuery();
      
      int price=0;

      ps=con.prepareStatement(insert);

      if(seatClass=="economy" && cap.getInt("capacity_economy")+10 > pCount.getInt("count")){

        if(priceRes.next()) price=priceRes.getInt("economy");
        ps.setInt(1,lastBooking.getInt("id")+1);
        ps.setInt(2,passID);
        ps.setInt(3,flightID);
        ps.setTimestamp(4,getCurrentTimeStamp());
        ps.setInt(5,price);
        ps.setString(6,seatClass);

        if(cap.getInt("capacity_economy")<=pCount.getInt("count")){ //setting null for row and letter 
          ps.setNull(7,Types.NULL);
          ps.setNull(8,Types.NULL);

        }else{ //getting the last tuple for this class to get next letter and row
          
          List<Integer> rowLetter = new ArrayList<Integer>();
          rowLetter=RowLetter2(cap, seatClass,  pCount);
          ps.setInt(7,rowLetter.get(0));
          ps.setString(8,seatLetters.get(rowLetter.get(1)));
       }
       ps.executeUpdate();
       return true;
      
      }
      else if(seatClass=="business" && cap.getInt("capacity_business") > pCount.getInt("count")){

        if(priceRes.next()) price=priceRes.getInt("business");
        ps.setInt(1,lastBooking.getInt("id")+1);
        ps.setInt(2,passID);
        ps.setInt(3,flightID);
        ps.setTimestamp(4,getCurrentTimeStamp());
        ps.setDouble(5,price);
        ps.setString(6,seatClass);
        List<Integer> rowLetter = new ArrayList<Integer>();
        rowLetter=RowLetter2(cap, seatClass,  pCount);
        ps.setInt(7,rowLetter.get(0));
        ps.setString(8,seatLetters.get(rowLetter.get(1)));

        ps.executeUpdate();
        return true;

      }
      else if(seatClass=="first" && cap.getInt("capacity_first") > pCount.getInt("count")){

        if(priceRes.next()) price=priceRes.getInt("first");
        ps.setInt(1,lastBooking.getInt("id")+1);
        ps.setInt(2,passID);
        ps.setInt(3,flightID);
        ps.setTimestamp(4,getCurrentTimeStamp());
        ps.setDouble(5,price);
        ps.setString(6,seatClass);
        List<Integer> rowLetter = new ArrayList<Integer>();
        rowLetter=RowLetter2(cap, seatClass,  pCount);
        ps.setInt(7,rowLetter.get(0));
        ps.setString(8,seatLetters.get(rowLetter.get(1)));
        
        ps.executeUpdate();
        return true;

      }
      
    }
    catch (SQLException se){
      se.printStackTrace();
    }
    return false;
   }

   /**
    * Attempts to upgrade overbooked economy passengers to business class
    * or first class (in that order until each seat class is filled).
    * Does so by altering the database records for the bookings such that the
    * seat and seat_class are updated if an upgrade can be processed.
    *
    * Upgrades should happen in order of earliest booking timestamp first.
    *
    * If economy passengers are left over without a seat (i.e. more than 10 overbooked passengers or not enough higher class seats), 
    * remove their bookings from the database.
    * 
    * @param  flightID  The flight to upgrade passengers in.
    * @return           the number of passengers upgraded, or -1 if an error occured.
    */
   public int upgrade(int flightID) {
      // Implement this method!
      return -1;
   }


   /* ----------------------- Helper functions below  ------------------------- */

    // A helpful function for adding a timestamp to new bookings.
    // Example of setting a timestamp in a PreparedStatement:
    // ps.setTimestamp(1, getCurrentTimeStamp());

    /**
    * Returns a SQL Timestamp object of the current time.
    * 
    * @return           Timestamp of current time.
    */
   private java.sql.Timestamp getCurrentTimeStamp() {
      java.util.Date now = new java.util.Date();
      return new java.sql.Timestamp(now.getTime());
   }

   // Add more helper functions below if desired.

  private List<Integer> RowLetter2(ResultSet cap,String seatClass, ResultSet passengerCount){
    List<Integer> rowLetter = new ArrayList<Integer>();
    int myLetter;
    int myRow;
    int startRow=1;
    try{
      if(seatClass=="economy"){
        startRow=(int) Math.ceil((double)cap.getInt("capacity_business")/6) + (int) Math.ceil((double)cap.getInt("capacity_first")/6) + 1; 
      }else if(seatClass=="business"){
        startRow=(int) Math.ceil((double)cap.getInt("capacity_first")/6) + 1; 
      }else if(seatClass=="first"){
        startRow=1; 
      }
     int currRow = startRow + passengerCount.getInt("count")/6;
     myLetter=passengerCount.getInt("count")%6;
     if(myLetter==0 && passengerCount.getInt("count")>0){
      myRow= currRow + 1 ;
      myLetter= 1;
     }
     else{
      myRow = currRow;
      myLetter=myLetter+1;
    }
    rowLetter.add(myRow);
    rowLetter.add(myLetter-1);

    return rowLetter;
    }
    catch(SQLException se){
      se.printStackTrace();
    }
    return rowLetter;
  }


  
  /* ----------------------- Main method below  ------------------------- */

   public static void main(String[] args) {
      // You can put testing code in here. It will not affect our autotester.
      try{
        
       Assignment2 a2 = new Assignment2();

       String url="jdbc:postgresql://localhost:5432/csc343h-saeifara";
		   boolean con = a2.connectDB(url, "saeifara", "");
		   boolean b= a2.bookSeat(8,69,"first");
       b= a2.bookSeat(9,69,"first");
       b= a2.bookSeat(10,69,"economy");
       b= a2.bookSeat(11,69,"business");
       b= a2.bookSeat(12,69,"economy");
       b= a2.bookSeat(13,69,"economy");
       b= a2.bookSeat(14,69,"economy");
       b= a2.bookSeat(15,69,"economy");

         
      }
      catch(SQLException se){
         se.printStackTrace();  
      }
   

}
}
