package teamarmada.oxtor.Repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import teamarmada.oxtor.R;

public class AdsRepository  {

    public static final String TAG=AdsRepository.class.getSimpleName();
    private InterstitialAd interstitialAd=null;
    private AppOpenAd appOpenAd = null;
    private AdView adView=null;
    private final AdRequest adRequest;
    private final Context context;

    public AdsRepository(Context context){
        this.context=context;
        adRequest=new AdRequest.Builder().build();
    }

    public void setInterstitialAd(InterstitialAd ad) {
        this.interstitialAd = ad;
        if(ad==null)
            loadInterstitialAd();
    }

    public void setAppOpenAd(AppOpenAd appOpenAd) {
        this.appOpenAd = appOpenAd;
        if(appOpenAd==null)
            loadAppOpenAd();
    }

    public InterstitialAd getInterstitialAd() {
        return interstitialAd;
    }

    public AppOpenAd getAppOpenAd() {
        return appOpenAd;
    }

    public AdView getBannerAd() {
        return adView;
    }

    public void loadInterstitialTestAd(){
        InterstitialAd.load(context, context.getString(R.string.interstitial_test_ad_id), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        interstitialAd=null;
                        logError(loadAdError);
                    }
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        super.onAdLoaded(interstitialAd);
                        setInterstitialAd(interstitialAd);
                    }
                });
    }

    public void loadInterstitialAd(){
        InterstitialAd.load(context, context.getString(R.string.interstitial_ad_id), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        interstitialAd=null;
                        logError(loadAdError);
                    }
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        super.onAdLoaded(interstitialAd);
                        setInterstitialAd(interstitialAd);
                    }
                });
    }

    public void loadAppOpenTestAd(){
        AppOpenAd.load(context, context.getString(R.string.app_open_test_ad_id), adRequest,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        appOpenAd=null;
                        logError(loadAdError);
                    }

                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                        super.onAdLoaded(appOpenAd);
                        setAppOpenAd(appOpenAd);
                    }
                });
    }

    public void loadAppOpenAd(){
        AppOpenAd.load(context, context.getString(R.string.app_open_ad_id), adRequest,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        appOpenAd=null;
                        logError(loadAdError);
                    }

                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                        super.onAdLoaded(appOpenAd);
                        setAppOpenAd(appOpenAd);
                    }
                });

    }

    public void loadBannerTestAd(AdSize adSize){
        adView=new AdView(context);
        adView.setAdUnitId(context.getString(R.string.banner_test_ad_id));
        adView.setAdSize(adSize);
        adView.loadAd(adRequest);
    }

    public void loadBannerAd(AdSize adSize){
        adView=new AdView(context);
        adView.setAdUnitId(context.getString(R.string.banner_ad_id));
        adView.setAdSize(adSize);
        adView.loadAd(adRequest);
    }

    private void logError(LoadAdError loadAdError){
        @SuppressLint("DefaultLocale")
        String error =String.format("domain: %s, code: %d, message: %s",
                loadAdError.getDomain(),
                loadAdError.getCode(),
                loadAdError.getMessage());
        Log.d(TAG, "loadAd: onAdFailedToLoad: "+error);
    }

}
