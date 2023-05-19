package teamarmada.oxtor.Livedata;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

public class MemoryLiveData extends LiveData<Boolean> {

    private final Context context;
    private final ComponentCallbacks2 callback;

    public MemoryLiveData(Context context) {
        this.context = context.getApplicationContext();
        callback=new ComponentCallbacks2() {
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
        };
    }

    @Override
    protected void onActive() {
        super.onActive();
        context.registerComponentCallbacks(callback);
        updateValue();
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        context.unregisterComponentCallbacks(callback);
    }

    private void updateValue() {
        setValue(isLowMemory());
    }

    private boolean isLowMemory() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.lowMemory;
    }

}
