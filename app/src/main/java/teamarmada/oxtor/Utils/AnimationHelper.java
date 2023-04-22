package teamarmada.oxtor.Utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

public class AnimationHelper extends ScaleGestureDetector.SimpleOnScaleGestureListener {


    public float f=1.0f;


    public static boolean rotateFab(final View v, boolean rotate){
        v.animate().setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                    }
                }).rotation(rotate?135f:0f).start();
    return rotate;
    }


    public static RotateAnimation getInfiniteRotationAnim(){
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );

        rotate.setDuration(800);
        rotate.setRepeatCount(Animation.INFINITE);
    return rotate;
    }


    public static void showIn(final View v) {
        v.setVisibility(View.VISIBLE);
        v.setAlpha(0f);

        v.animate()
                .setDuration(200)
                .translationY(0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                    }
                })
                .alpha(1f)
                .start();
        v.setTranslationY(v.getHeight());
    }

    public static void showOut(final View v) {
        v.setVisibility(View.VISIBLE);
        v.setAlpha(1f);
        v.animate()
                .setDuration(200)
                .translationY(v.getHeight())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        v.setVisibility(View.GONE);
                        super.onAnimationEnd(animation);
                    }
                }).alpha(0f)
                .start();
        v.setTranslationY(0);
    }


    public static void init(final View v) {
        v.setVisibility(View.GONE);
        v.setTranslationY(v.getHeight());
        v.setAlpha(0f);
    }





}
