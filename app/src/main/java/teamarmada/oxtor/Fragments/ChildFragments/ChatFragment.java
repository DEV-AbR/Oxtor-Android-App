package teamarmada.oxtor.Fragments.ChildFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import teamarmada.oxtor.databinding.FragmentChatBinding;

public class ChatFragment extends Fragment {

    public static final String TAG=ChatFragment.class.getSimpleName();
    private FragmentChatBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding=FragmentChatBinding.inflate(inflater,container,false);
        binding.setLifecycleOwner(this);
        binding.executePendingBindings();
        return binding.getRoot();
    }




}
