package com.example.quanlychitieu.ui;
import android.content.ContentValues;
import android.graphics.*;
import android.view.*;
import androidx.recyclerview.widget.*;
import com.example.quanlychitieu.databinding.ItemWorkspaceBinding;
import java.util.*;

final class WorkspaceAdapter extends ListAdapter<WorkspaceAdapter.Row,WorkspaceAdapter.Holder> {
 static class Row {
  final String key,kind,title,detail,amount,avatar;final ContentValues data;final boolean checked;
  Row(String key,String kind,String title,String detail,String amount,String avatar,ContentValues data){this.key=key;this.kind=kind;this.title=title;this.detail=detail;this.amount=amount;this.avatar=avatar;this.data=data==null?null:new ContentValues(data);checked=data!=null&&data.containsKey("is_bought")&&data.getAsInteger("is_bought")==1;}
  String signature(){return kind+title+detail+amount+avatar+checked+(data==null?"":data.toString());}
 }
 interface Actions {void open(Row row,View anchor);void menu(Row row,View anchor);void check(Row row,boolean checked);}
 private final Actions actions;
 WorkspaceAdapter(Actions actions){super(new DiffUtil.ItemCallback<Row>(){public boolean areItemsTheSame(Row a,Row b){return a.key.equals(b.key);}public boolean areContentsTheSame(Row a,Row b){return a.signature().equals(b.signature());}});this.actions=actions;setHasStableIds(false);}
 static class Holder extends RecyclerView.ViewHolder {final ItemWorkspaceBinding b;Holder(ItemWorkspaceBinding b){super(b.getRoot());this.b=b;}}
 public Holder onCreateViewHolder(ViewGroup p,int type){return new Holder(ItemWorkspaceBinding.inflate(LayoutInflater.from(p.getContext()),p,false));}
 public void onBindViewHolder(Holder h,int p){Row r=getItem(p);ItemWorkspaceBinding b=h.b;boolean section=r.kind.equals("section"),hero=r.kind.equals("hero"),grocery=r.kind.equals("grocery"),item=r.data!=null;
  b.label.setText(r.title);b.detail.setText(r.detail);b.amount.setText(r.amount);b.avatar.setText(r.avatar);
  b.label.setTextColor(hero?Color.WHITE:0xFF202D28);b.detail.setTextColor(hero?0xFFD5E8DE:0xFF69766F);b.amount.setTextColor(hero?Color.WHITE:r.kind.equals("debit")?0xFFA2502D:0xFF236C53);
  b.label.setTextSize(section?18:hero?14:16);b.amount.setTextSize(hero?32:18);
  b.detail.setVisibility(r.detail.isEmpty()?View.GONE:View.VISIBLE);b.amount.setVisibility(r.amount.isEmpty()?View.GONE:View.VISIBLE);b.avatar.setVisibility(r.avatar.isEmpty()||r.kind.equals("category")?View.GONE:View.VISIBLE);
  b.surface.setBackgroundTintList(android.content.res.ColorStateList.valueOf(hero?0xFF236C53:section?0xFFF7F8FA:Color.WHITE));
  b.more.setVisibility(item&&!r.kind.equals("suggestion")&&!r.kind.equals("credit")&&!r.kind.equals("debit")&&!r.kind.equals("balance")?View.VISIBLE:View.GONE);b.more.setContentDescription("Tùy chọn: "+r.title);
  b.check.setOnCheckedChangeListener(null);b.check.setVisibility(grocery?View.VISIBLE:View.GONE);b.check.setContentDescription("Đã mua "+r.title);b.check.setChecked(r.checked);
  b.label.setPaintFlags(r.checked?b.label.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG:b.label.getPaintFlags()&~Paint.STRIKE_THRU_TEXT_FLAG);b.label.setAlpha(r.checked?.5f:1f);
  b.check.setOnCheckedChangeListener((v,c)->{b.check.setEnabled(false);actions.check(r,c);});b.check.setEnabled(!grocery||r.data.getAsLong("expense_id")==null);
  b.chart.setVisibility(r.kind.equals("category")?View.VISIBLE:View.GONE);
  if(r.kind.equals("category"))b.chart.setProgress(Integer.parseInt(r.avatar));
  b.action.setVisibility(r.kind.equals("suggestion")?View.VISIBLE:View.GONE);b.action.setOnClickListener(v->actions.open(r,b.action));
  b.surface.setOnClickListener(item?v->actions.open(r,b.more):null);b.more.setOnClickListener(v->actions.menu(r,b.more));
 }
}
