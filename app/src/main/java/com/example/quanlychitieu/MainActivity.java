package com.example.quanlychitieu;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import androidx.activity.OnBackPressedCallback;
import com.example.quanlychitieu.databinding.ActivityMainBinding;
import com.example.quanlychitieu.ui.WorkspaceFragment;

/** Hosts navigation only. Data and screen rendering live outside the Activity. */
public class MainActivity extends AppCompatActivity {
 private ActivityMainBinding binding;
 public void openGroup(){
  binding.navigation.getMenu().findItem(R.id.nav_groups).setChecked(true);
  WorkspaceFragment f=WorkspaceFragment.create(1);f.getArguments().putBoolean("detail",true);
  getSupportFragmentManager().beginTransaction().replace(R.id.container,f).commit();
 }
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);binding=ActivityMainBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
  new WindowInsetsControllerCompat(getWindow(),binding.getRoot()).setAppearanceLightNavigationBars(true);
  ViewCompat.setOnApplyWindowInsetsListener(binding.main,(v,i)->{Insets b=i.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.ime());v.setPadding(b.left,b.top,b.right,b.bottom);return i;});
  binding.navigation.setOnItemSelectedListener(item->{int page=item.getItemId()==R.id.nav_groups?1:item.getItemId()==R.id.nav_grocery?2:0;
   getSupportFragmentManager().beginTransaction().replace(R.id.container,WorkspaceFragment.create(page)).commit();return true;});
  if(state==null)binding.navigation.setSelectedItemId(R.id.nav_home);
  getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){public void handleOnBackPressed(){
   androidx.fragment.app.Fragment f=getSupportFragmentManager().findFragmentById(R.id.container);
   if(f instanceof WorkspaceFragment && ((WorkspaceFragment)f).closeGroup())return;
   if(binding.navigation.getSelectedItemId()!=R.id.nav_home){binding.navigation.setSelectedItemId(R.id.nav_home);return;}
   setEnabled(false);getOnBackPressedDispatcher().onBackPressed();setEnabled(true);
  }});
 }
}
