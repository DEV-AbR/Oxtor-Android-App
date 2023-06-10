package teamarmada.oxtor.Ui.RecyclerViewAdapter;

import static android.content.Context.ACTIVITY_SERVICE;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;

import java.util.List;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.FileTask;
import teamarmada.oxtor.Utils.FileItemUtils;
import teamarmada.oxtor.databinding.ListFiletaskBinding;

public class TaskListAdapter <T extends StorageTask> extends ListAdapter<FileTask<T>,TaskListAdapter<T>.ViewHolder> implements Observer<List<FileTask<T>>> {

    private final MutableLiveData<List<FileTask<T>>> mutableList;
    private List<FileTask<T>> list;
    public static final String TAG= TaskListAdapter.class.getSimpleName();
    private Context context;

    public TaskListAdapter(MutableLiveData<List<FileTask<T>>> list){
        super(new DiffUtil.ItemCallback<FileTask<T>>() {

            @Override
            public boolean areItemsTheSame(@NonNull FileTask<T> oldItem, @NonNull FileTask<T> newItem) {
                return oldItem.equals(newItem);
            }

            @SuppressLint("DiffUtilEquals")
            @Override
            public boolean areContentsTheSame(@NonNull FileTask<T> oldItem, @NonNull FileTask<T> newItem) {
                return oldItem.toString().equals(newItem.toString());
            }
        });
        mutableList=list;
        mutableList.observeForever( this);
    }

    public List<FileTask<T>> getList() {
        return list;
    }

    public void setList(List<FileTask<T>> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context=parent.getContext();
        LayoutInflater layoutInflater=LayoutInflater.from(context);
        ListFiletaskBinding binding= ListFiletaskBinding.inflate(layoutInflater);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskListAdapter.ViewHolder holder, int position) {
        holder.bind();
    }



    @Override
    public int getItemCount() {
        return list.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onChanged(List<FileTask<T>> fileTasks) {
        this.list=fileTasks;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ListFiletaskBinding binding;

        public ViewHolder(ListFiletaskBinding binding){
            super(binding.getRoot());
            this.binding=binding;

        }

        public FileTask<T> getItem(){
            return list.get(getBindingAdapterPosition());
        }

        public void bind() {
            FileItem fileItem = getItem().getFileItem();
            binding.name.setText(fileItem.getFileName());
            binding.size.setText(FileItemUtils.byteToString(fileItem.getFileSize()));
            final String t = "Connecting...";
            binding.timestamp.setText(t);
            binding.progressbarTaskItem.setVisibility(View.VISIBLE);
            binding.progressbarTaskItem.setIndeterminate(true);
            if (getItem().getTask() instanceof UploadTask)
                Glide.with(binding.picture).load(Uri.parse(fileItem.getFilePath())).into(binding.picture);
            if (getItem().getTask() instanceof FileDownloadTask)
                FileItemUtils.loadPhoto(fileItem, binding.picture);
            binding.removeButton.setOnClickListener(v -> {
                try {
                    Toast.makeText(context, fileItem.getFileName() + " Removed", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    removeTask();
                }
            });
            try {
                FileTask<T> item = getItem();
                item.getTask()
                        .addOnCompleteListener(task -> removeTask())
                        .addOnCanceledListener(() -> cancelTask())
                        .addOnProgressListener(snapshot -> {
                            if (getAvailableMemory(context).lowMemory) {
                                if (item.getTask().isInProgress()) {
                                    item.getTask().pause();
                                }
                            } else {
                                if (item.getTask().isPaused()) {
                                    item.getTask().resume();
                                }
                                int progress = getTaskProgress(item, snapshot);
                                if (progress > 0) {
                                    binding.progressbarTaskItem.setIndeterminate(false);
                                    binding.progressbarTaskItem.setProgress(progress);
                                    String s = "Progress : " + progress + "%";
                                    binding.timestamp.setText(s);
                                } else {
                                    binding.progressbarTaskItem.setIndeterminate(true);
                                    String s = "Connecting...";
                                    binding.timestamp.setText(s);
                                }
                            }
                        });
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        @SuppressLint({"SuspiciousIndentation", "NotifyDataSetChanged"})
        public void  cancelTask(){
            try{
            try {
                if(getItem().getTask().cancel()){
                    list.remove(getItem());
                    mutableList.setValue(list);
                }
            }catch (Exception e){
                if(getItem().getTask().cancel()){
                    if(list.remove(getItem()))
                    mutableList.postValue(list);
                }
            }
            notifyDataSetChanged();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        @SuppressLint({"SuspiciousIndentation", "NotifyDataSetChanged"})
        public void  removeTask(){
            try{
            try {
                if(list.remove(getItem()))
                mutableList.setValue(list);
            }catch (Exception e){
                if(list.remove(getItem()))
                mutableList.postValue(list);
            }
            notifyDataSetChanged();
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        private int getTaskProgress(FileTask<T> item,Object snapshot){
            if(item.getTask() instanceof UploadTask){
                UploadTask.TaskSnapshot taskSnapshot= (UploadTask.TaskSnapshot) snapshot;
                double progress=(100.0*taskSnapshot.getBytesTransferred())/item.getFileItem().getFileSize();
                return (int) progress;
            }
            else if(item.getTask() instanceof FileDownloadTask){
                FileDownloadTask.TaskSnapshot taskSnapshot= (FileDownloadTask.TaskSnapshot) snapshot;
                double progress=(100.0*taskSnapshot.getBytesTransferred())/item.getFileItem().getFileSize();
                return (int) progress;
            }
            else if(item.getTask() instanceof StreamDownloadTask){
                StreamDownloadTask.TaskSnapshot taskSnapshot= (StreamDownloadTask.TaskSnapshot) snapshot;
                double progress=(100.0*taskSnapshot.getBytesTransferred())/item.getFileItem().getFileSize();
                return (int) progress;
            }
            else
                return 0;
        }

        private ActivityManager.MemoryInfo getAvailableMemory(Context context) {
            ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memoryInfo);
            return memoryInfo;
        }

    }

}