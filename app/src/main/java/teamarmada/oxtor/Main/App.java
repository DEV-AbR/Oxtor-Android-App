 package teamarmada.oxtor.Main;

 import android.app.Activity;
 import android.app.Application;
 import android.os.Bundle;
 import android.util.Log;

 import androidx.annotation.NonNull;
 import androidx.annotation.Nullable;
 import androidx.appcompat.app.AppCompatActivity;
 import androidx.lifecycle.LifecycleObserver;
 import androidx.startup.AppInitializer;

 import com.bumptech.glide.Glide;
 import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
 import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

 import dagger.hilt.android.HiltAndroidApp;
 import teamarmada.oxtor.Initializer.MobileAdsInitializer;
 import teamarmada.oxtor.R;
 import teamarmada.oxtor.Utils.InAppUpdate;

 @HiltAndroidApp
public class App extends Application implements LifecycleObserver, Application.ActivityLifecycleCallbacks {

    public App(){}

    @Override
    public void onCreate() {
        super.onCreate();
        AppInitializer.getInstance(this).initializeComponent(MobileAdsInitializer.class);
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(this).onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).trimMemory(level);
    }

     @Override
     public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        try {
            InAppUpdate.getInstance((AppCompatActivity) activity);
            ActivityLifecycleObserver.getInstance((AppCompatActivity) activity);
        }catch (ClassCastException e){
            e.printStackTrace();
        }
     }

     @Override
     public void onActivityStarted(@NonNull Activity activity) {

     }

     @Override
     public void onActivityResumed(@NonNull Activity activity) {

     }

     @Override
     public void onActivityPaused(@NonNull Activity activity) {

     }

     @Override
     public void onActivityStopped(@NonNull Activity activity) {

     }

     @Override
     public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

     }

     @Override
     public void onActivityDestroyed(@NonNull Activity activity) {

     }

 }
