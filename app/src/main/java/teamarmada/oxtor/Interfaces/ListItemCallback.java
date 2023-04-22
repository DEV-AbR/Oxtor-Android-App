package teamarmada.oxtor.Interfaces;

import androidx.lifecycle.Observer;

import java.util.List;

public interface ListItemCallback<T,VB> extends Observer<List<T>> {
    void bind(VB binding,T item,int position);
}

