package teamarmada.oxtor.Main;

import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;
import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.FileTask;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Ui.DialogFragment.ProgressDialog;
import teamarmada.oxtor.Ui.BottomSheet.TaskBottomSheet;
import teamarmada.oxtor.Utils.FileItemUtils;
import teamarmada.oxtor.Utils.InAppUpdate;
import teamarmada.oxtor.ViewModels.MainViewModel;

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
    private final String[] storagePermissions=new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static FloatingActionButton addButton;
    public static RecyclerView.OnScrollListener listener=new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if(dy>0&&addButton.isShown()) {
                addButton.hide();
            } else  {
                addButton.show();
            }
        }
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
        addButton=binding.add;
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
        navControllerMain.addOnDestinationChangedListener(this);
        NavigationUI.setupWithNavController(navView,navControllerMain);
        if(!checkForPermissions(permissions)) askPermission(permissions);
        initUI();
    }

    private void initUI(){
        addButton.setOnClickListener(v -> {
            if (!checkForPermissions(storagePermissions)){
                askPermission(storagePermissions);
            }
            else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                selectFileLauncher.launch(intent);
            }
        });
    }

    public void uploadFileByIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            List<Uri> paths=new ArrayList<>();
            try {
                ClipData clipData = intent.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    paths.add(clipData.getItemAt(i).getUri());
                }
            }catch (Exception e){
                e.printStackTrace();
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null) {
                    paths.add(uri);
                }
            }
            uploadSelectedFiles(paths);
            paths.clear();
        }
    }

    private final ActivityResultLauncher<Intent> selectFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Intent data = result.getData();
                        ClipData clipData = data.getClipData();
                        List<Uri> selectedFiles = new ArrayList<>();
                        if (clipData != null) {
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                Uri uri = clipData.getItemAt(i).getUri();
                                selectedFiles.add(uri);
                            }
                        } else {
                            Uri uri = data.getData();
                            if (uri != null) {
                                selectedFiles.add(uri);
                            }
                        }
                        uploadSelectedFiles(selectedFiles);
                    }
                }
            });

    private void uploadSelectedFiles(List<Uri> results){
        List<FileItem> fileItems=new ArrayList<>();
        long size=0;
        for(int i=0;i<results.size();i++){
            Uri uri=results.get(i);
            fileItems.add(FileItemUtils.getFileItemFromPath(this,uri));
            size+=fileItems.get(i).getFileSize();
        }
        if (size <= 5*FileItemUtils.ONE_GIGABYTE) {
            ActivityLifecycleObserver.getInstance(this).startUpload(fileItems);
        }
        else {
            Snackbar.make(binding.getRoot(), "Can't upload as you are only permitted 5GB of space on this account",Snackbar.LENGTH_SHORT).show();
        }
    }

    private void openInternetSetting() {
        try {
            Intent intent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
            startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try{
            if(mainViewModel.getAuthInstance()!=null) {
                Intent intent = getIntent();
                if (intent.getData() != null)
                    uploadFileByIntent(intent);
            }else{
                // TODO: 13-06-2023 to login
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        Snackbar snackbar=Snackbar.make(binding.getRoot(), R.string.no_connection_found, Snackbar.LENGTH_INDEFINITE)
                .setAction("Check", v -> openInternetSetting());
        mainViewModel.getInternetConnectionLiveData(this).observe(this, isConnected->{
                try {
                    if(isConnected){
                        snackbar.dismiss();
                    }
                    else{
                        snackbar.show();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
        });
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
        mainViewModel.mutableFileDownloadList.observe(this, fileTaskItems -> {
            for (FileTask<FileDownloadTask> fileTaskItem : fileTaskItems) {
                fileTaskItem.getTask().addOnCompleteListener(task->mainViewModel.removeFileDownloadItem(fileTaskItem));
            }
            whenListIsEmpty(mainViewModel.mutableFileDownloadList.getValue().isEmpty(),1);
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
                try {
                    if (!taskBottomSheet.isInLayout()) {
                        taskBottomSheet.showNow(getSupportFragmentManager(), "Tasks");
                        taskBottomSheet.setTab(tabPosition);
                    } else {
                        taskBottomSheet.dismiss();
                    }
                }catch (Exception e){
                    e.printStackTrace();
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

        rotate.setDuration(500);
        rotate.setRepeatCount(Animation.INFINITE);
        return rotate;
    }

    @Override
    public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
        binding.toolbar.setTitle(navDestination.getLabel());
        if(navDestination.getId()==R.id.navigation_home){
            addButton.show();
        }
        else{
            addButton.hide();
        }
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
            if(!mainViewModel.mutableUploadList.getValue().isEmpty()
                    || !mainViewModel.mutableFileDownloadList.getValue().isEmpty())
                createAlertDialog().show();
            else
                changeTheme(item);
            return true;
        }
        else
            return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding=null;
    }

    @Override
    public void enableFullscreen() {
        adViewContainer.removeAllViews();
        getSupportActionBar().hide();
        hideNavigationBar();
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void disableFullscreen() {
        try {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }catch (Exception e){
            e.printStackTrace();
        }
        
        getSupportActionBar().show();
        adViewContainer.post(() -> ActivityLifecycleObserver.getInstance(this).loadBanner(adViewContainer));
        showNavigationBar();
    }

    @Override
    public void hideNavigationBar() {
        navView.setVisibility(View.GONE);
    }

    @Override
    public void showNavigationBar() {
        navView.setVisibility(VISIBLE);
        try {
            navView.showContextMenu();
        }catch(Exception e){
            e.printStackTrace();
        }

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
