package teamarmada.oxtor.Fragments.ParentFragments;


import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Main.MainActivity.SORT_PREFERENCE;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import teamarmada.oxtor.Fragments.ChildFragments.FileItemFragment;
import teamarmada.oxtor.Interfaces.ListItemCallback;
import teamarmada.oxtor.Interfaces.ScreenManager;
import teamarmada.oxtor.Main.ActivityLifecycleObserver;
import teamarmada.oxtor.Main.MainActivity;
import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Ui.DialogFragment.ItemBottomSheet;
import teamarmada.oxtor.Ui.DialogFragment.TextInputDialog;
import teamarmada.oxtor.Ui.RecyclerViewAdapter.RecyclerViewAdapter;
import teamarmada.oxtor.Utils.AnimationHelper;
import teamarmada.oxtor.Utils.FileItemUtils;
import teamarmada.oxtor.Utils.Intents;
import teamarmada.oxtor.ViewModels.HomeViewModel;
import teamarmada.oxtor.databinding.FragmentBottomsheetItemBinding;
import teamarmada.oxtor.databinding.FragmentHomeBinding;
import teamarmada.oxtor.databinding.ListFileitemBinding;

@AndroidEntryPoint
public class HomeFragment extends Fragment implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener, MenuProvider {

    public static final String TAG=HomeFragment.class.getSimpleName();
    public static final String AUTHORITY="teamarmada.oxtor.fileprovider";
    private final String[] permissions=new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    private static final String[] array={
            "Sort by Name",
            "Sort by Time",
            "Sort by Size"
    };
    private View add_button_view;
    private RecyclerView recyclerView;
    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private boolean isRotated=true;
    private FloatingActionButton addButton,fileButton,cameraButton;
    private Query query=null;
    private ActionMode actionmode=null;
    private SwipeRefreshLayout swipeRefreshLayout=null;
    private RecyclerViewAdapter<FileItem, ListFileitemBinding> adapter;
    private ItemBottomSheet itemBottomSheet;
    private SharedPreferences sharedPreferences;
    private ActivityLifecycleObserver activityLifecycleObserver;
    private ScreenManager screenManager;

    public HomeFragment(){}

