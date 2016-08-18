package au.com.myextras.rss;

import com.tughi.xml.Document;
import com.tughi.xml.TagElement;
import com.tughi.xml.TextElement;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import au.com.myextras.utils.ConnectionHelper;

/**
 * RSS feed parser.
 */
public final class FeedParser {

    private static final DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    private Result result = new Result();

    private Result.Feed.Entry currentEntry;

    public static Result parse(String url) throws FeedParserException {
        return new FeedParser().parseUrl(url);
    }

    private Result parseUrl(String url) throws FeedParserException {
        try {
            URLConnection connection = new URL(url).openConnection();
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) connection;

                result.status = httpURLConnection.getResponseCode();
                result.url = httpURLConnection.getURL().toString();
                result.headers = httpURLConnection.getHeaderFields();

                if (result.status != HttpURLConnection.HTTP_OK) {
                    // unexpected response code
                    return result;
                }
            }

            String content = ConnectionHelper.load(connection);

            // create SAX parser
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setNamespaceAware(true);
            SAXParser saxParser = saxParserFactory.newSAXParser();

            Document document = new Document();
            createRssElements(document);
            DefaultHandler contentHandler = document.getContentHandler();

            // parse XML
            saxParser.parse(new InputSource(new StringReader(content)), contentHandler);

        } catch (Exception exception) {
            throw new FeedParserException("parse failed", exception);
        }

        return result;
    }

    private void createRssElements(Document document) {
        final String[] rssNamespaces = { "", "http://purl.org/rss/1.0/" };

        TagElement rssElement = new TagElement("rss");
        document.addChild(rssElement);

        TagElement channelElement = new TagElement("channel", rssNamespaces);
        rssElement.addChild(channelElement);

        channelElement.addChild(new TextElement("title", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleFeedTitle(text);
            }
        });

        channelElement.addChild(new TextElement("link", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleFeedLink(text);
            }
        });

        TagElement itemElement = new TagElement("item", rssNamespaces) {
            @Override
            protected void start(String namespace, String name, Attributes attributes) throws SAXException {
                handleEntryStart();
            }

            @Override
            protected void end(String namespace, String name) throws SAXException {
                handleEntryEnd();
            }
        };
        channelElement.addChild(itemElement);

        itemElement.addChild(new TextElement("title", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryTitle(text);
            }
        });

        itemElement.addChild(new TextElement("link", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryLink(text);
            }
        });

        itemElement.addChild(new TextElement("guid", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryGuid(text);
            }
        });

        itemElement.addChild(new TextElement("description", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntrySummary(text);
            }
        });

        itemElement.addChild(new TextElement("pubDate", rssNamespaces) {
            @Override
            protected void handleText(String text) {
                handleEntryPublished(text);
            }
        });
    }

    private void handleFeedTitle(String title) {
        result.feed.title = title;
    }

    private void handleFeedLink(String link) {
        result.feed.link = link;
    }

    private void handleEntryStart() {
        currentEntry = result.feed.new Entry();
    }

    private void handleEntryEnd() {
        result.feed.entries.add(currentEntry);
    }

    private void handleEntryTitle(String title) {
        currentEntry.title = title;
    }

    private void handleEntryLink(String link) {
        currentEntry.link = link;
    }

    private void handleEntryGuid(String guid) {
        currentEntry.guid = guid;
    }

    private void handleEntrySummary(String summary) {
        currentEntry.content = summary;
    }

    private void handleEntryPublished(String published) {
        currentEntry.published = published;

        try {
            currentEntry.publishedTimestamp = dateFormat.parse(published);
        } catch (ParseException exception) {
            throw new IllegalStateException("Invalid date format: " + published);
        }
    }

    public class Result {

        public String url;
        public int status;
        public Map<String, List<String>> headers;
        public Feed feed = new Feed();

        public class Feed {

            public String title;
            public String link;
            public List<Entry> entries = new LinkedList<>();

            public class Entry {

                public String title;
                public String link;
                public String guid;
                public String content;
                public String published;
                public Date publishedTimestamp;

            }
        }
    }

}