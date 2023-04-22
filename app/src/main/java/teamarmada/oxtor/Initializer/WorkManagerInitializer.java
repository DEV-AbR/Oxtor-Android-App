package teamarmada.oxtor.Initializer;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;
import androidx.work.WorkManager;

import java.util.Collections;
import java.util.List;

public class WorkManagerInitializer implements Initializer<WorkManager> {
    @NonNull
    @Override
    public WorkManager create(@NonNull Context context) {
        return WorkManager.getInstance(context);
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
