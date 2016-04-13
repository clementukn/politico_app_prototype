package com.example.clement.politico;

import android.text.Html;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by clement on 4/5/16.
 *
 * Parser for politico RSS feed and articles' HTML pages
 */
public class PoliticoParser {
    private static final String ns = null;

    // Entry point for parsing RSS feed. Returns a list of article items containing
    // Title, Description and link
    public List<ArticleItem> getArticleFromXml(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readRSS(parser);
        } finally {
            in.close();
        }
    }

    private List<ArticleItem> readRSS(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<ArticleItem> articles = new ArrayList<ArticleItem>();
        parser.require(XmlPullParser.START_TAG, ns, "rss");
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, ns, "channel");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the item tag
            if (name.equals("item")) {
                ArticleItem article = readXmlItem(parser);
                // makes sure the article is valid
                if (article.title != null && article.link != null) {
                    articles.add(article);
                }
            }
            else {
                skipXml(parser);
            }
        }
        return articles;
    }

    // Parses the contents of an item. If it encounters a title, description, or link tag, hands them
    // off to their respective methods for processing. Otherwise, skips the tag.
    private ArticleItem readXmlItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "item");
        String title = null;
        String summary = null;
        String link = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                title = readXmlTitle(parser);
            } else if (name.equals("description")) {
                summary = readXmlDescription(parser);
            } else if (name.equals("link")) {
                link = readXmlLink(parser);
            } else {
                skipXml(parser);
            }
        }
        return new ArticleItem(title, summary, link);
    }

    // Processes title tags in the feed.
    private String readXmlTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String title = readXmlText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return title;
    }

    // Processes link tags in the feed.
    private String readXmlLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "link");
        String link = readXmlText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "link");
        return link.contains("story") ? link : null;
    }

    // Processes description tags in the feed.
    private String readXmlDescription(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "description");
        String summary = readXmlText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "description");
        return summary;
    }

    // For the tags title and description, extracts their text values.
    private String readXmlText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Skips tags the parser isn't interested in..
    private void skipXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    static public class ArticleItem {
        public final String title;
        public final String description;
        public final String link;

        private ArticleItem(String title, String description, String link) {
            this.title = title;
            this.description = description;
            this.link = link;
        }

        @Override
        public String toString() {
            return this.title;
        }
    }

    static private String stripHtml(String html) {
        return Html.fromHtml(html).toString();
    }

    // Extracts text from article HTML to optimize quality of Intellexer's summary. If not, every
    // elements in the page can be part of the summary (e.g. "Follow us on Twitter")
    // Returns empty string if text couldn't be extracted
    public String getArticleTextFromHtml(String htmlArticle) throws IOException {
        final String start = "<div class=\"story-text \">"; // <style type="text/css">
        final String end = "<div class=\"story-share \">";

        int posStart = htmlArticle.indexOf(start);
        int posEnd = htmlArticle.indexOf(end);

        if (posStart <= -1 || posEnd <= -1) return "";
        String htmlBlock = htmlArticle.substring(posStart + start.length(), posEnd);

        StringBuilder parsedArticle = new StringBuilder();
        while ((posStart = htmlBlock.indexOf("<p>")) != -1) {
            posEnd = htmlBlock.indexOf("</p>", posStart);
            String sentence = htmlBlock.substring(posStart + 3, posEnd);
            sentence = stripHtml(sentence);
            parsedArticle.append(sentence);
            htmlBlock  = htmlBlock.substring(posEnd + 4);
        }

        return parsedArticle.toString();
    }
}
