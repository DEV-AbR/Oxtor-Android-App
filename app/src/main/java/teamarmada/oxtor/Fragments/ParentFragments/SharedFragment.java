package teamarmada.oxtor.Fragments.ParentFragments;

import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Main.MainActivity.SORT_PREFERENCE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import teamarmada.oxtor.Fragments.ChildFragments.FileItemFragment;
import teamarmada.oxtor.Interfaces.ListItemCallback;
import teamarmada.oxtor.Main.ActivityLifecycleObserver;
import teamarmada.oxtor.Main.MainActivity;
import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.SharedItem;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Ui.RecyclerViewAdapter.RecyclerViewAdapter;
import teamarmada.oxtor.Ui.DialogFragment.ItemBottomSheet;
import teamarmada.oxtor.Utils.FileItemUtils;
import teamarmada.oxtor.ViewModels.ShareViewModel;
import teamarmada.oxtor.databinding.FragmentBottomsheetItemBinding;
import teamarmada.oxtor.databinding.FragmentSharedBinding;
import teamarmada.oxtor.databinding.ListFileitemBinding;

@AndroidEntryPoint
public class SharedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, MenuProvider {

    public static final String TAG=SharedFragment.class.getSimpleName();
    private FragmentSharedBinding binding;
    private ShareViewModel shareViewModel;
    private Query query=null;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter<SharedItem, ListFileitemBinding> adapter;
    private ItemBottomSheet itemBottomSheet;
    private SharedPreferences sharedPreferences;
    private ActionMode actionmode=null;
    private SwipeRefreshLayout swipeRefreshLayout;
    private final String[] array={"Sort by size","Sort by Time","Sort by Name"};
    private final String[] permissions=new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private ActivityLifecycleObserver activityLifecycleObserver;

