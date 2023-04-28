package teamarmada.oxtor.Fragments.ParentFragments;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static teamarmada.oxtor.Main.MainActivity.ENCRYPTION_PASSWORD;
import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Main.MainActivity.USED_SPACE;
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;
import teamarmada.oxtor.Main.MainActivity;
import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.ProfileItem;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Ui.DialogFragment.TextInputDialog;
import teamarmada.oxtor.Utils.FileItemUtils;
import teamarmada.oxtor.ViewModels.ProfileViewModel;
import teamarmada.oxtor.databinding.FragmentProfileBinding;

@AndroidEntryPoint
public class ProfileFragment extends Fragment implements View.OnClickListener, MenuProvider {

    public static final String TAG="ProfileFragment";
    private FragmentProfileBinding binding;
    private SharedPreferences sharedPreferences;
    private ProfileViewModel profileViewModel;
    private NavController navController;
    private final String[] permissions=new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    public ProfileFragment(){}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().addMenuProvider(this,getViewLifecycleOwner());
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        sharedPreferences=requireContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        profileViewModel=new ViewModelProvider(this).get(ProfileViewModel.class);
        observeLoadingState();
        initUI();
        try {
            NavHostFragment navHostMain = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_main);
            if(navHostMain !=null);
            navController = navHostMain.getNavController();
        }catch (Exception e){
            e.printStackTrace();
        }
        binding.editpassword.setOnClickListener(this);
        binding.editname.setOnClickListener(this);
        binding.editusername.setOnClickListener(this);
        binding.editimage.setOnClickListener(this);
        binding.refreshButton.setOnClickListener(this);
        return binding.getRoot();
    }

    private void initUI() {
        Long usedSpace=sharedPreferences.getLong(USED_SPACE,0L);
        String a = "Used Space : " + FileItemUtils.byteToString(usedSpace);
        binding.usedSpace.setText(a);
        long b = (FileItemUtils.ONE_GIGABYTE - usedSpace);
        String c = "Available Space : " + FileItemUtils.byteToString(b);
        binding.availableSpace.setText(c);
        String d = "Total Space : " + FileItemUtils.byteToString(FileItemUtils.ONE_GIGABYTE);
        binding.totalSpace.setText(d);
        double e = ((usedSpace) * 100) / FileItemUtils.ONE_GIGABYTE;
        binding.spaceIndicator.setProgress((int) e);

        boolean bl=sharedPreferences.getBoolean(TO_ENCRYPT,false);
        binding.encryptionSwitch.setChecked(bl);
        getPassword(bl);
        binding.encryptionSwitch.setOnCheckedChangeListener((compoundButton, b1) -> {
            getPassword(b1);
            profileViewModel.updateEncryptionSetting(b1);
        });

        profileViewModel.getProfileItem().observe(getViewLifecycleOwner(), profileItem -> {
                    if(profileItem!=null)
                        try {

                            Glide.with(this).load(profileItem.getPhotoUrl()).into(binding.dpofuser);
                            if(profileItem.getUsername()!=null)
                                binding.username.setText(profileItem.getUsername());
                            else
                                profileViewModel.checkUsername();
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
        String currentName=profileViewModel.getProfileItem().getValue().getDisplayName();
        TextInputDialog textInputDialog =new TextInputDialog(R.string.update_name,null,"Current name : "
                +currentName, InputType.TYPE_CLASS_TEXT,requireContext());
        textInputDialog.showDialog(getChildFragmentManager(),profileViewModel::updateDisplayName);
    }

    private void updateUsername(){
        ProfileItem profileItem=profileViewModel.getProfileItem().getValue();
        String currentUsername=profileItem.getUsername();
        TextInputDialog textInputDialog =new TextInputDialog(R.string.update_username,getString(R.string.at),
                "Current Username : " +currentUsername, InputType.TYPE_CLASS_TEXT, requireContext());
        textInputDialog.showDialog(getChildFragmentManager(), msg -> {
            if(textInputDialog.isUsernameValid(msg)){
                profileViewModel.updateUsername(msg).addOnSuccessListener(task -> {
                        profileItem.setUsername(msg);
                        profileViewModel.getProfileItem().postValue(profileItem);
                        binding.username.setText(msg);
                        Toast.makeText(getContext(),R.string.username_updated,Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e-> Toast.makeText(getContext(),e.toString(),Toast.LENGTH_SHORT).show());
            }
            else
                Toast.makeText(getContext(),R.string.dont_use_any_special_character,Toast.LENGTH_SHORT).show();
        });
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

    private void getPassword(boolean b){
        if(b){
            binding.password.setVisibility(VISIBLE);
            binding.editpassword.setVisibility(VISIBLE);
            binding.password.setText(MainActivity.getEncryptionPassword());
        }
        else{
            binding.password.setVisibility(INVISIBLE);
            binding.editpassword.setVisibility(INVISIBLE);
        }
    }

    private void generateNewPassword() {
        String s=UUID.randomUUID().toString();
        binding.password.setText(s);
        sharedPreferences.edit().putString(ENCRYPTION_PASSWORD,s).apply();
    }

    private void deleteAccount() {
        profileViewModel.deleteAccount().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Toast.makeText(getContext(), "Come back again to signIn, bye... :-)", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getContext(),R.string.some_error_occurred,Toast.LENGTH_SHORT).show();
            }
            getActivity().finish();
        });
    }

    public void signOut(){
        profileViewModel.abortAllTasks();
        profileViewModel.signOut().addOnCompleteListener(task->{
            if(task.isSuccessful()){
                Toast.makeText(getContext(), "Come back again to signIn, bye... :-)", Toast.LENGTH_SHORT).show();
                navController.navigate(R.id.action_navigation_profile_to_navigation_login);
            }
        });
    }

    private void openWebPage() {
        try{
        Uri url = Uri.parse("https://oxt.web.app");
        Intent i= new Intent(Intent.ACTION_VIEW,url);
        startActivity(i);
        }catch (Exception e){
            Toast.makeText(getContext(),"Some error occurred",Toast.LENGTH_SHORT).show();
        }
    }

    private void observeLoadingState() {
        profileViewModel.getIsTaskRunning().observe(getViewLifecycleOwner(), MainActivity.observer);
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
                    profileViewModel.updateDisplayPicture(file, bytes);
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
            case R.id.editusername:
                updateUsername();
                break;
            case R.id.editimage:
                updatePicture();
                break;
            case R.id.editpassword:
                generateNewPassword();
                break;
            case R.id.refresh_button:
                profileViewModel.fetchUsedSpace();
                profileViewModel.checkUsername();
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
                openWebPage();
                return true;
        }
        return false;
    }

}