package external;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;

import com.google.maps.DirectionsApi.RouteRestriction;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.TravelMode;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import java.util.ArrayList;
import java.util.List;
import com.google.maps.DirectionsApi;
import com.google.maps.RoadsApi;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.LatLng;
import com.google.maps.model.SnappedPoint;

public class GoogMatrixRequest {
	// Earth's mean radius in meter
		private static final int R = 6378137;
		private static final String API_KEY = "AIzaSyBdIJ5MrQ4rwNhyx52hGx2J3KDwftrGps0";
//		private static final String API_KEY = "AIzaSyC91YIc2UnTLDQ8FX3sfgHce1lr1AWmyjY";
		// set up key
		private static final GeoApiContext distCalcer = new GeoApiContext.Builder().apiKey(API_KEY).build();

		private OkHttpClient client;
	
//  private String addrOne;
//  private String addrTwo;
//  private double weight;
//  private boolean mode;
  
//  public GoogMatrixRequest(String addrOne, String addrTwo, double weight, boolean mode) {
//	   this.client = new OkHttpClient();
//	   this.addrOne = addrOne;
//	   this.addrTwo = addrTwo;
//	   this.weight = weight;
//	   this.mode = mode;
//  }

  public String run(String url) throws IOException {
	  Request request = new Request.Builder()
			  .url(url)
			  .build();
	  okhttp3.Response response = client.newCall(request).execute();
	  // response.body().rows[0].elements[0].distance.value
	  System.out.println(response);
	  return response.body().string();
  }
  
  public static double getBicyclingDistance(String addrOne, String addrTwo) throws ApiException, InterruptedException, IOException{
	   	
	  DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(distCalcer); 
	  TravelMode travel;
	  
	  travel = TravelMode.BICYCLING;
	  
	  DistanceMatrix result = req.origins(addrOne)
			  .destinations(addrTwo)
	          .mode(travel)
	          .avoid(RouteRestriction.TOLLS)
	          .language("en-US")
	          .await();
	  double distApart = result.rows[0].elements[0].distance.inMeters * 0.000621371192;
	  return distApart;
  }
  
  public static double getDirectDistance (String stationAddr, String receiverAddr) throws ApiException, InterruptedException, IOException {
	  
	  GeocodingResult[] resultOrigin = GeocodingApi.geocode(distCalcer, stationAddr).await();
	  GeocodingResult[] resultDestination = GeocodingApi.geocode(distCalcer, receiverAddr).await();
	  double lat1 = resultOrigin[0].geometry.location.lat;
	  double lng1 = resultOrigin[0].geometry.location.lng;
	  double lat2 = resultDestination[0].geometry.location.lat;
	  double lng2 = resultDestination[0].geometry.location.lng;
	  
	  double dlng = Radians(lng2 - lng1);
	  double dlat = Radians(lat2 - lat1);
	  
	  double a = (Math.sin(dlat / 2) * Math.sin(dlat / 2)) + Math.cos(Radians(lat1)) * Math.cos(Radians(lat2)) * (Math.sin(dlng / 2) * Math.sin(dlng / 2));
      double angle = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
      //return the distance in miles
      return R * angle * 0.000621371192;
  }
  
  private static double Radians (double x) {
	  return x * Math.PI / 180;
  }
  
