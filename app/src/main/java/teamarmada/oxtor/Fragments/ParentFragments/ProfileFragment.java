package teamarmada.oxtor.Fragments.ParentFragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static teamarmada.oxtor.Main.MainActivity.ENCRYPTION_PASSWORD;
import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Model.ProfileItem.TO_ENCRYPT;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
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
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;
import teamarmada.oxtor.BuildConfig;
import teamarmada.oxtor.Main.MainActivity;
import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Ui.DialogFragment.TextInputDialog;
import teamarmada.oxtor.Utils.FileItemUtils;
import teamarmada.oxtor.ViewModels.MainViewModel;
import teamarmada.oxtor.databinding.FragmentProfileBinding;

@AndroidEntryPoint
public class ProfileFragment extends Fragment implements View.OnClickListener, MenuProvider {

    public static final String TAG="ProfileFragment";
    private FragmentProfileBinding binding;
    private SharedPreferences sharedPreferences;
    private MainViewModel mainViewModel;
    private NavController navController;
    private final String[] permissions=new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private AlertDialog dialog;
    public ProfileFragment(){}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().addMenuProvider(this,getViewLifecycleOwner());
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        sharedPreferences=requireContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        mainViewModel=new ViewModelProvider(this).get(MainViewModel.class);
        observeLoadingState();
        initUI();
        try {
            NavHostFragment navHostMain = (NavHostFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_main);
            assert navHostMain != null;
            navController = navHostMain.getNavController();
        }catch (Exception e){
            e.printStackTrace();
        }
        dialog=new MaterialAlertDialogBuilder(requireContext(),R.style.Theme_Oxtor_AlertDialog)
                .setTitle("Oxtor App")
                .setMessage("Version "+ BuildConfig.VERSION_NAME)
                .setCancelable(true)
                .setPositiveButton("View Privacy policy", (dialogInterface, pos) -> {
                    try{
                        Uri url = Uri.parse("https://oxt.web.app");
                        Intent i= new Intent(Intent.ACTION_VIEW,url);
                        startActivity(i);
                    }catch (Exception e){
                        Snackbar.make(binding.getRoot(),"Some error occurred",Snackbar.LENGTH_SHORT).show();
                    }
                }).create();

        binding.editname.setOnClickListener(this);
        binding.editimage.setOnClickListener(this);
        binding.refreshButton.setOnClickListener(this);
        return binding.getRoot();
    }

    private void initUI() {
        mainViewModel.checkUsedSpace();
        mainViewModel.getUsedSpace().observe(getViewLifecycleOwner(),usedSpace->{
            String a = "Used Space : " + FileItemUtils.formatByteSize(usedSpace);
            binding.usedSpace.setText(a);
            long b = ((5*FileItemUtils.ONE_GIGABYTE) - usedSpace);
            String c = "Available Space : " + FileItemUtils.formatByteSize(b);
            binding.availableSpace.setText(c);
            String d = "Total Space : " + FileItemUtils.formatByteSize(5*FileItemUtils.ONE_GIGABYTE);
            binding.totalSpace.setText(d);
            double e = ((usedSpace) * 100) / (5*FileItemUtils.ONE_GIGABYTE);
            binding.spaceIndicator.setProgress((int) e);
        });


        mainViewModel.getProfileItem().observe(getViewLifecycleOwner(), profileItem -> {
                    if(profileItem!=null)
                        try {
                            Glide.with(this).load(profileItem.getPhotoUrl()).into(binding.dpofuser);

                            if (profileItem.getDisplayName() == null)
                                binding.nameofuser.setHint("Name");
                            else
                                binding.nameofuser.setText(profileItem.getDisplayName());
                            if(profileItem.getEmail()==null)
                                binding.emailofuser.setText(profileItem.getPhoneNumber());
                            else
                                binding.emailofuser.setText(profileItem.getEmail());
                        }
                        catch (Exception ex){
                            ex.printStackTrace();
                        }
                });
    }

    private void updateName() {
        String currentName=mainViewModel.getProfileItem().getValue().getDisplayName();
        TextInputDialog textInputDialog =new TextInputDialog(R.string.update_name,null,"Current name : "
                +currentName, InputType.TYPE_CLASS_TEXT,requireContext());
        textInputDialog.addCallback(mainViewModel::updateDisplayName);
        textInputDialog.show(getChildFragmentManager(),"Input");
    }

    private void updatePicture(){
        if(checkForPermissions()) {
            intentLauncher.launch("image/*");
        }
        else {
            askPermission();
        }
    }

    private void askPermission() {
        permissionLauncher.launch(permissions);
    }


    private void deleteAccount() {
        mainViewModel.deleteAccount().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Snackbar.make(binding.getRoot(), "Come back again to signIn, bye... :-)", Snackbar.LENGTH_SHORT).show();
            }
            else {
                Snackbar.make(binding.getRoot(),R.string.some_error_occurred,Snackbar.LENGTH_SHORT).show();
            }
            getActivity().finish();
        });
    }

    public void signOut(){
        mainViewModel.abortAllTasks();
        mainViewModel.signOut().addOnCompleteListener(task->{
            if(task.isSuccessful()){
                Snackbar.make(binding.getRoot(), "Come back again to signIn, bye... :-)", Snackbar.LENGTH_SHORT).show();
                try {
                    navController.navigate(R.id.action_navigation_profile_to_navigation_login);
                }catch (Exception e){
                    e.printStackTrace();
                    getActivity().finish();
                }
            }
        });
    }

    private void observeLoadingState() {
        mainViewModel.getIsTaskRunning().observe(getViewLifecycleOwner(), MainActivity.observer);
    }


    public boolean checkForPermissions(){
        return  ActivityCompat.checkSelfPermission(requireContext(), permissions[0]) == PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(requireContext(), permissions[1]) == PackageManager.PERMISSION_GRANTED;
    }

    private final ActivityResultLauncher<String> intentLauncher=registerForActivityResult(
            new ActivityResultContracts.GetContent(), path-> {
                try {
                    FileItem file=  FileItemUtils.getFileItemFromPath(requireContext(),path);
                    InputStream inputStream=getContext().getContentResolver().openInputStream(path);
                    ByteArrayOutputStream outputStream=new ByteArrayOutputStream(file.getFileSize().intValue());
                    byte[] bytes=new byte[file.getFileSize().intValue()];
                    int read;
                    while ((read=inputStream.read(bytes))!=-1){
                        outputStream.write(bytes,0,read);
                    }
                    bytes=outputStream.toByteArray();
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                    mainViewModel.updateDisplayPicture(file, bytes);
                } catch (Exception e){
                    Log.e(TAG, "onActivityResult: ", e);
                }
            });

    private final ActivityResultLauncher<String[]> permissionLauncher=registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if(checkForPermissions())
                    intentLauncher.launch("image/*");
                else {
                    Snackbar.make(binding.getRoot(), R.string.permission_rejected, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.grant, v -> askPermission())
                            .show();
                }
            });

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.editname:
                updateName();
                break;
            case R.id.editimage:
                updatePicture();
                break;
            case R.id.refresh_button:
                mainViewModel.checkUsedSpace();
                break;
        }
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.profile_menu,menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.signout:
                signOut();
                return true;
            case R.id.delete_account:
                deleteAccount();
                return true;
            case R.id.webpage:
                dialog.show();
                return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding=null;
    }

}