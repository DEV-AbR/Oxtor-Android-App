package teamarmada.oxtor.Livedata;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import androidx.lifecycle.LiveData;

public class MemoryLiveData extends LiveData<Boolean> implements ComponentCallbacks2 {

    private final Context context;

    public MemoryLiveData(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected void onActive() {
        super.onActive();
        context.registerComponentCallbacks(this);
        updateValue();
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        context.unregisterComponentCallbacks(this);
    }

    private void updateValue() {
        setValue(isLowMemory());
    }

    private boolean isLowMemory() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.lowMemory;
        } else {
            return false;
        }
    }

    @Override
    public void onTrimMemory(int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            updateValue();
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        // Not used
    }

    @Override
    public void onLowMemory() {
        updateValue();
    }

}
