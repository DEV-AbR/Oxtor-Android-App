package teamarmada.oxtor.Ui.Dialog;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import teamarmada.oxtor.Fragments.ChildFragments.DownloadTaskFragment;
import teamarmada.oxtor.Fragments.ChildFragments.UploadTaskFragment;
import teamarmada.oxtor.R;
import teamarmada.oxtor.ViewModels.MainViewModel;
import teamarmada.oxtor.databinding.FragmentBottomsheetFiletaskBinding;


public class TaskBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG= TaskBottomSheet.class.getSimpleName();
    private final int[] tabList= new int[]{R.string.uploadtask, R.string.downloadtask};
    private ViewPager2 viewpager2;
    private TabLayout tablayout;
    private int pos=0;
    public TaskBottomSheet(){}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentBottomsheetFiletaskBinding binding = FragmentBottomsheetFiletaskBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        tablayout= binding.tabLayout;
        viewpager2= binding.viewpagerFiletasks;
        viewpager2.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                    default: return new UploadTaskFragment();
                    case 1: return new DownloadTaskFragment();
                }
            }

            @Override
            public int getItemCount() {
                return tabList.length;
            }
        });

        viewpager2.setSaveEnabled(false);
        viewpager2.setCurrentItem(pos,false);

        new TabLayoutMediator(tablayout,
                viewpager2,
                true,
                true,
                (tab, position) -> tab.setText(tabList[position])
        ).attach();

        tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override
            public void onTabSelected(TabLayout.Tab tab) {viewpager2.setCurrentItem(tab.getPosition());}

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });


        viewpager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tablayout.selectTab(tablayout.getTabAt(position));
            }
        });
        return binding.getRoot();
    }


    @Override
    public void setStyle(int style, int theme) {
        super.setStyle(STYLE_NORMAL, R.style.Theme_Oxtor_BottomSheetStyle);
    }

    public void setTab(int pos){
        this.pos=pos;
    }

}
