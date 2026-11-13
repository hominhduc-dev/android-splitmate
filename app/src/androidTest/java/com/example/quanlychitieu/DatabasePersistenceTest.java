package com.example.quanlychitieu;
import android.content.*;
import android.database.sqlite.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.quanlychitieu.data.local.DatabaseHelper;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatabasePersistenceTest {
 @Test public void migratesWithoutLosingDataAndPersistsExactShares(){
 Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();String file="splitmate-test-"+System.nanoTime()+".db";
 DatabaseHelper db=new DatabaseHelper(context,file);
 try{
  ContentValues g=new ContentValues();g.put("name","Migration test");g.put("created_at",1L);long group=db.save("groups",0,g);
  List<Long> ids=new ArrayList<>();for(String name:new String[]{"An","Bình","Cường"}){ContentValues m=new ContentValues();m.put("group_id",group);m.put("name",name);ids.add(db.save("members",0,m));}
  db.expense(0,group,"Tiền ăn",100000,ids.get(0),"Ăn uống",ids);
  db.getWritableDatabase().execSQL("DROP TABLE grocery_items");db.getWritableDatabase().execSQL("CREATE TABLE grocery_items(id INTEGER PRIMARY KEY,group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,name TEXT NOT NULL,quantity INTEGER NOT NULL,estimated_price INTEGER NOT NULL,is_bought INTEGER NOT NULL DEFAULT 0)");db.getWritableDatabase().setVersion(1);db.close();db=new DatabaseHelper(context,file);
  assertEquals(3,db.getReadableDatabase().getVersion());
  assertEquals(100000,db.scalar("SELECT SUM(share_amount) FROM expense_shares WHERE expense_id IN (SELECT id FROM expenses WHERE group_id=?)",group));
  assertEquals(66666,db.balance(ids.get(0)));assertEquals(-33333,db.balance(ids.get(1)));
  long expense=db.rows("SELECT id FROM expenses").get(0).getAsLong("id");long created=db.rows("SELECT created_at FROM expenses").get(0).getAsLong("created_at");
  db.expense(expense,group,"Chỉnh sửa",300000,ids.get(0),"Ăn uống",ids);
  assertEquals(created,db.rows("SELECT created_at FROM expenses").get(0).getAsLong("created_at").longValue());
  assertEquals(200000,db.balance(ids.get(0)));
  try{db.expense(0,group,"Sai",123,ids.get(0),"Khác",Collections.emptyList());fail("Must reject empty participants");}catch(IllegalArgumentException expected){}
  assertEquals(1,db.rows("SELECT * FROM expenses").size());
  db.delete("expenses",expense);assertEquals(0,db.balance(ids.get(0)));assertTrue(db.rows("SELECT * FROM expense_shares").isEmpty());
 }finally{db.close();context.deleteDatabase(file);}
 }
}
