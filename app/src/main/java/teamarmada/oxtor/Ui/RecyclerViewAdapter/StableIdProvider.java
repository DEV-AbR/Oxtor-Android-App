package teamarmada.oxtor.Ui.RecyclerViewAdapter;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.widget.RecyclerView;

public class StableIdProvider extends ItemKeyProvider<Long> {
    private final RecyclerView recyclerView;

    public StableIdProvider(RecyclerView recyclerView) {
        super(ItemKeyProvider.SCOPE_MAPPED);
        this.recyclerView=recyclerView;
    }

    @Nullable
    @Override
    public Long getKey(int position) {
        return recyclerView.getAdapter().getItemId(position);
    }

    @Override
    public int getPosition(@NonNull Long key) {
        if(key==null)
        return recyclerView.findViewHolderForItemId(key).getLayoutPosition();
        else return RecyclerView.NO_POSITION;
    }

    public static class ItemLookup extends ItemDetailsLookup<Long> {

        private final RecyclerView recyclerView;

        public ItemLookup(RecyclerView recyclerView){
            this.recyclerView=recyclerView;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View view=recyclerView.findChildViewUnder(e.getX(),e.getY());
            if(view!=null){
                RecyclerViewAdapter.ViewHolder viewHolder=(RecyclerViewAdapter.ViewHolder)
                        recyclerView.getChildViewHolder(view);
                return viewHolder.getItemDetails();
            }
            return null;
        }


    }
}
