package teamarmada.oxtor.Fragments.ChildFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.firebase.storage.UploadTask;

import java.util.List;

import kotlin.jvm.internal.markers.KMutableList;
import teamarmada.oxtor.Model.FileTask;
import teamarmada.oxtor.Ui.RecyclerViewAdapter.TaskListAdapter;
import teamarmada.oxtor.ViewModels.MainViewModel;
import teamarmada.oxtor.databinding.FragmentTaskBinding;

public class UploadTaskFragment extends Fragment {

    public static final String TAG=UploadTaskFragment.class.getSimpleName();
    private FragmentTaskBinding binding;

    public UploadTaskFragment(){}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskBinding.inflate(inflater, container, false);
        MainViewModel mainViewModel=new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        RecyclerView recyclerView = binding.recyclerviewTasks;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL,false));
        recyclerView.addItemDecoration(new MaterialDividerItemDecoration(requireContext(),MaterialDividerItemDecoration.VERTICAL));
        TaskListAdapter<UploadTask> adapter=new TaskListAdapter<>(mainViewModel.mutableUploadList);
        recyclerView.setAdapter(adapter);
        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding=null;
    }
}