    public SharedFragment(){}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MenuHost menuHost=requireActivity();
        menuHost.addMenuProvider(this,getViewLifecycleOwner());
        binding = FragmentSharedBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        sharedPreferences=requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        recyclerView= binding.recyclerviewShared;
        swipeRefreshLayout= binding.swipeShared;
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> actionmode!=null);
        shareViewModel=new ViewModelProvider(this).get(ShareViewModel.class);
        activityLifecycleObserver = ActivityLifecycleObserver.getInstance((AppCompatActivity) requireActivity());
        observeLoadingState();
        initUI();
        return binding.getRoot();
    }

    private void initUI(){
        switch (sharedPreferences.getInt(SORT_PREFERENCE,1)){
            default:
            case 1:
                query=shareViewModel.queryToSortSharedItemByTimestamp();
                break;
            case 2:
                query= shareViewModel.queryToSortSharedItemBySize();
                break;
            case 0:
                query= shareViewModel.queryToSortSharedItemByName();
                break;
        }
        adapter=new RecyclerViewAdapter<>(
                getLifecycle(),
                R.layout.list_fileitem,
                query,
                true,
                SharedItem.class,
                itemCallback);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                RecyclerView.VERTICAL, false));
        itemBottomSheet =new ItemBottomSheet(R.layout.fragment_bottomsheet_item);
        recyclerView.setAdapter(adapter);
    }

    private final ItemBottomSheet.BottomSheetCallback bottomSheetCallback=
            new ItemBottomSheet.BottomSheetCallback() {
                @Override
                public void bind(FragmentBottomsheetItemBinding binding) {
                    binding.viewpagerHome.setAdapter(new FragmentStateAdapter(itemBottomSheet) {
                        @NonNull
                        @Override
                        public Fragment createFragment(int pos) {
                            final SharedItem sharedItem= adapter.getItem(pos);
                            View.OnClickListener listener= v1 -> {
                                itemBottomSheet.dismiss();
                                List<SharedItem> sharedItems=new ArrayList<>();
                                sharedItems.add(sharedItem);
                                onOptionSelected(v1.getId(),sharedItems);
                            };
                            final FileItem fileItem=sharedItem.getFileItem();
                            binding.deleteButton.setOnClickListener(listener);
                            binding.downloadButton.setOnClickListener(listener);
                            binding.renameButton.setVisibility(View.GONE);
                            binding.shareButton.setVisibility(View.GONE);
                            if(fileItem!=null)
                                return new FileItemFragment(fileItem);
                            else
                                throw new NullPointerException("Shared item has not been converted to fileItem");
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


    private final ListItemCallback<SharedItem,ListFileitemBinding> itemCallback=
            new ListItemCallback<SharedItem, ListFileitemBinding>() {
                @SuppressLint("SuspiciousIndentation")
                @Override
                public void bind(ListFileitemBinding recBinding, SharedItem item, int position) {
                    final FileItem fileItem=item.getFileItem();
                    if(fileItem==null) return;

                    if (fileItem.getFileType().contains("image")) {
                        Glide.with(recBinding.picture).load(fileItem).into(recBinding.picture);
                    }
                    else{
                        FileItemUtils.loadPhoto(fileItem,recBinding.picture);
                    }
                    recBinding.size.setText(fileItem.getFileName());
                    try {
                        if (item.getEmailOfReceiver().equals(shareViewModel.getProfileItem().getValue().getEmail()))
                            recBinding.name.setText(item.getUsernameOfSender());
                        else if (item.getPhoneNumberOfReceiver().equals(shareViewModel.getProfileItem().getValue().getPhoneNumber()))
                            recBinding.name.setText(item.getUsernameOfSender());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    try {
                        if (item.getEmailOfSender().equals(shareViewModel.getProfileItem().getValue().getEmail()))
                            recBinding.name.setText(item.getUsernameOfReceiver());
                        else if (item.getPhoneNumberOfSender().equals(shareViewModel.getProfileItem().getValue().getPhoneNumber()))
                            recBinding.name.setText(item.getUsernameOfReceiver());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    if(item.getTimeStamp()!=null){
                        String time= FileItemUtils.getTimestampString(item.getTimeStamp());
                        recBinding.timestamp.setText(time);
                    }
                    recBinding.getRoot().setOnClickListener(v->{
                        if(adapter.getSelectionTracker().getSelection().isEmpty()&&!itemBottomSheet.isInLayout()) {
                            itemBottomSheet.setItemPosition(position);
                            itemBottomSheet.addCallback(bottomSheetCallback);
                            itemBottomSheet.show(getChildFragmentManager(),"Preview");
                        }
                        else if(itemBottomSheet.isInLayout()){
                            itemBottomSheet.dismiss();
                        }
                    });
                }
                @Override
                public void onChanged(List<SharedItem> items) {
                    if (!adapter.getSelectionTracker().getSelection().isEmpty()) {
                        if (actionmode == null) {
                            actionmode = ((AppCompatActivity) requireActivity())
                                    .startSupportActionMode(new ActionMode.Callback() {
                                        @Override
                                        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                                            if (!items.isEmpty()) {
                                                try {
                                                    mode.setTitle("1");
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                                mode.getMenuInflater().inflate(R.menu.shared_action_mode_menu, menu);
                                                return true;
                                            } else {
                                                onDestroyActionMode(mode);
                                                return false;
                                            }
                                        }

                                        @Override
                                        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                                            return true;
                                        }

                                        @Override
                                        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                                            if (item.getItemId() == R.id.select_all_button) {
                                                if (adapter.selectActionForAll()) {
                                                    item.setTitle(R.string.unselect_all_item);
                                                } else {
                                                    item.setTitle(R.string.selectall);
                                                }
                                            } else {
                                                onOptionSelected(item.getItemId(), items);
                                                onDestroyActionMode(mode);
                                            }
                                            return true;
                                        }

                                        @Override
                                        public void onDestroyActionMode(ActionMode mode) {
                                            mode.finish();
                                            actionmode = null;
                                            adapter.getSelectedItems().clear();
                                            adapter.getSelectionTracker().clearSelection();
                                        }
                                    });
                        } else {
                            try {
                                String s = String.valueOf(adapter.getSelectionTracker().getSelection().size());
                                actionmode.setTitle(s);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (actionmode != null) {
                        actionmode.finish();
                    }
                }
            };

    private final ActivityResultLauncher<String[]> permissionLauncher= registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result ->  {
                if(!checkForPermissions()) {
                    Snackbar.make(binding.getRoot(), R.string.permission_rejected, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.grant, v -> askPermission())
                            .show();
                }
            });

    private void askPermission() {
        permissionLauncher.launch(permissions);
    }

    private boolean checkForPermissions(){
        return ContextCompat.checkSelfPermission(requireContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(requireContext(),
                        permissions[1]) == PackageManager.PERMISSION_GRANTED;
    }

    private void onOptionSelected(int itemId, List<SharedItem> list) {
        switch (itemId){
            case R.id.download_button:
                if (!checkForPermissions())
                    askPermission();
                else {
                    downloadSelectedFiles(list);
                }

                break;
            case R.id.delete_button:
                deleteSelectedFiles(list);
                break;
        }
    }

    private void downloadSelectedFiles(List<SharedItem> list) {
        final List<FileItem> fileItems=new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            fileItems.add(list.get(i).getFileItem());
        }
        activityLifecycleObserver.startDownload(fileItems);
    }
    
    private void deleteSelectedFiles(List<SharedItem> list) {
        for (SharedItem sharedItem : list) {
            shareViewModel.deleteSharedPosts(sharedItem);
        }
    }

    private AlertDialog createPreferenceChooserDialog(){
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(requireContext(),R.style.Theme_Oxtor_AlertDialog);
        builder.setCancelable(true).setTitle(R.string.sort);
        builder.setSingleChoiceItems(array,sharedPreferences.getInt(SORT_PREFERENCE,1),
                (dialog, which) -> {
                    switch (which){
                        case 2:
                            query=shareViewModel.queryToSortSharedItemBySize();
                            break;
                        case 1:
                            query=shareViewModel.queryToSortSharedItemByTimestamp();
                            break;
                        case 0:
                            query=shareViewModel.queryToSortSharedItemByName();
                            break;
                    }
                    adapter.changeAdapterQuery(query,true);
                    sharedPreferences.edit().putInt(SORT_PREFERENCE,which).apply();
                    dialog.dismiss();
                });
        return builder.create();
    }

    private void observeLoadingState(){
        shareViewModel.getIsTaskRunning().observe(getViewLifecycleOwner(), MainActivity.observer);
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
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.shared_menu,menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.group) {
            createPreferenceChooserDialog().show();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(actionmode!=null)
            actionmode.finish();
    }

}
