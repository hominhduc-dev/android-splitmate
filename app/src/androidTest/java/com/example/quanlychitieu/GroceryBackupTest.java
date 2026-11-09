package com.example.quanlychitieu;
import android.content.*;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.quanlychitieu.database.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GroceryBackupTest {
 @Test public void conversionCannotDuplicateAndBackupRestoresLinks() throws Exception {
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();String name="extended-test-"+System.nanoTime()+".db";DatabaseHelper db=new DatabaseHelper(c,name);File backup=new File(c.getCacheDir(),"roundtrip.json");
  try{
   long group=db.createDemo();assertEquals(group,db.createDemo());assertEquals(1,db.rows("SELECT * FROM groups").size());
   List<ContentValues> members=db.rows("SELECT * FROM members WHERE group_id=? ORDER BY id",""+group);List<Long> participants=new ArrayList<>();for(ContentValues m:members)participants.add(m.getAsLong("id"));
   long grocery=db.rows("SELECT id FROM grocery_items WHERE is_bought=1").get(0).getAsLong("id");
   long second=db.rows("SELECT id FROM grocery_items WHERE is_bought=0 ORDER BY id LIMIT 1").get(0).getAsLong("id");db.markBought(second,true);
   long expense=db.convertGroceries(group,Arrays.asList(grocery,second),"Đi chợ thực tế",75000,participants.get(0),"Mua sắm",participants);
   assertEquals(75000,db.rows("SELECT amount FROM expenses WHERE id=?",""+expense).get(0).getAsLong("amount").longValue());
   assertEquals(2,db.rows("SELECT id FROM grocery_items WHERE expense_id=?",""+expense).size());
   int count=db.rows("SELECT * FROM expenses").size();
   try{db.convertGroceries(group,Collections.singletonList(grocery),"Trùng",25000,participants.get(0),"Mua sắm",participants);fail("Duplicate conversion must fail");}catch(IllegalArgumentException expected){}
   assertEquals(count,db.rows("SELECT * FROM expenses").size());
   try{db.markBought(grocery,false);fail("Linked groceries cannot be unchecked");}catch(IllegalArgumentException expected){}
   BackupStore.exportTo(db,c.getContentResolver(),Uri.fromFile(backup));BackupStore.Archive archive=BackupStore.read(c.getContentResolver(),Uri.fromFile(backup));assertEquals(1,archive.groupCount());assertEquals(4,archive.groceryCount());
   BackupStore.restore(db,archive);assertEquals(2,db.rows("SELECT * FROM groups").size());
   ContentValues restored=db.rows("SELECT * FROM groups WHERE id<>?",""+group).get(0);long restoredGroup=restored.getAsLong("id");
   ContentValues restoredGrocery=db.rows("SELECT * FROM grocery_items WHERE group_id=? AND expense_id IS NOT NULL",""+restoredGroup).get(0);assertNotEquals(expense,restoredGrocery.getAsLong("expense_id").longValue());
   List<ContentValues> restoredMembers=db.rows("SELECT * FROM members WHERE group_id=? ORDER BY id",""+restoredGroup);for(int i=0;i<members.size();i++)assertEquals(db.balance(participants.get(i)),db.balance(restoredMembers.get(i).getAsLong("id")));
   JSONObject invalid=new JSONObject(BackupStore.encode(db));invalid.getJSONObject("tables").getJSONArray("expense_shares").getJSONObject(0).put("member_id",999999999);try{BackupStore.decode(invalid.toString());fail("Invalid references must fail");}catch(IllegalArgumentException expected){}
   assertEquals(2,db.rows("SELECT * FROM groups").size());
   db.delete("expenses",expense);assertNull(db.rows("SELECT expense_id FROM grocery_items WHERE id=?",""+grocery).get(0).getAsLong("expense_id"));
   db.convertGroceries(group,Collections.singletonList(grocery),"Ghi lại",25000,participants.get(0),"Mua sắm",participants);
  }finally{db.close();c.deleteDatabase(name);backup.delete();}
 }
}
