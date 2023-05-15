package teamarmada.oxtor.Livedata;

import android.app.ActivityManager;
import android.content.Context;

import androidx.lifecycle.LiveData;

public class MemoryLiveData extends LiveData<Boolean> {
    private final Context context;

    public MemoryLiveData(Context context) {
        this.context = context;
    }

    @Override
    protected void onActive() {
        super.onActive();
        updateValue();
    }

    private void updateValue() {
        setValue(onLowMemory());
    }

    private boolean onLowMemory() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.lowMemory;
        } else {
            return false;
        }
    }

}

