package com.example.csc_492_hw4;

import android.net.Uri;
import android.util.Log;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class CreateUserAccountAPIVolley {

    private static final String dataUrl =
            "http://christopherhield-001-site4.htempurl.com/api/UserAccounts/CreateUserAccount";

    private static final String TAG = "CreateUserAccountAPIVol";

    private final RequestQueue queue;
    private final String urlToUse;
    private final MainActivity mainActivity;


    public CreateUserAccountAPIVolley(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.queue = Volley.newRequestQueue(mainActivity);
        Uri.Builder buildURL = Uri.parse(dataUrl).buildUpon();
        urlToUse = buildURL.build().toString();
    }

    public void createUser(String firstName, String lastName,
                           String email, String username, String password) {

        Log.d(TAG, "createUser: " + firstName + " " + lastName + " " + email + " " + username + " " + password);

        Response.Listener<JSONObject> listener = jsonObject -> {
            Log.d(TAG, "onResponse: " + jsonObject);
            mainActivity.handleCreateUserAccountSuccess(
                    jsonObject.optString("firstName"),
                    jsonObject.optString("lastName"),
                    jsonObject.optString("email"),
                    jsonObject.optString("userName"));
        };

        Response.ErrorListener error = volleyError -> {
            Log.d(TAG, "onErrorResponse1: " + volleyError.getMessage());

            if (volleyError instanceof NoConnectionError) {
                Log.d(TAG, "No network connection");
                mainActivity.noNetworkRegister();
            }
            else{
                if (volleyError.networkResponse != null) {
                    String s = new String(volleyError.networkResponse.data);
                    Log.d(TAG, "onErrorResponse2: " + s);
                    mainActivity.handleCreateUserAccountFail(s);
                } else {
                    mainActivity.handleCreateUserAccountFail(volleyError.getMessage());
                }
            }

        };

        // Make JSON object to write to the POST endpoint

        JSONObject jsonParams;
        try {
            jsonParams = new JSONObject();
            jsonParams.put("firstName", firstName);
            jsonParams.put("lastName", lastName);
            jsonParams.put("userName", username);
            jsonParams.put("password", password);
            jsonParams.put("email", email);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        // Request a string response from the provided URL.
        JsonObjectRequest jsonObjectRequest =
                new JsonObjectRequest(
                        Request.Method.POST,
                        urlToUse,
                        jsonParams,
                        listener,
                        error);

        queue.add(jsonObjectRequest);

    }
}
