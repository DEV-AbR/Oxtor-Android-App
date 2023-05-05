package teamarmada.oxtor.Main;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

import teamarmada.oxtor.Interfaces.ScreenManager;
import teamarmada.oxtor.Model.FileTask;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Ui.DialogFragment.ProgressDialog;
import teamarmada.oxtor.Ui.DialogFragment.TaskBottomSheet;
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
    private BottomNavigationView navView;
    private FrameLayout adViewContainer;
    private static LinearProgressIndicator progressIndicator;
    private NavController navControllerMain;
    private static SharedPreferences sharedPreferences;
    private MainViewModel mainViewModel;
    private boolean isDarkModeOn;
    private TaskBottomSheet taskBottomSheet;
    private InAppUpdate inAppUpdate;
    private ProgressDialog progressDialog;

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

    public static String getEncryptionPassword() {
        String s=sharedPreferences.getString(ENCRYPTION_PASSWORD,null);
        if(s==null){
            final String t=UUID.randomUUID().toString();
            sharedPreferences.edit().putString(ENCRYPTION_PASSWORD,t).apply();
            s=t;
        }
        return s;
    }

    public MainActivity(){}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        isDarkModeOn = sharedPreferences.getBoolean(IS_DARK_MODE_ON, false);
        AppCompatDelegate.setDefaultNightMode(isDarkModeOn? AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO);
        binding =ActivityMainBinding.inflate(getLayoutInflater());
        binding.setLifecycleOwner(this);
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        addMenuProvider(this,this);
        navView= binding.navView;
        progressIndicator = binding.progressBar;
        adViewContainer=binding.adView;
        inAppUpdate=InAppUpdate.getInstance(this);
        taskBottomSheet=new TaskBottomSheet();
        progressDialog= new ProgressDialog();
        mainViewModel =new ViewModelProvider(this).get(MainViewModel.class);
        NavHostFragment navHostMain = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_main);
        assert navHostMain !=null;
        navControllerMain = navHostMain.getNavController();
        navControllerMain.setLifecycleOwner(this);
        NavigationUI.setupWithNavController(navView, navControllerMain);
        navControllerMain.addOnDestinationChangedListener(this);
        if(!checkForPermissions()) askPermission();
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

    private void observeLoadingState(){
        mainViewModel.getIsTaskRunning().observe(this,observer);
    }

    private void observeUploadTasks(){
        mainViewModel.mutableUploadList.observe(this, fileTaskItems -> {
            for (FileTask<UploadTask> fileTaskItem : fileTaskItems) {
                fileTaskItem.getTask().addOnCompleteListener(task->mainViewModel.removeUploadItem(fileTaskItem));
            }
            whenListIsEmpty(mainViewModel.mutableUploadList.getValue().isEmpty(),0);
        });
    }

    private void observeDownloadTasks(){
        mainViewModel.mutableDownloadList.observe(this, fileTaskItems -> {
            for (FileTask<StreamDownloadTask> fileTaskItem : fileTaskItems) {
                fileTaskItem.getTask().addOnCompleteListener(task->mainViewModel.removeDownloadItem(fileTaskItem));
            }
            whenListIsEmpty(mainViewModel.mutableDownloadList.getValue().isEmpty(),1);
        });
    }

    private void whenListIsEmpty(boolean isEmpty,int tabPosition){
        if(isEmpty) {
            binding.taskButtonMain.clearAnimation();
            binding.taskButtonMain.hide();
        }
        else{
            binding.taskButtonMain.show();
            binding.taskButtonMain.setAnimation(getInfiniteRotationAnim());
            binding.taskButtonMain.setOnClickListener(v->{
                if(!taskBottomSheet.isInLayout()) {
                    taskBottomSheet.showNow(getSupportFragmentManager(), "Tasks");
                    taskBottomSheet.setTab(tabPosition);
                }
                else {
                    taskBottomSheet.dismiss();
                }
            });
        }
    }

    public RotateAnimation getInfiniteRotationAnim(){
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );

        rotate.setDuration(500); // it was 800 before
        rotate.setRepeatCount(Animation.INFINITE);
        return rotate;
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
    
    private final ActivityResultLauncher<String[]> permissionLauncher=
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result->{
                if(!checkForPermissions()) {
                    Snackbar.make(binding.getRoot(), R.string.permission_rejected, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.grant, v -> askPermission())
                            .show();
                }
            });

    private void askPermission(){
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

    private AlertDialog createAlertDialog(){
        return new MaterialAlertDialogBuilder(this,R.style.Theme_Oxtor_AlertDialog)
                .setCancelable(false)
                .setTitle("Task still running")
                .setMessage("Changing theme might disrupt the ongoing task")
                .setNegativeButton(R.string.cancel,((dialogInterface, i) -> dialogInterface.dismiss()))
                .create();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        MenuItem item=menu.getItem(0);
        item.setIcon(isDarkModeOn?R.drawable.ic_baseline_wb_sunny_24:R.drawable.ic_baseline_dark_mode_24);
    }

    private void changeTheme(MenuItem item) {
        isDarkModeOn=!isDarkModeOn;
        AppCompatDelegate.setDefaultNightMode(isDarkModeOn?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO);
        item.setIcon(isDarkModeOn?R.drawable.ic_baseline_wb_sunny_24:R.drawable.ic_baseline_dark_mode_24);
        sharedPreferences.edit().putBoolean(IS_DARK_MODE_ON, isDarkModeOn).apply();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.nightmode){
            if(!mainViewModel.mutableUploadList.getValue().isEmpty() ||
                    !mainViewModel.mutableDownloadList.getValue().isEmpty())
                createAlertDialog().show();
            else
                changeTheme(item);
            return true;
        }
        else
            return false;
    }

    @Override
    public void enableFullscreen() {
        adViewContainer.removeAllViews();
        getSupportActionBar().hide();
        hideNavigationBar();
//        try {
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
    }

    @Override
    public void disableFullscreen() {
        adViewContainer.post(() -> ActivityLifecycleObserver.getInstance(this).loadBanner(adViewContainer));
        getSupportActionBar().show();
        showNavigationBar();
//        try {
//            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
    }

    @Override
    public void hideNavigationBar() {
        navView.setVisibility(View.GONE);
    }

    @Override
    public void showNavigationBar() {
        navView.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SuspiciousIndentation")
    @Override
    public void showProgressDialog (){
        try{
            if(!progressDialog.isAdded())
                progressDialog.show(getSupportFragmentManager(),"Loading");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @SuppressLint("SuspiciousIndentation")
    @Override
    public void hideProgressDialog (){
        try{
            if(progressDialog.isAdded())
                progressDialog.dismiss();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
