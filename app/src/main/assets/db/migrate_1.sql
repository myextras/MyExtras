-- create table for bulletins
CREATE TABLE bulletins (
    _id INTEGER PRIMARY KEY,

    code TEXT NOT NULL,
    guid TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    link TEXT,
    content TEXT,
    published INTEGER NOT NULL,

    accessed INTEGER NOT NULL DEFAULT 0
);

