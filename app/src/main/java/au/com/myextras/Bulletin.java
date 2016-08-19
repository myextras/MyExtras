package au.com.myextras;

import android.content.ContentResolver;
import android.net.Uri;

public class Bulletin {

    public static final Uri CONTENT_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(BulletinsProvider.AUTHORITY)
            .path("bulletins")
            .build();

    public interface Column {

        public static final String ID = "_id";

        public static final String CODE = "code";
        public static final String GUID = "guid";
        public static final String TITLE = "title";
        public static final String LINK = "link";
        public static final String CONTENT = "content";
        public static final String PUBLISHED = "published";

        public static final String ACCESSED = "accessed";

    }

}
