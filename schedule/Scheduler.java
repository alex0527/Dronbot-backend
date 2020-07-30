package schedule;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.maps.errors.ApiException;

import db.MySQLConnection;
import entity.Order;
import external.GoogMatrixRequest;

//Knapsack problem + traveling salesman problem
public class Scheduler {

	private final static String STATION1 = "Sunset Blvd & Pacheco St, San Francisco, CA 94116";
	private final static String STATION2 = "2310 Folsom St, San Francisco, CA 94110";
	private final static String STATION3 = "241 Vienna St, San Francisco, CA 94112";
	private final static float DRONE_CAPACITY = 5.0f;
	private final static float ROBOT_CAPACITY = 20.0f;
	private final static double DRONE_SPEED = 50.0;
	private final static double ROBOT_SPEED = 10.0;
	private final static String STATUS_DISPATCHED = "dispatched";
	private final static String STATUS_INTRANSIT = "in transit";
	private final static String STATUS_DELIVERED = "delivered";
	

	private List<Order> selectedOrders;
	private int stationId;
	private String carrier;
	private boolean machineAssigned;
	private long dispatchTime = 8 * 60 * 1000;

	public Scheduler(int stationId, String carrier) {
		selectedOrders = new ArrayList<>();
		this.stationId = stationId;
		this.carrier = carrier;
	}

