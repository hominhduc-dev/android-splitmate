package com.example.quanlychitieu;

import android.widget.EditText;
import androidx.lifecycle.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.quanlychitieu.ui.common.BaseViewModel;
import com.example.quanlychitieu.ui.grocery.GroceryViewModel;
import com.example.quanlychitieu.ui.group.GroupViewModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WorkspaceFlowTest {
 static class DataIdle implements IdlingResource {
  volatile boolean idle;volatile ResourceCallback callback;
  public String getName(){return "SQLite worker";}
  public boolean isIdleNow(){return idle;}
  public void registerIdleTransitionCallback(ResourceCallback c){callback=c;}
  void set(Boolean busy){idle=!Boolean.TRUE.equals(busy);if(idle&&callback!=null)callback.onTransitionToIdle();}
 }
 private void capture(String name){
  android.graphics.Bitmap shot=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
  java.io.File file=new java.io.File(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),name);
  try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new AssertionError(e);}finally{shot.recycle();}
 }
 @Test public void groupExpenseDebtAndGrocerySurviveRecreation(){
  DataIdle idle=new DataIdle();final GroupViewModel[] model={null};final GroceryViewModel[] grocery={null};Observer<Boolean> observer=idle::set;final long[] created={0};
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{model[0]=new ViewModelProvider(a).get(GroupViewModel.class);grocery[0]=new ViewModelProvider(a).get(GroceryViewModel.class);BaseViewModel.busy().observeForever(observer);});IdlingRegistry.getInstance().register(idle);
   try{
    onView(withId(R.id.nav_groups)).perform(click());onView(withId(R.id.add)).perform(click());
    onView(isAssignableFrom(EditText.class)).perform(replaceText("Kiểm thử SplitMate"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard());onView(withId(android.R.id.button1)).perform(click());
    onView(withText("Thành viên")).perform(click());
    scenario.onActivity(a->created[0]=model[0].getState().getValue().groupId);
    for(String member:new String[]{"An","Bình","Cường"}){
     onView(withId(R.id.add)).perform(click());onView(isAssignableFrom(EditText.class)).perform(replaceText(member),androidx.test.espresso.action.ViewActions.closeSoftKeyboard());onView(withId(android.R.id.button1)).perform(click());
    }
    onView(withText("Chi tiêu")).perform(click());onView(withId(R.id.add)).perform(click());
    onView(withHint("Tên khoản chi")).perform(replaceText("Đi chợ chung"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
    onView(withHint("Số tiền (VNĐ)")).perform(replaceText("300000"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
    onView(withText(startsWith("Mỗi người chịu"))).check(matches(withText(containsString("An: 100.000 đ"))));
    onView(withId(android.R.id.button1)).perform(click());
    onView(withId(R.id.filter)).perform(click());onView(withHint("Tìm theo tên khoản chi")).perform(replaceText("Không tồn tại"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard());onView(withId(android.R.id.button1)).perform(click());onView(withText("Không có kết quả phù hợp")).check(matches(isDisplayed()));
    onView(withId(R.id.filter)).perform(click());onView(withId(android.R.id.button3)).perform(click());
    onView(withText("Công nợ")).perform(click());
    scenario.onActivity(a->{GroupViewModel.State s=model[0].getState().getValue();assertEquals(300000,s.total);assertEquals(200000,s.debt);assertEquals(200000L,s.members.get(0).balance);});
    capture("splitmate-debt.png");
    onView(withText("Bình → An")).perform(click());
    onView(withHint("Số tiền đã chuyển (VNĐ)")).perform(replaceText("100000"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
    onView(withId(android.R.id.button1)).perform(click());
    scenario.onActivity(a->assertEquals(100000,model[0].getState().getValue().debt));
    onView(withId(R.id.nav_grocery)).perform(click());onView(withId(R.id.add)).perform(click());
    onView(withHint("Tên món hàng")).perform(replaceText("Gạo"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
    onView(withHint("Số lượng")).perform(replaceText("2"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard());onView(withHint("Đơn giá dự kiến (VNĐ)")).perform(replaceText("25000"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
    onView(withId(android.R.id.button1)).perform(click());onView(withContentDescription("Đã mua Gạo")).perform(click());onView(withText("Đã mua")).perform(click());
    onView(withContentDescription("Đã mua Gạo")).check(matches(isChecked()));capture("splitmate-grocery.png");scenario.recreate();
    onView(withContentDescription("Đã mua Gạo")).check(matches(isChecked()));
    scenario.onActivity(a->{assertEquals(0,grocery[0].getState().getValue().pending);assertEquals(300000,model[0].getState().getValue().total);});
    onView(withId(R.id.convert)).perform(click());onView(withId(android.R.id.button1)).perform(click());
    onView(withHint("Số tiền (VNĐ)")).perform(replaceText("60000"),androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
    onView(withId(android.R.id.button1)).perform(click());
    scenario.onActivity(a->{assertEquals(360000,model[0].getState().getValue().total);assertNotNull(grocery[0].getState().getValue().items.get(0).expenseId);});
    onView(withContentDescription("Đã mua Gạo")).check(matches(not(isEnabled())));capture("splitmate-grocery-linked.png");
   }finally{
    scenario.onActivity(a->{if(created[0]!=0)model[0].deleteGroup(created[0]);});
    onIdle();scenario.onActivity(a->BaseViewModel.busy().removeObserver(observer));IdlingRegistry.getInstance().unregister(idle);
   }
  }
 }
}
