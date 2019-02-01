package com.oc.pokercounter;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DataOpenHelper dataOpenHelper;

    private List<Map<String, Object>> matchPlayerList;
    private List<String> matchPlayerNameList;
    private Map<String, Object> gameInformation;
    private List<Map<String, Object>> scoreGridViewData;

    private Integer matchId;
    private Integer resumeMatchId;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_match:
                    return true;
                case R.id.navigation_score_board:
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        DataOpenHelper dataOpenHelper = new DataOpenHelper(this);
        this.dataOpenHelper = dataOpenHelper;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.match, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_player:
                this.addPlayer();
                return true;
            case R.id.new_match:
                this.newMatch();
                return true;
            case R.id.add_game:
                this.addGame();
                return true;
            case R.id.resume_match:
                this.resumeMatch();
                return true;
            case R.id.delete_game:
                this.deleteGame();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addPlayer() {
        final EditText editText = new EditText(MainActivity.this);
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
        inputDialog.setTitle("Please input player's name").setView(editText);
        inputDialog.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String editTextString = editText.getText().toString();
                if(editTextString == null || editTextString.equals("")) {
                    Toast.makeText(MainActivity.this, "Player's name should not be empty.", Toast.LENGTH_LONG).show();
                    return;
                }
                SQLiteDatabase db = MainActivity.this.dataOpenHelper.getWritableDatabase();
                db.beginTransaction();
                ContentValues contentValues = new ContentValues();
                contentValues.put("name", editTextString);
                db.insert(DataOpenHelper.TABLE_NAME_PLAYER, null, contentValues);
                db.setTransactionSuccessful();
                db.endTransaction();
                db.close();
                Toast.makeText(MainActivity.this, "Add player " + editText.getText().toString() + " successfully.", Toast.LENGTH_LONG).show();
            }
        });
        inputDialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        inputDialog.show();
    }

    private void newMatch() {
        this.matchPlayerList = new ArrayList<>();
        this.matchPlayerNameList = new ArrayList<>();
        this.scoreGridViewData = new ArrayList<>();
        this.matchId = null;
        final List<Map<String, Object>> playerList = new ArrayList<>();
        final List<String> playerNameList = new ArrayList<>();
        SQLiteDatabase db = this.dataOpenHelper.getReadableDatabase();
        Cursor cursor = db.query(DataOpenHelper.TABLE_NAME_PLAYER, new String[] {"id", "name"}, null, null, null, null, "id");
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                Map<String, Object> playerMap = new HashMap<>();
                playerMap.put("id", cursor.getInt(0));
                playerMap.put("name", cursor.getString(1));
                playerList.add(playerMap);
                playerNameList.add(cursor.getString(1));
            }
        }

        String[] items =  playerNameList.toArray(new String[playerList.size()]);
        matchPlayerList.clear();
        AlertDialog.Builder multiChoiceDialog = new AlertDialog.Builder(MainActivity.this);
        multiChoiceDialog.setTitle("Please choose players");
        multiChoiceDialog.setMultiChoiceItems(items, null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            matchPlayerList.add(playerList.get(which));
                            matchPlayerNameList.add((String)playerList.get(which).get("name"));
                        } else {
                            matchPlayerList.remove(playerList.get(which));
                            matchPlayerNameList.remove(playerList.get(which).get("name"));
                        }
                    }
                });
        multiChoiceDialog.setPositiveButton("confirm",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(matchPlayerList.size() != 5) {
                        Toast.makeText(MainActivity.this, "Failure!! Only support 5 players' game.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    SQLiteDatabase db = dataOpenHelper.getWritableDatabase();
                    db.beginTransaction();
                    db.execSQL("insert into " + DataOpenHelper.TABLE_NAME_MATCH + " (id, match_time) values (null, datetime('now', 'localtime'))");
                    Cursor cursor = db.rawQuery("select last_insert_rowid() from " + DataOpenHelper.TABLE_NAME_MATCH, null);
                    cursor.moveToFirst();
                    matchId = cursor.getInt(0);

                    for(int i = 0; i < matchPlayerList.size(); i++) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("match_id", matchId);
                        contentValues.put("player_id", matchPlayerList.get(i).get("id").toString());
                        contentValues.put("score", 0);
                        contentValues.put("order_num", i);
                        db.insert(DataOpenHelper.TABLE_NAME_MATCH_PLAYER, null, contentValues);
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    db.close();
                    showHeadGridView();
                    showScoreGridView();
                }
            });
        multiChoiceDialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        multiChoiceDialog.show();
    }

    private void showHeadGridView() {
        String[] from = {"name"};
        int[] to = {R.id.headGridViewText};
        SimpleAdapter adapter = new SimpleAdapter(this, this.matchPlayerList, R.layout.gridview_item, from, to);
        GridView headGridView = findViewById(R.id.headGridView);
        headGridView.setAdapter(adapter);
    }

    private void addGame() {
        if(matchId == null) {
            Toast.makeText(MainActivity.this, "Failure!! Please new or resume a match first.", Toast.LENGTH_LONG).show();
            return;
        }
        gameInformation = new HashMap<>();
        this.showWhoIsLandlordDialog();
    }

    private void showWhoIsLandlordDialog() {
        final List<Map<String, Object>> landlordList = new ArrayList<>();
        AlertDialog.Builder customizeDialog = new AlertDialog.Builder(MainActivity.this);
        customizeDialog.setTitle("Who is landlord?");

        String[] items =  this.matchPlayerNameList.toArray(new String[this.matchPlayerNameList.size()]);
        customizeDialog.setMultiChoiceItems(items, null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            landlordList.add(matchPlayerList.get(which));
                        } else {
                            landlordList.remove(matchPlayerList.get(which));
                        }
                    }
                });

        customizeDialog.setPositiveButton("next",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(landlordList.size() < 1 || landlordList.size() > 2) {
                            Toast.makeText(MainActivity.this, "Failure!! The number of landlord is impossible.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        gameInformation.put("landlordList", landlordList);
                        showWhoIsWinnerDialog();
                    }
                });

        customizeDialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        customizeDialog.show();
    }

    private void showWhoIsWinnerDialog() {
        final String[] items = { "farmer","landlord" };
        gameInformation.put("winner", 0);
        AlertDialog.Builder singleChoiceDialog = new AlertDialog.Builder(MainActivity.this);
        singleChoiceDialog.setTitle("Who is winner?");
        singleChoiceDialog.setSingleChoiceItems(items, 0,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    gameInformation.put("winner", which);
                }
            });
        singleChoiceDialog.setPositiveButton("next",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(!gameInformation.containsKey("winner")) {
                        gameInformation.put("winner", 0);
                    }
                    showWhatIsTheNumberOfBombs();
                }
            });
        singleChoiceDialog.setNegativeButton("previous", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showWhoIsLandlordDialog();
            }
        });
        singleChoiceDialog.show();
    }

    private void showWhatIsTheNumberOfBombs() {
        final EditText editText = new EditText(MainActivity.this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
        inputDialog.setTitle("What is the number of bombs?").setView(editText);
        inputDialog.setPositiveButton("confirm",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String editTextString = editText.getText().toString();
                    if(editTextString == null || editTextString.equals("")) {
                        editTextString = "0";
                    }
                    gameInformation.put("bombs", editTextString);
                    saveGameInformation();
                    showScoreGridView();
                }
            });
        inputDialog.setNegativeButton("previous", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showWhoIsWinnerDialog();
            }
        });
        inputDialog.show();
    }

    private void saveGameInformation() {
        Map<String, Object> scoreMap = this.calculateScore();
        Integer gameOrder = 0;
        SQLiteDatabase db = dataOpenHelper.getWritableDatabase();
        db.beginTransaction();
        Cursor cursor = db.rawQuery("select max(order_num) from " + DataOpenHelper.TABLE_NAME_GAME + " where match_id = ?", new String[] {matchId.toString()});
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            gameOrder = cursor.getInt(0) + 1;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("match_id", matchId);
        contentValues.put("order_num", gameOrder);
        contentValues.put("bomb", Integer.valueOf(gameInformation.get("bombs").toString()));
        contentValues.put("landlord_win", (Integer)gameInformation.get("winner"));
        db.insert(DataOpenHelper.TABLE_NAME_GAME, null, contentValues);

        cursor = db.rawQuery("select last_insert_rowid() from " + DataOpenHelper.TABLE_NAME_GAME, null);
        cursor.moveToFirst();
        Integer gameId = cursor.getInt(0);
        List<Map<String, Object>> landlordList = (List<Map<String,Object>>) gameInformation.get("landlordList");

        for(int i = 0; i < matchPlayerList.size(); i++) {
            Map<String, Object> matchPlayer = matchPlayerList.get(i);
            String matchPlayerId = matchPlayer.get("id").toString();
            Integer gameScore = 0;
            contentValues = new ContentValues();
            contentValues.put("game_id", gameId);
            contentValues.put("player_id", matchPlayerId);
            if(landlordList.contains(matchPlayer)) {
                contentValues.put("landlord", "1");
                gameScore = (Integer) scoreMap.get("landlordScore");
            } else {
                contentValues.put("landlord", "0");
                gameScore = (Integer) scoreMap.get("farmerScore");
            }
            contentValues.put("score", gameScore);
            db.insert(DataOpenHelper.TABLE_NAME_GAME_PLAYER, null, contentValues);


            cursor = db.rawQuery("select score from " + DataOpenHelper.TABLE_NAME_MATCH_PLAYER + " where match_id = ? and player_id = ?", new String[] {matchId.toString(), matchPlayerId});
            cursor.moveToFirst();
            Integer score = cursor.getInt(0);
            score += gameScore;
            contentValues = new ContentValues();
            contentValues.put("score", score);
            db.update(DataOpenHelper.TABLE_NAME_MATCH_PLAYER, contentValues, "match_id = ? and player_id = ?", new String[] {matchId.toString(), matchPlayerId});

            Map<String, Object> playerScoreMap = new HashMap<>();
            playerScoreMap.put("score", score);
            scoreGridViewData.add(playerScoreMap);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    private Map<String, Object> calculateScore() {
        Map<String, Object> scoreMap = new HashMap<>();
        List<Map<String, Object>> landlordList = (List<Map<String,Object>>) gameInformation.get("landlordList");
        Integer landlordNumber = landlordList.size();
        Integer farmerNumber = 5 - landlordNumber;
        Integer bombNumber = Integer.valueOf(gameInformation.get("bombs").toString());
        Integer farmerScore = (int) Math.pow(2, bombNumber + 1);
        Integer landlordScore = farmerScore * farmerNumber / landlordNumber;
        Integer winner = (Integer) gameInformation.get("winner");
        if(winner == 0) {
            landlordScore *= -1;
        } else {
            farmerScore *= -1;
        }
        scoreMap.put("landlordScore", landlordScore);
        scoreMap.put("farmerScore", farmerScore);
        return scoreMap;
    }

    private void showScoreGridView() {
        String[] from = {"score"};
        int[] to = {R.id.headGridViewText};
        SimpleAdapter adapter = new SimpleAdapter(this, this.scoreGridViewData, R.layout.gridview_item, from, to);
        GridView scoreGridView = findViewById(R.id.scoreGridView);
        scoreGridView.setAdapter(adapter);
    }

    private void resumeMatch() {
        final List<Map<String, Object>> matchList = new ArrayList<>();
        final List<String> matchNameList = new ArrayList<>();
        SQLiteDatabase db = this.dataOpenHelper.getReadableDatabase();
        Cursor cursor = db.query(DataOpenHelper.TABLE_NAME_MATCH, new String[] {"id", "match_time"}, null, null, null, null, "match_time desc");
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                Map<String, Object> matchMap = new HashMap<>();
                matchMap.put("id", cursor.getInt(0));
                matchMap.put("match_time", cursor.getString(1));
                matchList.add(matchMap);
                matchNameList.add(cursor.getString(1));
            }
        }
        resumeMatchId = (Integer) matchList.get(0).get("id");

        String[] items =  matchNameList.toArray(new String[matchNameList.size()]);
        AlertDialog.Builder singleChoiceDialog = new AlertDialog.Builder(MainActivity.this);
        singleChoiceDialog.setTitle("Please choose a match");
        singleChoiceDialog.setSingleChoiceItems(items, 0,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    resumeMatchId = (Integer) matchList.get(which).get("id");
                }
            });
        singleChoiceDialog.setPositiveButton("confirm",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(resumeMatchId == null) {
                        resumeMatchId = (Integer) matchList.get(0).get("id");
                    }
                    matchId = resumeMatchId;
                    resumeMatchPlayerList();
                    showHeadGridView();
                    resumeScoreGridViewData();
                    showScoreGridView();
                }
            });
        singleChoiceDialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resumeMatchId = null;
            }
        });
        singleChoiceDialog.show();
    }

    private void resumeMatchPlayerList() {
        matchPlayerList = new ArrayList<>();
        matchPlayerNameList = new ArrayList<>();
        SQLiteDatabase db = this.dataOpenHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select player_id, name from t_match_player inner join t_player on t_match_player.player_id = t_player.id where match_id = ? order by order_num", new String[] {matchId.toString()});
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                Map<String, Object> playerMap = new HashMap<>();
                playerMap.put("id", cursor.getInt(0));
                playerMap.put("name", cursor.getString(1));
                matchPlayerList.add(playerMap);
                matchPlayerNameList.add(cursor.getString(1));
            }
        }
        db.close();
    }

    private void resumeScoreGridViewData() {
        scoreGridViewData = new ArrayList<>();
        SQLiteDatabase db = this.dataOpenHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select id from t_game where match_id = ? order by order_num", new String[] {matchId.toString()});
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                for(Map<String, Object> matchPlayerMap : matchPlayerList) {
                    Cursor gamePlayerCursor = db.rawQuery("select score from t_game_player where game_id = ? and player_id  = ? ", new String[] {String.valueOf(cursor.getInt(0)), matchPlayerMap.get("id").toString()});
                    if (gamePlayerCursor.getCount() > 0) {
                        gamePlayerCursor.moveToFirst();
                        int playerGameScore = gamePlayerCursor.getInt(0);
                        int totalScore = playerGameScore;
                        int formerIndex = scoreGridViewData.size() - 5;
                        if(formerIndex >= 0) {
                            Integer formerScore = (Integer)scoreGridViewData.get(formerIndex).get("score");
                            totalScore += formerScore;
                        }
                        Map<String, Object> playerScoreMap = new HashMap<>();
                        playerScoreMap.put("score", totalScore);
                        scoreGridViewData.add(playerScoreMap);
                    }
                }
            }
        }
        db.close();
    }

    private void deleteGame() {
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
        inputDialog.setTitle("Are you sure you want to delete a game?");
        inputDialog.setPositiveButton("yes",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(matchId == null || scoreGridViewData.size() < 5) {
                        Toast.makeText(MainActivity.this, "Nothing to delete.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    SQLiteDatabase db = MainActivity.this.dataOpenHelper.getReadableDatabase();
                    Cursor cursor = db.rawQuery("select id from t_game where match_id = ? order by order_num desc", new String[] {matchId.toString()});
                    Integer gameId = null;
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        gameId = cursor.getInt(0);
                        db.beginTransaction();
                        Cursor gamePlayerCursor = db.rawQuery("select player_id, score from t_game_player where game_id = ? ", new String[] {gameId.toString()});
                        if (gamePlayerCursor.getCount() > 0) {
                            while(gamePlayerCursor.moveToNext()) {
                                String playerId = String.valueOf(gamePlayerCursor.getInt(0));
                                Cursor matchPlayerCursor = db.rawQuery("select score from t_match_player where match_id = ? and player_id  = ? ", new String[] {matchId.toString(), playerId});
                                Integer scoreBeforeDeleted = null;
                                if (matchPlayerCursor.getCount() > 0) {
                                    matchPlayerCursor.moveToFirst();
                                    scoreBeforeDeleted = matchPlayerCursor.getInt(0);
                                }
                                Integer scoreAfterDelete = scoreBeforeDeleted - gamePlayerCursor.getInt(1);
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("score", scoreAfterDelete);
                                db.update(DataOpenHelper.TABLE_NAME_MATCH_PLAYER, contentValues, "match_id = ? and player_id = ?", new String[] {matchId.toString(), playerId});
                            }
                        }
                        db.delete(DataOpenHelper.TABLE_NAME_GAME_PLAYER, "game_id = ?", new String[] {gameId.toString()});
                        db.delete(DataOpenHelper.TABLE_NAME_GAME, "id = ?", new String[] {gameId.toString()});
                        db.setTransactionSuccessful();
                        db.endTransaction();
                        db.close();
                        for(int i = 0; i < 5; i++) {
                            scoreGridViewData.remove(scoreGridViewData.size() - 1);
                        }
                        showScoreGridView();
                    }
                }
            });
        inputDialog.setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        inputDialog.show();
    }
}
