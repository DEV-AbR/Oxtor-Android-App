package teamarmada.oxtor.Fragments.ChildFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.firebase.storage.StreamDownloadTask;

import teamarmada.oxtor.Ui.RecyclerViewAdapter.TaskListAdapter;
import teamarmada.oxtor.ViewModels.MainViewModel;
import teamarmada.oxtor.databinding.FragmentTaskBinding;

public class DownloadTaskFragment extends Fragment {

    public static final String TAG=DownloadTaskFragment.class.getSimpleName();
    public DownloadTaskFragment(){}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        teamarmada.oxtor.databinding.FragmentTaskBinding bindings = FragmentTaskBinding.inflate(inflater, container, false);
        bindings.setLifecycleOwner(this);
        MainViewModel mainViewModel=new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        RecyclerView recyclerView = bindings.recyclerviewTasks;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL,false));
        TaskListAdapter<StreamDownloadTask> adapter = new TaskListAdapter<>(
                mainViewModel.mutableDownloadList.getValue(),
                mainViewModel.mutableDownloadList::postValue);
        recyclerView.addItemDecoration(new MaterialDividerItemDecoration(requireContext(),MaterialDividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);
        return bindings.getRoot();
    }

}
