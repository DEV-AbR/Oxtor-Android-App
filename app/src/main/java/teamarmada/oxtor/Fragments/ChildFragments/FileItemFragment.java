package teamarmada.oxtor.Fragments.ChildFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    private FragmentFileitemBinding binding;
    public FileItemFragment(FileItem fileItem){
        item=fileItem;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFileitemBinding.inflate(inflater, container, false);
        try {
            if(item!=null) {
                binding.filename.setText(item.getFileName());
                binding.fileSize.setText(FileItemUtils.formatByteSize(item.getFileSize()));
                if(item.getDownloadUrl()==null){
                    binding.isencrypted.setText("[File deleted]");
                    binding.pictureOfFile.setImageResource(R.drawable.ic_baseline_file_present_24);
                    return binding.getRoot();
                }
                binding.isencrypted.setText("Encrypted: " + item.isEncrypted());
                binding.timestamp.setText(FileItemUtils.getFormattedTimestamp(item.getTimeStamp()));
                Glide.with(this).load(item).fitCenter().into(binding.pictureOfFile);
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding=null;
    }

}
