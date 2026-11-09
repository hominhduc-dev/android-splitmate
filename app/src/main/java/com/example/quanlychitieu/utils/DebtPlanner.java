package com.example.quanlychitieu.utils;

import java.util.*;
/** Greedy matching of net balances. Deterministic; does not claim the minimum number of transfers. */
public final class DebtPlanner {
 public static final class Transfer {
  public final long from,to,amount;
  public Transfer(long from,long to,long amount){this.from=from;this.to=to;this.amount=amount;}
 }
 public static List<Transfer> plan(Map<Long,Long> balances){
  List<long[]> debtors=new ArrayList<>(),creditors=new ArrayList<>();long sum=0;
  for(Map.Entry<Long,Long> e:balances.entrySet()){sum=Math.addExact(sum,e.getValue());if(e.getValue()==Long.MIN_VALUE)throw new IllegalArgumentException("Số dư không hợp lệ");if(e.getValue()<0)debtors.add(new long[]{e.getKey(),-e.getValue()});if(e.getValue()>0)creditors.add(new long[]{e.getKey(),e.getValue()});}
  if(sum!=0)throw new IllegalArgumentException("Tổng số dư phải bằng 0");
  Comparator<long[]> order=(a,b)->{int c=Long.compare(b[1],a[1]);return c==0?Long.compare(a[0],b[0]):c;};debtors.sort(order);creditors.sort(order);
  List<Transfer> result=new ArrayList<>();int i=0,j=0;while(i<debtors.size()&&j<creditors.size()){long[] a=debtors.get(i),b=creditors.get(j);long n=Math.min(a[1],b[1]);result.add(new Transfer(a[0],b[0],n));a[1]-=n;b[1]-=n;if(a[1]==0)i++;if(b[1]==0)j++;}return result;
 }
}
