package au.com.myextras.rss;

/**
 * Exception thrown by the {@link FeedParser}.
 */
public class FeedParserException extends Exception {

    public FeedParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public FeedParserException(String message) {
        super(message);
    }

}