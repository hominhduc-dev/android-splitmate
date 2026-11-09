package com.example.quanlychitieu;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import com.example.quanlychitieu.utils.DebtPlanner;
public class DebtPlannerTest {
 @Test public void suggestedTransfersCancelEveryBalance(){
  Map<Long,Long> balances=new LinkedHashMap<>();balances.put(1L,200000L);balances.put(2L,-100000L);balances.put(3L,-100000L);
  List<DebtPlanner.Transfer> transfers=DebtPlanner.plan(balances);assertEquals(2,transfers.size());
  for(DebtPlanner.Transfer t:transfers){assertTrue(t.amount>0);balances.put(t.from,balances.get(t.from)+t.amount);balances.put(t.to,balances.get(t.to)-t.amount);}for(long balance:balances.values())assertEquals(0,balance);
 }
 @Test public void handlesMultipleCreditorsAndRounding(){Map<Long,Long> b=new LinkedHashMap<>();b.put(1L,33334L);b.put(2L,33333L);b.put(3L,-66667L);List<DebtPlanner.Transfer> t=DebtPlanner.plan(b);assertEquals(2,t.size());assertEquals(66667,t.stream().mapToLong(x->x.amount).sum());}
 @Test public void balancedGroupNeedsNoTransfer(){assertTrue(DebtPlanner.plan(Collections.singletonMap(1L,0L)).isEmpty());}
 @Test(expected=IllegalArgumentException.class) public void rejectsInconsistentBalances(){DebtPlanner.plan(Collections.singletonMap(1L,100L));}
}
