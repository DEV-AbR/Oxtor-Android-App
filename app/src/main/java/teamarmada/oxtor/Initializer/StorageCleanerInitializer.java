package teamarmada.oxtor.Initializer;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.bumptech.glide.load.engine.Initializable;

import java.util.Collections;
import java.util.List;

import kotlin.Unit;
import teamarmada.oxtor.Service.CloudStorageCleaner;

public class StorageCleanerInitializer implements Initializer<Unit> {

    @NonNull
    @Override
    public Unit create(@NonNull Context context) {
        Intent serviceIntent = new Intent(context, CloudStorageCleaner.class);
        context.startService(serviceIntent);
        return Unit.INSTANCE;

    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.singletonList(MobileAdsInitializer.class);
    }

}
