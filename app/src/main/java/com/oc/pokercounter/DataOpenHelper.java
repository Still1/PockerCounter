package com.oc.pokercounter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class DataOpenHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "pokerCounter";
    public static final String TABLE_NAME_PLAYER = "t_player";
    public static final String TABLE_NAME_MATCH = "t_match";
    public static final String TABLE_NAME_MATCH_PLAYER = "t_match_player";
    public static final String TABLE_NAME_GAME = "t_game";
    public static final String TABLE_NAME_GAME_PLAYER = "t_game_player";

    public DataOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createPlayerSql = "create table if not exists " + TABLE_NAME_PLAYER + " (id integer primary key autoincrement, name text)";
        String createMatchSql = "create table if not exists " + TABLE_NAME_MATCH + " (id integer primary key autoincrement, match_time timestamp default(datetime('now', 'localtime')))";
        String createMatchPlayerSql = "create table if not exists " + TABLE_NAME_MATCH_PLAYER + " (match_id integer primary key autoincrement, name text)";
        String createGameSql = "create table if not exists " + TABLE_NAME_GAME + " (id integer primary key autoincrement, name text)";
        String createGamePlayerSql = "create table if not exists " + TABLE_NAME_GAME_PLAYER + " (id integer primary key autoincrement, name text)";
        db.execSQL(createPlayerSql);
        db.execSQL(createMatchSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "drop table if exists " + TABLE_NAME_PLAYER;
        db.execSQL(sql);
        onCreate(db);
    }
}
