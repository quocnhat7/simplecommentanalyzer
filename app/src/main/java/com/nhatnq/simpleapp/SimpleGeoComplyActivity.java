package com.nhatnq.simpleapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nhatnq.simpleapp.model.GeocomplyUrlItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleGeoComplyActivity extends AppCompatActivity {

    //Text field for user input
    private EditText edtInput;
    //Click to run your analyser
    private Button btnAnalyze;
    //Show JSON result
    private TextView tvResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.geocomply);

        tvResult = (TextView) findViewById(R.id.textview);

        edtInput = (EditText) findViewById(R.id.edittext);
        edtInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String content = editable.toString();
                SimpleGeoComplyActivity.this.btnAnalyze.setEnabled(!TextUtils.isEmpty(content));
            }
        });

        btnAnalyze = (Button) findViewById(R.id.button);
        btnAnalyze.setEnabled(false);
        btnAnalyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputAnalyzer analyzer = new InputAnalyzer();
                String input = edtInput.getText().toString();
                if(TextUtils.isEmpty(input)){
                    Toast.makeText(getBaseContext(), R.string.empty_input, Toast.LENGTH_SHORT).show();
                }else {
                    analyzer.execute(input);
                }
            }
        });

        edtInput.setText("@billgates do you know where is @elonmusk? and Olympics 2020 is happening; https://olympics.com/tokyo-2020/en/ đâ d a");
    }

    /**
     * Analyze user input to find 'mentions' and 'links'
     * @param input Text input from user
     * @return JSON object which includes all mentions and links
     */
    private JSONObject analyze(String input){
        List<String> mentions = new LinkedList<>();
        List<GeocomplyUrlItem> urlItems = new LinkedList<>();

        //Regex of web page title tag
        final Pattern PAGE_TITLE = Pattern.compile("\\<title>(.*)\\</title>", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);

        Matcher matcher = Patterns.WEB_URL.matcher(input);
        while(matcher.find()){
            String url = matcher.group();
            String title = "";
            try{
                URL link = new URL(url);
                BufferedReader br = new BufferedReader(new InputStreamReader(link.openStream()));

                String line = "";
                while ((line = br.readLine()) != null) {
                    //Try to match each line immediately with TITLE regex
                    Matcher webTitleMatcher = PAGE_TITLE.matcher(line);
                    if (webTitleMatcher.find()) {
                        /* replace any occurrences of whitespace (which may
                         * include line feeds and other uglies) as well
                         * as HTML brackets with a space */
                        title = webTitleMatcher.group(1).replaceAll("[\\s\\<>]+", " ").trim();
                        //Stop now as we found it ^^
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                //If we can not find the title (wrong url or other reasons, ...)
                title = url;
            }

            urlItems.add(new GeocomplyUrlItem(title, url));
        }

        //Regex of mention format
        final Pattern MENTION = Pattern.compile("@(?:[a-zA-Z0-9]+)");
        matcher = MENTION.matcher(input);
        while(matcher.find()){
            String mention = matcher.group();
            //Remove '@' at the beginning
            mention = mention.substring(1);
            mentions.add(mention);
        }

        JSONObject jsonObject = new JSONObject();
        appendMentionJson(jsonObject, mentions);
        appendLinkJson(jsonObject, urlItems);
        return jsonObject;
    }

    /**
     * Append 'mentions' into final JSON if found any mention pattern from input
     * @param jsonObject the target JSON object which you want to add into
     * @param mentions list of names which were mentioned
     */
    private void appendMentionJson(JSONObject jsonObject, List<String> mentions){
        if(mentions == null || mentions.isEmpty()){
            //Stop if the collection is empty
            return;
        }

        if(jsonObject == null) jsonObject = new JSONObject();
        try{
            JSONArray jsonArray = new JSONArray();
            //Push each of 'mention' to array
            for(String m : mentions){
                jsonArray.put(m);
            }
            //Add this JSON array into main JSON object
            jsonObject.put("mentions", jsonArray);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * Append 'links' JSON object into final JSON if found any link from input
     * @param jsonObject
     * @param urlItems
     */
    private void appendLinkJson(JSONObject jsonObject, List<GeocomplyUrlItem> urlItems){
        if(urlItems == null || urlItems.isEmpty()){
            //Stop if the collection is empty
            return;
        }

        if(jsonObject == null) jsonObject = new JSONObject();
        try{
            JSONArray jsonArray = new JSONArray();
            //Push each of URL to array
            for(GeocomplyUrlItem item : urlItems){
                JSONObject object = new JSONObject();
                object.put("title", item.getTitle());
                object.put("url", item.getUrl());
                jsonArray.put(object);
            }
            //Add this JSON array into main JSON object
            jsonObject.put("links", jsonArray);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    private class InputAnalyzer extends AsyncTask<String, Void, JSONObject>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tvResult.setText(R.string.starting);
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);

            if(jsonObject != null){
                tvResult.setText(jsonObject.toString());
            }else tvResult.setText(R.string.unknown_result);
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            return analyze(strings[0]);
        }
    }

}