  /**
	 * Convert the address from String to lat/lng double 
	 * 
	 * @param address
	 * @return
	 * @throws ApiException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static double[] getLatLngDouble(String address) throws ApiException, InterruptedException, IOException {
		double[] latLng = new double[2];
		latLng[0] = getLatLng(address).lat;
		latLng[1] = getLatLng(address).lng;
		return latLng;
	}
	
	/**
	 * Convert the address from String to LatLng 
	 * 
	 * @param address
	 * @return
	 * @throws ApiException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static LatLng getLatLng(String address) throws ApiException, InterruptedException, IOException {
		GeocodingResult[] result = GeocodingApi.geocode(distCalcer, address).await();
		LatLng latLng = result[0].geometry.location;
		return latLng;
	}
	
	/**
	 * Calculate the current location based on the complete ratio of the total distance from station and receiver. 
	 * 
	 * @param stationAddr
	 * @param receiverAddr
	 * @param ratio 		
	 * @return
	 * @throws ApiException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static LatLng getNewLocation(String stationAddr, String receiverAddr, double ratio)
			throws ApiException, InterruptedException, IOException {
		LatLng newLatLng = null;
		GeocodingResult[] resultOrigin = GeocodingApi.geocode(distCalcer, stationAddr).await();
		GeocodingResult[] resultDestination = GeocodingApi.geocode(distCalcer, receiverAddr).await();
		double lat1 = resultOrigin[0].geometry.location.lat;
		double lng1 = resultOrigin[0].geometry.location.lng;
		double lat2 = resultDestination[0].geometry.location.lat;
		double lng2 = resultDestination[0].geometry.location.lng;
		double dlng = Radians(lng2 - lng1);
		double dlat = Radians(lat2 - lat1);

		double a = (Math.sin(dlat / 2) * Math.sin(dlat / 2))
				+ Math.cos(Radians(lat1)) * Math.cos(Radians(lat2)) * (Math.sin(dlng / 2) * Math.sin(dlng / 2));
		double angle = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		// distance in km
		double wholeDistance = R * angle;

		double dx = ratio * wholeDistance * Math.sin(angle);
		double dy = ratio * wholeDistance * Math.cos(angle);

		double curr_lat = lat1 + Radians(dy / R);
		double curr_log = lng1 + Radians(dx / R) / Math.cos(Radians(lat1));
		newLatLng.lat = curr_lat;
		newLatLng.lng = curr_log;
		// convert lat/lng to formatted address, don't need for now
		// String currAddress = GeocodingApi.reverseGeocode(distCalcer, new
		// LatLng(curr_lat, curr_log)).await()[0].formattedAddress;

		return newLatLng;
	}

	/**
	 * Interpolate the route points between sender and receiver location using Road Api. 
	 * The performance is acceptable when the distance of points in the path is less than 300m so we don't use it here.  
	 * 
	 * @param sender   	latitude and longitude of the sender location 
	 * @param receiver  latitude and longitude of the receiver location
	 * @return
	 * @throws ApiException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static double[][] getRoutePoints(double[] sender, double[] receiver)
			throws ApiException, InterruptedException, IOException {

		LatLng[] path = new LatLng[] { new LatLng(sender[0], sender[1]), new LatLng(receiver[0], receiver[1]) };
		System.out.print(sender[0]);
		SnappedPoint[] points = RoadsApi.snapToRoads(distCalcer, true, path).await();
		int n = points.length;
		double[][] result = new double[n][2];

		for (int i = 0; i < n; i++) {
			result[i][0] = points[i].location.lat;
			result[i][1] = points[i].location.lng;
		}
		return result;
	}

	/**
	 * Get the location points on the route from senderAdd to receiverAddr, using Direction Api.
	 * 
	 * @param senderAddr
	 * @param receiverAddr
	 * @return
	 * @throws ApiException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static List<LatLng> getDirectionPoints(String senderAddr, String receiverAddr)
			throws ApiException, InterruptedException, IOException {
		DirectionsResult result = DirectionsApi.newRequest(distCalcer).mode(TravelMode.BICYCLING).origin(senderAddr)
				.destination(receiverAddr).await();
		DirectionsStep[] res = result.routes[0].legs[0].steps;
//		System.out.println(res.length);
		List<LatLng> pointsLatLngs = new ArrayList<>();
		for (int i = 0; i < res.length; i++) {
			pointsLatLngs.add(res[i].startLocation);
			System.out.println(res[i].startLocation);
		}
		return pointsLatLngs;
	}

//  .avoid(
//  DirectionsApi.RouteRestriction.HIGHWAYS,
//  DirectionsApi.RouteRestriction.TOLLS,
//  DirectionsApi.RouteRestriction.FERRIES)

  public static double[][] getDistance(String stationAddr, String receiverAddr, double weight, double length, double width, double height, boolean fragile) throws IOException, ApiException, InterruptedException {
	  // max dimension at any side
	  double max = length >= width ? length : width;
	  max = height >= max ? height : max;
	  
	  boolean mode = true; // true as drone; false as robot
	  double[][] result = new double[6][2]; //空出
	  /* case 1. weight > 20,  //neither, warning weight, no need to check dimension, no need to check fragile.*/
	  if (weight > 20) {
		  //neither, warning weight, no need to check dimension, no need to check fragile.
		  return result;
	  } 
	  /* case 2. 0 < weight <= 5, drone or robot, 
		 then, 1) dimension max > 25.00, neither, warning dimension, no need to check fragile
		  	   2) dimension 13 < max <= 25, only robot, no need to check fragile
		  	 	   			recommend robot: option a. fastest <=30 mins, shipping cost * 1.2;  option b.  <= 1 hour, shipping cost * 1.1, option c. cheapest <=2 hours, shipping cost
		  	 	   		    //speed not change, priority change.
		  	   2) dimension max <= 13, drone or robot, check fragile // for testing
		  	 	   			(1) fragile: recommend robot: option a. fastest <=30 mins, shipping cost * 1.2;  option b. <= 1 hour, shipping cost * 1.1 c. cheapest <=2 hours, shipping cost
		  	 	   			(2) not fragile: recommend drone or robot: option a. drone fastest <=30 mins, shipping cost * 1.2;  option b. drone <= 1 hour, shipping cost * 1.1; option  c. cheapest <=2 hours, shipping cost
		  	 	   																d. robot <=30 mins, shipping cost * 1.2;  option e. robot <= 1 hour, shipping cost * 1.1; 	f. robot cheapest <=2 hours, shipping cost
		  		 * 
		  		 */
	  else if (weight > 0 && weight <= 5) {
		  if (max > 25.00) {
			  // neither, warning dimension, no need to check fragile
			  return result;
			  
		  } else if (max > 13 && max <= 25) {
		      mode = false;
			  double robotDistance = getBicyclingDistance(stationAddr, receiverAddr);
			  // 30 mins sends
				result[0][0] = robotDistance / 10; //? time return
				result[0][1] = calculatePrice(weight, robotDistance, false, length, width, height, fragile) * 1.2; // fragile or true or false?
			  //double droneDistance = getDirectDistance(stationAddr, receiverAddr);
			  //result[0][0] = droneDistance / 50;
				//need hardcord fastest, cheapest, <=30, <= 1 hour
			  // 1 hour sends
				result[1][0] = robotDistance / 10; // ？不用返回也不应该返回时间
				result[1][1] = calculatePrice(weight, robotDistance, false, length, width, height, fragile) * 1.1;
				
			  // 2 hours sends
				result[2][0] = robotDistance / 10; // ？不用返回也不应该返回时间
				result[2][1] = calculatePrice(weight, robotDistance, false, length, width, height, fragile);
		  } else {
			// method 1 drone
			  if (fragile) {
				  //mode = false;
				  double robotDistance = getBicyclingDistance(stationAddr, receiverAddr);
				  // 30 mins sends
					result[0][0] = robotDistance / 10; //? time return
					result[0][1] = calculatePrice(weight, robotDistance, false, length, width, height, true) * 1.2;
				  //double droneDistance = getDirectDistance(stationAddr, receiverAddr);
				  //result[0][0] = droneDistance / 50;
					//need hardcord fastest, cheapest, <=30, <= 1 hour
				  // 1 hour sends
					result[1][0] = robotDistance / 10; // ？不用返回也不应该返回时间
					result[1][1] = calculatePrice(weight, robotDistance, false, length, width, height, true) * 1.1;
					
				  // 2 hours sends
					result[2][0] = robotDistance / 10; // ？不用返回也不应该返回时间
					result[2][1] = calculatePrice(weight, robotDistance, false, length, width, height, true);
				  
			  } else {
				  // mode = true drone
				  double droneDistance = getDirectDistance(stationAddr, receiverAddr);
				  // 30 mins sends
					result[0][0] = droneDistance / 50; //? time return
					result[0][1] = calculatePrice(weight, droneDistance, true, length, width, height, false) * 1.2;
				  //double droneDistance = getDirectDistance(stationAddr, receiverAddr);
				  //result[0][0] = droneDistance / 50;
					//frontend: need hardcord fastest, cheapest, <=30, <= 1 hour
				  // 1 hour sends
					result[1][0] = droneDistance / 50; // ？不用返回也不应该返回时间
					result[1][1] = calculatePrice(weight, droneDistance, true, length, width, height, false) * 1.1;
					
				  // 2 hours sends
					result[2][0] = droneDistance / 50; // ？不用返回也不应该返回时间
					result[2][1] = calculatePrice(weight, droneDistance, true, length, width, height, false);
					
					// mode = false robot
					  double robotDistance = getBicyclingDistance(stationAddr, receiverAddr);
					  // 30 mins sends
						result[3][0] = robotDistance / 10; //? time return
						result[3][1] = calculatePrice(weight, robotDistance, false, length, width, height, false) * 1.2;
					  //double droneDistance = getDirectDistance(stationAddr, receiverAddr);
					  //result[0][0] = droneDistance / 50;
						//need hardcord fastest, cheapest, <=30, <= 1 hour
					  // 1 hour sends
						result[4][0] = robotDistance / 10; // ？不用返回也不应该返回时间
						result[4][1] = calculatePrice(weight, robotDistance, false, length, width, height, false) * 1.1;
						
					  // 2 hours sends
						result[5][0] = robotDistance / 10; // ？不用返回也不应该返回时间
						result[5][1] = calculatePrice(weight, robotDistance, false, length, width, height, false);
			  }
			

			  }


	  }

