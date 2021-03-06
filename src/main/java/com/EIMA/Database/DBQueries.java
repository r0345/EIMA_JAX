package com.EIMA.Database;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;

import com.EIMA.auth.AuthUtils;
import com.EIMA.auth.AuthUtils.Privlege;
import com.EIMA.models.*;

//Make a list of our queries and add calls to them here returning types from "com.EIMA.models" when relevant.

public class DBQueries {

	////////////////////////////////////////////////////////
	//////// UTILITY FUNCTIONS//////////////////////////////
	////////////////////////////////////////////////////////

	// Checks the active tokens list and sees if the provided is active
	// This must also use a prepared statement. This should be called by other
	// statements to verify
	public static boolean tokenIsValid(String token) {
		Connection db = DBConnection.getConnection();
		String sql = "Select uname from Users where token=" + sq(token) + ";";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            String result = "";
            while(rs.next()){
                result = rs.getString("uname");
            }
            return result != null && result.length() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
		return false;
	}

	// Gets the Privlege level of a user in their current incident
	public static AuthUtils.Privlege getUserCurrIncidentPrivLevel(String token) {
		Privlege priv = Privlege.noAccess;
		Connection db = DBConnection.getConnection();
		String sql = "Select permission from " +
				"Users inner join Members on Users.current_incident=Members.incident_id " +
				"and Users.uid=Members.uid where token=" + sq(token) + ";";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            String result = "";
            while(rs.next()){
                result = rs.getString(1);
            }
            if (result != null){
            	//System.out.println("result:" + result);
	            switch (Integer.parseInt(result)) {
		            case 3:
		            	priv = Privlege.admin;
		            	break;
		            case 2:
		            	priv = Privlege.mapEditor;
		            	break;
		            case 1:
		            	priv = Privlege.user;
	            }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
        return priv;
	}

	// gets the incident Id for the current incident a user is in
	public static int getUserCurrentIncident(String token) {
		int incident = -1;
		Connection db = DBConnection.getConnection();
		String sql = "Select current_incident from Users where token=" + sq(token) + ";";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            String result = "";
            while(rs.next()){
                result = rs.getString("current_incident");
            }
            if (result != null) {
            	incident = Integer.parseInt(result); 
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
		return incident;
	}

	/**
	 * Takes username/password. returns a true false value. This SHOULD use a
	 * Prepared statement to Prevent SQL injection. This is important. Other
	 * queries don't require this as non of their input makes it to the actual
	 * SQL statement exception of token validation (I think).
	 * 
	 * http://www.theserverside.com/news/1365244/Why-Prepared-Statements-are-
	 * important-and-how-to-use-them-properly
	 * http://stackoverflow.com/questions/24692296/how-to-use-prepared-statement
	 * -for-select-query-in-java
	 * http://www.java2novice.com/jdbc/prepared-statement/
	 */
	public static boolean validateLogin(String uname, String pwd) {
		Connection db = DBConnection.getConnection();
		String sql = "Select uname from Users where uname=" + sq(uname) + "and password=" + sq(pwd) + ";";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            String result = "";
            while(rs.next()){
                result = rs.getString("uname");
            }
            return result != null && result.length() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
		return false;
	}

	// Verifys and incident ID is valid return true/false.
	public static boolean isValidIncident(int incidentID) {
		Connection db = DBConnection.getConnection();
		String sql = "Select incident_id from Incidents where incident_id=" + incidentID + ";";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            String result = "";
            while(rs.next()){
                result = rs.getString("incident_id");
            }
            return result != null && result.length() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
		return false;
	}
	////////////////////////////////////////////////////////
	//////// Core Data Modifications Functions /////////////
	////////////////////////////////////////////////////////

	// Sets a user's active token, for a login action.
	public static void setUserToken(String uname, String theirToken) {
		Connection db = DBConnection.getConnection();
		String sql = "Update Users set token=" + sq(theirToken) + " where uname=" + sq(uname) + ";";
        try {
            db.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
	}

	// Removes the active token from a user for a logout action.
	public static void removeUserToken(String theirToken) {
		Connection db = DBConnection.getConnection();
		String sql = "Update Users set token=null, current_incident=null where token=" + sq(theirToken) + ";";
        try {
            db.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
	}

	// Should add a user to an incident; Their privlege should be set at No
	// access
	public static void addUserToIncident(int incidentID, String token) {
		Connection db = DBConnection.getConnection();
		String sql = "";
        try {
        	sql = "select uid from users where token=" + sq(token) + ";";
        	ResultSet rs = db.createStatement().executeQuery(sql);
        	int uid = -1;
        	while (rs.next()) {
        		uid = rs.getInt(1);
        	}
        	sql = "select incident_member from members where " +
        			"uid=" + uid + " and incident_id=" + incidentID + ";";
        	rs = db.createStatement().executeQuery(sql);
        	String member = "null";
        	while (rs.next()) {
        		member = rs.getString(1);
        	}
        	if (member == null || member.equalsIgnoreCase("null")) {
        		
	        	sql = "values(nextval('incident_member_seq'));";
	            rs = db.createStatement().executeQuery(sql);
	            while(rs.next()){
	                member = rs.getString(1);
	            }
	            
	            /*sql = "Select uid from Users where token=" + sq(token) + ";";
	            rs = db.createStatement().executeQuery(sql);
	            int uid = 0;
	            while(rs.next()){
	                uid = rs.getInt(1);
	            }*/
	            
	            sql = "Insert into Members values(" +
	            		member + "," + uid + "," + incidentID + ",0);" 
	            		;
	            db.createStatement().execute(sql);
	            
	            EIMAProfile user = getUserProfile(token);
	            EIMAAsset userAsset = new EIMAAsset(
	            		user.getName(),user.getName(),user.getUnit(), null,
	            		user.getOrganization(),user.getStatus(),user.getUnitType(),
	            		true
	            );
	            setUserCurrentIncident(token, incidentID);
	            addMapAsset(userAsset, token);
        	}
        	else {
        		setUserCurrentIncident(token, incidentID);
        	}
            /*sql = "Select uname from Users where token=" + sq(token) + ";";
            rs = db.createStatement().executeQuery(sql);
            String uname = "";
            while(rs.next()){
                uname = rs.getString(1);
            }
            System.out.println("uname: " + uname);*/
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
	}

	// Creates a new incident with blank data, returns a integer of the created
	// Incident ID; this can be a sequence or random doesn't matter.
	public static int createNewIncident(String token) {
		Connection db = DBConnection.getConnection();
		int incidentId = -1;
        try {
        	String sql = "values(nextval('incident_id_seq'));";
            ResultSet rs = db.createStatement().executeQuery(sql);
            while(rs.next()){
            	incidentId = rs.getInt(1);
            }
            sql = "Insert into Incidents (incident_id) values (" + incidentId + ");";
            db.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
        addUserToIncident(incidentId, token);
        int memberId = getMemberId(token);
        try {
        	String sql = "Update members set permission=3 where incident_member=" + memberId + ";";
        	db.createStatement().execute(sql);
        } catch(SQLException e) {
        	e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
		return incidentId;
	}

	// Creates a new incident with STANDALONE Data, returns a integer of the
	// created
	// Incident ID; this can be a sequence or random doesn't matter, check for
	// conflicts if its random.
	public static int createIncidentFromData(String token, EIMAAsset[] assets, EIMACircle[] circles,
			EIMAPolygon[] polygons) {
		int incident = createNewIncident(token);
		for (EIMAAsset asset : assets) {
			addMapAsset(asset, token);
		}
		for (EIMACircle circle : circles) {
			addMapAsset(circle, token);
		}
		for (EIMAPolygon poly : polygons) {
			addMapAsset(poly, token);
		}
		return incident;
	}

	// Returns true if a user is in an incident, false if not.
	public static boolean userIsInIncident(String token) {
		Connection db = DBConnection.getConnection();
		String sql = "Select current_incident from Users where token=" + sq(token) + ";";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            String result = null;
            while(rs.next()){
                result = rs.getString(1);
            }
            return result != null && result.length() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
		return false;
	}

	// Removes a user from incident. Called when a user 'leaves'
	public static void removeUserFromIncident(String token) {
		Connection db = DBConnection.getConnection();
		
		//int incident = getUserCurrentIncident(token);
		//int member = getMemberId(token);
		EIMAProfile profile = getUserProfile(token);
		EIMAAsset user = new EIMAAsset();
		user.setName(profile.getName());
		user.setUsername(profile.getName());
		user.setUser(true);
		deleteMapAsset(user, token);
		
		/*int objectId = -1;
		sql = "Select object_id from Map_objects where " +
				"incident=" + incident +
				" and incident_member=" + member + ";";
		try {
			ResultSet rs = db.createStatement().executeQuery(sql);
			while (rs.next()) {
				objectId = rs.getInt(1);
			}
		} catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }*/
		/*sql = "Update Map_objects set gps_x=null,gps_y=null,radius=null,x_array=null,y_array=null,notes=null " +
				"where object_id=" + objectId + ";";
		try {
			db.createStatement().execute(sql);
		} catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }*/
		
		logout(token);
		String sql = "Update Users set current_incident=null where token=" + sq(token) + ";";
		try {
			db.createStatement().execute(sql);
		} catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
	}

	// Gets a user profile from a user's token, see EIMA PROFILE for formatting
	public static EIMAProfile getUserProfile(String token) {
		Connection db = DBConnection.getConnection();
		String sql = "Select uname, organization, unit, unit_type from Users where token=" + sq(token) + ";";
		EIMAProfile profile = null;
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            String uname = "";
            String organization = "";
            String unit = "";
            String unitType = "";
            while(rs.next()){
                uname = rs.getString("uname");
                organization = rs.getString("organization");
                unit = rs.getString("unit");
                unitType = rs.getString("unit_type");
            }
            profile = new EIMAProfile(uname, unit, organization, "", unitType); // EIMAProfile.status
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
        return profile;
	}

	// Sets a user profile based on data sent from user.
	public static void setUserProfile(String token, EIMAProfile profile) {
		Connection db = DBConnection.getConnection();
		String sql = "Update Users set " +
				"uname=" + sq(profile.getName()) + "," +
				"organization=" + sq(profile.getOrganization()) + "," +
				"unit=" + sq(profile.getUnit()) + "," +
				"unit_type=" + sq(profile.getUnitType()) + "," +
				"status=" + sq(profile.getStatus()) +
				" where token=" + sq(token) + ";"
				;
        try {
            db.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
    }

	// Adds a user to the userlist. Returns true if succesful, false if name is
	// taken.
	public static boolean registerUser(String uname, String password) {
		Connection db = DBConnection.getConnection();
		String sql = "Insert into Users (uid, uname, password)" +
				"values(nextval('uid_seq')," + 
				sq(uname) + "," + sq(password) + ");";
        try {
            db.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
            return false;
        }
		return true;
	}

	// Gets Assets of an incident
	public static EIMAAsset[] getAssets(String token) {
		ArrayList<EIMAAsset> assets = new ArrayList<EIMAAsset>();
		int incidentId = getUserCurrentIncident(token);
		Connection db = DBConnection.getConnection();
		String sql = "Select client_side_id, asset_name, unit, unit_type, organization, status, gps_x, gps_y, incident_member, notes from " +
				"Map_objects where incident=" + incidentId + " and " +
				"icon_type != 'circle' and icon_type !='poly';";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            while(rs.next()){
                String id = rs.getString("client_side_id");
                String name = rs.getString("asset_name");
                String unit = rs.getString("unit");
                String org = rs.getString("organization");
                String status = rs.getString("status");
                String unit_type = rs.getString("unit_type");
                String member = rs.getString("incident_member");
                String x = rs.getString("gps_x");
                String y = rs.getString("gps_y");
                boolean isUser = member != null;
                if (x != null && y != null) {
                	double xCoord = Double.parseDouble(x);
                	double yCoord = Double.parseDouble(y);
                	assets.add(new EIMAAsset(id, name, unit, new GPSPosition(xCoord,yCoord), org, status, unit_type, isUser));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
        EIMAAsset[] arr = new EIMAAsset[assets.size()];
        for (int i = 0; i < arr.length; i++) {
        	arr[i] = assets.get(i);
        }
		return arr;
	}

	// Gets Circles of an incident
	public static EIMACircle[] getCircles(String token) {
		ArrayList<EIMACircle> circles = new ArrayList<EIMACircle>();
		int incidentId = getUserCurrentIncident(token);
		Connection db = DBConnection.getConnection();
		String sql = "Select client_side_id, gps_x, gps_y, radius, notes, zone_type from " +
				"Map_objects where incident=" + incidentId + " and " +
				"icon_type='circle';";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            while(rs.next()){
                String id = rs.getString("client_side_id");
                String xCoord = rs.getString("gps_x");
                String yCoord = rs.getString("gps_y");
                String radius = rs.getString("radius");
                String notes = rs.getString("notes");
                String circle_type = rs.getString("zone_type");
                if (xCoord != null && yCoord != null && radius != null) {
                	double lat = Double.parseDouble(xCoord);
                	double lon = Double.parseDouble(yCoord);
                	double rad = Double.parseDouble(radius);
                	circles.add(new EIMACircle(id, new GPSPosition(lat, lon), rad, notes, circle_type));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
        EIMACircle[] arr = new EIMACircle[circles.size()];
        for (int i = 0; i < arr.length; i++) {
        	arr[i] = circles.get(i);
        }
		return arr;
	}

	// Gets Polygons of an incident
	public static EIMAPolygon[] getPolygons(String token) {
		ArrayList<EIMAPolygon> polygons = new ArrayList<EIMAPolygon>();
		int incidentId = getUserCurrentIncident(token);
		Connection db = DBConnection.getConnection();
		String sql = "Select client_side_id, x_array, y_array, zone_type, notes from " +
				"Map_objects where incident=" + incidentId + " and " +
				"icon_type='poly';";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            while(rs.next()){
                String id = rs.getString("client_side_id");
                /*ResultSet xVals = rs.getArray("x_array").getResultSet();
                ResultSet yVals = rs.getArray("y_array").getResultSet();
                ArrayList<Double> xList = new ArrayList<Double>();
                while (xVals.next()) {
                	xList.add(xVals.getDouble(1));
                }
                ArrayList<Double> yList = new ArrayList<Double>();
                while (yVals.next()) {
                	yList.add(yVals.getDouble(1));
                }
                double[] x = new double [xList.size()];
                for (int i = 0; i < x.length; i++) {
                	x[i] = xList.get(i);
                }
                double[] y = new double [yList.size()];
                for (int i = 0; i < y.length; i++) {
                	y[i] = yList.get(i);
                }*/
                String xVals = rs.getString("x_array");
                String yVals = rs.getString("y_array");
                double[] x = dbStringToArray(xVals);
                double[] y = dbStringToArray(yVals);
                String type = rs.getString("zone_type");
                String notes = rs.getString("notes");
            	GPSPosition[] coords = new GPSPosition[x.length];
            	for (int i = 0; i < coords.length; i++) {
            		coords[i] = new GPSPosition(x[i], y[i]);
            	}
            	polygons.add(new EIMAPolygon(id, coords, type, notes));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
        EIMAPolygon[] arr = new EIMAPolygon [polygons.size()];
        for (int i = 0; i < arr.length; i++) {
        	arr[i] = polygons.get(i);
        }
		return arr;
	}

	// Updates the user's location within the incident
	public static void updateUserLocation(String token, GPSPosition location) {
		Connection db = DBConnection.getConnection();
		int member = getMemberId(token);
		String sql = "Select client_side_id, unit, organization, status, unit_type, asset_name from " +
				"Map_objects where incident_member=" + member + ";";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            while(rs.next()){
                String clientSideId = rs.getString("client_side_id");
                String unit = rs.getString("unit");
                String organization = rs.getString("organization");
                String status = rs.getString("status");
                String unitType = rs.getString("unit_type");
                String assetName = rs.getString("asset_name");
                EIMAAsset asset = new EIMAAsset(clientSideId, assetName, unit, location, organization, status, unitType, true);
                updateMapAsset(asset, token);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
	}

	// Gets user list from incident.
	public static EIMAUser[] getUserList(String token) {
		ArrayList<EIMAUser> users = new ArrayList<EIMAUser>();
		Connection db = DBConnection.getConnection();
		int incident = getUserCurrentIncident(token); 
		String sql = "Select uname, unit, organization, status, unit_type, permission from " +
				"Members inner join Users on Members.uid=Users.uid "+
				"where Members.incident_id=" + incident + ";";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            while(rs.next()){
                String name = rs.getString("uname");
                String unit = rs.getString("unit");
                String organization = rs.getString("organization");
                String status = rs.getString("status");
                String unitType = rs.getString("unit_type");
                String priv = rs.getString("permission");
                users.add(new EIMAUser(name, unit, organization, status, unitType, priv));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
        EIMAUser[] arr = new EIMAUser[users.size()];
        for (int i = 0; i < arr.length; i++) {
        	arr[i] = users.get(i);
        }
		return arr;
	}

	// Adds a message to a user. Uses other persons username.
	public static void sendMessageTo(String token, String user, String message) {
		int event = registerNewEvent(token);
		String toAdmins = (user != null && user.equals("ADMINS")) ? "TRUE" : "FALSE";
		if (user != null && (user.equals("USERS") || user.equals("ADMINS"))) {
			user = null;
		}
		Connection db = DBConnection.getConnection();
		String sql = "";
        try {
            String recipient  = "null";
            sql = "Select incident_member from " +
            		"Members m inner join Users u on m.incident_id = u.current_incident " +
            		"and m.uid=u.uid " +
            		"where u.uname=" + sq(user) + ";";
            ResultSet rs = db.createStatement().executeQuery(sql);
            while(rs.next()){
                recipient = rs.getString(1);
            }
            sql = "Insert into Message_events values (" +
            		event + "," + recipient + "," + toAdmins + "," + sq(message) + ");";
            db.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
	}

	// Modifies a user's privlege -- takes username and privLevel -- ("noAccess"
	// "user" "mapEditor" "admin"), you can't edit your own privlege level
	public static boolean modifyPrivlege(String token, String username, String privLevel) {
		Connection db = DBConnection.getConnection();
		String sender = "";
		String sql = "";
		int priv = 0;
		if (privLevel.equalsIgnoreCase("admin")) {
			priv = 3;
		} else if (privLevel.equalsIgnoreCase("mapEditor")) {
			priv = 2;
		} else if (privLevel.equalsIgnoreCase("user")) {
			priv = 1;
		}
        try {
	        	sql = "Select uname from Users where token=" + sq(token) + ";";
	        	ResultSet rs = db.createStatement().executeQuery(sql);
	        	while(rs.next()){
	                sender = rs.getString(1);
	            }
	        	if (sender !=null && sender.equalsIgnoreCase(username)) {
	        		return false;
	        	}
        	String recipientId = "null";
        	int incident = getUserCurrentIncident(token);
        	sql = "Select incident_member from " +
    				"Members inner join Users on Members.incident_id=Users.current_incident "+
        			" and Members.uid=Users.uid " +
    				"where Users.uname=" + sq(username) + " and Members.incident_id=" + incident + ";";
            rs = db.createStatement().executeQuery(sql);
            while(rs.next()){
            	recipientId = rs.getString(1);
            }
            if (recipientId == null || recipientId.equalsIgnoreCase("null")) {
            	return false;
            }
            
            int eventId = registerNewEvent(token);            
            sql = "Insert into Permission_events values(" +
            		eventId + "," + recipientId + "," + priv + ");";
            db.createStatement().execute(sql);
            
            sql = "Update Members set permission=" + priv +
            		" where incident_member=" + recipientId + ";";
            db.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
		return true;
	}

	// Gets a list of all alerts a person has recieved.
	public static EIMAAlert[] getAlerts(String token) {
		ArrayList<EIMAAlert> alerts = new ArrayList<EIMAAlert>();
		Connection db = DBConnection.getConnection();
		String sql = "";
        try {
        	/*int member = -1;
        	sql = "Select incident_member from " +
    				"Members inner join Users on Members.incident_id=Users.current_incident "+
    				"where Users.token=" + sq(token) + " limit 1;";
            ResultSet rs = db.createStatement().executeQuery(sql);
            while(rs.next()){
            	member = rs.getInt(1);
            }*/
            int member = getMemberId(token);
            int incidentId = getUserCurrentIncident(token);
            AuthUtils.Privlege priv = getUserCurrIncidentPrivLevel(token);
            
            sql = "Select Users.uname, Events.time, Message_events.message from " +
            		"Users inner join Members on Users.uid=Members.uid " +
            		"inner join Events on Members.incident_member=Events.sender " +
            		"inner join Message_events on Events.event_id=Message_events.event_id " +
            		"where Members.incident_id=" + incidentId +
            		" and (Message_events.recipient=" + member;
            if (priv == AuthUtils.Privlege.admin) {
            	sql += " or Message_events.recipient is null);";
            } else {
            	sql += " or (Message_events.recipient is null and Message_events.to_admins=FALSE));";
            }
            ResultSet rs = db.createStatement().executeQuery(sql);
            while (rs.next()) {
            	String uname = rs.getString(1);
            	long time = rs.getLong(2);
            	String msg = rs.getString(3);
            	alerts.add(new EIMAAlert(msg, uname, time));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
        EIMAAlert[] arr = new EIMAAlert[alerts.size()];
        for (int i = 0; i < arr.length; i++) {
        	arr[i] = alerts.get(i);
        }
		return arr;
	}

	// MapAsset is an a super class for polygons, circles and pins. actions
	// dependent on what might have to use instance of or call other funcitons
	// if you want to structure it a certain way.
	// Deletes the asset from the map.
	public static void deleteMapAsset(MapAsset a, String token) {
		int incidentId = getUserCurrentIncident(token);
		String clientSideId = "";
		if (a instanceof EIMAAsset) {
			clientSideId = ((EIMAAsset)a).getUsername();
		} else if (a instanceof EIMACircle) {
			clientSideId = ((EIMACircle)a).getUid();
		} else if (a instanceof EIMAPolygon) {
			clientSideId = ((EIMAPolygon)a).getUid();
		}
		Connection db = DBConnection.getConnection();
		String sql = "Update Map_objects set gps_x=null,gps_y=null,radius=null,x_array=null,y_array=null,notes=null " +
				"where incident=" + incidentId +
				" and client_side_id=" + sq(clientSideId) + ";";
		try {
			db.createStatement().execute(sql);
		} catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
		sql = "Select object_id from Map_objects where incident=" + incidentId +
				" and client_side_id=" + sq(clientSideId) + ";";
		int objectId = -1;
		try {
			ResultSet rs = db.createStatement().executeQuery(sql);
			while (rs.next()) {
				objectId = rs.getInt(1);
			}
		} catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
		int event = registerNewEvent(token);
		sql = "Insert into Map_events (event_id,object_id,gps_x,gps_y,x_array,y_array,radius,notes) values(" +
				event + "," + objectId + ",null,null,null,null,null,null);";
		try {
			db.createStatement().execute(sql);
		} catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
	}

	// Updates the data of an asset.
	public static void updateMapAsset(MapAsset a, String token) {
		int incidentId = getUserCurrentIncident(token);
		Connection db = DBConnection.getConnection();
		String sql = "";
        try {
        	if (a instanceof EIMAAsset) {
        		EIMAAsset asset = (EIMAAsset) a;
        		int objectId = -1;
        		sql = "Select object_id from Map_objects where incident=" + incidentId +
        				" and client_side_id=" + sq(asset.getUsername()) + ";";
        		ResultSet rs = db.createStatement().executeQuery(sql);
        		while (rs.next()) {
        			objectId = rs.getInt(1);
        		}
        		String x = null;
        		String y = null;
        		if (asset.getPosition() != null) {
        			x = "" + asset.getPosition().getLatitude();
        			y = "" + asset.getPosition().getLongitude();
        		}
        		sql = "Update Map_objects set gps_x=" + x + "," +
        				"gps_y=" + y + "," +
        				"status=" + sq(asset.getStatus()) +
        				" where object_id=" + objectId + ";";
        		db.createStatement().execute(sql);
        		int eventId = registerNewEvent(token);
        		sql = "Insert into Map_events (event_id, object_id, gps_x, gps_y, status) " +
        		"values(" + eventId + "," + objectId + "," + x + "," +
        		y + "," + sq(asset.getStatus()) + ");";
        		db.createStatement().execute(sql);
        	} else if (a instanceof EIMACircle) {
        		EIMACircle circle = (EIMACircle) a;
        		int objectId = -1;
        		sql = "Select object_id from Map_objects where incident=" + incidentId +
        				" and client_side_id=" + sq(circle.getUid()) + ";";
        		ResultSet rs = db.createStatement().executeQuery(sql);
        		while (rs.next()) {
        			objectId = rs.getInt(1);
        		}
        		sql = "Update Map_objects set gps_x=" + circle.getCenter().getLatitude() + "," +
        				"gps_y=" + circle.getCenter().getLongitude() + "," +
        				"radius=" + circle.getRadius() + "," +
        				"notes=" + sq(circle.getNote()) +
        				" where object_id=" + objectId + ";";
        		db.createStatement().execute(sql);
        		int eventId = registerNewEvent(token);
        		sql = "Insert into Map_events (event_id, object_id, gps_x, gps_y, radius, notes) " +
        		"values(" + eventId + "," + objectId + "," + circle.getCenter().getLatitude() + "," +
        		circle.getCenter().getLongitude() + "," + circle.getRadius() + "," +
        		sq(circle.getNote()) + ");";
        		db.createStatement().execute(sql);
        	} else if (a instanceof EIMAPolygon) {
        		EIMAPolygon poly = (EIMAPolygon) a;
        		int objectId = -1;
        		sql = "Select object_id from Map_objects where incident=" + incidentId +
        				" and client_side_id=" + sq(poly.getUid()) + ";";
        		ResultSet rs = db.createStatement().executeQuery(sql);
        		while (rs.next()) {
        			objectId = rs.getInt(1);
        		}
        		String[] coords = {"null","null"};
        		if (poly.getPoints() != null) {
        			coords = gpsStringify(poly.getPoints());
        		}
        		sql = "Update Map_objects set x_array=" + coords[0] + "," +
        				"y_array=" + coords[1] + "," +
        				"notes=" + sq(poly.getNote()) +
        				" where object_id=" + objectId + ";";
        		db.createStatement().execute(sql);
        		int eventId = registerNewEvent(token);
    			sql = "Insert into Map_events (event_id, object_id, x_array, y_array, notes) values( " +
    					eventId + "," + objectId + "," + coords[0] + "," + coords[1] + "," +
    					sq(poly.getNote()) + ");";
    			db.createStatement().execute(sql);
        	}
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
	}

	// Adds a new asset to the map.
	public static void addMapAsset(MapAsset a, String token) {
		int objectId = -1;
		int incidentId = -1;
		String incidentMember = null;
		Connection db = DBConnection.getConnection();
		String sql = "";
        try {
        	sql = "values(nextval('object_id_seq'));";
        	ResultSet rs = db.createStatement().executeQuery(sql);
        	while (rs.next()) {
        		objectId = rs.getInt(1);
        	}
        	incidentId = getUserCurrentIncident(token);
        	if (a instanceof EIMAAsset) {
        		EIMAAsset eimaAsset = (EIMAAsset)a;
        		String icon = "null";
        		if (eimaAsset.isUser()){
        			icon = "person";
        			sql = "Select incident_member from " +
            				"Members inner join Users on Members.incident_id=Users.current_incident "+
        					"and Members.uid=Users.uid " +
            				"where Users.token=" + sq(token) + ";";
                    rs = db.createStatement().executeQuery(sql);
                    while(rs.next()){
                    	incidentMember = rs.getString(1);
                    }
        		} // Will status and unit and stuff ever get updated?
        		String x = null;
        		String y = null;
        		if (eimaAsset.getPosition() != null) {
        			x = "" + (eimaAsset.getPosition().getLatitude());
        			y = "" + (eimaAsset.getPosition().getLongitude());
        		}
    			sql = "Insert into Map_objects (object_id, client_side_id, icon_type, incident, incident_member, gps_x, gps_y, notes," +
    					"status, unit, unit_type, organization, asset_name)" +
    					"values (" + objectId + "," + sq(eimaAsset.getUsername()) + "," +
    					sq(icon) + "," + incidentId + "," +
    					incidentMember + "," + x + "," + y + "," + "null" + "," +
    					sq(eimaAsset.getStatus()) + "," + sq(eimaAsset.getUnit()) + "," + sq(eimaAsset.getUnitType()) + "," +
    					sq(eimaAsset.getOrganization()) + "," + sq(eimaAsset.getName()) + 
    					");";
    			db.createStatement().execute(sql);
    			int eventId = registerNewEvent(token);
    			
    			sql = "Insert into Map_events (event_id, object_id, gps_x, gps_y, status, notes) values( " +
    					eventId + "," + objectId + "," + x + "," + y + "," + sq(eimaAsset.getStatus()) + ",null);";
    			db.createStatement().execute(sql);
        	} else if (a instanceof EIMACircle) {
        		EIMACircle circle = (EIMACircle) a;
        		sql = "Insert into Map_objects (object_id, client_side_id, icon_type, incident, gps_x, gps_y, radius, notes, zone_type) " +
        		"values (" + objectId + "," + sq(circle.getUid()) + "," + sq("circle") + "," + incidentId +
        		"," + circle.getCenter().getLatitude() + "," + circle.getCenter().getLongitude() + "," +
        		circle.getRadius() + "," + sq(circle.getNote()) + "," + sq(circle.getType()) + ");";
        		db.createStatement().execute(sql);
        		int eventId = registerNewEvent(token);
    			sql = "Insert into Map_events (event_id, object_id, gps_x, gps_y, radius, notes) values( " +
    					eventId + "," + objectId + "," + circle.getCenter().getLatitude() + "," + circle.getCenter().getLongitude() +
    					"," + circle.getRadius() + "," + sq(circle.getNote()) + ");";
    			db.createStatement().execute(sql);
        	} else if (a instanceof EIMAPolygon) {
        		EIMAPolygon poly = (EIMAPolygon) a;
        		String[] coords = {"null","null"};
        		if (poly.getPoints() != null) {
        			coords = gpsStringify(poly.getPoints());
        		}
        		sql = "Insert into Map_objects (object_id, client_side_id, icon_type, incident, x_array, y_array, notes, zone_type) " +
        		"values (" + objectId + "," + sq(poly.getUid()) + "," + sq("poly") + "," + incidentId +
        		"," + coords[0] + "," + coords[1] + "," +
        		sq(poly.getNote()) + "," + sq(poly.getType()) + ");";
        		db.createStatement().execute(sql);
        		int eventId = registerNewEvent(token);
    			sql = "Insert into Map_events (event_id, object_id, x_array, y_array, notes) values( " +
    					eventId + "," + objectId + "," + coords[0] + "," + coords[1] + "," +
    					sq(poly.getNote()) + ");";
    			db.createStatement().execute(sql);
        	}
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
	}

	public static void setUserCurrentIncident(String token, int incidentId) {
		Connection db = DBConnection.getConnection();
		String sql = "Update Users set current_incident=" + incidentId +
						" where token=" + sq(token) + ";";
        try {
            db.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
        arrive(token);
	}
	
	public static void logout(String token) {
		int event = registerNewEvent(token);
		Connection db = DBConnection.getConnection();
		String sql = "Insert into In_out_events " +
		"values(" + event + ",'O');";
        try {
            db.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
	}
	
	public static void arrive(String token) {
		int event = registerNewEvent(token);
		Connection db = DBConnection.getConnection();
		String sql = "Insert into In_out_events " +
		"values(" + event + ",'I');";
        try {
            db.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
	}
	
	private static String sq(String s) {
		if (s==null || s.equals("")){
			return "null";
		} else {
			return "'" + s.replaceAll("'", "") + "'";
		}
	}
	
	private static int registerNewEvent(String token) {
		int event = -1;
		Connection db = DBConnection.getConnection();
		String sql = "values(nextval('event_id_seq'));";
        try {
            ResultSet rs = db.createStatement().executeQuery(sql);
            while(rs.next()){
                event = rs.getInt(1);
            }
            /*int member = -1;
            sql = "Select incident_member from " +
            		"Members m inner join Users u on m.incident_id = u.current_incident " +
            		"where token=" + sq(token) + ";";
            rs = db.createStatement().executeQuery(sql);
            while(rs.next()){
                member = rs.getInt(1);
            }*/
            int member = getMemberId(token);
            sql = "Insert into Events (event_id, sender, time) " +
            		"values(" + event + "," + member + "," + System.currentTimeMillis()+ ");";
            db.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
		return event;
	}
	
	private static int getMemberId(String token) {
		Connection db = DBConnection.getConnection();
		int memberId = -1;
		String sql = "Select incident_member from " +
        		"Members m inner join Users u on m.incident_id = u.current_incident " +
				"and m.uid = u.uid " +
        		"where token=" + sq(token) + ";";
		try {
			ResultSet rs = db.createStatement().executeQuery(sql);
			while (rs.next()) {
				memberId = rs.getInt(1);
			}
		} catch (SQLException e) {
            e.printStackTrace();
            System.out.println("error: " + e.getErrorCode());
        }
		return memberId;
	}
	
	private static String[] gpsStringify(GPSPosition[] arr) {
		String[] s = {"'{","'{"};
		int end = arr.length - 1;
		for (int i = 0; i <= end; i++) {
			if (i == end) {
				s[0] += arr[i].getLatitude() + "}'";
				s[1] += arr[i].getLongitude() + "}'";
			} else {
				s[0] += arr[i].getLatitude() + ",";
				s[1] += arr[i].getLongitude() + ",";
			}
		}
		return s;
	}
	private static double[] dbStringToArray(String arr) {
		System.out.println(arr);
		if (arr == null || arr.length() == 0 || arr.equals("'{}'")) {
			return new double[0];
		} else {
			arr = arr.substring(1, arr.length() - 1);
			System.out.println(arr);
			String[] strs = arr.split(",");
			double[] vals = new double[strs.length];
			for (int i = 0; i < vals.length; i++) {
				vals[i] = Double.parseDouble(strs[i]);
			}
			return vals;
		}
	}
}