    @NonNull
    public ViewModel getViewModel(){
        return homeViewModel;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().addMenuProvider(this,getViewLifecycleOwner());
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        sharedPreferences=requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        addButton=binding.add;
        fileButton=binding.file;
        cameraButton=binding.camera;
        swipeRefreshLayout=binding.swipeHome;
        recyclerView=binding.recyclerviewHome;
        try {
            screenManager=(ScreenManager) requireActivity();
        }catch (Exception e){
            e.printStackTrace();
        }
        homeViewModel=new ViewModelProvider(this).get(HomeViewModel.class);
        activityLifecycleObserver = ActivityLifecycleObserver.getInstance((AppCompatActivity) requireActivity());
        observeLoadingState();
        initUI();
        AnimationHelper.init(fileButton);
        AnimationHelper.init(cameraButton);
        fileButton.setOnClickListener(this);
        cameraButton.setOnClickListener(this);
        addButton.setOnClickListener(v -> {
            add_button_view=v;
            if (!checkForPermissions()){
                askPermission();
            }
            else {
                initAnim(add_button_view);
            }
        });
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> actionmode!=null);
        return binding.getRoot();
    }

    private void initAnim(View view){
        AnimationHelper.rotateFab(view,isRotated);
        if(isRotated){
            AnimationHelper.showIn(cameraButton);
            AnimationHelper.showIn(fileButton);
            isRotated=false;
        }else{
            AnimationHelper.showOut(cameraButton);
            AnimationHelper.showOut(fileButton);
            isRotated=true;
        }
    }

    private void initUI(){

        switch (sharedPreferences.getInt(SORT_PREFERENCE,1)){
            default:
            case 1:
                query=homeViewModel.queryToSortByTimestamp();
                break;
            case 2:
                query= homeViewModel.queryToSortBySize();
                break;
            case 0:
                query= homeViewModel.queryToSortByName();
                break;
        }
        adapter=new RecyclerViewAdapter<>(
                getLifecycle(),
                R.layout.list_fileitem,
                query,
                true,
                FileItem.class,
                itemCallback);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                RecyclerView.VERTICAL, false));
        itemBottomSheet =new ItemBottomSheet(R.layout.fragment_bottomsheet_item);
        recyclerView.setAdapter(adapter);
    }

    private boolean checkForPermissions(){
        return ContextCompat.checkSelfPermission(requireContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED
                &&
               ContextCompat.checkSelfPermission(requireContext(),
                permissions[1]) == PackageManager.PERMISSION_GRANTED;
    }

    private final ActivityResultLauncher<String[]> permissionLauncher=
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->  {
                            if(checkForPermissions()){
                                try {
                                    initAnim(add_button_view);
                                }catch (Exception e){e.printStackTrace();}
                            }
                            else {
                                Snackbar.make(binding.getRoot(), R.string.permission_rejected, Snackbar.LENGTH_SHORT)
                                .setAction(R.string.grant, v -> askPermission())
                                .show();
                            }
                    });

    private void askPermission() {
        permissionLauncher.launch(permissions);
    }

    private final ActivityResultLauncher<String> selectFileLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), results -> {
                initAnim(add_button_view);
                if (!results.isEmpty()) {
                    screenManager.disableTouchableLayout();
                    List<Uri> paths=new ArrayList<>();
                    for (int i = 0; i < results.size(); i++) {
                        final File file=new File(String.valueOf(results.get(i)));
                        try {
                            paths.add( FileProvider.getUriForFile(getContext(), AUTHORITY, file));
                        }catch(Exception e){
                            paths.add(results.get(i));
                        }
                    }
                    uploadSelectedFiles(paths);
                }
            });

    private final ActivityResultLauncher<Intent> intentLauncher=
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result!=null&&result.getResultCode()== Activity.RESULT_OK){
                    if(result.getData()!=null){
                        screenManager.disableTouchableLayout();
                        parseURIs(result);
                    }
                }
            });

    private void parseURIs(ActivityResult result) {
        List<Uri> list=new ArrayList<>();
        ClipData clipData=result.getData().getClipData();
        if(clipData!=null){
            for(int i=0;i<clipData.getItemCount();i++){
                Uri fileUri=clipData.getItemAt(i).getUri();
                File file=new File(fileUri.toString());
                try {
                    fileUri = FileProvider.getUriForFile(getContext(), AUTHORITY, file);
                }catch (Exception e){
                    e.printStackTrace();
                }
                list.add(fileUri);
            }
        }
        else{
            Uri fileUri=result.getData().getData();
            File file=new File(fileUri.toString());
            try {
                fileUri = FileProvider.getUriForFile(getContext(), AUTHORITY, file);
            }catch(Exception e){
                e.printStackTrace();
            }
            list.add(fileUri);
        }
        uploadSelectedFiles(list);
    }

    private final ItemBottomSheet.BottomSheetCallback bottomSheetCallback=
            new ItemBottomSheet.BottomSheetCallback() {
                @Override
                public void bind(FragmentBottomsheetItemBinding binding) {
                    binding.viewpagerHome.setAdapter(new FragmentStateAdapter(itemBottomSheet) {
                        @NonNull
                        @Override
                        public Fragment createFragment(int pos) {
                            final FileItem fileItem = adapter.getItem(pos);
                            View.OnClickListener listener= v1 -> {
                                itemBottomSheet.dismiss();
                                List<FileItem> fileItems=new ArrayList<>();
                                fileItems.add(fileItem);
                                onOptionSelected(v1.getId(), fileItems);
                            };
                            binding.deleteButton.setOnClickListener(listener);
                            binding.downloadButton.setOnClickListener(listener);
                            binding.renameButton.setOnClickListener(listener);
                            binding.shareButton.setOnClickListener(listener);
                            return new FileItemFragment(fileItem);
                        }

                        @Override
                        public int getItemCount() {
                            return adapter.getItemCount();
                        }
                    });
                    binding.viewpagerHome.setSaveEnabled(false);
                }

                @Override
                public void onClick(View v) {}
            };

    private final ListItemCallback<FileItem, ListFileitemBinding> itemCallback=
            new ListItemCallback<FileItem, ListFileitemBinding>() {
                @Override
                public void bind(ListFileitemBinding recBinding,FileItem item,int position) {
                    if (item.getFileType().contains("image")) {
                        Glide.with(recBinding.picture)
                                .load(item)
                                .into(recBinding.picture);
                    }
                    else FileItemUtils.loadPhoto(item,recBinding.picture);
                    recBinding.name.setText(item.getFileName());
                    if(item.getTimeStamp()!=null) {
                        recBinding.timestamp.setText(FileItemUtils.getTimestampString(item.getTimeStamp()));
                    }
                    recBinding.size.setText(FileItemUtils.byteToString(item.getFileSize()));
                    recBinding.getRoot().setOnClickListener(v->{
                        if(adapter.getSelectionTracker().getSelection().isEmpty()&&!itemBottomSheet.isInLayout()){
                            itemBottomSheet.showBottomSheet(getChildFragmentManager(),bottomSheetCallback);
                            itemBottomSheet.setItemPosition(position);
                        }
                        else if(itemBottomSheet.isInLayout()) itemBottomSheet.dismiss();
                    });
                }
                @Override
                public void onChanged(List<FileItem> items) {
                    if(!adapter.getSelectionTracker().getSelection().isEmpty()){
                        if(actionmode==null) {
                            actionmode=((AppCompatActivity) requireActivity())
                                .startSupportActionMode(new ActionMode.Callback() {
                                    @Override
                                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                                        if(!items.isEmpty())
                                        {
                                            try{
                                                mode.setTitle("1");
                                            }catch (Exception e){
                                                Log.e(TAG, "onCreateActionMode: ",e);
                                            }
                                            mode.getMenuInflater().inflate(R.menu.home_action_mode_menu,menu);
                                        return true;}
                                        else { onDestroyActionMode(mode);
                                        return false;}
                                    }
                                    @Override
                                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                                        if(actionmode!=null){
                                            mode.setTitle("1");
                                        return true;
                                        }
                                        return false;
                                    }
                                    @Override
                                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                                        if(item.getItemId()==R.id.select_all_button){
                                            if(adapter.selectActionForAll()){
                                                item.setTitle(R.string.unselect_all_item);
                                            }
                                            else{
                                                item.setTitle(R.string.selectall);
                                            }
                                        }
                                        else{
                                            onOptionSelected(item.getItemId(),items);
                                            onDestroyActionMode(mode);
                                        }
                                        return true;
                                    }
                                    @Override
                                    public void onDestroyActionMode(ActionMode mode) {
                                        mode.finish();
                                        actionmode=null;
                                        adapter.getSelectedItems().clear();
                                        adapter.getSelectionTracker().clearSelection();
                                    }
                                });
                        }
                        else
                        {   try{
                                String s=String.valueOf(adapter.getSelectionTracker().getSelection().size());
                                actionmode.setTitle(s);
                            }catch (Exception e){
                                Log.e(TAG, "onChanged: ",e);
                            }
                        }
                    }
                    else if(actionmode!=null)actionmode.finish();
                }
    };

    private void onOptionSelected(@IdRes int iD, List<FileItem> fileItems) {
        switch (iD) {
            case R.id.download_button:
                onClickDownloadButton(fileItems);
                break;
            case R.id.share_button:
                onClickShareButton(fileItems);
                break;
            case R.id.rename_button:
               onClickRenameButton(fileItems);
               break;
            case R.id.delete_button:
               onClickDeleteButton(fileItems);
               break;
        }
    }

    private void onClickDownloadButton(List<FileItem> fileItems) {
        if (!checkForPermissions()) {
            askPermission();
        } else {
            activityLifecycleObserver.startDownload(fileItems);
        }
    }

    private void onClickShareButton(List<FileItem> fileItems){
        screenManager.disableTouchableLayout();
        homeViewModel.fetchUsername()
                .addOnCompleteListener(task -> {
                    if(task.isComplete()) {
                        screenManager.enableTouchableLayout();
                    }
                })
                .addOnSuccessListener(s -> {
                    if(s!=null) {
                        List<FileItem> list=new ArrayList<>();
                        for (int i = 0; i < fileItems.size(); i++) {
                        if(fileItems.get(i).isEncrypted())
                            list.add(fileItems.get(i));
                        }
                        if (!list.isEmpty()){
                            for (int i = 0; i < list.size(); i++) {
                                fileItems.remove(list.get(i));
                            }
                            createFileShareDialog(list,fileItems).show();
                        }
                        else {
                            shareSelectedFiles(fileItems);
                        }
                    }
                    else {
                        Snackbar.make(binding.getRoot(), R.string.create_username, Snackbar.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Snackbar.make(binding.getRoot(),R.string.some_error_occurred,Snackbar.LENGTH_SHORT).show());
    }

    private void onClickRenameButton(List<FileItem> fileItems){
        TextInputDialog dialog=new TextInputDialog(R.string.rename_file,null,
                "Current FileName : "+fileItems.get(0).getFileName(), InputType.TYPE_CLASS_TEXT, requireContext());
        dialog.showDialog(getChildFragmentManager(),msg -> homeViewModel.renameFile(msg,fileItems.get(0)));
    }

    private void onClickDeleteButton(List<FileItem> fileItems){
        for (int i = 0; i < fileItems.size(); i++) {
            final FileItem fileItem=fileItems.get(i);
            homeViewModel.deleteFile(fileItem);
        }
    }

    private void uploadSelectedFiles(List<Uri> results){
        screenManager.enableTouchableLayout();
        List<FileItem> fileItems=new ArrayList<>();
        for(int i=0;i<results.size();i++){
            Uri uri=results.get(i);
            fileItems.add(FileItemUtils.getFileItemFromPath(requireContext(),uri));
        }
        activityLifecycleObserver.startUpload(fileItems);
    }

    private void shareSelectedFiles(List<FileItem> fileItems){
        TextInputDialog dialog= new TextInputDialog(R.string.enter_Receivers_email_or_phone_number,getString(R.string.at), null,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, requireContext());
        dialog.showDialog(getChildFragmentManager(), msg-> homeViewModel.shareFile(fileItems,msg)
                .addOnCompleteListener( task -> {
                    homeViewModel.setIsTaskRunning(!task.isComplete());
                    if(task.isSuccessful()) {
                        Snackbar.make(binding.getRoot(),R.string.itemshared,Snackbar.LENGTH_SHORT).show();
                    }
                    else
                        Snackbar.make(binding.getRoot(), R.string.some_error_occurred,Snackbar.LENGTH_SHORT).show();
                }));
    }

    private AlertDialog createFileShareDialog(List<FileItem> encryptedFiles,List<FileItem> fileItems){
        return new MaterialAlertDialogBuilder(requireContext(),R.style.Theme_Oxtor_AlertDialog)
                .setCancelable(false)
                .setTitle("Encrypted files can't be shared, yet")
                .setMessage(encryptedFiles.size() +" of the selected items are encrypted. So should we send the rest of the items")
                .setPositiveButton("Share rest of the items", (dialogInterface, i) -> shareSelectedFiles(fileItems))
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                .create();
    }

    private AlertDialog createPreferenceChooserDialog(){
        return new MaterialAlertDialogBuilder(requireContext(),R.style.Theme_Oxtor_AlertDialog)
                .setCancelable(true).setTitle(R.string.sort)
                .setSingleChoiceItems(array,sharedPreferences.getInt(SORT_PREFERENCE,1),
                (dialog, which) -> {
                    switch (which){
                        case 2:
                            query=homeViewModel.queryToSortBySize();
                            break;
                        case 1:
                            query=homeViewModel.queryToSortByTimestamp();
                            break;
                        case 0:
                            query=homeViewModel.queryToSortByName();
                            break;
                    }
                    adapter.changeAdapterQuery(query,true);
                    sharedPreferences.edit().putInt(SORT_PREFERENCE,which).apply();
                    dialog.dismiss();
                }).create();
    }

    private void observeLoadingState(){
        homeViewModel.getIsTaskRunning().observe(getViewLifecycleOwner(), MainActivity.observer);
    }

    @Override
    public void onRefresh() {
        (new Handler()).postDelayed(() -> {
            if(swipeRefreshLayout.isRefreshing())
                swipeRefreshLayout.setRefreshing(false);
            adapter.changeAdapterQuery(query,true);
        },500);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.file:
                selectFileLauncher.launch("*/*");
            break;
            case R.id.camera:
                try {
                    intentLauncher.launch(Intents.getMediaChooserIntent(getContext()));
                }catch(Exception e){
                    e.printStackTrace();
                    Snackbar.make(binding.getRoot(),"Some error occurred",Snackbar.LENGTH_SHORT).show();
                }
                break;
        }
        AnimationHelper.rotateFab(addButton.getRootView(),isRotated);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(actionmode!=null)
            actionmode.finish();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.home_menu,menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.group) {
            createPreferenceChooserDialog().show();
            return true;
        }
        else
            return false;
    }

}
