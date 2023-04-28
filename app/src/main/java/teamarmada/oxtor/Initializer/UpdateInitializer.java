package teamarmada.oxtor.Initializer;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Collections;
import java.util.List;

import kotlin.Unit;
import teamarmada.oxtor.R;

public class UpdateInitializer implements Initializer<Unit> {

    @NonNull
    @Override
    public Unit create(@NonNull Context context) {
        FirebaseRemoteConfig frc= FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        frc.activate()
                .onSuccessTask(aBoolean -> frc.setConfigSettingsAsync(configSettings))
                .onSuccessTask(unused -> frc.setDefaultsAsync(R.xml.default_config_values))
                .onSuccessTask(unused -> frc.fetchAndActivate())
                .addOnCompleteListener(task -> {
                    boolean updated = task.getResult();
                    if (task.isSuccessful()) {
                        Log.d("TAG", "Config params updated: " + updated);
                    } else {
                        Log.d("TAG", "Config params updated: " + updated);
                    }
                });
        return Unit.INSTANCE;
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.singletonList(AppCheckInitializer.class);
    }
}
