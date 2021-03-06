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
    Connection connection;
    
    // Can use if you wish: seat letters
    List<String> seatLetters = Arrays.asList("A", "B", "C", "D", "E", "F");
    List<String> seatClasses = Arrays.asList("economy", "business", "first");

    class Seat{
	int row;
	String seatLetter;

	Seat(int row, String seatLetter){
	    this.row = row;
	    this.seatLetter = seatLetter;
	}
    }

    class SeatClassInfo{
	int startRow;
	int capacity;
	int booked; // number of seats booked

	SeatClassInfo(int startRow, int capacity, int booked){
	    this.startRow = startRow;
	    this.capacity = capacity;
	    this.booked = booked;
	}
    }

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
	try {
	    Class.forName("org.postgresql.Driver");
	}
	catch (ClassNotFoundException e) {
	    System.err.println("Failed to find the JDBC driver");
	}
	
	try{
	    connection = DriverManager.getConnection(URL, username, password);
	    String queryString = "set search_path to air_travel,public";
	    PreparedStatement pStatement = connection.prepareStatement(queryString);
	    pStatement.executeUpdate();
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
	    connection.close();
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
    
    try{
      if(!passengerExists(passID) || !flightExists(flightID)) return false;

      String insert = "INSERT INTO booking VALUES(?,?,?, ?, ?, ?::seat_class, ?, ?)";
      String capacity = "select flight.id, plane,capacity_first,capacity_business, capacity_economy from flight join plane on flight.plane=plane.tail_number where flight.id= ?";
      PreparedStatement ps=connection.prepareStatement(capacity);
      ps.setInt(1,flightID);     
      ResultSet cap = ps.executeQuery();
      cap.next();

      String passCount = "select count(*) as count from booking where seat_class= ?::seat_class and flight_id= ?";
      ps=connection.prepareStatement(passCount);
      ps.setString(1,seatClass); 
      ps.setInt(2,flightID); 
      ResultSet pCount = ps.executeQuery();
      pCount.next();

      String maxBookID="SELECT id FROM booking WHERE id=(SELECT max(id) FROM booking)";
      ps =connection.prepareStatement(maxBookID);
      ResultSet lastBooking = ps.executeQuery();
      int id=1;
      if(lastBooking.next()) id=lastBooking.getInt("id")+1;

      String priceRow="SELECT * FROM price where flight_id= ?";
      ps =connection.prepareStatement(priceRow);
      ps.setInt(1,flightID); 
      ResultSet priceRes = ps.executeQuery();
      
      int price=0;

      ps=connection.prepareStatement(insert);

      if(seatClass=="economy" && cap.getInt("capacity_economy")+10 > pCount.getInt("count")){

        if(priceRes.next()) price=priceRes.getInt("economy");
        ps.setInt(1,id);
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
        ps.setInt(1,id);
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
        ps.setInt(1,id);
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


  private List<Integer> RowLetter2(ResultSet cap,String seatClass, ResultSet passengerCount){
    List<Integer> rowLetter = new ArrayList<Integer>();
    int myLetter;
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
      myLetter= 1;
     }
     else{
      myLetter=myLetter+1;
    }
    rowLetter.add(currRow);
    rowLetter.add(myLetter-1);

    return rowLetter;
    }
    catch(SQLException se){
      se.printStackTrace();
    }
    return rowLetter;
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

	ResultSet rs;
	String queryString = "SELECT id " +
	    " FROM booking WHERE flight_id= ? " +
	    " AND seat_class='economy' AND (row IS NULL OR letter IS NULL) ORDER by datetime; ";

	String updateStatement = "UPDATE booking SET seat_class = ?::seat_class, row  = ? , letter  = ? WHERE id = ?;";
	String dleteStatement = "DELETE FROM booking WHERE id = ?;";	
	int numUpgrades=0;
	
	try{

	    PreparedStatement ps = connection.prepareStatement(queryString);
	    PreparedStatement psUpd;
	    ps.setInt(1, flightID);
	    rs = ps.executeQuery();
	    
	    
	    while (rs.next()) {
		String newSeatClass="business";
		int bookingId = rs.getInt(1);

		Seat newSeat= getAvailableSeat(flightID, newSeatClass);


		if(newSeat==null){
		    newSeatClass="first";
		    newSeat= getAvailableSeat(flightID, newSeatClass);
		}

		//TODO --DELETE
		if(newSeat==null){
		    psUpd = connection.prepareStatement(dleteStatement);
		    psUpd.setInt(1, bookingId);
		}else{
		    psUpd = connection.prepareStatement(updateStatement);
		    psUpd.setString(1, newSeatClass);
		    psUpd.setInt(2, newSeat.row);
		    psUpd.setString(3, newSeat.seatLetter);
		    psUpd.setInt(4, bookingId);
		    numUpgrades++;
		}
		// call executeUpdate to execute our sql update statement
		psUpd.executeUpdate();
		psUpd.close();
	    }
	    return numUpgrades;	    
	}
	catch (SQLException se){
	    System.err.println("SQL Exception." +  "<Message>: " + se.getMessage());
	    return -1;
	}
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

    /**
     * Returns false if passenger cannot be found, otherwise true.
     * 
     * @param  passID     id of the passenger
     * @return            true if valid passenger, false otherwise. 
     */
    private boolean passengerExists(int passID) {
	ResultSet rs;

	try{
	    String queryString = "select id from passenger where id = ?";
	    PreparedStatement ps = connection.prepareStatement(queryString);

	    // Validate passenger:
	    ps.setInt(1, passID);
	    rs = ps.executeQuery();

	    if(rs.next()){
		return true;
	    }else{
		return false;
	    }
	}
	catch (SQLException se){
	    System.err.println("SQL Exception." +  "<Message>: " + se.getMessage());
	    return false;
	}
    }

    /**
     * Returns false if flight cannot be found, otherwise true.
     * 
     * @param  flightID   id of the flight
     * @return            true if valid flight, false otherwise. 
     */
    private boolean flightExists(int flightID) {
	ResultSet rs;

	try{
	    String queryString = "select id from flight where id = ?";
	    PreparedStatement ps = connection.prepareStatement(queryString);

	    // Validate flight:
	    ps.setInt(1, flightID);
	    rs = ps.executeQuery();

	    if(rs.next()){
		return true;
	    }else{
		return false;
	    }
	}
	catch (SQLException se){
	    System.err.println("SQL Exception." +  "<Message>: " + se.getMessage());
	    return false;
	}
    }

    /**
     * Returns Seat object for a given flight and seat class
     * 
     * @param  flightID   id of the flight
     * @param  seatClass  the class of the seat (economy, business, or first) 
     * @return            Seat (row, and seatLetter)
     */
    private Seat getAvailableSeat(int flightID, String seatClass) {
	ResultSet rs;
	String queryString;


	SeatClassInfo  sci = getSeatClassInfo(flightID, seatClass);
	if (sci == null){
	    return null;
	}

	if( !seatClass.equals("economy") && sci.booked >=sci.capacity){
	    return null;
	}
	    
	if( seatClass.equals("economy")){
	    if(sci.booked >= sci.capacity +10 ){
		return null;
	    }
		
	    if(sci.booked >= sci.capacity){
		return new Seat(0, null);
	    }
	}

	return new Seat(sci.startRow + sci.booked/6, seatLetters.get(sci.booked%6));

    }



    private SeatClassInfo getSeatClassInfo(int flightID, String seatClass) {
	ResultSet rs;
	String queryString=null;

	try{
		
		if (seatClass== "first"){
		    queryString = "SELECT 1, capacity_first, " +
			" (select count(*) from booking where flight_id=? and seat_class='first') " +
			" from plane p, flight f where  f.plane = p.tail_number and f.id= ?";
    }
		else if(seatClass== "business"){
		    queryString = "SELECT ceil(capacity_first::numeric/6)+1, capacity_business, " +
			" (select count(*) from booking where flight_id=? and seat_class='business') " +
			" from plane p, flight f where  f.plane = p.tail_number and f.id= ?";
    }
		else if(seatClass== "economy"){
		    queryString = "SELECT ceil(capacity_first::numeric/6)+ceil(capacity_business::numeric/6)+1, capacity_economy, " +
			" (select count(*) from booking where flight_id=? and seat_class='economy') " +
			" from plane p, flight f where  f.plane = p.tail_number and f.id= ?"; 
		} 

	    PreparedStatement ps = connection.prepareStatement(queryString);

	    
	    ps.setInt(1, flightID);
	    ps.setInt(2, flightID);
	    rs = ps.executeQuery();

	    if(rs.next()){
		return new SeatClassInfo(rs.getInt(1),rs.getInt(2), rs.getInt(3));
	    }
	    else{
		return null;
	    }
	}
	catch (SQLException se){
	    System.err.println("SQL Exception." +  "<Message>: " + se.getMessage());
	    return null;
	}
    }
    
    /* ----------------------- Main method below  ------------------------- */
    
    public static void main(String[] args) {
	// You can put testing code in here. It will not affect our autotester.
	System.out.println("Running the code!");
	try{
	    
	    Assignment2 a2 = new Assignment2();
	    String url = "jdbc:postgresql://localhost:5432/csc343h-saeifara";
	    a2.connectDB(url, "saeifara", "");

	    for(int i=1;i<=24;++i){
	     a2.bookSeat(i,5,"economy");
       } 
       a2.bookSeat(1,500,"economy");

       a2.upgrade(5);
	    System.out.println("Exiting!");
	}
	catch (SQLException se){
	    
	    System.err.println("SQL Exception." + "<Message>: " + se.getMessage()); 
	}

	
    }
    
}
