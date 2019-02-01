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
                SQLiteDatabase db = MainActivity.this.dataOpenHelper.getWritableDatabase();
                db.beginTransaction();
                ContentValues contentValues = new ContentValues();
                contentValues.put("name", editText.getText().toString());
                db.insert(DataOpenHelper.TABLE_NAME_PLAYER, null, contentValues);
                db.setTransactionSuccessful();
                db.endTransaction();
                db.close();
                Toast.makeText(MainActivity.this, "Add player " + editText.getText().toString() + " successfully.", Toast.LENGTH_SHORT).show();
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
                    SQLiteDatabase db = dataOpenHelper.getWritableDatabase();
                    db.beginTransaction();
                    db.execSQL("insert into " + DataOpenHelper.TABLE_NAME_MATCH + " (id, match_time) values (null, null)");
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
                    MainActivity.this.showHeadGridView();

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
                    showWhatIsTheNumberOfBombs();
                }
            });
        singleChoiceDialog.setNegativeButton("previous", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showWhoIsWinnerDialog();
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
                    gameInformation.put("bombs", editText.getText().toString());
                    saveGameInformation();
                    showScoreGridView();
                }
            });
        inputDialog.setNegativeButton("previous", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showWhatIsTheNumberOfBombs();
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
}
