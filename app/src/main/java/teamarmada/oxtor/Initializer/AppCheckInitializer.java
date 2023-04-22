package teamarmada.oxtor.Initializer;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

import java.util.Collections;
import java.util.List;

import teamarmada.oxtor.BuildConfig;

public class AppCheckInitializer implements Initializer<FirebaseAppCheck> {

    public static final String TAG=AppCheckInitializer.class.getSimpleName();


    @NonNull
    @Override
    public FirebaseAppCheck create(@NonNull Context context) {

        FirebaseApp app=FirebaseApp.initializeApp(context);
        assert app != null;
        FirebaseAppCheck appCheck=FirebaseAppCheck.getInstance(app);

        if(BuildConfig.DEBUG)
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());
        else
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        return appCheck;
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.singletonList(WorkManagerInitializer.class);
    }

}
