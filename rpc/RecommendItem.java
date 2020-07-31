package rpc;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;

import external.GoogMatrixRequest;

/**
 * Servlet implementation class RecommendItem
 */
public class RecommendItem extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public RecommendItem() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		GeoApiContext context = new GeoApiContext.Builder().apiKey("YOUR API KEY").build();
		GeocodingResult[] results = null;
		try {
			results = GeocodingApi.geocode(context, "1600 Amphitheatre Parkway Mountain View, CA 94043").await();
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(gson.toJson(results[0].addressComponents));
		JSONObject obj = new JSONObject();
		obj.put("result", gson.toJson(results[0].addressComponents));
		RpcHelper.writeJsonObject(response, obj);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		JSONObject input = RpcHelper.readJSONObject(request);
		//senderAddr as stationAddr
		String stationId= input.getString("address");
		String stationAddr;
		if (stationId.equals("")) { //默认0.00 price
			stationAddr = "";
		} else if (stationId.equals("1")) {
			stationAddr = "Parkside, San Francisco, CA, USA";
		} else if (stationId.equals("2")) {
			stationAddr = "Mission District, San Francisco, CA, USA";
		} else {
			stationAddr = "Excelsior, San Francisco, CA, USA";
		}
		//String stationAddr = input.getString("address");
		String receiverAddr = input.getString("receiverAddr");
		double weight = input.getDouble("weight");
		// change to one decimal
		weight = new BigDecimal(weight).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		double length = input.getDouble("length");
		double width = input.getDouble("width");
		double height = input.getDouble("height");
		boolean fragile = input.getBoolean("fragile");
		
		// time, price // per row as per item
		// time, price
		double[][] result = new double[6][2];
		try {
			result = GoogMatrixRequest.getDistance(stationAddr, receiverAddr, weight, length, width, height, fragile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		JSONObject obj = new JSONObject();
//		obj.put("Drone Estimated Delivery Time (fastest)", String.format("%.2f", result[0][0]) + " hour(s)");
//		obj.put("Drone Price (fastest)", "$" + String.format("%.2f", result[0][1]));
//		obj.put("Robot Estimated Delivery Time (cheapest)", String.format("%.2f", result[1][0]) + " hour(s)");
//		obj.put("Robot Price (cheapest)", "$" + String.format("%.2f", result[1][1]));
//		RpcHelper.writeJsonObject(response, obj);

		double max = length >= width ? length : width;
		  max = height >= max ? height : max;
		
		// response words in the front end. time (3种标准即可), carrier, price, + words // add time feature name; and add fastest/cheapest
		JSONArray array = new JSONArray();
		
		 if (weight > 20) {
			  //neither, warning weight, no need to check dimension, no need to check fragile.
			 array.put(new JSONObject().put("Deliverable", "No"));
		  } 
		 
		  else if (weight > 0 && weight <= 5) {
			  if (max > 25.00) {
				  // neither, warning dimension, no need to check fragile
				  array.put(new JSONObject().put("Deliverable", "No"));
			  } else if (max > 13 && max <= 25) { 
				  // array.put(new JSONObject().put("dispatch within: ", "30 mins").put("carrier", "robot").put("time", String.format("%.2f", result[0][0]))
					//.put("price", String.format("%.2f", result[0][1])));
				  array.put(new JSONObject().put("dispatch within: ", "30 mins").put("carrier", "robot").put("price", String.format("%.1f", result[0][1])));
					array.put(new JSONObject().put("dispatch within: ", "1 hour").put("carrier", "robot").put("price", String.format("%.1f", result[1][1])));
					array.put(new JSONObject().put("dispatch within: ", "2 hours").put("carrier", "robot").put("price", String.format("%.1f", result[2][1])));
			  } 
		  else {
					// method 1 robot
				  if (fragile) {
					  array.put(new JSONObject().put("dispatch within: ", "30 mins").put("carrier", "robot").put("price", String.format("%.1f", result[0][1])));
						array.put(new JSONObject().put("dispatch within: ", "1 hour").put("carrier", "robot").put("price", String.format("%.1f", result[1][1])));
						array.put(new JSONObject().put("dispatch within: ", "2 hours").put("carrier", "robot").put("price", String.format("%.1f", result[2][1])));
				  } else {
					  array.put(new JSONObject().put("dispatch within: ", "30 mins").put("carrier", "drone").put("price", String.format("%.1f", result[0][1])));
						array.put(new JSONObject().put("dispatch within: ", "1 hour").put("carrier", "drone").put("price", String.format("%.1f", result[1][1])));
						array.put(new JSONObject().put("dispatch within: ", "2 hours").put("carrier", "drone").put("price", String.format("%.1f", result[2][1])));
					  array.put(new JSONObject().put("dispatch within: ", "30 mins").put("carrier", "robot").put("price", String.format("%.1f", result[3][1])));
						array.put(new JSONObject().put("dispatch within: ", "1 hour").put("carrier", "robot").put("price", String.format("%.1f", result[4][1])));
						array.put(new JSONObject().put("dispatch within: ", "2 hours").put("carrier", "robot").put("price", String.format("%.1f", result[5][1])));
				  }
		  }
		  }
		 // 5 < weight <= 20
		  else {
			  if (max > 25.00) {
				  // neither, warning dimension, no need to check fragile
				  array.put(new JSONObject().put("Deliverable", "No"));
			  } else if (max <= 25.00) {
				  array.put(new JSONObject().put("dispatch within: ", "30 mins").put("carrier", "robot").put("price", String.format("%.1f", result[0][1])));
					array.put(new JSONObject().put("dispatch within: ", "1 hour").put("carrier", "robot").put("price", String.format("%.1f", result[1][1])));
					array.put(new JSONObject().put("dispatch within: ", "2 hours").put("carrier", "robot").put("price", String.format("%.1f", result[2][1])));
			  }
		  }
		 
		  
		
		RpcHelper.writeJsonArray(response, array); //统一
	}
}