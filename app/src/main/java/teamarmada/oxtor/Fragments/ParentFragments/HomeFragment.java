package teamarmada.oxtor.Fragments.ParentFragments;


import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Main.MainActivity.SORT_PREFERENCE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import teamarmada.oxtor.Fragments.ChildFragments.FileItemFragment;
import teamarmada.oxtor.Ui.RecyclerViewAdapter.RecyclerViewAdapter.ListItemCallback;
import teamarmada.oxtor.Main.ScreenManager;
import teamarmada.oxtor.Main.ActivityLifecycleObserver;
import teamarmada.oxtor.Main.MainActivity;
import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Ui.BottomSheet.ItemBottomSheet;
import teamarmada.oxtor.Ui.DialogFragment.TextInputDialog;
import teamarmada.oxtor.Ui.RecyclerViewAdapter.RecyclerViewAdapter;
import teamarmada.oxtor.Utils.FileItemUtils;
import teamarmada.oxtor.ViewModels.MainViewModel;
import teamarmada.oxtor.databinding.BottomsheetFileitemBinding;
import teamarmada.oxtor.databinding.FragmentHomeBinding;

@AndroidEntryPoint
public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, MenuProvider {

    public static final String TAG=HomeFragment.class.getSimpleName();
    private static final String[] sort_prefs={"Sort by Name", "Sort by Time", "Sort by Size"};
    private final int[] tabList= new int[]{
            R.string.images,
            R.string.videos,
            R.string.audio,
            R.string.documents,
            R.string.deleted};
    private RecyclerView recyclerView;
    private FragmentHomeBinding binding;
    private MainViewModel mainViewModel;
    private Query query=null;
    private ActionMode actionmode=null;
    private SwipeRefreshLayout swipeRefreshLayout=null;
    private RecyclerViewAdapter adapter;
    private ItemBottomSheet itemBottomSheet;
    private SharedPreferences sharedPreferences;
    private ActivityLifecycleObserver activityLifecycleObserver;
    private ScreenManager screenManager;

    public HomeFragment(){}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().addMenuProvider(this,getViewLifecycleOwner());
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        sharedPreferences=requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            screenManager=(ScreenManager) requireActivity();
        }catch (Exception e){
            e.printStackTrace();
        }
        mainViewModel=new ViewModelProvider(this).get(MainViewModel.class);
        activityLifecycleObserver = ActivityLifecycleObserver.getInstance((AppCompatActivity) requireActivity());
        observeLoadingState();
        initUI();

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> actionmode!=null);
        return binding.getRoot();
    }

    private void initUI(){
        int which=sharedPreferences.getInt(SORT_PREFERENCE,1);
        Task<Query> queryTask=mainViewModel.queryToSortByTimestamp();
        switch (which){
            case 2:
                queryTask=mainViewModel.queryToSortBySize();
                break;
            case 1:
                queryTask=mainViewModel.queryToSortByTimestamp();
                break;
            case 0:
                queryTask=mainViewModel.queryToSortByName();
                break;
        }
//        queryTask.addOnSuccessListener(query1->query=query1)
//                 .addOnFailureListener(e->Snackbar.make(binding.getRoot(),e.toString(),Snackbar.LENGTH_SHORT).show());
//        adapter=new RecyclerViewAdapter(getLifecycle(), query, true, itemCallback);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                RecyclerView.VERTICAL, false));
        itemBottomSheet =new ItemBottomSheet(R.layout.bottomsheet_fileitem);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(MainActivity.listener);

    }


    private final ItemBottomSheet.BottomSheetCallback bottomSheetCallback=
            new ItemBottomSheet.BottomSheetCallback() {
                @Override
                public void bind(BottomsheetFileitemBinding binding) {
                    binding.viewpagerHome.setAdapter(new FragmentStateAdapter(itemBottomSheet) {
                        @NonNull
                        @Override
                        public Fragment createFragment(int pos) {
                            final FileItem fileItem = adapter.getItem(pos);
                            View.OnClickListener listener= v1 -> {
                                itemBottomSheet.dismiss();
                                if(fileItem.getDownloadUrl()!=null){
                                List<FileItem> fileItems=new ArrayList<>();
                                fileItems.add(fileItem);
                                try {
                                    onOptionSelected(v1.getId(), fileItems);
                                }catch(Exception e){
                                    e.printStackTrace();
                                }
                                }else{
                                    Toast.makeText(getContext(),"File does not exist anymore",Toast.LENGTH_SHORT).show();
                                }
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

//    private final ListItemCallback itemCallback=
//            new ListItemCallback() {
//                @Override
//                public void bind(ListitemFileBinding recBinding, FileItem item, int position) {
//
//                    recBinding.name.setText(item.getFileName());
//                    recBinding.size.setText(FileItemUtils.formatByteSize(item.getFileSize()));
//                    if(item.getDownloadUrl()==null){
//                            recBinding.picture.setImageResource(R.drawable.ic_baseline_file_present_24);
//                            recBinding.timestamp.setText("[File deleted]");
//                            return;
//                    }
//                    if(item.getTimeStamp()!=null) {
//                        recBinding.timestamp.setText(FileItemUtils.getFormattedTimestamp(item.getTimeStamp()));
//                    }
//                    if (item.getFileType().contains("image")) {
//                        Glide.with(recBinding.picture).load(item).into(recBinding.picture);
//                    }
//                    else {
//                        FileItemUtils.setFileItemIcon(item,recBinding.picture);
//                    }
//                    recBinding.getRoot().setOnClickListener(v->{
//                        if(adapter.getSelectionTracker().getSelection().isEmpty()&&!itemBottomSheet.isInLayout()){
//                            itemBottomSheet.addCallback(bottomSheetCallback);
//                            itemBottomSheet.showNow(getChildFragmentManager(),"Preview");
//                            itemBottomSheet.setItemPosition(position);
//                        }
//                        else if(itemBottomSheet.isInLayout()) {
//                            itemBottomSheet.dismiss();
//                        }
//                    });
//                }
//                @Override
//                public void onChanged(List<FileItem> items) {
//                    if(!adapter.getSelectionTracker().getSelection().isEmpty()){
//                        if(actionmode==null) {
//                            actionmode=((AppCompatActivity) requireActivity())
//                                .startSupportActionMode(new ActionMode.Callback() {
//                                    @Override
//                                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
//                                        if(!items.isEmpty()) {
//                                            try{
//                                                mode.setTitle("1");
//                                            }catch (Exception e){
//                                                e.printStackTrace();
//                                            }
//                                            mode.getMenuInflater().inflate(R.menu.home_action_mode_menu,menu);
//                                            return true;
//                                        }
//                                        else {
//                                            onDestroyActionMode(mode);
//                                            return false;
//                                        }
//                                    }
//                                    @Override
//                                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//                                        if(actionmode!=null){
//                                            mode.setTitle("1");
//                                            return true;
//                                        }
//                                        return false;
//                                    }
//                                    @Override
//                                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
//                                        if(item.getItemId()==R.id.select_all_button){
//                                            if(adapter.selectActionForAll()){
//                                                item.setTitle(R.string.unselect_all_item);
//                                            }
//                                            else{
//                                                item.setTitle(R.string.selectall);
//                                            }
//                                        }
//                                        else{
//                                            onOptionSelected(item.getItemId(),items);
//                                            onDestroyActionMode(mode);
//                                        }
//                                        return true;
//                                    }
//                                    @Override
//                                    public void onDestroyActionMode(ActionMode mode) {
//                                        mode.finish();
//                                        actionmode=null;
//                                        adapter.getSelectedItems().clear();
//                                        adapter.getSelectionTracker().clearSelection();
//                                    }
//                                });
//                        }
//                        else
//                        {   try{
//                                String s=String.valueOf(adapter.getSelectionTracker().getSelection().size());
//                                actionmode.setTitle(s);
//                            }catch (Exception e){
//                                Log.e(TAG, "onChanged: ",e);
//                            }
//                        }
//                    }
//                    else if(actionmode!=null)actionmode.finish();
//                }
//    };

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
        activityLifecycleObserver.startDownload(fileItems);
    }

    private void onClickShareButton(List<FileItem> fileItems){
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, fileItems.get(0).getDownloadUrl());
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

    private void onClickRenameButton(List<FileItem> fileItems){
        TextInputDialog dialog=new TextInputDialog(R.string.rename_file,null,
                "Current FileName : "+fileItems.get(0).getFileName(), InputType.TYPE_CLASS_TEXT, requireContext());
        dialog.addCallback(msg -> mainViewModel.renameFile(msg,fileItems.get(0)));
        dialog.show(getChildFragmentManager(),"Input");
    }

    private void onClickDeleteButton(List<FileItem> fileItems){
        screenManager.showProgressDialog();
        mainViewModel.deleteFiles(fileItems)
                .addOnCompleteListener(task-> screenManager.hideProgressDialog())
                .addOnSuccessListener(result->adapter.restartListening())
                .addOnFailureListener(e->Toast.makeText(getContext(),e.toString(),Toast.LENGTH_SHORT).show());
    }



    private AlertDialog createPreferenceChooserDialog(){
        return new MaterialAlertDialogBuilder(requireContext(),R.style.Theme_Oxtor_AlertDialog)
                .setCancelable(true).setTitle(R.string.sort)
                .setSingleChoiceItems(sort_prefs,sharedPreferences.getInt(SORT_PREFERENCE,1),
                (dialog, which) -> {
                    Task<Query> queryTask=mainViewModel.queryToSortByTimestamp();
                    switch (which){
                        case 2:
                            queryTask=mainViewModel.queryToSortBySize();
                            break;
                        case 1:
                            queryTask=mainViewModel.queryToSortByTimestamp();
                            break;
                        case 0:
                            queryTask=mainViewModel.queryToSortByName();
                            break;
                    }
                    queryTask.addOnSuccessListener(query1->query=query1)
                             .addOnFailureListener(e->Snackbar.make(binding.getRoot(),e.toString(),Snackbar.LENGTH_SHORT).show());
                    adapter.changeAdapterQuery(query,true);
                    sharedPreferences.edit().putInt(SORT_PREFERENCE,which).apply();
                }).create();
    }

    private void observeLoadingState(){
        mainViewModel.getIsTaskRunning().observe(getViewLifecycleOwner(), MainActivity.observer);
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
    public void onDestroyView() {
        if(actionmode!=null)
            actionmode.finish();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding=null;
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
