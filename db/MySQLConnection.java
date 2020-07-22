package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


import entity.Order;

public class MySQLConnection {

	private Connection conn;

	public MySQLConnection() {
		try { //连接数据库
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

	public String getFullname(String userId) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return "";
		}
		String name = "";
		String sql = "SELECT first_name, last_name FROM users WHERE user_id = ? ";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				name = rs.getString("first_name") + " " + rs.getString("last_name");
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

	public boolean addUser(String userId, String password, String firstname, String lastname,String emailAddress, String phoneNumber) {
		if (conn == null) {
			System.err.println("DB connection failed");
			return false;
		}

		String sql = "INSERT IGNORE INTO users VALUES (?, ?, ?, ?, ?, ?)";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			statement.setString(2, password);
			statement.setString(3, firstname);
			statement.setString(4, lastname);
			statement.setString(5, emailAddress);
			statement.setString(6, phoneNumber);

			return statement.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public List<String> getUserProfiles(String userId){
		if (conn == null) {
			System.err.println("DB connection failed");
			return new ArrayList<>();
		}
		List<String> user = new ArrayList<>();
		try {
			String sql = "SELECT email_address,phone_number FROM dispatch.users WHERE user_id = ?;";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1,userId);
			ResultSet rs = statement.executeQuery();
			while(rs.next()) {
				String emailAddress = rs.getString("email_address");
				user.add(emailAddress);
				String phoneNumber = rs.getString("phone_number");
				user.add(phoneNumber);
			}
		}catch(SQLException e) {
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
			String status = (order.getActive() == true) ? "active":"overdue";
			statement.setString(3, status);
			int b1 = statement.executeUpdate();

			String sql2 = "INSERT IGNORE INTO orders(order_id,user_id,tracking_id,active,sender_id,recipient_id,package_weight,package_height,package_fragile,total_cost,package_width,package_length,carrier,delivery_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
			statement.setString(14, order.getDeliveryTime());
//		statement.setString(11, order.getOrderCreateTime());
			int b2 = statement.executeUpdate();
			return b1 == 1 && b2 ==1;
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
					+ "t.created_at, t.delivered_at "
					+ "FROM users u, orders o, contact c, tracking t "
					+ "WHERE u.user_id = ? "
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
				String delivered_at = rs.getString("delivered_at");
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
					+ "t.created_at, t.delivered_at "
					+ "FROM users u, orders o, contact c, tracking t "
					+ "WHERE u.user_id = ? "
					+ 	"AND t.status = 'active' "
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
				String delivered_at = rs.getString("delivered_at");
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
					+ "(SELECT o.order_id, o.total_cost, m.machine_type, t.delivered_at, CONCAT(c.first_name, ' ', c.last_name) AS sender_name, "
					+ "c.address AS sender_address, c.phone_number AS sender_phone, c.email_address AS sender_email, "
					+ "o.package_weight, o.package_height, package_fragile "
					+ "FROM orders o, contact c, machine m, tracking t "
					+ "WHERE o.order_id = ? " 
					+	"AND o.sender_id = c.contact_id "
					+ 	"AND o.machine_id = m.machine_id "
					+ 	"AND o.tracking_id = t.tracking_id)";
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
				String machine_type = rs.getString("machine_type");
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
			}
		}
		catch (SQLException e){
			e.printStackTrace();
		}
		return items;
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
	
	
}