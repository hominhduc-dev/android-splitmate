package com.example.quanlychitieu.data.local;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;
import com.example.quanlychitieu.domain.SplitCalculator;
/**
 * Tầng truy cập SQLite: tạo 6 bảng, nâng cấp phiên bản DB và các thao tác cần transaction.
 * Giao diện không gọi trực tiếp lớp này mà đi qua các Repository trong data/repository.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
 private static DatabaseHelper instance;
 /** Cả app dùng chung một kết nối database. */
 public static synchronized DatabaseHelper getInstance(Context c){if(instance==null)instance=new DatabaseHelper(c.getApplicationContext());return instance;}
 public DatabaseHelper(Context c){this(c,"splitmate.db");}
 public DatabaseHelper(Context c,String name){super(c,name,null,3);}
 public void onConfigure(SQLiteDatabase db){db.setForeignKeyConstraintsEnabled(true);}
 public void onCreate(SQLiteDatabase d){
 d.execSQL("CREATE TABLE groups(id INTEGER PRIMARY KEY,name TEXT NOT NULL,created_at INTEGER NOT NULL)");
 d.execSQL("CREATE TABLE members(id INTEGER PRIMARY KEY,group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,name TEXT NOT NULL)");
 d.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY,group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,title TEXT NOT NULL,amount INTEGER NOT NULL CHECK(amount>0),payer_id INTEGER NOT NULL REFERENCES members(id),category TEXT NOT NULL,created_at INTEGER NOT NULL)");
 d.execSQL("CREATE TABLE expense_shares(id INTEGER PRIMARY KEY,expense_id INTEGER NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,member_id INTEGER NOT NULL REFERENCES members(id),share_amount INTEGER NOT NULL CHECK(share_amount>=0),UNIQUE(expense_id,member_id))");
 d.execSQL("CREATE TABLE grocery_items(id INTEGER PRIMARY KEY,group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,name TEXT NOT NULL,quantity INTEGER NOT NULL CHECK(quantity>0),estimated_price INTEGER NOT NULL CHECK(estimated_price>=0),is_bought INTEGER NOT NULL DEFAULT 0 CHECK(is_bought IN(0,1)),unit TEXT NOT NULL DEFAULT 'phần',expense_id INTEGER REFERENCES expenses(id) ON DELETE SET NULL)");
 d.execSQL("CREATE TABLE settlements(id INTEGER PRIMARY KEY,group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,from_member_id INTEGER NOT NULL REFERENCES members(id),to_member_id INTEGER NOT NULL REFERENCES members(id),amount INTEGER NOT NULL CHECK(amount>0),created_at INTEGER NOT NULL,CHECK(from_member_id<>to_member_id))");
 createIndexes(d);
 }
 public void onUpgrade(SQLiteDatabase d,int o,int n){if(o<2)createIndexes(d);if(o<3){d.execSQL("ALTER TABLE grocery_items ADD COLUMN unit TEXT NOT NULL DEFAULT 'phần'");d.execSQL("ALTER TABLE grocery_items ADD COLUMN expense_id INTEGER REFERENCES expenses(id) ON DELETE SET NULL");}}
 private void createIndexes(SQLiteDatabase d){
  d.execSQL("CREATE INDEX IF NOT EXISTS idx_members_group ON members(group_id)");
  d.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_group_date ON expenses(group_id,created_at)");
  d.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_payer ON expenses(payer_id)");
  d.execSQL("CREATE INDEX IF NOT EXISTS idx_shares_member ON expense_shares(member_id)");
  d.execSQL("CREATE INDEX IF NOT EXISTS idx_groceries_group ON grocery_items(group_id)");
  d.execSQL("CREATE INDEX IF NOT EXISTS idx_settlements_group ON settlements(group_id)");
  d.execSQL("CREATE INDEX IF NOT EXISTS idx_settlements_from ON settlements(from_member_id)");
  d.execSQL("CREATE INDEX IF NOT EXISTS idx_settlements_to ON settlements(to_member_id)");
 }
 public List<ContentValues> rows(String sql,String... args){List<ContentValues> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery(sql,args)){while(c.moveToNext()){ContentValues v=new ContentValues();DatabaseUtils.cursorRowToContentValues(c,v);out.add(v);}}return out;}
 public long scalar(String sql,long id){try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{""+id})){return c.moveToFirst()?c.getLong(0):0;}}
 public long save(String table,long id,ContentValues v){if(id==0)return getWritableDatabase().insertOrThrow(table,null,v);getWritableDatabase().update(table,v,"id=?",new String[]{""+id});return id;}
 public void delete(String table,long id){getWritableDatabase().delete(table,"id=?",new String[]{""+id});}
 public void deleteGroup(long id){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{d.delete("settlements","group_id=?",new String[]{""+id});d.delete("expenses","group_id=?",new String[]{""+id});d.delete("groups","id=?",new String[]{""+id});d.setTransactionSuccessful();}finally{d.endTransaction();}}
 public long expense(long id,long group,String title,long amount,long payer,String category,List<Long> ids){
 Set<Long> members=new HashSet<>();for(ContentValues m:rows("SELECT * FROM members WHERE group_id=?",""+group))members.add(m.getAsLong("id"));
 if(!members.contains(payer)||!members.containsAll(ids))throw new IllegalArgumentException("Thành viên không thuộc nhóm");
 Map<Long,Long> shares=SplitCalculator.splitEqually(amount,ids);SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
 ContentValues v=new ContentValues();v.put("group_id",group);v.put("title",title);v.put("amount",amount);v.put("payer_id",payer);v.put("category",category);if(id==0)v.put("created_at",System.currentTimeMillis());long eid=save("expenses",id,v);
 d.delete("expense_shares","expense_id=?",new String[]{""+eid});for(Map.Entry<Long,Long> e:shares.entrySet()){v=new ContentValues();v.put("expense_id",eid);v.put("member_id",e.getKey());v.put("share_amount",e.getValue());save("expense_shares",0,v);}d.setTransactionSuccessful();return eid;}finally{d.endTransaction();}}
 /** Creates the expense, shares and grocery links atomically. Repeated conversion is rejected. */
 public long convertGroceries(long group,List<Long> groceries,String title,long amount,long payer,String category,List<Long> participants){
  if(groceries==null||groceries.isEmpty()||new HashSet<>(groceries).size()!=groceries.size())throw new IllegalArgumentException("Chọn ít nhất một món đã mua");
  SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
   for(long item:groceries){List<ContentValues> found=rows("SELECT * FROM grocery_items WHERE id=? AND group_id=?", ""+item,""+group);
    if(found.isEmpty()||found.get(0).getAsInteger("is_bought")!=1||found.get(0).getAsLong("expense_id")!=null)throw new IllegalArgumentException("Món hàng đã ghi chi hoặc không còn ở trạng thái đã mua");}
   long expense=expense(0,group,title,amount,payer,category,participants);ContentValues link=new ContentValues();link.put("expense_id",expense);
   for(long item:groceries)save("grocery_items",item,link);d.setTransactionSuccessful();return expense;
  }finally{d.endTransaction();}
 }
 public void markBought(long item,boolean bought){
  List<ContentValues> list=rows("SELECT expense_id FROM grocery_items WHERE id=?",""+item);
  if(list.isEmpty())throw new IllegalArgumentException("Món hàng không còn tồn tại");
  if(list.get(0).getAsLong("expense_id")!=null)throw new IllegalArgumentException("Món này đã ghi chi. Xóa khoản chi liên kết trước khi bỏ đánh dấu.");
  ContentValues v=new ContentValues();v.put("is_bought",bought?1:0);save("grocery_items",item,v);
 }
 public void settle(long group,long from,long to,long amount){
  SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
   if(from==to||amount<=0||rows("SELECT id FROM members WHERE group_id=? AND id IN (?,?)",""+group,""+from,""+to).size()!=2||balance(from)>=0||balance(to)<=0||amount>Math.min(-balance(from),balance(to)))throw new IllegalArgumentException("Số tiền vượt công nợ hoặc chọn sai người chuyển / nhận");
   ContentValues v=new ContentValues();v.put("group_id",group);v.put("from_member_id",from);v.put("to_member_id",to);v.put("amount",amount);v.put("created_at",System.currentTimeMillis());save("settlements",0,v);d.setTransactionSuccessful();
  }finally{d.endTransaction();}
 }
 public long createDemo(){
  List<ContentValues> existing=rows("SELECT id FROM groups WHERE name=?","Demo · Phòng trọ xanh");if(!existing.isEmpty())return existing.get(0).getAsLong("id");
  SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
   ContentValues v=new ContentValues();v.put("name","Demo · Phòng trọ xanh");v.put("created_at",System.currentTimeMillis());long group=save("groups",0,v);List<Long> members=new ArrayList<>();
   for(String name:new String[]{"An","Bình","Cường"}){v=new ContentValues();v.put("name",name);v.put("group_id",group);members.add(save("members",0,v));}
   expense(0,group,"Bữa tối cuối tuần",300000,members.get(0),"Ăn uống",members);
   expense(0,group,"Tiền điện tháng này",450000,members.get(1),"Nhà ở",members);
   expense(0,group,"Đi siêu thị",90000,members.get(0),"Di chuyển",members);
   String[] names={"Gạo","Trứng","Dầu ăn","Rau xanh"},units={"kg","quả","chai","bó"};int[] quantities={2,10,1,2};long[] prices={25000,3000,45000,10000};
   for(int i=0;i<names.length;i++){v=new ContentValues();v.put("group_id",group);v.put("name",names[i]);v.put("quantity",quantities[i]);v.put("unit",units[i]);v.put("estimated_price",prices[i]);v.put("is_bought",i==3?1:0);save("grocery_items",0,v);}
   d.setTransactionSuccessful();return group;
  }finally{d.endTransaction();}
 }
 public long paid(long member){return scalar("SELECT COALESCE(SUM(amount),0) FROM expenses WHERE payer_id=?",member);}
 public long owed(long member){return scalar("SELECT COALESCE(SUM(share_amount),0) FROM expense_shares WHERE member_id=?",member);}
 public long balance(long member){return paid(member)-owed(member)+scalar("SELECT COALESCE(SUM(amount),0) FROM settlements WHERE from_member_id=?",member)-scalar("SELECT COALESCE(SUM(amount),0) FROM settlements WHERE to_member_id=?",member);}
}