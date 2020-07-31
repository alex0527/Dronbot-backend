package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import entity.Order;
import entity.User;
import entity.TrackingInfo;

public class MySQLConnection {

	private Connection conn;

	public MySQLConnection() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
			conn = DriverManager.getConnection(MySQLDBUtil.URL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public List<String> getFullname(String userId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return new ArrayList<>();
		}
		List<String> name = new ArrayList<>();
		String sql = "SELECT first_name, last_name FROM users WHERE user_id = ? ";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				name.add(rs.getString("first_name"));
				name.add(rs.getString("last_name"));
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return name;
	}

	public boolean verifyLogin(String userId, String password) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return false;
		}
		String sql = "SELECT user_id FROM users WHERE user_id = ? AND password = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			statement.setString(2, password);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				return true;
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return false;
	}

	public boolean addUser(String userId, String password, String firstname, String lastname, String emailAddress,
			String phoneNumber, String address) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return false;
		}

		String sql = "INSERT IGNORE INTO users VALUES (?,?,?,?,?,?,?)";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			statement.setString(2, password);
			statement.setString(3, firstname);
			statement.setString(4, lastname);
			statement.setString(5, emailAddress);
			statement.setString(6, phoneNumber);
			statement.setString(7, address);

			return statement.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public List<String> getUserProfiles(String userId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return new ArrayList<>();
		}
		List<String> user = new ArrayList<>();
		try {
			String sql = "SELECT email_address,phone_number,address FROM dispatch.users WHERE user_id = ?;";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String emailAddress = rs.getString("email_address");
				user.add(emailAddress);
				String phoneNumber = rs.getString("phone_number");
				user.add(phoneNumber);
				String address = rs.getString("address");
				user.add(address);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return user;
	}

	public List<String> getTimes(String trackingId){
		if (conn == null) {
			System.err.println("DB connection failed");
			return new ArrayList<>();
		}
		List<String> times = new ArrayList<>();
		try {
			String sql = "SELECT created_at,delivered_at FROM tracking WHERE tracking_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1,trackingId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()) {
				String createdTime = rs.getString("created_at");
				times.add(createdTime);
				String deliveredTime = rs.getString("delivered_at");
				times.add(deliveredTime);
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return times;
	}

	
	
	public boolean addTrackingInfo(String trackingId, String created_at, String delievered_at) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return false;
		}

		String sql = "INSERT IGNORE INTO dispatch.tracking(tracking_id,created_at,delivered_at)values(?,?,?)";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, trackingId);
			statement.setString(2, created_at);
			statement.setString(3, delievered_at);
			return statement.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean addContact(String firstName, String lastName,String emailAddress, String phoneNumber, String address) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return false;
		}

		String sql = "INSERT IGNORE INTO dispatch.contact(first_name,last_name,phone_number,email_address,address) VALUES(?,?,?,?,?)";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, firstName);
			statement.setString(2, lastName);
			statement.setString(3, phoneNumber);
			statement.setString(4, emailAddress);
			statement.setString(5, address);
			return statement.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	public int getContactId(String firstName, String lastName,String emailAddress, String phoneNumber, String address) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return -1;
		}
		int id = -1;
		try {
			String sql = "SELECT contact_id FROM dispatch.contact WHERE first_name = ? and last_name = ? and phone_number = ? and email_address = ? and address = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, firstName);
			statement.setString(2, lastName);
			statement.setString(3, phoneNumber);
			statement.setString(4, emailAddress);
			statement.setString(5, address);
			ResultSet rs = statement.executeQuery();
			while(rs.next()) {
				id = rs.getInt("contact_id");
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return id;

	}

	public boolean createOrder(Order order, int senderId, int recipientId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return false;
		}

		try {
			String sql = "INSERT IGNORE INTO tracking(tracking_id,created_at,status) VALUES (?, ?, ?)";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, order.getTrackingId());
			statement.setString(2, order.getOrderCreateTime());
			String status = (order.getActive() == true) ? "ordered" : "overdue";
			statement.setString(3, status);
			int b1 = statement.executeUpdate();

			String sql2 = "INSERT IGNORE INTO orders(order_id,user_id,tracking_id,active,sender_id,recipient_id,package_weight,package_height,package_fragile,total_cost,package_width,package_length,carrier,appointment_time,station_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			statement = conn.prepareStatement(sql2);
			statement.setString(1, order.getOrderId());
			statement.setString(2, order.getUserId());
			statement.setString(3, order.getTrackingId());
			statement.setBoolean(4, order.getActive());
			statement.setInt(5, senderId);
			statement.setInt(6, recipientId);
			statement.setFloat(7, order.getPackageWeight());
			statement.setFloat(8, order.getPackageHeight());
			statement.setBoolean(9, order.getIsFragile());
			statement.setFloat(10, order.getTotalCost());
			statement.setFloat(11, order.getPackageWidth());
			statement.setFloat(12, order.getPackageLength());
			statement.setString(13, order.getCarrier());
			statement.setString(14, order.getAppointmentTime());
			statement.setInt(15, order.getStationId());
//		statement.setString(11, order.getOrderCreateTime());
			int b2 = statement.executeUpdate();
			return b1 == 1 && b2 == 1;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	
	public List<String> getHistory(String user_id) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return new ArrayList<String>();
		}
		List<String> items = new ArrayList<String>();
		try {
			String sql = "SELECT o.order_id, o.tracking_id, c.first_name, c.last_name, c.address, t.status, "
					+ "t.created_at, t.estimated_delivered_at "
					+ "FROM users u, orders o, contact c, tracking t "
					+ "WHERE u.user_id = ? "
					+ 	"AND u.user_id = o.user_id "
					+	"AND o.recipient_id = c.contact_id "
					+ 	"AND o.tracking_id = t.tracking_id "
					+ 	"ORDER BY created_at";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, user_id);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String order_id = rs.getString("order_id");
				items.add(order_id);
				String status = rs.getString("status");
				items.add(status);
				String name = rs.getString("first_name") + " " + rs.getString("last_name");
				items.add(name);
				String address = rs.getString("address");
				items.add(address);
				String delivered_at = rs.getString("estimated_delivered_at");
				items.add(delivered_at);
				String created_at = rs.getString("created_at");
				items.add(created_at);
				String tracking_id = rs.getString("tracking_id");
				items.add(tracking_id);
			}
		}
		catch (SQLException e){
			e.printStackTrace();
		}
		return items;
	}

	public List<String> getActive(String user_id) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return new ArrayList<String>();
		}
		List<String> items = new ArrayList<String>();
		try {
			String sql = "SELECT o.order_id, o.tracking_id, c.first_name, c.last_name, c.address, t.status, "
					+ "t.created_at, t.estimated_delivered_at "
					+ "FROM users u, orders o, contact c, tracking t "
					+ "WHERE u.user_id = ? "
					+ 	"AND t.status != 'delivered' "
					+ 	"AND u.user_id = o.user_id " 
					+	"AND o.recipient_id = c.contact_id "
					+ 	"AND o.tracking_id = t.tracking_id";
			System.out.println(sql);
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, user_id);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String order_id = rs.getString("order_id");
				items.add(order_id);
				String status = rs.getString("status");
				items.add(status);
				String name = rs.getString("first_name") + " " + rs.getString("last_name");
				items.add(name);
				String address = rs.getString("address");
				items.add(address);
				String delivered_at = rs.getString("estimated_delivered_at");
				items.add(delivered_at);
				String created_at = rs.getString("created_at");
				items.add(created_at);
				String tracking_id = rs.getString("tracking_id");
				items.add(tracking_id);
			}
		}
		catch (SQLException e){
			e.printStackTrace();
		}
		return items;
	}

	public List<String> getDetail(String order_id) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return new ArrayList<String>();
		}
		List<String> items = new ArrayList<String>();
		try {
			// create the view with general information
			String sql1 = "CREATE OR REPLACE VIEW G AS "
					+ "(SELECT o.order_id, o.total_cost, o.carrier, t.estimated_delivered_at AS delivered_at, CONCAT(c.first_name, ' ', c.last_name) AS sender_name, "
					+ "s.address AS sender_address, c.phone_number AS sender_phone, c.email_address AS sender_email, "
					+ "o.package_weight, o.package_height, o.package_fragile, o.package_width, o.package_length "
					+ "FROM orders o, contact c, machine m, tracking t, station s "
					+ "WHERE o.order_id = ? " 
					+	"AND o.sender_id = c.contact_id "
					+ 	"AND o.tracking_id = t.tracking_id "
					+ 	"AND o.station_id = s.station_id)";
			PreparedStatement statement1 = conn.prepareStatement(sql1);
			statement1.setString(1, order_id);
			statement1.executeUpdate();
			
			// create view with recipient information
			String sql2 = "CREATE OR REPLACE VIEW R AS "
					+ "(SELECT o.order_id, CONCAT(c.first_name, ' ', c.last_name) AS recipient_name, c.address AS recipient_address, "
					+ "c.phone_number AS recipient_phone, c.email_address AS recipient_email "
					+ "FROM orders o, contact c "
					+ "WHERE o.order_id = ? "
					+ "AND o.recipient_id = c.contact_id)";
			PreparedStatement statement2 = conn.prepareStatement(sql2);
			statement2.setString(1, order_id);
			statement2.executeUpdate();
			
			// join two views
			String sql3 = "SELECT * FROM G LEFT JOIN R ON G.order_id = R.order_id";
			PreparedStatement statement3 = conn.prepareStatement(sql3);
			ResultSet rs = statement3.executeQuery();
			
			while (rs.next()) {
				String cost = rs.getString("total_cost");
				items.add(cost);
				String machine_type = rs.getString("carrier");
				items.add(machine_type);
				String delivered_at = rs.getString("delivered_at");
				items.add(delivered_at);
				String sender_name = rs.getString("sender_name");
				items.add(sender_name);
				String sender_address = rs.getString("sender_address");
				items.add(sender_address);
				String sender_phone = rs.getString("sender_phone");
				items.add(sender_phone);
				String sender_email = rs.getString("sender_email");
				items.add(sender_email);
				String recipient_name = rs.getString("recipient_name");
				items.add(recipient_name);
				String recipient_address = rs.getString("recipient_address");
				items.add(recipient_address);
				String recipient_phone = rs.getString("recipient_phone");
				items.add(recipient_phone);
				String recipient_email = rs.getString("recipient_email");
				items.add(recipient_email);
				String weight = rs.getString("package_weight");
				items.add(weight);
				String height = rs.getString("package_height");
				items.add(height);
				String fragile = rs.getString("package_fragile");
				items.add(fragile);
				String length = rs.getString("package_length");
				items.add(length);
				String width = rs.getString("package_width");
				items.add(width);
			}
		}
		catch (SQLException e){
			e.printStackTrace();
		}
		return items;
	}

	// 通过station id,来获取该station近30分钟状态为ordered的订单的list。
	public List<Order> getStastionOrderList(int stationId, String carrier) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return new ArrayList<>();
		}
		List<Order> stationOrders = new ArrayList<>();
		try {
			String sql = "select o.order_id,o.tracking_id,o.package_weight,o.total_cost,c.address,o.appointment_time from dispatch.orders o, dispatch.contact c, dispatch.tracking t where o.tracking_id = t.tracking_id and o.recipient_id = c.contact_id and t.status = ? and o.station_id = ? and o.carrier = ?;";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, "ordered");
			statement.setInt(2, stationId);
			statement.setString(3, carrier);
			ResultSet rs = statement.executeQuery();
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date cT = new Date();
			String cT1 = df.format(cT);
			Date currentTime = null;
			try {
				currentTime = df.parse(cT1);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			long currentTimeInMS = currentTime.getTime();
			while (rs.next()) {
				String apT = rs.getString("appointment_time");
				Date appointmentTime = null;
				try {
					appointmentTime = df.parse(apT);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				long appointmentTimeInMS = appointmentTime.getTime();
				if (appointmentTimeInMS - currentTimeInMS <= 30 * 60000) { // check是否是30分钟以内的订单
					String orderId = rs.getString("order_id");
					String trackingId = rs.getString("tracking_id");
					Float packageWeight = rs.getFloat("package_weight");
					Float totalCost = rs.getFloat("total_cost");
					String destinationAddress = rs.getString("address");
					Order order = new Order();
					order.setOrderId(orderId);
					order.setTrackingId(trackingId);
					order.setPackageWeight(packageWeight);
					order.setTotalCost(totalCost);
					order.setRecipientAddress(destinationAddress);
					order.setAppointmentTime(apT);
					stationOrders.add(order);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return stationOrders;
	}
	
	public List<Integer> getAvailableMachine(int stationId, String carrier) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return new ArrayList<>();
		}
		List<Integer> machines = new ArrayList<>();
		try {
			String sql = "select m.machine_id from dispatch.machine m where m.station_id = ? and m.machine_type = ? and m.available = ?;";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setInt(1, stationId);
			statement.setString(2, carrier);
			//statement.setBoolean(3, true);
			statement.setInt(3, 1);
			ResultSet rs = statement.executeQuery();
			
			while (rs.next()) {
				int machineId = rs.getInt("machine_id");
				machines.add(machineId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return machines;
	}
	
	public void flipMachineAvailability(int machineId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return;
		}
		try {
			String sql = "UPDATE machine SET available = !available WHERE machine_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setInt(1, machineId);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateStationIdInMachine(int machineId, int stationId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return;
		}
		try {
			String sql = "UPDATE machine SET station_id = ? WHERE machine_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setInt(1, stationId);
			statement.setInt(2, machineId);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int getStationIdInMachine(int machineId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return 0;
		}
		int stationId = 0;
		try {
			String sql = "SELECT station_id FROM dispatch.machine WHERE machine_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setInt(1, machineId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				stationId = rs.getInt("station_id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return stationId;
	}
	
	public int getMachineIdInOrder(String orderId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return 0;
		}
		int machineId = 0;
		try {
			String sql = "SELECT machine_id FROM dispatch.orders WHERE order_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, orderId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				machineId = rs.getInt("machine_id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return machineId;
	}
	
	public void updateMachineIdInOrder(int machineId, String orderId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return;
		}
		try {
			String sql = "UPDATE orders SET machine_id = ? WHERE order_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setInt(1, machineId);
			statement.setString(2, orderId);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateDelayedInTracking(String trackingId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return;
		}
		try {
			String sql = "update dispatch.tracking SET tracking.delay = true where tracking_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, trackingId);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateStatusInTracking(String status, String trackingId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return;
		}
		try {
			String sql = "update dispatch.tracking SET tracking.status = ? where tracking_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, status);
			statement.setString(2, trackingId);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateTrackingAfterDispatch(String status, String previousAddress, String prevDestiStartTime, String estimatedDeliveredTime, String trackingId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return;
		}

		try {
			String sql = "UPDATE dispatch.tracking SET status = ?, previous_destination = ?, previous_destination_start_time = ?, estimated_delivered_at = ? WHERE tracking_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, status);
			statement.setString(2, previousAddress);
			statement.setString(3, prevDestiStartTime);
			statement.setString(4, estimatedDeliveredTime);
			statement.setString(5, trackingId);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String getRecipientIdInOrder(String orderId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return "";
		}
		String recipientId = "";
		try {
			String sql = "SELECT recipient_id FROM dispatch.orders WHERE order_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, orderId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				recipientId = rs.getString("recipient_id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return recipientId;
	}
	
	public String getRecipientAddress(String recipientId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return "";
		}
		String address = "";
		try {
			String sql = "SELECT address FROM dispatch.contact WHERE contact_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, recipientId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				address = rs.getString("address");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return address;
	}
	
	public String getMachineType(String orderId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return "";
		}
		String machineType = "";
		try {
			String sql = "SELECT carrier FROM dispatch.orders WHERE order_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, orderId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				machineType = rs.getString("carrier");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return machineType;
	}
	
	/**
	 * Store the latest update time into the tacking table 
	 * @param trackingId
	 * @param deliverStatus
	 * @param updateTime
	 */
	public void updateTimes(String trackingId, String deliverStatus, String updateTime){

		if (conn == null) {
			System.err.println("DB connection failed");
			return;
		}
		
		try {
			String sql = "UPDATE tracking SET status = ?, last_update = ? WHERE tracking_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1,deliverStatus);
			statement.setString(2,updateTime);
			statement.setString(3,trackingId);
			statement.executeUpdate();
			
		}catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	public boolean updateProfile(User user) {
		// get independent parameters
		String user_id = user.getUser_id();
		String first_name = user.getFirst_name();
		String last_name = user.getLast_name();
		String email_address = user.getEmail();
		String phone_number = user.getPhone();
		String address = user.getAddress();
		
		if (conn == null) {
			System.err.println("DB Connection Failed");
			return false;
		}
		
		try {
			String close_safe_update = "SET SQL_SAFE_UPDATES = 0";
			PreparedStatement statement1 = conn.prepareStatement(close_safe_update);
			statement1.executeQuery();
			
			String update = "UPDATE dispatch.users " + 
					"SET first_name = ?, " + 
					"	 last_name = ?, " + 
					"	 email_address = ?, " + 
					"	 phone_number = ?, "
					+ 	"address = ? " + 
					"WHERE user_id = ?";
			PreparedStatement statement2 = conn.prepareStatement(update);
			statement2.setString(1, first_name);
			statement2.setString(2,  last_name);
			statement2.setString(3,  email_address);
			statement2.setString(4, phone_number);
			statement2.setString(5,  address);
			statement2.setString(6, user_id);
			int rs = statement2.executeUpdate();
			
			String open_safe_update = "SET SQL_SAFE_UPDATES = 1";
			PreparedStatement statement3 = conn.prepareStatement(open_safe_update);
			statement3.executeQuery();
			
			return rs == 1;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public String getOrderId(String trackingId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return "";
		}
		String orderId = "";
		try {
			String sql = "SELECT order_id FROM orders WHERE tracking_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1,trackingId);
//			statement.executeUpdate();
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				orderId = rs.getString("order_id");
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return orderId;
	}
	
	public TrackingInfo getTrackingInfo(String trackingId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return null;
		}
		TrackingInfo trackingInfo = new TrackingInfo();
		try {
			String sql = "SELECT * FROM tracking WHERE tracking_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, trackingId);
//			statement.executeUpdate();
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String status = rs.getString("status");
				trackingInfo.setStatus(status);
				String createdAt = rs.getString("created_at");
				trackingInfo.setCreatedAt(createdAt);
				String deliveredAt = rs.getString("estimated_delivered_at");
				trackingInfo.setDeliveredAt(deliveredAt);
				boolean delay = rs.getBoolean("delay");
				trackingInfo.setDelay(delay);
				String destination = rs.getString("previous_destination");
				trackingInfo.setDestination(destination);
				String transitStart = rs.getString("previous_destination_start_time");
				trackingInfo.setTransitStart(transitStart);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return trackingInfo;
	}
	
}
