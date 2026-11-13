package com.example.quanlychitieu.domain;
import java.util.*;
public final class SplitCalculator {
 public static Map<Long,Long> splitEqually(long amount,List<Long> ids){
 if(amount<=0||ids==null||ids.isEmpty()||ids.contains(null)||new HashSet<>(ids).size()!=ids.size()) throw new IllegalArgumentException("Số tiền hoặc thành viên không hợp lệ");
 Map<Long,Long> result=new LinkedHashMap<>(); long each=amount/ids.size(), remainder=amount%ids.size();
 for(int i=0;i<ids.size();i++) result.put(ids.get(i),each+(i<remainder?1:0)); return result;
 }
}