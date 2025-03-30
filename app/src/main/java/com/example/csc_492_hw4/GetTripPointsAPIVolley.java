package com.example.csc_492_hw4;

import android.net.Uri;
import android.util.Log;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class GetTripPointsAPIVolley {
    private static final String TAG = "PointsGetter";
    private static final String dataUrl =
            "http://christopherhield-001-site4.htempurl.com/api/Datapoints/GetTrip";
    public static final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
    private final RequestQueue queue;
    private final TripFollowerActivity mainActivity;


    public GetTripPointsAPIVolley(TripFollowerActivity mainActivity)  {
        this.mainActivity = mainActivity;
        this.queue = Volley.newRequestQueue(mainActivity);
    }

    public void getPoints(String tripId) {

        Uri.Builder buildURL = Uri.parse(dataUrl).buildUpon();
        buildURL.appendPath(tripId);
        String urlToUse = buildURL.build().toString();

        Response.Listener<JSONArray> listener = results -> {
            Log.d(TAG, "onResponse: " + results);
            ArrayList<LatLngTime> points = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject jo = results.optJSONObject(i);
                try {
                    double lat = jo.optDouble("latitude");
                    double lon = jo.optDouble("longitude");
                    Date d = sdf.parse(jo.optString("datetime"));
                    points.add(new LatLngTime(lat, lon, d));
                } catch (Exception e) {
                    Log.d(TAG, "getPoints: " + e.getMessage());
                }
            }
            mainActivity.acceptInitialPathPoints(points);
        };

        Response.ErrorListener error = volleyError -> {
            if (volleyError instanceof NoConnectionError) {
                Log.d(TAG, "No network connection");
                mainActivity.NoNetworkEarlyPoints();
            }
            else{
                if (volleyError.networkResponse != null) {
                    if (volleyError.networkResponse.statusCode == 404) {
                        mainActivity.tripNotFound(tripId);
                        return;
                    }
                    String s = new String(volleyError.networkResponse.data);
                    Log.d(TAG, "onErrorResponse: " + s);
                    mainActivity.getTripPointsError(s, tripId);
                }
                Log.d(TAG, "getPoints: " + volleyError.getMessage());
            }


        };

        // Request a string response from the provided URL.
        JsonArrayRequest jsonArrayRequest =
                new JsonArrayRequest(
                        Request.Method.GET,
                        urlToUse,
                        null,
                        listener,
                        error);

        queue.add(jsonArrayRequest);
    }
}
