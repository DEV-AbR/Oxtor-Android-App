package teamarmada.oxtor.Fragments.ChildFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.Query;

import dagger.hilt.android.AndroidEntryPoint;
import teamarmada.oxtor.Ui.RecyclerViewAdapter.RecyclerViewAdapter;
import teamarmada.oxtor.ViewModels.MainViewModel;
import teamarmada.oxtor.databinding.FragmentListBinding;

@AndroidEntryPoint
public class ListFragment extends Fragment {
    
    private FragmentListBinding binding;
    private Query query;
    private final RecyclerViewAdapter.ListItemCallback listItemCallback;

    public ListFragment(RecyclerViewAdapter.ListItemCallback listItemCallback) {
        this.listItemCallback = listItemCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding=FragmentListBinding.inflate(inflater,container,false);
        MainViewModel homeViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        homeViewModel.queryToSortByTimestamp().addOnSuccessListener(query1->query=query1);
        String fileType="image";
        switch(fileType){
            case "image":

                break;
            case "video":

                break;
            case "audio":

                break;
            case "application":
            case "text":
            case "file":
            default:

                break;
        }

        RecyclerViewAdapter recyclerViewAdapter=new RecyclerViewAdapter(
                getViewLifecycleOwner().getLifecycle(), query,true,listItemCallback);
        binding.recyclerview.setAdapter(recyclerViewAdapter);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding=null;
    }
}
