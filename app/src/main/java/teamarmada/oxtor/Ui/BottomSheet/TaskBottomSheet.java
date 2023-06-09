package teamarmada.oxtor.Ui.BottomSheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import teamarmada.oxtor.Fragments.ChildFragments.DownloadTaskFragment;
import teamarmada.oxtor.Fragments.ChildFragments.UploadTaskFragment;
import teamarmada.oxtor.R;
import teamarmada.oxtor.databinding.BottomsheetFiletaskBinding;


public class TaskBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG= TaskBottomSheet.class.getSimpleName();
    private final int[] tabList= new int[]{R.string.uploadtask, R.string.downloadtask};
    private BottomsheetFiletaskBinding binding;
    private ViewPager2 viewpager2;
    private TabLayout tablayout;
    private int pos=0;

    public TaskBottomSheet(){}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomsheetFiletaskBinding.inflate(inflater, container, false);
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
}
