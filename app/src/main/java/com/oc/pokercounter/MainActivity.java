package com.oc.pokercounter;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DataOpenHelper dataOpenHelper;

    private List<Map<String, Object>> headGridViewData;

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
        final List<String> yourChoices = new ArrayList<>();

        final List<String> playerList = new ArrayList<String>();
        SQLiteDatabase db = this.dataOpenHelper.getReadableDatabase();
        Cursor cursor = db.query(DataOpenHelper.TABLE_NAME_PLAYER, new String[] {"id", "name"}, null, null, null, null, null);
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                playerList.add(cursor.getString(1));
            }
        }

        String[] items =  playerList.toArray(new String[playerList.size()]);
        yourChoices.clear();
        AlertDialog.Builder multiChoiceDialog = new AlertDialog.Builder(MainActivity.this);
        multiChoiceDialog.setTitle("Please choose players");
        multiChoiceDialog.setMultiChoiceItems(items, null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            yourChoices.add(playerList.get(which));
                        } else {
                            yourChoices.remove(playerList.get(which));
                        }
                    }
                });
        multiChoiceDialog.setPositiveButton("confirm",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = MainActivity.this.dataOpenHelper.getWritableDatabase();
                        db.beginTransaction();
                        db.execSQL("insert into " + DataOpenHelper.TABLE_NAME_MATCH + " (id, match_time) values (null, null)");
                        db.setTransactionSuccessful();
                        db.endTransaction();
                        db.close();
                        MainActivity.this.headGridViewData = new ArrayList<>();
                        for(int i = 0; i < 1; i++) {
                            for(String s : yourChoices) {
                                Map<String, Object> map = new HashMap<String, Object>();
                                map.put("text", s);
                                MainActivity.this.headGridViewData.add(map);
                            }
                        }
                        MainActivity.this.showHeadGridView();

                    }
                });
        multiChoiceDialog.show();
    }

    private void showHeadGridView() {
        String[] from = {"text"};
        int[] to = {R.id.headGridViewText};
        SimpleAdapter adapter = new SimpleAdapter(this, this.headGridViewData, R.layout.gridview_item, from, to);
        GridView headGridView = findViewById(R.id.headGridView);
        headGridView.setAdapter(adapter);

        GridView scoreGridView = findViewById(R.id.scoreGridView);
        scoreGridView.setAdapter(adapter);
    }
}
