package teamarmada.oxtor.Ui.DialogFragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import teamarmada.oxtor.R;
import teamarmada.oxtor.databinding.DialogEditBinding;


public class TextInputDialog extends DialogFragment {

    @StringRes int title;
    private final String message;
    private final int inputType;
    private SimpleCallback callback;
    private DialogEditBinding binding;
    private final Context context;
    private TextInputEditText editText;
    private final InputMethodManager imm;
    private final String text;
    public static final String TAG= TextInputDialog.class.getSimpleName();

    public TextInputDialog(@StringRes int title,String hint ,String message, int inputType, Context context){
        this.title=title;
        text=hint;
        this.message=message;
        this.context=context;
        this.inputType=inputType;
        imm=context.getSystemService(InputMethodManager.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding=DialogEditBinding.inflate(getLayoutInflater());
        editText=binding.editText;
        binding.editText.setInputType(inputType);
        if(text!=null)
            editText.setText(text);
        editText.requestFocus();
        MaterialAlertDialogBuilder builder=
                new MaterialAlertDialogBuilder(context,R.style.Theme_Oxtor_AlertDialog)
                .setView(binding.getRoot())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String s=binding.editText.getText().toString();
                    if(!s.isEmpty())
                        callback.getInput(s);
                    else
                        editText.setError("No Input Detected");
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        setCancelable(false);
        return builder.create();
    }

    public void addCallback(SimpleCallback callback){
        this.callback=callback;
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        try {
            super.showNow(manager, tag);
            if (imm != null && binding != null)
                imm.showSoftInput(binding.editText, InputMethodManager.SHOW_FORCED);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void dismiss() {
        try {
            super.dismissNow();
            if(imm!=null&&imm.isActive())
                imm.hideSoftInputFromWindow(binding.editText.getWindowToken(),0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public int getInputType(){return inputType;}

    public boolean isEmailValid(CharSequence charSequence){
       if(inputType== InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS||
       inputType== InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
           return Patterns.EMAIL_ADDRESS.matcher(charSequence).matches();
       else return false;
    }

    public boolean isPhoneNumberValid(CharSequence charSequence){
        if(inputType==InputType.TYPE_CLASS_PHONE)
            return Patterns.PHONE.matcher(charSequence).matches();
        else return false;
    }

    public boolean isUsernameValid(CharSequence charSequence){
        String expression= "@[a-zA-Z0-9_]{0,15}";
        Pattern pattern=Pattern.compile(expression);
        Matcher matcher=pattern.matcher(charSequence);
        return matcher.matches();
    }

    @Override
    public void onDestroyView() {
        binding=null;
    }

    public interface SimpleCallback{
        void getInput (String msg);
    }

}
