package teamarmada.oxtor.Ui.RecyclerViewAdapter;

import android.annotation.SuppressLint;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.databinding.ListFileitemBinding;


public class RecyclerViewAdapter extends ListAdapter<FileItem,RecyclerViewAdapter.ViewHolder>
        implements EventListener<QuerySnapshot>, DefaultLifecycleObserver {

    public static final String TAG= RecyclerViewAdapter.class.getSimpleName();

    private final List<DocumentSnapshot> snapshotList=new ArrayList<>();
    private final List<Long> keyList=new ArrayList<>();
    private final List<FileItem> selectedItems=new ArrayList<>();
    private final SparseBooleanArray sparseBooleanArray=new SparseBooleanArray();
    private RecyclerView.OnChildAttachStateChangeListener childAttachStateChangeListener;
    private ListenerRegistration listenerRegistration;
    private ListItemCallback listener;
    private Query query;
    private SelectionObserver selectionObserver;
    private SelectionTracker<Long> selectionTracker=null;
    private final boolean enableSelection;


    public RecyclerViewAdapter(Lifecycle lifecycle,
                               Query query,
                               boolean enableSelection,
                               ListItemCallback listener){
        super(new DiffUtil.ItemCallback<FileItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull FileItem oldItem, @NonNull FileItem newItem) {
                return oldItem.equals(newItem);
            }

            @SuppressLint("DiffUtilEquals")
            @Override
            public boolean areContentsTheSame(@NonNull FileItem oldItem, @NonNull FileItem newItem) {
                return oldItem.toString().equals(newItem.toString());
            }
        });
        lifecycle.addObserver(this);
        this.query=query;
        this.listener=listener;
        this.enableSelection=enableSelection;
        setHasStableIds(true);
    }

    public List<FileItem> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectionTracker(RecyclerView rv){
        this.selectionTracker=new SelectionTracker.Builder<>("FileItem",
                rv,
                new StableIdProvider(rv),
                new StableIdProvider.ItemLookup(rv),
                StorageStrategy.createLongStorage())
                .withSelectionPredicate(
                        new SelectionTracker.SelectionPredicate<Long>() {
                            @Override
                            public boolean canSetStateForKey(@NonNull Long key, boolean nextState) {
                                return true;
                            }

                            @Override
                            public boolean canSetStateAtPosition(int position, boolean nextState) {
                                return true;
                            }

                            @Override
                            public boolean canSelectMultiple() {
                                return true;
                            }
                        }
                )
                .build();

        selectionObserver=new SelectionObserver(rv);
        this.selectionTracker.addObserver(selectionObserver);
    }

    public SelectionTracker<Long> getSelectionTracker() {
        return selectionTracker;
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater=LayoutInflater.from(parent.getContext());
        ListFileitemBinding binding=ListFileitemBinding.inflate(inflater,parent,false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
        try {
            final int i = holder.getBindingAdapterPosition();
            listener.bind(holder.binding, getItem(i), i);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getItemId(int position) {
        return ((long) position);
    }

    @Override
    public int getItemCount() {
        return snapshotList.size();
    }

    public void changeAdapterQuery(Query mQuery, boolean enableSelection){
        stopListening();
        if(mQuery!=null)
            query=mQuery;
        if(!enableSelection)
            if(selectionTracker!=null)
            {
                selectionTracker.clearSelection();
                selectionTracker=null;
                selectionObserver=null;
            }
        startListening();
    }

    public void setItemCallback(ListItemCallback listener){
        this.listener=listener;
    }

    @Override
    public FileItem getItem(int position) {
        DocumentSnapshot snapshot=snapshotList.get(position);
        try{
            return snapshot.toObject(FileItem.class);
        }catch (Exception e){
            e.printStackTrace();
            try {
                Constructor<FileItem> tConstructor;
                tConstructor = FileItem.class.getConstructor(DocumentSnapshot.class);
                return tConstructor.newInstance(snapshot);
            } catch (Exception ex) {
                ex.printStackTrace();
                return new FileItem(snapshot);
            }
        }
    }

    @Override
    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
        if (e != null) {
            Log.w(TAG, "onEvent:error", e);
            return;
        }
        for (DocumentChange change : documentSnapshots.getDocumentChanges()) {
            switch (change.getType()) {
                case ADDED:
                    onDocumentAdded(change);
                    break;
                case MODIFIED:
                    onDocumentModified(change);
                    break;
                case REMOVED:
                    onDocumentRemoved(change);
                    break;
            }
        }
        sortList();
    }


    @SuppressLint("NotifyDataSetChanged")
    private void sortList() {
        Comparator<DocumentSnapshot> comparator = (snapshot1, snapshot2) -> {
            String downloadUrl1 = snapshot1.getString(FileItem.DOWNLOAD_URL);
            String downloadUrl2 = snapshot2.getString(FileItem.DOWNLOAD_URL);
            if (downloadUrl1 != null && downloadUrl2 != null) {
                return 0; // Both have downloadUrl, consider them equal
            } else if (downloadUrl1 != null) {
                return -1; // Only snapshot1 has downloadUrl, so it should come before snapshot2
            } else if (downloadUrl2 != null) {
                return 1; // Only snapshot2 has downloadUrl, so it should come before snapshot1
            } else {
                return 0; // Both don't have downloadUrl, consider them equal
            }
        };
        snapshotList.sort(comparator);
        notifyDataSetChanged();
    }



    private void onDocumentAdded(DocumentChange change){
        snapshotList.add(change.getNewIndex(), change.getDocument());
        notifyItemInserted(change.getNewIndex());
    }

    private void onDocumentModified(DocumentChange change){

        if (change.getOldIndex() == change.getNewIndex()) {
            snapshotList.set(change.getOldIndex(), change.getDocument());
            notifyItemChanged(change.getOldIndex());
        } else {
            snapshotList.remove(change.getOldIndex());
            snapshotList.add(change.getNewIndex(), change.getDocument());
            notifyItemMoved(change.getOldIndex(), change.getNewIndex());
        }
    }

    private void onDocumentRemoved(DocumentChange change){
        snapshotList.remove(change.getOldIndex());
        notifyDataSetChanged();
    }

    public Query getQuery() {
        return query;
    }

    private void startListening() {
        if (query != null && listenerRegistration == null) {
            listenerRegistration = query.addSnapshotListener(this);
        }
    }

    private void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null; }
        snapshotList.clear();
        notifyDataSetChanged();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        startListening();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStop(owner);
        stopListening();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if(!enableSelection){
            selectionObserver=null;
            selectionTracker=null;
        }
        else{
            if(getSelectionTracker()==null){
                setSelectionTracker(recyclerView);
            }
        }


        childAttachStateChangeListener=new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findContainingViewHolder(view);
                if(viewHolder!=null&&selectionTracker!=null){
                    int i=viewHolder.getBindingAdapterPosition();
                    if(!selectionTracker.getSelection().isEmpty())
                        view.setSelected(sparseBooleanArray.get(i));
                    else view.setSelected(false);
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {

                RecyclerView.ViewHolder viewHolder=recyclerView.findContainingViewHolder(view);
                if(viewHolder!=null&&selectionTracker!=null){
                    int i=viewHolder.getBindingAdapterPosition();
                    if(!selectionTracker.getSelection().isEmpty())
                        sparseBooleanArray.put(i,view.isSelected());
                    else sparseBooleanArray.put(i,false);
                }

            }
        };


        recyclerView.addOnChildAttachStateChangeListener(childAttachStateChangeListener);

    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        recyclerView.removeOnChildAttachStateChangeListener(childAttachStateChangeListener);
        if(selectionTracker!=null)
            selectionTracker.clearSelection();
    }

    public boolean selectActionForAll(){
        boolean b=snapshotList.size()==selectionTracker.getSelection().size();
        keyList.clear();
        snapshotList.forEach(ds -> keyList.add(snapshotList.indexOf(ds),(long)snapshotList.indexOf(ds)));
        return selectionTracker.setItemsSelected(keyList, !b);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ListFileitemBinding binding;

        public ViewHolder(ListFileitemBinding binding){
            super(binding.getRoot());
            this.binding=binding;
        }

        public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return new ItemDetailsLookup.ItemDetails<Long>() {
                @Override
                public int getPosition() {
                    return getBindingAdapterPosition();
                }

                @Nullable
                @Override
                public Long getSelectionKey() {
                    return getItemId();
                }
            };
        }

    }

    public class SelectionObserver extends SelectionTracker.SelectionObserver<Long> {
        private final RecyclerView rv;
        public SelectionObserver(RecyclerView rv){
            this.rv=rv;
        }

        @Override
        public void onItemStateChanged(@NonNull Long key, boolean selected) {
            super.onItemStateChanged(key, selected);
            Log.d(TAG, "onItemStateChanged: item at: "+key+" selected: "+selected);
            FileItem t =getItem(key.intValue());

            if(selected){
                selectedItems.add(t);
            }
            else {
                selectedItems.remove(t);
            }

            RecyclerView.ViewHolder viewHolder=rv.findViewHolderForItemId(key);
            if(viewHolder!=null) {
                rv.findViewHolderForItemId(key).setIsRecyclable(!selected);
                rv.findViewHolderForItemId(key).itemView.setSelected(selected);
            }
            listener.onChanged(selectedItems);
        }

    }

    public interface ListItemCallback extends Observer<List<FileItem>> {
        void bind(ListFileitemBinding binding, FileItem item, int position);
    }

}


