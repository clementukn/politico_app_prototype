package com.example.clement.politico;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;
import com.example.clement.politico.PoliticoParser.ArticleItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // key for extra message in SingleSummaryActivity
    public final static String EXTRA_TITLE = "com.example.clement.politico.title";
    public final static String EXTRA_DESCRIPTION = "com.example.clement.politico.description";
    public final static String EXTRA_SUMMARY = "com.example.clement.politico.summary";
    public final static String EXTRA_SENTIMENT = "com.example.clement.politico.sentiment";
    // URL for Politco's congress RSS feed
    private static final String CONGRESS_URL = "http://www.politico.com/rss/congress.xml";
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.list_view);

        // adding politico background to Action Bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_logo, null));
        //actionBar.setBackgroundDrawable((Drawable) getDrawable(R.drawable.action_bar_logo));
        actionBar.setTitle("");
    }

    @Override
    public void onStart() {
        super.onStart();
        loadArticles();
    }


    /* Load articles' title from RSS feed into listView in an AsyncTask
     */
    private void loadArticles() {
        //TODO: should check connectivity first
        // AsyncTask to load Congress articles
        new DownloadXmlTask().execute(CONGRESS_URL);
    }

    // AsyncTask to download RSS feed and load articles' title into ListView
    private class DownloadXmlTask extends AsyncTask<String, Void, List<ArticleItem>> {

        @Override
        protected List<ArticleItem> doInBackground(String... urls) {
            try {
                return loadArticlesFromXml(urls[0]);
            } catch (IOException e) {
                displayErrorMsg(getResources().getString(R.string.connection_error));
                return new ArrayList<ArticleItem>();
            } catch (XmlPullParserException e) {
                displayErrorMsg(getResources().getString(R.string.xml_error));
                return new ArrayList<ArticleItem>();
            }
        }

        @Override
        protected void onPostExecute(List<ArticleItem> articles) {
            refreshListView(articles);
        }
    }

    private void refreshListView(List<ArticleItem> articles) {
        // add articles' title into ListView
        ArrayAdapter<ArticleItem> adapter = new CustomArrayAdapter(this , R.layout.custom_list , articles);
        listView.setAdapter(adapter);

        // a click on a title runs a Summary activity
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                int itemPosition = position;

                ArticleItem article = (ArticleItem) listView.getItemAtPosition(position);
                new SummarizeSingleArticleTask().execute(article);
            }
        });
    }

    /* Fetches XML from politico.com, parses it and returns list of ArticleItem containing
       Title, Description and Link
     */
    private List<ArticleItem> loadArticlesFromXml(String urlString) throws IOException, XmlPullParserException {
        InputStream stream = null;
        PoliticoParser politicoXmlParser = new PoliticoParser();
        List<ArticleItem> articles = null;

        try {
            stream = downloadXml(urlString);
            articles = politicoXmlParser.getArticleFromXml(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        return articles;
    }

    private InputStream downloadXml(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);

        conn.connect();
        InputStream stream = conn.getInputStream();
        return stream;
    }

    /* Task that downloads article HTML page, parses it to extract text, calls Intellexer API
       to summarize it and displays result in a new activity.
     */
    private class SummarizeSingleArticleTask extends AsyncTask<ArticleItem, Void, Summary> {

        @Override
        protected Summary doInBackground(ArticleItem... articles) {
            try {
                return summarizeSingleArticle(articles[0]);
            } catch (IOException e) {
                displayErrorMsg(getResources().getString(R.string.connection_error));
                return new Summary();
            }
        }

        @Override
        protected void onPostExecute(Summary summary) {
            // check no exception were thrown
            if (summary.summaryText == null) return;

            // passing data and displaying summarized article into new activity
            Intent intent = new Intent(MainActivity.this, SingleSummaryActivity.class);
            intent.putExtra(EXTRA_TITLE, summary.article.title);
            intent.putExtra(EXTRA_DESCRIPTION, summary.article.description);
            intent.putStringArrayListExtra(EXTRA_SUMMARY, (ArrayList<String>) summary.summaryText);
            intent.putIntegerArrayListExtra(EXTRA_SENTIMENT, (ArrayList<Integer>) summary.sentiment);
            startActivity(intent);
        }
    }

    // Wrapper for summary object
    public class Summary {
        public List<Integer> sentiment; // stores positive, neutral and negative weights
        public List<String> summaryText = null;
        public ArticleItem article;
    }

    /* Fetches summarized article from API and displays it in new activity
     */
    private Summary summarizeSingleArticle(ArticleItem article) throws IOException {
        Summary summary = new Summary();
        PoliticoParser politicoHtmlParser = new PoliticoParser();
        IntellexerApi intellexer = new IntellexerApi();

        String articleHtml = getHtmlPageFromUrl(article.link);
        // for optimal performance, retrieve text from HMTL article
        String articleText = politicoHtmlParser.getArticleTextFromHtml(articleHtml);

        // summarize the text using Intellexer API
        summary.summaryText = intellexer.summarizeFromText(articleText);
        // get sentiment analysis using Intellexer API
        summary.sentiment = intellexer.sentimentFromText(articleText);
        // attach article infos to it
        summary.article = article;

        return summary;
    }

    // Fetches article's HTML page
    private String getHtmlPageFromUrl(String urlArticle) throws IOException {
        URL url = new URL(urlArticle);
        BufferedReader reader = null;
        StringBuilder htmlPage = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            for (String line; (line = reader.readLine()) != null;) {
                htmlPage.append(line.trim());
            }
        } finally {
            if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
        }

        return htmlPage.toString();
    }

    private void displayErrorMsg(String errMessage) {
        Toast.makeText(getApplicationContext(),errMessage, Toast.LENGTH_LONG)
                .show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

