package au.com.myextras;

import android.content.ContentResolver;
import android.net.Uri;

public class Entry {

    public static final Uri CONTENT_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(BulletinsProvider.AUTHORITY)
            .path("entries")
            .build();

    public interface Columns {

        public static final String ID = "_id";

        public static final String BULLETIN = "bulletin";

        public static final String GUID = "guid";
        public static final String TITLE = "title";
        public static final String LINK = "link";
        public static final String CONTENT = "content";
        public static final String PUBLISHED = "published";
        public static final String IMPORTANT = "important";

        public static final String ACCESSED = "accessed";

    }

}
