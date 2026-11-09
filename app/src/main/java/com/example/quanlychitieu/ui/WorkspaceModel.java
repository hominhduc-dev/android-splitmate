package com.example.quanlychitieu.ui;

import android.app.Application;
import android.content.ContentValues;
import androidx.lifecycle.*;
import com.example.quanlychitieu.database.DatabaseHelper;
import java.util.*;
import java.util.concurrent.*;

/** One ordered worker owns all database reads and writes. Snapshots contain no Views. */
public class WorkspaceModel extends AndroidViewModel {
 public static class Snapshot {
  public long groupId,total,pending,debt;
  public List<ContentValues> groups,members,expenses,groceries,settlements,categories,shares;
 }
 public final MutableLiveData<Snapshot> data=new MutableLiveData<>();
 public final MutableLiveData<Boolean> busy=new MutableLiveData<>(false);
 public final MutableLiveData<String> error=new MutableLiveData<>();
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final DatabaseHelper db; private long requestedGroup; private int generation;
 public WorkspaceModel(Application app){super(app);db=new DatabaseHelper(app);requestedGroup=app.getSharedPreferences("workspace",0).getLong("group",0);load();}
 public void select(long group){requestedGroup=group;load();}
 public interface Write {void run(DatabaseHelper db) throws Exception;}
 public void write(Write action,Runnable success){busy.setValue(true);worker.execute(()->{try{action.run(db);new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(success!=null)success.run();load();});}catch(Exception e){new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{busy.setValue(false);error.setValue(e instanceof android.database.sqlite.SQLiteConstraintException?"Thành viên đã có giao dịch. Bạn chỉ có thể đổi tên.":e.getMessage()==null?"Không thể lưu. Vui lòng thử lại.":e.getMessage());});}});}
 public <T> void read(java.util.concurrent.Callable<T> task,java.util.function.Consumer<T> result){
  busy.setValue(true);worker.execute(()->{try{T value=task.call();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{busy.setValue(false);result.accept(value);});}catch(Exception e){new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{busy.setValue(false);error.setValue(e.getMessage()==null?"Không thể đọc tệp":e.getMessage());});}});
 }
 public void load(){final int ticket=++generation;final long requested=requestedGroup;busy.setValue(true);worker.execute(()->{try{Snapshot s=new Snapshot();
 s.groups=db.rows("SELECT g.*,COUNT(m.id) AS member_count FROM groups g LEFT JOIN members m ON m.group_id=g.id GROUP BY g.id ORDER BY g.created_at DESC");s.groupId=requested;boolean found=false;for(ContentValues g:s.groups)if(g.getAsLong("id")==requested)found=true;if(!found)s.groupId=s.groups.isEmpty()?0:s.groups.get(0).getAsLong("id");
 String key=""+s.groupId;
 s.members=db.rows("SELECT m.*,COALESCE(p.n,0) AS paid,COALESCE(o.n,0) AS owed,COALESCE(p.n,0)-COALESCE(o.n,0)+COALESCE(a.n,0)-COALESCE(b.n,0) AS balance FROM members m LEFT JOIN (SELECT payer_id,SUM(amount) n FROM expenses GROUP BY payer_id) p ON p.payer_id=m.id LEFT JOIN (SELECT member_id,SUM(share_amount) n FROM expense_shares GROUP BY member_id) o ON o.member_id=m.id LEFT JOIN (SELECT from_member_id,SUM(amount) n FROM settlements GROUP BY from_member_id) a ON a.from_member_id=m.id LEFT JOIN (SELECT to_member_id,SUM(amount) n FROM settlements GROUP BY to_member_id) b ON b.to_member_id=m.id WHERE m.group_id=? ORDER BY m.id",key);
 s.expenses=db.rows("SELECT e.*,m.name AS payer FROM expenses e JOIN members m ON m.id=e.payer_id WHERE e.group_id=? ORDER BY e.created_at DESC,e.id DESC",key);
 s.groceries=db.rows("SELECT * FROM grocery_items WHERE group_id=? ORDER BY id DESC",key);
 s.settlements=db.rows("SELECT s.*,a.name AS sender,b.name AS receiver FROM settlements s JOIN members a ON a.id=s.from_member_id JOIN members b ON b.id=s.to_member_id WHERE s.group_id=? ORDER BY s.created_at DESC",key);
 s.categories=db.rows("SELECT category,SUM(amount) AS total FROM expenses WHERE group_id=? GROUP BY category ORDER BY total DESC",key);
 s.shares=db.rows("SELECT x.* FROM expense_shares x JOIN expenses e ON e.id=x.expense_id WHERE e.group_id=?",key);
 for(ContentValues e:s.expenses)s.total+=e.getAsLong("amount");for(ContentValues g:s.groceries)if(g.getAsInteger("is_bought")==0)s.pending+=g.getAsLong("quantity")*g.getAsLong("estimated_price");for(ContentValues m:s.members)s.debt+=Math.max(0,m.getAsLong("balance"));
 new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(ticket!=generation)return;requestedGroup=s.groupId;getApplication().getSharedPreferences("workspace",0).edit().putLong("group",s.groupId).apply();data.setValue(s);busy.setValue(false);});
 }catch(Exception e){new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(ticket==generation){busy.setValue(false);error.setValue("Không thể tải dữ liệu. Vui lòng mở lại ứng dụng.");}});}});}
 @Override protected void onCleared(){worker.execute(db::close);worker.shutdown();}
}
