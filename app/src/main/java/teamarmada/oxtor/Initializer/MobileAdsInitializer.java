package teamarmada.oxtor.Initializer;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.google.android.gms.ads.MobileAds;

import java.util.Collections;
import java.util.List;

import kotlin.Unit;

public class MobileAdsInitializer implements Initializer<Unit> {
    public static final String TAG=MobileAdsInitializer.class.getSimpleName();
    @NonNull
    @Override
    public Unit create(@NonNull Context context) {
        Log.d(TAG, "create: ");
        MobileAds.initialize(context);
        return Unit.INSTANCE;
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.singletonList(AppCheckInitializer.class);
    }
}
