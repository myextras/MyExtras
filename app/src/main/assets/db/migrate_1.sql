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

-- a trigger that limits the entries table to 50 records
CREATE TRIGGER delete_old_entries
    AFTER INSERT ON entries
    BEGIN
        DELETE FROM entries WHERE published < (SELECT published FROM entries ORDER BY published DESC LIMIT 49, 1);
    END;
