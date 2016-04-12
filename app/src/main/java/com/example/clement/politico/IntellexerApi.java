package com.example.clement.politico;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;


/**
 * Wrap up for intellexer API
 *
 */
public class IntellexerApi {
    static private final String URL_API = "http://api.intellexer.com/";
    static private final String KEY = "apiKey=bc783f08-3739-4a57-b299-e6def9797d15";
    static public final int POSITIVE = 0;
    static public final int NEUTRAL = 1;
    static public final int NEGATIVE = 2;

    // Nbr of sentences in the summary
    private int SUM_RESTRICTION = 4;

    // Summarizes from a text: calls API and parses JSON.
    // Returns summary as a list of sentences
    public List<String> summarizeFromText(String text) {
        // building request
        StringBuilder request = new StringBuilder();
        request.append(URL_API + "summarizeText?" + KEY);
        request.append("&summaryRestriction=" + SUM_RESTRICTION + "&returnedTopicsCount=1");

        String jsonString = performPostCall(request.toString(), "application/raw", text);
        return parseJsonSummary(jsonString);
    }

    // Summarizes from URL: calls API and parses JSON
    // Returns summary as a list of sentences
    public List<String> summarizeFromUrl(String url) {
        // building request
        StringBuilder request = new StringBuilder();
        request.append(URL_API + "summarize?" + KEY);
        request.append("&url=" + url);
        request.append("&summaryRestriction=" + SUM_RESTRICTION + "&returnedTopicsCount=1");

        String jsonString = performPostCall(request.toString(), "application/json", "[]");

        return parseJsonSummary(jsonString);
    }

    // Get sentiment analysis of text: calls API and parses JSON
    // Returns sentiment analysis as a list of weight each representing
    // positive, neutral and negative sentiments
    public List<Integer> sentimentFromText(String text) {
        // building request
        StringBuilder request = new StringBuilder();
        request.append(URL_API + "analyzeSentiments?" + KEY);
        request.append("&loadSentences=true");

        // preparing content of POST message
        String content = "[{\"id\":\"id01\", \"text\":\"" + convertToJsonFormat(text) + "\"}]";
        String jsonString = performPostCall(request.toString(), "application/json", content);

        return parseJsonSentiment(jsonString);
    }

    // Converts special characters to JSON format so that the API
    // can correctly identifies quotes and returns a better analysis
    // TODO: to be improved
    static private String convertToJsonFormat(String text) {
        String jsonCompat = new String(text);
        jsonCompat = jsonCompat.replaceAll("[“”]", "\"");
        //jsonCompat = jsonCompat.replaceAll("’", "'");
        jsonCompat = jsonCompat.replaceAll("\n", " ");
        jsonCompat = jsonCompat.replaceAll("\"", "\\\\\"");
        return jsonCompat;
    }

    // Generic method to POST messages to the API
    static private String performPostCall(String urlString, String contentType, String content) {
        HttpURLConnection urlConnection=null;
        String response = "";
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.setRequestProperty("Content-Type", contentType);

            urlConnection.connect();

            // Send POST output.
            DataOutputStream printout = new DataOutputStream(urlConnection.getOutputStream());
            //printout.writeUTF(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
            //printout.writeUTF("[]");
            byte[] data = content.getBytes("UTF-8");
            printout.write(data);
            printout.flush();
            printout.close();

            int responseCode = urlConnection.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else {
                System.out.println(urlConnection.getResponseMessage());
                response = "";
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    // Parses JSON returned from API and extracts summary.
    // Returns summary as a list of sentences
    static private List<String> parseJsonSummary(String jsonString) {
        ArrayList<String> summary = new ArrayList<>();

        try {
            JSONObject jsonRootObject = new JSONObject(jsonString);

            //Get the instance of JSONArray that contains JSONObjects
            JSONArray jsonArray = jsonRootObject.optJSONArray("items");

            //Iterate the jsonArray and print the info of JSONObjects
            for (int i = 0; i < jsonArray.length(); ++i){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                summary.add(jsonObject.optString("text").toString());
            }
        } catch (JSONException e) {e.printStackTrace();}
        return summary;
    }

    // Parses JSON returned from API and extracts sentiment analysis.
    // Returns sentiment analysis as a list of integers representing
    // weight for positive, neutral and negative sentiments
    static private List<Integer> parseJsonSentiment(String jsonString) {
        // positive, neutral and negative sentiments from the text
        Integer[] sentiment = {0, 0, 0};

        try {
            JSONObject jsonRootObject = new JSONObject(jsonString);

            //Get the instance of JSONArray that contains JSONObjects
            JSONArray jsonArray = jsonRootObject.optJSONArray("sentences");

            //Iterate the jsonArray and print the info of JSONObjects
            for (int i=0; i < jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                double weight = Double.parseDouble(jsonObject.optString("w"));
                if (weight > 0) {
                    ++sentiment[POSITIVE];
                }
                else if (weight == 0) {
                    ++sentiment[NEUTRAL];
                }
                else {
                    ++sentiment[NEGATIVE];
                }
            }
        } catch (JSONException e) {e.printStackTrace();}

        return new ArrayList<Integer>(Arrays.asList(sentiment));
    }
}