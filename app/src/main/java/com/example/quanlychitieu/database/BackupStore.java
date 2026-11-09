package com.example.quanlychitieu.database;

import android.content.*;
import android.net.Uri;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Portable JSON backup with a strict schema. Restore always adds new groups in one transaction. */
public final class BackupStore {
 private static final String[] TABLES={"groups","members","expenses","expense_shares","grocery_items","settlements"};
 private static final String[][] COLUMNS={
  {"id","name","created_at"},{"id","group_id","name"},
  {"id","group_id","title","amount","payer_id","category","created_at"},
  {"id","expense_id","member_id","share_amount"},
  {"id","group_id","name","quantity","estimated_price","is_bought","unit","expense_id"},
  {"id","group_id","from_member_id","to_member_id","amount","created_at"}};
 public static class Archive {
  final Map<String,List<ContentValues>> tables=new LinkedHashMap<>();
  public int groupCount(){return tables.get("groups").size();}
  public int expenseCount(){return tables.get("expenses").size();}
  public int groceryCount(){return tables.get("grocery_items").size();}
 }
 public static String encode(DatabaseHelper db) throws Exception {
  JSONObject root=new JSONObject();root.put("format","SplitMate");root.put("version",1);JSONObject tables=new JSONObject();root.put("tables",tables);
  SQLiteDatabase d=db.getWritableDatabase();d.beginTransaction();try{
   for(int i=0;i<TABLES.length;i++){JSONArray array=new JSONArray();for(ContentValues row:db.rows("SELECT * FROM "+TABLES[i]+" ORDER BY id")){JSONObject value=new JSONObject();for(String col:COLUMNS[i])value.put(col,row.get(col)==null?JSONObject.NULL:textColumn(col)?row.getAsString(col):row.getAsLong(col));array.put(value);}tables.put(TABLES[i],array);}d.setTransactionSuccessful();
  }finally{d.endTransaction();}return root.toString();
 }
 public static void exportTo(DatabaseHelper db,ContentResolver resolver,Uri uri) throws Exception {
  byte[] bytes=encode(db).getBytes(StandardCharsets.UTF_8);try(OutputStream out=resolver.openOutputStream(uri,"wt")){if(out==null)throw new IOException("Không mở được tệp sao lưu");out.write(bytes);}
 }
 public static Archive read(ContentResolver resolver,Uri uri) throws Exception {
  try(InputStream in=resolver.openInputStream(uri)){if(in==null)throw new IOException("Không mở được tệp");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>10*1024*1024)throw invalid();out.write(buffer,0,n);}return decode(new String(out.toByteArray(),StandardCharsets.UTF_8));}
 }
 private static IllegalArgumentException invalid(){return new IllegalArgumentException("Tệp sao lưu không hợp lệ hoặc vượt giới hạn dữ liệu");}
 private static boolean textColumn(String col){return col.equals("name")||col.equals("title")||col.equals("category")||col.equals("unit");}
 public static Archive decode(String json) throws Exception {
  try{return decodeChecked(json);}catch(JSONException|ArithmeticException e){throw invalid();}
 }
 private static Archive decodeChecked(String json) throws Exception {
  if(json.length()>10*1024*1024)throw invalid();JSONObject root=new JSONObject(json);if(!"SplitMate".equals(root.optString("format"))||!(root.get("version") instanceof Integer)||root.getInt("version")!=1)throw invalid();JSONObject tables=root.getJSONObject("tables");Archive a=new Archive();int count=0;
  for(int i=0;i<TABLES.length;i++){JSONArray array=tables.getJSONArray(TABLES[i]);count+=array.length();if(count>100000||array.length()>25000)throw invalid();List<ContentValues> list=new ArrayList<>();Set<Long> ids=new HashSet<>();
   for(int j=0;j<array.length();j++){JSONObject object=array.getJSONObject(j);ContentValues row=new ContentValues();for(String col:COLUMNS[i]){
    if(col.equals("expense_id")&&TABLES[i].equals("grocery_items")&&object.isNull(col)){row.putNull(col);continue;}
    Object value=object.get(col);if(textColumn(col)){if(!(value instanceof String)||((String)value).trim().isEmpty()||((String)value).length()>200)throw invalid();row.put(col,((String)value).trim());}
    else {if(!(value instanceof Integer)&&!(value instanceof Long))throw invalid();long n=((Number)value).longValue();if(n<0)throw invalid();row.put(col,n);}
   }long id=row.getAsLong("id");if(id==0||!ids.add(id))throw invalid();list.add(row);
   }a.tables.put(TABLES[i],list);
  }validate(a);return a;
 }
 private static Map<Long,ContentValues> index(Archive a,String table){Map<Long,ContentValues> map=new HashMap<>();for(ContentValues row:a.tables.get(table))map.put(row.getAsLong("id"),row);return map;}
 private static long n(ContentValues v,String col){return v.getAsLong(col);}
 private static void require(boolean ok){if(!ok)throw invalid();}
 private static void member(Map<Long,ContentValues> members,long id,long group){require(members.containsKey(id)&&n(members.get(id),"group_id")==group);}
 private static void validate(Archive a){
  Map<Long,ContentValues> groups=index(a,"groups"),members=index(a,"members"),expenses=index(a,"expenses");
  for(ContentValues m:members.values())require(groups.containsKey(n(m,"group_id")));
  for(ContentValues e:expenses.values()){require(groups.containsKey(n(e,"group_id"))&&n(e,"amount")>0&&n(e,"amount")<=1000000000000L);member(members,n(e,"payer_id"),n(e,"group_id"));}
  Map<Long,Long> totals=new HashMap<>();Set<String> pairs=new HashSet<>();
  for(ContentValues share:a.tables.get("expense_shares")){long eid=n(share,"expense_id"),mid=n(share,"member_id");require(expenses.containsKey(eid)&&pairs.add(eid+":"+mid));member(members,mid,n(expenses.get(eid),"group_id"));require(n(share,"share_amount")<=1000000000000L);totals.put(eid,Math.addExact(totals.getOrDefault(eid,0L),n(share,"share_amount")));}
  for(ContentValues e:expenses.values())require(totals.containsKey(n(e,"id"))&&totals.get(n(e,"id"))==n(e,"amount"));
  for(ContentValues g:a.tables.get("grocery_items")){require(groups.containsKey(n(g,"group_id"))&&n(g,"quantity")>0&&n(g,"quantity")<=1000000&&n(g,"estimated_price")<=1000000000000L/n(g,"quantity")&&n(g,"is_bought")<=1);Long eid=g.getAsLong("expense_id");if(eid!=null)require(expenses.containsKey(eid)&&n(expenses.get(eid),"group_id")==n(g,"group_id")&&n(g,"is_bought")==1);}
  for(ContentValues t:a.tables.get("settlements")){long group=n(t,"group_id");require(groups.containsKey(group)&&n(t,"amount")>0&&n(t,"amount")<=1000000000000L&&n(t,"from_member_id")!=n(t,"to_member_id"));member(members,n(t,"from_member_id"),group);member(members,n(t,"to_member_id"),group);}
 }
 public static void restore(DatabaseHelper db,Archive archive){
  validate(archive);SQLiteDatabase d=db.getWritableDatabase();d.beginTransaction();try{
   Map<Long,Long> groups=new HashMap<>(),members=new HashMap<>(),expenses=new HashMap<>();
   for(String table:TABLES)for(ContentValues original:archive.tables.get(table)){ContentValues v=new ContentValues(original);long old=n(v,"id");v.remove("id");
    if(table.equals("groups")){String name=v.getAsString("name");v.put("name",name.substring(0,Math.min(180,name.length()))+" (khôi phục)");}
    else if(v.containsKey("group_id"))v.put("group_id",groups.get(n(v,"group_id")));
    if(table.equals("expenses"))v.put("payer_id",members.get(n(v,"payer_id")));
    if(table.equals("expense_shares")){v.put("expense_id",expenses.get(n(v,"expense_id")));v.put("member_id",members.get(n(v,"member_id")));}
    if(table.equals("grocery_items")&&v.getAsLong("expense_id")!=null)v.put("expense_id",expenses.get(n(v,"expense_id")));
    if(table.equals("settlements")){v.put("from_member_id",members.get(n(v,"from_member_id")));v.put("to_member_id",members.get(n(v,"to_member_id")));}
    long id=db.save(table,0,v);if(table.equals("groups"))groups.put(old,id);else if(table.equals("members"))members.put(old,id);else if(table.equals("expenses"))expenses.put(old,id);
   }d.setTransactionSuccessful();
  }finally{d.endTransaction();}
 }
}
