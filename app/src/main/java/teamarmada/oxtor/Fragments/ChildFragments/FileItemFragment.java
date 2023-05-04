package teamarmada.oxtor.Fragments.ChildFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Utils.FileItemUtils;
import teamarmada.oxtor.databinding.FragmentFileitemBinding;


public class FileItemFragment extends Fragment {

    public static final String TAG=FileItemFragment.class.getSimpleName();
    private final FileItem item;

    public FileItemFragment(FileItem fileItem){
        item=fileItem;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentFileitemBinding binding = FragmentFileitemBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        try {
            if(item!=null) {
                binding.filename.setText(item.getFileName());
                binding.fileSize.setText(FileItemUtils.byteToString(item.getFileSize()));
                binding.isencrypted.setText("Encrypted: " + item.isEncrypted());
                binding.timestamp.setText(FileItemUtils.getTimestampString(item.getTimeStamp()));
                Glide.with(this).load(item).into(binding.pictureOfFile);
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        binding.executePendingBindings();
        return binding.getRoot();
    }

}