	public void dispatchOrders() throws ApiException, InterruptedException, IOException {
		// select orders with status "ordered" and the appointment time is within 30
		// mins of the current time
		MySQLConnection connection = new MySQLConnection();
		List<Order> orders = connection.getStastionOrderList(stationId, carrier);
		connection.close();
		// case 1: no valid orders were selected
		if (orders == null || orders.size() == 0) {
			System.out.println("No apporiate orders were selected");
			return;
		}
		int n = orders.size();
		
		// case 2: the number of valid orders is not enough to run Knapsack algorithm
		if (n < 3 && carrier.equals("drone") || n < 5 && carrier.equals("robot")) {
			// find out all orders with a appointment time within 12 min of the current time
			// (12 - 8 = 4)
			System.out.println("The number of apporiate orders is not enough to run Knapsack algo");
			System.out.println("The number of apporiate orders is " + n + ", which is less than the minimum number requirement: " + (carrier.equals("drone") ? 3 : 5));
			List<Order> urgentOrders = new ArrayList<>();
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
			for (Order order : orders) {
				String apT = order.getAppointmentTime();
				Date appointmentTime = null;
				try {
					appointmentTime = df.parse(apT);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				long appointmentTimeInMS = appointmentTime.getTime();
				if (appointmentTimeInMS - currentTimeInMS <= 12 * 60000) {
					urgentOrders.add(order);
				}
			}
			if (urgentOrders.size() == 0) {
				return;
			}
			System.out.println("Urgent selected orders: ");
			
			int k = 1;
			while (urgentOrders.size() != 0) {
				System.out.println("Round " + (k++) + ":");
				for (int i = 0; i < urgentOrders.size(); i++) {
					System.out.println("order " + (i + 1) + " ID: " + urgentOrders.get(i).getOrderId());
					System.out.println("order " + (i + 1) + " trackingID: " + urgentOrders.get(i).getTrackingId());
					System.out.println("order " + (i + 1) + " weight: " + urgentOrders.get(i).getPackageWeight());
					System.out.println("order " + (i + 1) + " cost: " + urgentOrders.get(i).getTotalCost());
					System.out.println("order " + (i + 1) + " destination: " + urgentOrders.get(i).getRecipientAddress());
					System.out.println();
				}
				List<List<Order>> knapSackResult = knapsack(urgentOrders);
				urgentOrders = knapSackResult.get(1);
				//only run TSP when the machine is assigned successfully
				if (machineAssigned) {
					double[][] matrixDistance = buildMatrixDistance(knapSackResult.get(0));
					TSP(matrixDistance, knapSackResult.get(0));
					machineAssigned = false;
					dispatchTime = 8 * 60 * 1000;
				} else {
					System.out.println("No machine is available, the orders above may be delayed!");
				}
			}
			return;
		}
		// case 3: the number of valid orders is enough to run Knapsack algorithm
		//	       check if the machine is assigned successfully
		selectedOrders = knapsack(orders).get(0);
		System.out.println("Knapsack algorithm selected orders: ");
		for (int i = 0; i < selectedOrders.size(); i++) {
			System.out.println("order " + (i + 1) + " ID: " + selectedOrders.get(i).getOrderId());
			System.out.println("order " + (i + 1) + " trackingID: " + selectedOrders.get(i).getTrackingId());
			System.out.println("order " + (i + 1) + " weight: " + selectedOrders.get(i).getPackageWeight());
			System.out.println("order " + (i + 1) + " cost: " + selectedOrders.get(i).getTotalCost());
			System.out.println("order " + (i + 1) + " destination: " + selectedOrders.get(i).getRecipientAddress());
			System.out.println();
		}
		//only run TSP when the machine is assigned successfully
		if (machineAssigned) {
			double[][] matrixDistance = buildMatrixDistance(selectedOrders);
			TSP(matrixDistance, selectedOrders);
			machineAssigned = false;
		} else {
			System.out.println("No machine is available! The orders above may be delayed!");
		}
	}

	// List<0> is the selected order list; List<1> is the remain order list
	private List<List<Order>> knapsack(List<Order> orders) {
		List<List<Order>> result = new ArrayList<>();
		List<Order> selected = new ArrayList<>();
		List<Order> remain = new ArrayList<>();
		Map<String, Order> orderIdToOrder = new HashMap<>();
		for (Order order : orders) {
			orderIdToOrder.put(order.getOrderId(), order);
		}

		int n = orders.size();
		int[] weights = new int[n];
		int[] prices = new int[n];

		int k = 0;
		for (Order order : orders) {
			weights[k] = (int) (order.getPackageWeight() * 10);
			prices[k] = (int) (order.getTotalCost() * 10);
			k++;
		}
		// m is max capacity
		int m = carrier.equals("drone") ? (int) (DRONE_CAPACITY * 10) : (int) (ROBOT_CAPACITY * 10);
//		M[i][j] represents the max price of putting the first i orders in the vehicle with capacity of j
//		initialize: M[0][j] == 0 when 0 <= j <= m; M[i][0] == 0 when 0 <= i <= n;
//		induction rule:
//		case 1: M[i][j] = max(M[i - 1][j], M[i - 1][j - weight[i - 1]] + price[i - 1]) if j >= weight[i - 1];
//		case 2: M[i][j] = M[i - 1][j]
//		Time: O(n * m)
//		Space: O(n * m)

		int[][] M = new int[n + 1][m + 1];
		for (int i = 0; i <= n; i++) {
			for (int j = 0; j <= m; j++) {
				if (i == 0 || j == 0) {
					continue;
				}
				if (j >= weights[i - 1]) {
					M[i][j] = Math.max(M[i - 1][j], M[i - 1][j - weights[i - 1]] + prices[i - 1]);
				} else {
					M[i][j] = M[i - 1][j];
				}
			}
		}
		int maxPrice = M[n][m];
		System.out.println("Knapsack results: ");
		System.out.println("The max prices: " + ((float) maxPrice) / 10);

		// print out the selected order id's
		int capacity = m;
		for (int i = n; i > 0 && maxPrice > 0; i--) {
			// either the result comes from the top M[i - 1][capacity] or from prices[i - 1]
			// + M[i - 1][capacity - weights[i - 1]]
			if (maxPrice == M[i - 1][capacity]) {
				remain.add(orderIdToOrder.get(orders.get(i - 1).getOrderId()));
				continue;
			} else {
				// this item is included
//				System.out
//						.println("The order ID is " + orders.get(i - 1).getOrderId() + "; the weight of the order is " + (((float)weights[i - 1])) / 10);
				selected.add(orderIdToOrder.get(orders.get(i - 1).getOrderId()));
				maxPrice -= prices[i - 1];
				capacity -= weights[i - 1];
			}
		}
		try {
			assignMachine(selected);
		} catch (ApiException | InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		result.add(selected);
		result.add(remain);
		return result;
	}

	private void assignMachine(List<Order> selected) throws ApiException, InterruptedException, IOException {
		if (selected == null || selected.size() == 0) {
			return;
		}
		//assignMachine will check the machine availability, 
		//case 1: if a machine is available in the current station, the machine will be assigned
		//case 2: if a machine is available in a different station, the machine will move the current station
		//case 3: if no machine is available, update "delayed" in the tracking DB
		int neiStation1 = 0;
		int neiStation2 = 0;
		if (stationId == 1) {
			neiStation1 = 2;
			neiStation2 = 3;
		} else if (stationId == 2) {
			neiStation1 = 1;
			neiStation2 = 3;
		} else {
			neiStation1 = 1;
			neiStation2 = 2;
		}
		MySQLConnection connection = new MySQLConnection();
		List<Integer> machines = connection.getAvailableMachine(stationId, carrier);
		List<Integer> neighbor1 = connection.getAvailableMachine(neiStation1, carrier);
		List<Integer> neighbor2 = connection.getAvailableMachine(neiStation2, carrier);
		connection.close();

		//case 1 & case 2
		if (machines.size() > 0 || neighbor1.size() > 0 || neighbor2.size() > 0) {
			machineAssigned = true;
			int machineId = 0;
			if (machines.size() > 0) {
				machineId = machines.get(0);
				System.out.println("machine " + machineId + " was assigned from the same station");
			} else if (neighbor1.size() == 0) {
				machineId = neighbor2.get(0);
				System.out.println("machine " + machineId + " was assigned from another station - station " + neiStation2);
			} else if (neighbor2.size() == 0) {
				machineId = neighbor1.get(0);
				System.out.println("machine " + machineId + " was assigned from another station - station " + neiStation1);
			} else {
				String neighborAddr1 = neiStation1 == 1 ? STATION1 : (neiStation1 == 2 ? STATION2 : STATION3);
				String neighborAddr2 = neiStation2 == 1 ? STATION1 : (neiStation2 == 2 ? STATION2 : STATION3);
				String curAddr = stationId == 1 ? STATION1 : (stationId == 2 ? STATION2 : STATION3);
				double distance1 = carrier.equals("drone") ? GoogMatrixRequest.getDirectDistance(neighborAddr1, curAddr)
														   : GoogMatrixRequest.getBicyclingDistance(neighborAddr1, curAddr);
				double distance2 = carrier.equals("drone") ? GoogMatrixRequest.getDirectDistance(neighborAddr2, curAddr)
						   								   : GoogMatrixRequest.getBicyclingDistance(neighborAddr2, curAddr);
				if (distance1 < distance2) {
					machineId = neighbor1.get(0);
					System.out.println("machine " + machineId + " was assigned from another station - station " + neiStation1);
				} else {
					machineId = neighbor2.get(0);
					System.out.println("machine " + machineId + " was assigned from another station - station " + neiStation2);
				}
			}
			//update stationId of the machineId if the machine is assigned from another station
			//update machine availability in machine DB
			//update machineId in order DB, update status to dispatched in tracking db
			MySQLConnection connection1 = new MySQLConnection();
			if (machines.size() == 0) {
				//the machine is assinged from another station, calculate the travel time from another station 
				//to the current station
				int assignStationId = connection1.getStationIdInMachine(machineId);
				String curAddr = stationId == 1 ? STATION1 : (stationId == 2 ? STATION2 : STATION3);
				String assignAddr = assignStationId == 1 ? STATION1 : (assignStationId == 2 ? STATION2 : STATION3);
				double distance = carrier.equals("drone") ? GoogMatrixRequest.getDirectDistance(assignAddr, curAddr)
						                                  : GoogMatrixRequest.getBicyclingDistance(assignAddr, curAddr);
				double speed = carrier.equals("drone") ? DRONE_SPEED : ROBOT_SPEED;
				int travelTimeInSecond = (int)(distance / speed * 60 * 60);
				dispatchTime += travelTimeInSecond * 1000;
				//check if there is any delayed order
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
				long dispatchEndTimeInMS = currentTime.getTime() + dispatchTime;
				for (Order order : selected) {
					String apT = order.getAppointmentTime();
					Date appointmentTime = null;
					try {
						appointmentTime = df.parse(apT);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					long appointmentTimeInMS = appointmentTime.getTime();
					if (dispatchEndTimeInMS > appointmentTimeInMS) {
						connection1.updateDelayedInTracking(order.getTrackingId());
					}
				}
				//update the stationId in the machine
				connection1.updateStationIdInMachine(machineId, stationId);
			}
			connection1.flipMachineAvailability(machineId);
			for (Order order : selected) {
				connection1.updateMachineIdInOrder(machineId, order.getOrderId());
			}
			connection1.close();
		} else {
			//case 3, mark delay = true in tracking DB
			MySQLConnection connection1 = new MySQLConnection();
			for (Order order : selected) {
				connection1.updateDelayedInTracking(order.getTrackingId());
			}
			connection1.close();
		}
	}

	private double[][] buildMatrixDistance(List<Order> orders) throws ApiException, InterruptedException, IOException {
		if (orders == null || orders.size() == 0) {
			return null;
		}
		// get the total number of vertices
		int n = orders.size();
		String[] addresses = new String[n + 1];
		// addresses[0] is the station address
		addresses[0] = stationId == 1 ? STATION1 : (stationId == 2 ? STATION2 : STATION3);
		// addresses[1, 2, 3...] is the order destinations
		for (int i = 0; i < n; i++) {
			addresses[i + 1] = orders.get(i).getRecipientAddress();
		}
//		int[][] matrixDistance = { { 0, 5}, 
//								   { 3, 0}, };	

//		int[][] matrixDistance = { { 0, 3, 100, 8, 9 }, 
//						    		{ 3, 0, 3, 10, 5 }, 
//                          		{ 100, 3, 0, 4, 3 }, 
//									{ 8, 10, 4, 0, 20 },
//				                   { 9, 5, 3, 20, 0 } };

		double[][] matrixDistance = new double[n + 1][n + 1];
		for (int i = 0; i < matrixDistance.length; i++) {
			for (int j = i + 1; j < matrixDistance.length; j++) {
				if (carrier.equals("drone")) {
					double distance = GoogMatrixRequest.getDirectDistance(addresses[i], addresses[j]);
					BigDecimal bd = new BigDecimal(distance).setScale(2, RoundingMode.HALF_UP);
					double newDistance = bd.doubleValue();
					matrixDistance[i][j] = newDistance;
					matrixDistance[j][i] = newDistance;
				} else {
					double distanceIJ = GoogMatrixRequest.getBicyclingDistance(addresses[i], addresses[j]);
					double distanceJI = GoogMatrixRequest.getBicyclingDistance(addresses[j], addresses[i]);
					BigDecimal bd1 = new BigDecimal(distanceIJ).setScale(2, RoundingMode.HALF_UP);
					double newDistanceIJ = bd1.doubleValue();
					BigDecimal bd2 = new BigDecimal(distanceJI).setScale(2, RoundingMode.HALF_UP);
					double newDistanceJI = bd2.doubleValue();
					matrixDistance[i][j] = newDistanceIJ;
					matrixDistance[j][i] = newDistanceJI;
				}
			}
		}
		return matrixDistance;
	}

	private void TSP(double[][] matrixDistance, List<Order> orders) {
		if (matrixDistance == null || matrixDistance.length == 0 || matrixDistance[0].length == 0 || orders == null || orders.size() == 0) {
			return;
		}
		long startTime = System.nanoTime();
		// reference: https://blog.csdn.net/qq_39559641/article/details/101209534
		// the number of the vertices need to be visited
		int n = matrixDistance.length;
		// m represents the total addresses in the set
		int m = 1 << (n - 1);
		// M[i][j, k, l, o] represents the minimum distance starting from address i, the
		// addresses in the set{j, k, l, o}
		// need to be visited once and only once
		double[][] M = new double[n][m];
		// initialize M
		for (int i = 1; i < n; i++) {
			M[i][0] = matrixDistance[i][0];
		}

		for (int j = 1; j < m; j++) {
			for (int i = 0; i < n; i++) {
				M[i][j] = 1000.0;
				// if i is included in the set j, then skip it
				if (((j >> (i - 1)) & 1) == 1) {
					continue;
				}
				// if i is not included in the set j, then check all states in set j
				for (int k = 1; k < n; k++) {
					// if k is not in set j, skip it
					if (((j >> (k - 1)) & 1) == 0) {
						continue;
					}
					// j ^ (1 << (k - 1)) represents to exclude k from the set j
					M[i][j] = Math.min(M[i][j], matrixDistance[i][k] + M[k][j ^ (1 << (k - 1))]);
				}
			}
		}
		System.out.println("The distance matrix:");
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				System.out.print(matrixDistance[i][j] + " ");
			}
			System.out.println();
		}
		System.out.println();
		// getPath() will get the optimal path based on the M[][]
		List<Integer> path = getPath(matrixDistance, M);
		List<Integer> pathCopy = new ArrayList<>();
		for (int i = 0; i < path.size(); i++) {
			pathCopy.add(path.get(i));
		}
		updateTrackingDB(pathCopy, matrixDistance, orders);
		System.out.println("The minimum distance is " + String.format("%.2f", M[0][m - 1]) + " miles");
		StringBuilder pathString = new StringBuilder();
		pathString.append("station" + stationId).append("---> ");
		for (int i = 1; i < path.size(); i++) {
			pathString.append("order" + path.get(i))
					.append("(" + (matrixDistance[path.get(i - 1)][path.get(i)]) + " miles)").append("---> ");
		}
		pathString.append("station" + stationId)
				.append("(" + (matrixDistance[path.get(path.size() - 1)][0]) + " miles)");
		System.out.println("The shortest deliver path:");
		System.out.println(pathString.toString());
		long stopTime = System.nanoTime();
		System.out.println(
				"The number of TSP nodes: " + n + "; Time used to find the shortest path using TSP: " + (stopTime - startTime) + " millisecond");
		System.out.println();
		System.out.println();
	}
	
	private void updateTrackingDB(List<Integer> path, double[][] matrixDistance, List<Order> orders) {
		//path: 0 --> 4 --> 3 --> 2 --> 1
		int n = path.size();
		//path: 0 --> 4 --> 3 --> 2 --> 1 --> 0
		path.add(0);
		List<Order> shortestPathOrders = new ArrayList<>();
		for (int i = 1; i < n; i++) {
			shortestPathOrders.add(orders.get(path.get(i) - 1));
		}
		//find the intervals in seconds
		int[] prevDestiStartTimeInterval = new int[n];
		int[] expectedDeliverTimeInterval = new int[n];
		
		int previousExpectedDeliverTimeInterval = (int)(dispatchTime / 1000) - 300;
		for (int i = 0; i < n; i++) {
			prevDestiStartTimeInterval[i] = previousExpectedDeliverTimeInterval + 300;
			int start = path.get(i);
			int end = path.get(i + 1);
			double distance = matrixDistance[start][end];
			double speed = carrier.equals("drone") ? DRONE_SPEED : ROBOT_SPEED;
			int travelTimeInSecond = (int)(distance / speed * 60 * 60);
			expectedDeliverTimeInterval[i] = prevDestiStartTimeInterval[i] + travelTimeInSecond;
			previousExpectedDeliverTimeInterval = expectedDeliverTimeInterval[i];
		}
		//find the previous destination address
		String[] previousAddress = new String[n - 1];
		previousAddress[0] = stationId == 1 ? STATION1 : (stationId == 2 ? STATION2 : STATION3);
		for (int i = 1; i < n - 1; i++) {
			previousAddress[i] = shortestPathOrders.get(i - 1).getRecipientAddress();
		}
		//update previous destination, previous_destination_start_time, estimated_delivered_at, and status in tracking DB
		MySQLConnection connection = new MySQLConnection();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date cT = new Date();
		String cT1 = df.format(cT);
		System.out.println("Current date/time (base time): " + cT1);
		System.out.print("Previous_destination_start_time_interval (in second): ");
		for (int i = 0; i < prevDestiStartTimeInterval.length; i++) {
			System.out.print(prevDestiStartTimeInterval[i] + " ");
		}
		System.out.println();
		System.out.print("Estimated_deliver_time_interval (in second): ");
		for (int i = 0; i < expectedDeliverTimeInterval.length; i++) {
			System.out.print(expectedDeliverTimeInterval[i] + " ");
		}
		System.out.println();
		Date currentTime = null;
		try {
			currentTime = df.parse(cT1);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		long currentTimeInMS = currentTime.getTime();
		for (int i = 0; i < shortestPathOrders.size(); i++) {
			String trackingId = shortestPathOrders.get(i).getTrackingId();
			String previousAddr = previousAddress[i];
			long previousDestinationStartTime = currentTimeInMS + prevDestiStartTimeInterval[i] * 1000;
			String previousTime = df.format(new Date(previousDestinationStartTime));
			long estimatedDeliveredTime = currentTimeInMS + expectedDeliverTimeInterval[i] * 1000;
			String estimatedTime = df.format(new Date(estimatedDeliveredTime));
			connection.updateTrackingAfterDispatch(STATUS_DISPATCHED, previousAddr, previousTime, estimatedTime, trackingId);
		}
		System.out.println("For all selected orders: status was changed from ordered to dispatched; previous_destination_address, previous_destination_start_time, and estimated_deliver_time were updated in the tracking DB");
		connection.close();
		System.out.println("The dispatch time is " + (int)(dispatchTime / 1000) + " seconds");
		System.out.println("The default dispatch time is 480 seconds; however, if the machine is assigned from another station, the travel time from another station to current station will be added to the dispatch time.");
		scheduledIntransitStatusUpdate(shortestPathOrders);
		for (int i = 0; i < shortestPathOrders.size(); i++) {
			Order order = shortestPathOrders.get(i);
			int delay = expectedDeliverTimeInterval[i];
			scheduledDeliveredStatusUpdate(order.getTrackingId(), delay);
		}
		scheduledMachineAvailabilityUpdate(shortestPathOrders.get(0), expectedDeliverTimeInterval[expectedDeliverTimeInterval.length - 1]);
	}
	
	//update machine availability after finishing deliver
	private void scheduledMachineAvailabilityUpdate(Order order, int delay) {
		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
		scheduledExecutorService.schedule(new Runnable() {
			@Override
			public void run() {
				MySQLConnection connection = new MySQLConnection();
				int machineId = connection.getMachineIdInOrder(order.getOrderId());
				connection.flipMachineAvailability(machineId);
				System.out.println("Machine " + machineId + " is done with the jobs assigned and is available now");
				connection.close();
			}
		}, delay, TimeUnit.SECONDS);
		scheduledExecutorService.shutdown();
	}
	
	//update status in tracking from in transit to delivered
	private void scheduledDeliveredStatusUpdate(String trackingId, int delay) {
		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
		scheduledExecutorService.schedule(new Runnable() {
			@Override
			public void run() {
				MySQLConnection connection = new MySQLConnection();
				connection.updateStatusInTracking(STATUS_DELIVERED, trackingId);
				System.out.println("trackingId " + trackingId + ": status was updated from in transit to delivered");
				connection.close();
			}
		}, delay, TimeUnit.SECONDS);
		scheduledExecutorService.shutdown();
	}
	
	//update status in Tracking from dispatched to in transit after delay dispatchTime
	private void scheduledIntransitStatusUpdate(List<Order> shortestPathOrders) {
		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
		scheduledExecutorService.schedule(new Runnable() {
			@Override
			public void run() {
				MySQLConnection connection = new MySQLConnection();
				for (Order order : shortestPathOrders) {
					connection.updateStatusInTracking(STATUS_INTRANSIT, order.getTrackingId());
					System.out.println("trackingId " + order.getTrackingId() + ": status was updated from dispatched to in transit");
				}
				connection.close();
			}
		}, dispatchTime, TimeUnit.MILLISECONDS);
		scheduledExecutorService.shutdown();
	}
	
	//get TSP path
	private List<Integer> getPath(double[][] matrixDistance, double[][] M) {
		List<Integer> path = new ArrayList<>();
		int n = M.length;
		int m = M[0].length;
		// visited used to mark visited vertex
		boolean[] visited = new boolean[n];
		int parent = 0, set = m - 1, temp = 0;
		double min = 1000.0;
		path.add(0);
		while (!isVisited(visited)) {
			for (int i = 1; i < n; i++) {
				if (!visited[i] && (set & (1 << (i - 1))) != 0) {
					if (min > matrixDistance[parent][i] + M[i][(set ^ (1 << (i - 1)))]) {
						min = matrixDistance[parent][i] + M[i][(set ^ (1 << (i - 1)))];
						temp = i;
					}
				}
			}
			parent = temp;
			path.add(parent);
			visited[parent] = true;
			set ^= (1 << (parent - 1));
			min = 1000.0;
		}
		return path;
	}

	private boolean isVisited(boolean[] visited) {
		int n = visited.length;
		for (int i = 1; i < n; i++) {
			if (!visited[i]) {
				return false;
			}
		}
		return true;
	}

//	public static void main(String[] args) {
//		System.out.println(Scheduler.dispatchOrders());
//		String[] array = new String[4];
//		int[][] matrixDistance = buildMatrixDistance(array);
//		TSP(matrixDistance);
//		Scheduler scheduler = new Scheduler(1, "robot");
//		try {
//			scheduler.dispatchOrders();
//		} catch (ApiException | InterruptedException | IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		MySQLConnection connection = new MySQLConnection();
//		connection.updateStatusInTracking(STATUS_INTRANSIT, "1232");
//		connection.close();
//		System.out.println(stationId);
//		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		Date cT = new Date();
//		String cT1 = df.format(cT);
//		System.out.println(cT1);
//		Date currentTime = null;
//		try {
//			currentTime = df.parse(cT1);
//		} catch (ParseException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		long currentTimeInMS = currentTime.getTime();
//		long nextTimeInMS = currentTimeInMS + 8 * 60000;
//		System.out.println(df.format(new Date(nextTimeInMS)));
//		Order order1 = new Order();
//		order1.setTrackingId("1232");
//		Order order2 = new Order();
//		order2.setTrackingId("1234");
//		Order order3 = new Order();
//		order3.setTrackingId("4qXomjfo");
//		Order order4 = new Order();
//		order4.setTrackingId("ph6gBSf3");
//		Order order5 = new Order();
//		order5.setTrackingId("w5WEY9br");
//		List<Order> orders = new ArrayList<>();
//		orders.add(order1);
//		orders.add(order2);
//		orders.add(order3);
//		orders.add(order4);
//		orders.add(order5);
//		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
//		scheduledExecutorService.schedule(new Runnable() {
//			@Override
//			public void run() {
//				System.out.println("second thread is started");
//				System.out.println(java.time.LocalDateTime.now()); 
//				MySQLConnection connection = new MySQLConnection();
//				
//				connection.updateStatusInTracking(STATUS_INTRANSIT, "8AtMICKi");
//				
//				connection.close();
//				System.out.println("Done");
//			}
//		}, 5 * 1000, TimeUnit.MILLISECONDS);
//		scheduledExecutorService.shutdown();
//		System.out.println(java.time.LocalDateTime.now()); 
//		System.out.println("main thread is done");
//		MySQLConnection connection = new MySQLConnection();
//		int machineId = connection.getMachineIdInOrder("3IRH4UCY");
//		connection.flipMachineAvailability(machineId);
//		connection.close();
//		System.out.println(machineId);
//	}
}