		 /* case 3. 
		   * 5 < weight <= 20, only robot.
		   * then, 1) dimension max > 25.00, warning dimension is to large. should not exceed 25.00. personalized suggestion. 
		   * 	   2) dimension max <= 25, robot, fragile or not 都是: recommend robot, option a. fastest <=30 mins, shipping cost * 1.2;  option b.  <= 1 hour, shipping cost * 1.1
		   * 											c. cheapest <= 2 hours, shipping cost 
		   */
	  else {
		  
		  if (max > 25.00) {
			  return result; // warning dimension is to large. 
		  } else if (max <= 25) {
			// mode = false robot
			  double robotDistance = getBicyclingDistance(stationAddr, receiverAddr); // 显示function
			  // 30 mins sends
				result[0][0] = robotDistance / 10; //? time return
				result[0][1] = calculatePrice(weight, robotDistance, false, length, width, height, fragile) * 1.2;
			  //double droneDistance = getDirectDistance(stationAddr, receiverAddr);
			  //result[0][0] = droneDistance / 50;
				// need hardcord fastest, cheapest, <=30, <= 1 hour
			  // 1 hour sends
				result[1][0] = robotDistance / 10; // ？不用返回也不应该返回时间
				result[1][1] = calculatePrice(weight, robotDistance, false, length, width, height, fragile) * 1.1;
				
			  // 2 hours sends
				result[2][0] = robotDistance / 10; // ？不用返回也不应该返回时间
				result[2][1] = calculatePrice(weight, robotDistance, false, length, width, height, fragile);
		  }
		 
		  
	  }
		  
		  
//	  GoogMatrixRequest request = new GoogMatrixRequest();
//	  String url_request = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=Seattle&destinations=San+Francisco&mode=bicycling&language=en-GB&key=" + API_KEY;
//	  String response = request.run(url_request);
//	  long robotDistance = response.rows[0].elements[0].distance.value;
//	  System.out.println(calculatePrice(weight, robotDistance, false));
	  return result;
  }
   
  public static double calculatePrice(double weight, double distance, boolean mode, double length, double width, double height, boolean fragile) {
	  // from, to, weight, mode
	  double cost = 0.0;

	  
	  // query db to get data, assume we already have base price, additional rate
	  if (mode == true) {
			// method 1 drone
			cost = 3.99 + distance * weight * 0.4;
		} else {
			// method 2 robot
			cost = 1.99 + distance * weight * 0.2;
		}
		if (mode) {
			System.out.println("Vehicle: Drone; The shipping cost: " + cost + "; The distance: " + distance);
		} else {
			System.out.println("Vehicle: Robot; The shipping cost: " + cost + "; The distance: " + distance);
		}
		 cost = new BigDecimal(cost).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		return cost;
	  
	  } 
  }