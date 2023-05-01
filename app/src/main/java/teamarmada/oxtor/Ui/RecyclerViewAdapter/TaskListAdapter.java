package teamarmada.oxtor.Ui.RecyclerViewAdapter;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
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
import teamarmada.oxtor.databinding.ListTaskItemBinding;

public class TaskListAdapter <T extends StorageTask> extends RecyclerView.Adapter<TaskListAdapter.ViewHolder> {

    private List<FileTask<T>> list;
    public static final String TAG= TaskListAdapter.class.getSimpleName();
    public Observer<List<FileTask<T>>> listObserver;
    private Context context;

    public TaskListAdapter(List<FileTask<T>> list, Observer<List<FileTask<T>>> listObserver){
        this.list=list;
        this.listObserver=listObserver;
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
        ListTaskItemBinding binding=ListTaskItemBinding.inflate(layoutInflater);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskListAdapter.ViewHolder holder, int position) {
        holder.bind();
        FileTask<T> item=list.get(position);
        item.getTask()
                .addOnCompleteListener(task -> holder.removeTask())
                .addOnCanceledListener(holder::cancelTask)
                .addOnProgressListener(snapshot -> {
                    if(getAvailableMemory(context).lowMemory){
                        if(item.getTask().isInProgress()) {
                            item.getTask().pause();
                        }
                    }
                    else{
                        if(item.getTask().isPaused()) {
                            item.getTask().resume();
                        }
                        int progress= getTaskProgress(item,snapshot);
                        if(progress>0){
                            holder.binding.progressbarTaskItem.setIndeterminate(false);
                            holder.binding.progressbarTaskItem.setProgress(progress);
                            String s="Progress : "+progress+"%";
                            holder.binding.timestamp.setText(s);
                        }
                        else{
                            holder.binding.progressbarTaskItem.setIndeterminate(true);
                            String s="Connecting...";
                            holder.binding.timestamp.setText(s);
                        }
                    }
                });

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

    private void makeToast(String msg){
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ListTaskItemBinding binding;

        public ViewHolder(ListTaskItemBinding binding){
            super(binding.getRoot());
            this.binding=binding;

        }

        public FileTask<T> getItem(){
            return list.get(getBindingAdapterPosition());
        }

        public void bind(){
            FileItem fileItem=getItem().getFileItem();
            binding.name.setText(fileItem.getFileName());
            binding.size.setText(FileItemUtils.byteToString(fileItem.getFileSize()));
            final String t="Connecting...";
            binding.timestamp.setText(t);
            binding.progressbarTaskItem.setVisibility(View.VISIBLE);
            binding.progressbarTaskItem.setIndeterminate(true);
            if(getItem().getTask() instanceof UploadTask)
                Glide.with(binding.picture).load(Uri.parse(fileItem.getFilePath())).into(binding.picture);
            if(getItem().getTask() instanceof FileDownloadTask)
                FileItemUtils.loadPhoto(fileItem,binding.picture);
            binding.removeButton.setOnClickListener(v->{
                removeTask();
                makeToast(fileItem.getFileName() + " Removed");
            });
        }

        public void  cancelTask(){
            try {
                getItem().getTask().cancel();
                list.remove(getAbsoluteAdapterPosition());
                listObserver.onChanged(list);
            }catch (Exception e){
                e.printStackTrace();
                return;
            }
            try{
                notifyItemRemoved(getAbsoluteAdapterPosition());
            }catch (Exception e){
                notifyDataSetChanged();
            }
        }

        public void  removeTask(){
            try {
                list.remove(getAbsoluteAdapterPosition());
                listObserver.onChanged(list);
            }catch (Exception e){
                e.printStackTrace();
                return;
            }
            try{
                notifyItemRemoved(getAbsoluteAdapterPosition());
            }catch (Exception e){
                notifyDataSetChanged();
            }
        }
    }

}