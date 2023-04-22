package teamarmada.oxtor.Ui.Dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import teamarmada.oxtor.R;
import teamarmada.oxtor.databinding.FragmentBottomsheetItemBinding;

public class ItemBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = ItemBottomSheet.class.getSimpleName();
    private BottomSheetCallback callback;
    private final int layoutID;
    private FragmentBottomsheetItemBinding binding;

    public ItemBottomSheet(@LayoutRes int layoutID){
        this.layoutID=layoutID;
    }

    public void showBottomSheet(FragmentManager fragmentManager, BottomSheetCallback callback){
        this.callback=callback;
        showNow(fragmentManager,TAG);
    }


    public void setItemPosition(int i){
        binding.viewpagerHome.setCurrentItem(i,false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, layoutID, container, false);
        binding.setLifecycleOwner(this);
        callback.bind(binding);
        return binding.getRoot();
    }

    @Override
    public void setStyle(int style, int theme) {
        super.setStyle(STYLE_NORMAL, R.style.Theme_Oxtor_BottomSheetStyle);
    }

    public interface BottomSheetCallback extends View.OnClickListener{
        void bind(FragmentBottomsheetItemBinding binding);
    }

}
