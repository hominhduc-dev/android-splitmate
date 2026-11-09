package com.example.quanlychitieu;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import com.example.quanlychitieu.utils.SplitCalculator;
public class SplitCalculatorTest {
 @Test public void distributesRemainderExactly(){Map<Long,Long> s=SplitCalculator.splitEqually(100000,Arrays.asList(1L,2L,3L));assertEquals(Long.valueOf(33334),s.get(1L));assertEquals(Long.valueOf(33333),s.get(2L));assertEquals(100000L,s.values().stream().mapToLong(Long::longValue).sum());}
 @Test public void handlesLessDongThanMembers(){assertEquals(Long.valueOf(0),SplitCalculator.splitEqually(1,Arrays.asList(1L,2L)).get(2L));}
 @Test(expected=IllegalArgumentException.class) public void rejectsZero(){SplitCalculator.splitEqually(0,Arrays.asList(1L));}
 @Test(expected=IllegalArgumentException.class) public void rejectsDuplicates(){SplitCalculator.splitEqually(10,Arrays.asList(1L,1L));}
 @Test(expected=IllegalArgumentException.class) public void rejectsEmpty(){SplitCalculator.splitEqually(10,Collections.emptyList());}
}