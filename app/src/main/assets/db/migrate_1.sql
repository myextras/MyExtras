-- create table for bulletins
CREATE TABLE bulletins (
    _id INTEGER PRIMARY KEY,

    code TEXT NOT NULL,
    guid TEXT NOT NULL,
    title TEXT NOT NULL,
    link TEXT,
    content TEXT,
    published INTEGER NOT NULL,
    important INTEGER NOT NULL DEFAULT 0,

    accessed INTEGER NOT NULL DEFAULT 0,

    UNIQUE (code, guid)
);
