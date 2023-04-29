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
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
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
import com.google.gson.Gson;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import teamarmada.oxtor.Interfaces.ListItemCallback;


public class RecyclerViewAdapter<T,VB extends ViewDataBinding>
        extends ListAdapter<T,RecyclerViewAdapter.ViewHolder>
        implements EventListener<QuerySnapshot>, DefaultLifecycleObserver {

    public static final String TAG= RecyclerViewAdapter.class.getSimpleName();

    private final List<DocumentSnapshot> snapshotList=new ArrayList<>();
    private final List<Long> keyList=new ArrayList<>();
    private final List<T> selectedItems=new ArrayList<>();
    private final SparseBooleanArray sparseBooleanArray=new SparseBooleanArray();
    private RecyclerView.OnChildAttachStateChangeListener childAttachStateChangeListener;
    private ListenerRegistration listenerRegistration;
    private ListItemCallback<T, VB> listener;
    private final Class<T> tClass;
    private Query query;
    private final int itemLayoutID;
    private SelectionObserver selectionObserver;
    private SelectionTracker<Long> selectionTracker=null;
    private final boolean enableSelection;


    public RecyclerViewAdapter(Lifecycle lifecycle,
                               @LayoutRes int itemLayoutID,
                               @NonNull Query mQuery,
                               boolean enableSelection,
                               Class<T> tClass,
                               ListItemCallback<T,VB> listener){
        super(new DiffUtil.ItemCallback<T>() {
            @Override
            public boolean areItemsTheSame(@NonNull T oldItem, @NonNull T newItem) {
                return oldItem.equals(newItem);
            }

            @SuppressLint("DiffUtilEquals")
            @Override
            public boolean areContentsTheSame(@NonNull T oldItem, @NonNull T newItem) {
                return oldItem.toString().equals(newItem.toString());
            }
        });
        lifecycle.addObserver(this);
        this.query=mQuery;
        this.listener=listener;
        this.tClass=tClass;
        this.itemLayoutID=itemLayoutID;
        this.enableSelection=enableSelection;
        setHasStableIds(true);
    }

    public List<T> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectionTracker(RecyclerView rv){
        this.selectionTracker=new SelectionTracker.Builder<>("FileItem",
                rv,
                new StableIdProvider(rv),
                new StableIdProvider.ItemLookup(rv),
                StorageStrategy.createLongStorage())
                .withSelectionPredicate(//SelectionPredicates.createSelectAnything()
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater=LayoutInflater.from(parent.getContext());
        VB binding= DataBindingUtil.inflate(inflater,itemLayoutID,parent,false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
        try{
            listener.bind((VB) holder.binding, getItem(holder.getBindingAdapterPosition()),
                    holder.getBindingAdapterPosition());
        }
        catch (ClassCastException e){
        }
    }

    @Override
    public T getItem(int position) {
        DocumentSnapshot snapshot=snapshotList.get(position);
        try{
        return snapshot.toObject(tClass);
        }catch (Exception e){
            Constructor<T> tConstructor;

            try {
                tConstructor = tClass.getConstructor(DocumentSnapshot.class);
                return tConstructor.newInstance(snapshot);
            } catch (Exception ex) {

                try{
                    (new Gson()).fromJson(snapshot.toString(),tClass);
                }catch (Exception eu){
                    return null;
                }
            }
        }
        return null;
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

    public void setItemCallback(ListItemCallback<T,VB> listener){
        this.listener=listener;
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

    }

    public void onDocumentAdded(DocumentChange change){

        snapshotList.add(change.getNewIndex(), change.getDocument());
        notifyItemInserted(change.getNewIndex());
    }

    public void onDocumentModified(DocumentChange change){

        if (change.getOldIndex() == change.getNewIndex()) {
            snapshotList.set(change.getOldIndex(), change.getDocument());
            notifyItemChanged(change.getOldIndex());
        } else {
            snapshotList.remove(change.getOldIndex());
            snapshotList.add(change.getNewIndex(), change.getDocument());
            notifyItemMoved(change.getOldIndex(), change.getNewIndex());
        }
    }

    public void onDocumentRemoved(DocumentChange change){

        snapshotList.remove(change.getOldIndex());
        //if(change.getOldIndex()!=0)
        //    notifyItemRemoved(change.getOldIndex());
        //else
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
        public VB binding;
        public ViewHolder(VB binding){
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
            T t =getItem(key.intValue());

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

}


