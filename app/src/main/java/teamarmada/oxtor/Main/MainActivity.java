package teamarmada.oxtor.Main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

import teamarmada.oxtor.Interfaces.ScreenManager;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Ui.DialogFragment.TaskBottomSheet;
import teamarmada.oxtor.Utils.AnimationHelper;
import teamarmada.oxtor.Utils.InAppUpdate;
import teamarmada.oxtor.ViewModels.MainViewModel;
import teamarmada.oxtor.databinding.ActivityMainBinding;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements  MenuProvider, ScreenManager, NavController.OnDestinationChangedListener {

    public static final String TAG=MainActivity.class.getSimpleName();

    public static final String PREFS="preferences";
    public static final String ENCRYPTION_PASSWORD="encryptionPassword";
    public static final String IS_DARK_MODE_ON="isDarkModeOn";
    public static final String USED_SPACE="usedSpace";
    public static final String SORT_PREFERENCE="sortPreference";

    private ActivityMainBinding binding;
    public BottomNavigationView navView;
    private FrameLayout adViewContainer;
    private static LinearProgressIndicator progressIndicator;
    private NavController navControllerMain;
    private static SharedPreferences sharedPreferences;
    private MainViewModel mainViewModel;
    private boolean isDarkModeOn;
    private TaskBottomSheet taskBottomSheet;
    private InAppUpdate inAppUpdate;

    private final String[] permissions=new String[]{
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_NETWORK_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.VIBRATE
    };

    public static Observer<Boolean> observer=aBoolean ->{

        if (aBoolean) {
            progressIndicator.show();
        } else {
            progressIndicator.hide();
        }
    };

    public MainActivity(){}

    public static String getEncryptionPassword() {
        String s=sharedPreferences.getString(ENCRYPTION_PASSWORD,null);
        if(s==null){
            final String t=UUID.randomUUID().toString();
            sharedPreferences.edit().putString(ENCRYPTION_PASSWORD,t).apply();
            s=t;
        }
        return s;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        isDarkModeOn = sharedPreferences.getBoolean(IS_DARK_MODE_ON, false);
        if (isDarkModeOn) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        binding =ActivityMainBinding.inflate(getLayoutInflater());
        binding.setLifecycleOwner(this);
        setContentView(binding.getRoot());
        inAppUpdate=new InAppUpdate(this);
        navView= binding.navView;
        progressIndicator = binding.progressBar;
        setSupportActionBar(binding.toolbar);
        addMenuProvider(this,this);
        NavHostFragment navHostMain = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_main);
        assert navHostMain !=null;
        navControllerMain = navHostMain.getNavController();
        navControllerMain.setLifecycleOwner(this);
        NavigationUI.setupWithNavController(navView, navControllerMain);
        navControllerMain.addOnDestinationChangedListener(this);
        if(!checkForPermissions()) permissionLauncher.launch(permissions);
        mainViewModel =new ViewModelProvider(this).get(MainViewModel.class);
        taskBottomSheet=new TaskBottomSheet();
        adViewContainer=binding.adView;
    }

    @Override
    protected void onStart() {
        super.onStart();
        observeUploadTasks();
        observeDownloadTasks();
        observeLoadingState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        inAppUpdate.onActivityResult(requestCode,resultCode, data);
    }

    public boolean checkForPermissions(){
        return  ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this, permissions[1]) == PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this, permissions[2]) == PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this, permissions[3]) == PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this, permissions[4]) == PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this,permissions[5]) == PackageManager.PERMISSION_GRANTED;
    }

    private void observeLoadingState(){
        mainViewModel.getIsTaskRunning().observe(this,observer);
    }

    private void observeUploadTasks(){
        mainViewModel.mutableUploadList.observe(this, fileTaskItems -> {
            fileTaskItems.forEach(fileItemTask->
                    fileItemTask.getTask()
                            .addOnCompleteListener(task -> {
                            if(task.isComplete()||task.isCanceled())
                                mainViewModel.removeUploadItem(fileItemTask);
                        })
                            .addOnFailureListener(e -> {
                            Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();
                        }));
            whenListIsEmpty(mainViewModel.mutableUploadList.getValue().isEmpty(), v -> {
                if(!taskBottomSheet.isInLayout()) {
                    taskBottomSheet.setTab(0);
                    try {
                        taskBottomSheet.showNow(getSupportFragmentManager(), "Tasks");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else taskBottomSheet.dismiss();
            });
        });
    }

    private void observeDownloadTasks(){
        mainViewModel.mutableDownloadList.observe(this, fileTaskItems -> {
            fileTaskItems.forEach(fileTaskItem->
                fileTaskItem.getTask()
                        .addOnCompleteListener(task -> {
                            if(task.isComplete()||task.isCanceled())
                                mainViewModel.removeDownloadItem(fileTaskItem);
                        })
                        .addOnFailureListener(e-> {
                                Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();
                        }));
            whenListIsEmpty(mainViewModel.mutableDownloadList.getValue().isEmpty(), v -> {
                if(!taskBottomSheet.isInLayout()) {
                    taskBottomSheet.setTab(1);
                    try {
                        taskBottomSheet.showNow(getSupportFragmentManager(), "Tasks");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else taskBottomSheet.dismiss();
            });
        });
    }

    private void whenListIsEmpty(boolean isEmpty,View.OnClickListener onClickListener){
        if(isEmpty) {
            binding.taskButtonMain.clearAnimation();
            binding.taskButtonMain.hide();
        }
        else{
            binding.taskButtonMain.show();
            binding.taskButtonMain.setAnimation(AnimationHelper.getInfiniteRotationAnim());
            binding.taskButtonMain.setOnClickListener(onClickListener);
        }
    }

    private final ActivityResultLauncher<String[]> permissionLauncher=
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result->{
                if(checkForPermissions()) {
                    Snackbar.make(binding.getRoot(), R.string.permission_rejected, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.grant, v -> launchPermission())
                            .show();
                }
            });

    private void launchPermission(){
      permissionLauncher.launch(permissions);
    }

    @Override
    public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
        binding.toolbar.setTitle(navDestination.getLabel());
    }

    @Override
    public boolean onSupportNavigateUp() {
        return super.onSupportNavigateUp()||navControllerMain.navigateUp();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        MenuItem item=menu.getItem(0);
        if(isDarkModeOn) item.setIcon(R.drawable.ic_baseline_wb_sunny_24);
        else item.setIcon(R.drawable.ic_baseline_dark_mode_24);
    }

    private void changeTheme(MenuItem item) {
        isDarkModeOn=!isDarkModeOn;
        if (isDarkModeOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            item.setIcon(R.drawable.ic_baseline_wb_sunny_24);
        }
        else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            item.setIcon(R.drawable.ic_baseline_dark_mode_24);
        }
        sharedPreferences.edit().putBoolean(IS_DARK_MODE_ON, isDarkModeOn).apply();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.nightmode){
            changeTheme(item);
            return true;
        }
        else return false;
    }

    @Override
    public void enableFullscreen() {
        adViewContainer.removeAllViews();
        getSupportActionBar().hide();
        hideNavigationBar();
    }

    @Override
    public void disableFullscreen() {
        adViewContainer.post(() -> ActivityLifecycleObserver.getInstance(this).loadBanner(adViewContainer));
        getSupportActionBar().show();
        showNavigationBar();
    }

    @Override
    public void hideNavigationBar() {
        navView.setVisibility(View.GONE);
    }

    @Override
    public void showNavigationBar() {
        navView.setVisibility(View.VISIBLE);
    }

    @Override
    public void enableTouchableLayout() {
        try {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.dimAmount = 0.5f;
            getWindow().setAttributes(lp);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void disableTouchableLayout() {
        try {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.dimAmount = 0.0f;
            getWindow().setAttributes(lp);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}
