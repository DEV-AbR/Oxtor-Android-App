package teamarmada.oxtor.Fragments.ParentFragments;

import static android.app.Activity.RESULT_OK;
import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Model.ProfileItem.EMAIL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.os.BuildCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;

import teamarmada.oxtor.BuildConfig;
import teamarmada.oxtor.Interfaces.ScreenManager;
import teamarmada.oxtor.Main.MainActivity;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Ui.DialogFragment.TextInputDialog;
import teamarmada.oxtor.ViewModels.LoginViewModel;
import teamarmada.oxtor.databinding.FragmentLoginBinding;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    public LoginFragment() {}

    public final static String TAG = LoginFragment.class.getSimpleName();
    private FragmentLoginBinding binding;
    private LoginViewModel loginViewModel;
    private TextInputDialog enterCode;
    private String phoneNumber=null;
    private String email =null;
    private String id=null;
    private GoogleSignInClient gsi;
    private PhoneAuthProvider.ForceResendingToken resendToken=null;
    private NavController navController=null;
    private SharedPreferences sharedPreferences=null;
    private ScreenManager fullscreenManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        try {
            fullscreenManager=(ScreenManager) requireActivity();
            fullscreenManager.enableFullscreen();
        }catch (RuntimeException e){
            Log.e(TAG, "onCreateView: ",e);
        }
        try {
            sharedPreferences = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            NavHostFragment navHostMain = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_main);
            assert navHostMain !=null;
            navController = navHostMain.getNavController();
        }catch(Exception e){
            Log.e(TAG, "onCreateView: ",e);
        }
        binding.appLogo.setOnClickListener(v->{
            Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse("https://oxt.web.app"));
            startActivity(intent);
        });
        SignInButton googleSignIn = binding.googlesigin;
        MaterialButton phoneSignIn = binding.phonesignin;
        MaterialButton emailSignIn=binding.emailsignin;
        enterCode=new TextInputDialog(R.string.enter_sms_code,null,null, InputType.TYPE_CLASS_NUMBER,requireContext());
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        loginViewModel.getUser().observe(getViewLifecycleOwner(),this::updateUI);
        observeLoadingState();
        googleSignIn.setOnClickListener(v -> {
            loginViewModel.setIsTaskRunning(false);
            initGoogleSignIn();
        });
        phoneSignIn.setOnClickListener(v -> {
            loginViewModel.setIsTaskRunning(false);
            TextInputDialog textInputDialog = new TextInputDialog(R.string.sign_in_with_phone,getString(R.string.india_country_code),
                    "Enter your no. with Country Code",InputType.TYPE_CLASS_PHONE, getContext());
            textInputDialog.showDialog(getChildFragmentManager(), msg -> {
                if(textInputDialog.isPhoneNumberValid(msg)) {
                    phoneNumber=msg;
                    initPhoneSignIn();
                }
                else
                    Snackbar.make(binding.getRoot(),R.string.enter_country_code,Snackbar.LENGTH_SHORT).show();
            });
        });
        emailSignIn.setOnClickListener(v -> {
            loginViewModel.setIsTaskRunning(false);
            TextInputDialog emailDialog=new TextInputDialog(R.string.sign_in_with_email,null,
            "Enter your Email address",InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, getContext());
            emailDialog.showDialog(getChildFragmentManager(), msg -> {
                if(emailDialog.isEmailValid(msg)){
                sharedPreferences.edit().putString(EMAIL,msg).apply();
                email=msg;
                initEmailSignIn();
                }
                else
                    Snackbar.make(binding.getRoot(),"Enter valid email",Snackbar.LENGTH_SHORT).show();
            });
        });
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        try{
            Intent intent=requireActivity().getIntent();
            if(intent.getData()!=null)
                signInFromIntent(intent);
            else
                Tasks.await(loginViewModel.checkPendingSignIn());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void initGoogleSignIn(){
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gsi = GoogleSignIn.getClient(requireActivity(), gso);
        googleSignInLauncher.launch(gsi.getSignInIntent());
        loginViewModel.setIsTaskRunning(true);
    }

    private void initPhoneSignIn(){
    PhoneAuthOptions pao= new PhoneAuthOptions
                .Builder(loginViewModel.getAuthInstance())
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(onVerificationStateChangedCallback)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(pao);
        loginViewModel.setIsTaskRunning(true);
    }

    private void initEmailSignIn(){
        loginViewModel.setIsTaskRunning(true);
        ActionCodeSettings acs = ActionCodeSettings.newBuilder()
                .setUrl(getString(R.string.app_url))
                .setHandleCodeInApp(true)
                .setAndroidPackageName(requireContext().getPackageName(), false, null)
                .build();
        loginViewModel.getAuthInstance()
                .sendSignInLinkToEmail(email,acs)
                .addOnCompleteListener(requireActivity(),task -> {
                    loginViewModel.setIsTaskRunning(!task.isComplete());
                    if(task.isSuccessful()) {
                        loginViewModel.setIsTaskRunning(false);
                        MaterialAlertDialogBuilder builder= new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Oxtor_AlertDialog)
                                        .setCancelable(false).setTitle("Link Sent")
                                        .setMessage("Check spam if you don't find email in inbox")
                                        .setPositiveButton("Open Email", (dialog, which) -> {
                                            dialog.dismiss();
                                            Intent intent = new Intent(Intent.ACTION_MAIN);
                                            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
                                            intent.putExtra(Intent.EXTRA_EMAIL,email);
                                            startActivity(intent);
                                        })
                                        .setNegativeButton("Exit", (dialog, which) -> {
                                            dialog.dismiss();
                                            requireActivity().finish();
                                        });
                        AlertDialog dialog=builder.create();
                        dialog.show();
                        }
                    else {
                        Snackbar.make(binding.getRoot(), "An Error Occurred",Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    public ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.
                            getSignedInAccountFromIntent(result.getData());
                    loginViewModel.getGoogleSignInAccount(task)
                            .addOnCompleteListener(task1->gsi.signOut());
                }
            });

    private void resendCode(){
    PhoneAuthOptions pao= new PhoneAuthOptions
                .Builder(loginViewModel.getAuthInstance())
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setForceResendingToken(resendToken)
                .setCallbacks(onVerificationStateChangedCallback)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(pao);
        loginViewModel.setIsTaskRunning(true);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks 
            onVerificationStateChangedCallback= new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            Log.d(TAG, "onVerificationCompleted: ");
            final String code = phoneAuthCredential.getSmsCode();
            if (code != null) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(id, code);
                sharedPreferences.edit().remove(EMAIL).apply();
                loginViewModel.signIn(credential);
                loginViewModel.setIsTaskRunning(false);
            }
            else
                Toast.makeText(getContext(), "Couldn't receive sms code, try again...",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            id=s;
            resendToken=forceResendingToken;
            loginViewModel.setIsTaskRunning(false);
            loginViewModel.getAuthInstance()
                    .getFirebaseAuthSettings()
                    .setAutoRetrievedSmsCodeForPhoneNumber(phoneNumber,id);
            Snackbar.make(binding.getRoot(),"SMS code sent successfully", Snackbar.LENGTH_SHORT).show();
            showSmsDialog();
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            loginViewModel.setIsTaskRunning(false);
            Snackbar.make(binding.getRoot(), "An Error Occurred",Snackbar.LENGTH_SHORT).show();
        }

        @Override
        public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
            super.onCodeAutoRetrievalTimeOut(s);
            loginViewModel.setIsTaskRunning(false);
            if(enterCode.isInLayout())enterCode.dismiss();
            Snackbar snackBar=Snackbar.make(binding.getRoot(), "Code Retrieval Timed Out", Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("RESEND", v -> {
                 resendCode();
                 snackBar.dismiss();
            });
            snackBar.show();
        }
    };

    private void showSmsDialog(){
        enterCode.showDialog(getChildFragmentManager(), code -> {
            if (code != null) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(id, code);
                loginViewModel.signIn(credential);
                sharedPreferences.edit().remove(EMAIL).apply();
            }
        });
    }

    private void observeLoadingState() {
        loginViewModel.getIsTaskRunning().observe(getViewLifecycleOwner(),MainActivity.observer);
        loginViewModel.getIsTaskRunning().observe(getViewLifecycleOwner(),isRunning->{
            if(isRunning){
                binding.progressLogin.setVisibility(View.VISIBLE);
            }
            else{
                binding.progressLogin.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            try{
            navController.navigate(R.id.action_navigation_login_to_navigation_home);
            }catch(Exception e){
                e.printStackTrace();
                Snackbar.make(binding.getRoot(),"Some error occurred, Please restart the app",Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    public void signInFromIntent(Intent data){
        if(loginViewModel.getAuthInstance().isSignInWithEmailLink(data.getData().toString())) {
            String email=sharedPreferences.getString("email",null);
            Snackbar.make(binding.getRoot(),"SignIn link detected for : "+email,Snackbar.LENGTH_SHORT).show();
            loginViewModel.signInWithEmail(email, data.getData().toString());
            sharedPreferences.edit().remove(EMAIL).apply();
        }
    }

    @SuppressLint("SuspiciousIndentation")
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(fullscreenManager!=null)
            fullscreenManager.disableFullscreen();
    }

}