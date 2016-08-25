-- create the entries table
CREATE TABLE entries (
    _id INTEGER PRIMARY KEY,

    bulletin TEXT NOT NULL,
    guid TEXT NOT NULL,
    title TEXT NOT NULL,
    link TEXT,
    content TEXT,
    published INTEGER NOT NULL,
    important INTEGER NOT NULL DEFAULT 0,

    accessed INTEGER NOT NULL DEFAULT 0,

    UNIQUE (bulletin, guid)
);
