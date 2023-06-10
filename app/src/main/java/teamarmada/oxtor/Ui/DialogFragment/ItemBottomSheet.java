package teamarmada.oxtor.Ui.DialogFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import teamarmada.oxtor.R;
import teamarmada.oxtor.databinding.BottomsheetFileitemBinding;

public class ItemBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = ItemBottomSheet.class.getSimpleName();
    private BottomSheetCallback callback;
    private BottomsheetFileitemBinding binding;

    public ItemBottomSheet(@LayoutRes int layoutID){
    }

    public BottomsheetFileitemBinding getViewBinding(){return binding;}

    public void addCallback(BottomSheetCallback callback){
        this.callback=callback;
    }

    public void setItemPosition(int i){
        binding.viewpagerHome.setCurrentItem(i,false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomsheetFileitemBinding.inflate(inflater,container,false);

        callback.bind(binding);
        return binding.getRoot();
    }

    @Override
    public void setStyle(int style, int theme) {
        super.setStyle(STYLE_NORMAL, R.style.Theme_Oxtor_BottomSheetStyle);
    }

    @Override
    public void showNow(@NonNull FragmentManager manager, @Nullable String tag) {
        try {
            super.showNow(manager, tag);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding=null;
    }

    public interface BottomSheetCallback extends View.OnClickListener{
        void bind(BottomsheetFileitemBinding binding);
    }

}
