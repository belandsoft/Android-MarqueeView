package asia.ivity.android.marqueeview;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Provides a simple marquee effect for a single {@link android.widget.TextView}.
 *
 * @author Sebastian Roth <sebastian.roth@gmail.com>
 */
public class MarqueeView extends LinearLayout {
    private TextView mTextField;

    private ScrollView mScrollView;

    private Animation animation = null;

    private Paint mPaint;

    private boolean mMarqueeNeeded = false;

    private static final String TAG = MarqueeView.class.getSimpleName();

    private float mTextDifference;

    /**
     * Control the speed. This value is a percentage of "normal speed".
     * So 100 will be normal, 50 will be half as slow, 200 will be twice as fast.
     */
    private static final int DEFAULT_SPEED = 100;

    /**
     * Control the pause between the animations. Also, after starting this activity.
     */
    private static final int DEFAULT_ANIMATION_PAUSE = 2000;

    private int mSpeedPercent = DEFAULT_SPEED;

    private Interpolator mInterpolator = new LinearInterpolator();

    private Runnable mAnimationStartRunnable;

    private float mTextWidth;
    private int mOriginalGravity;

    private CharSequence mText = null;
    private int textColor = -1;
    private float textSize = -1;
    static float logicalScreenDensity = -1;

    /**
     * Sets the animation speed.
     * The lower the value, the faster the animation will be displayed.
     *
     * @param speed Milliseconds per PX.
     */
    public void setSpeed(int speed) {
        this.mSpeedPercent = speed;
    }

    /**
     * Sets a custom interpolator for the animation.
     *
     * @param interpolator Animation interpolator.
     */
    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public MarqueeView(Context context) {
        super(context);
        init(context);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public MarqueeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        extractAttributes(attrs);
        init(context);

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public MarqueeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        extractAttributes(attrs);
        init(context);
    }

    private void extractAttributes(AttributeSet attrs) {

        if (getContext() == null) {
            return;
        }

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.asia_ivity_android_marqueeview_MarqueeView);

        if (a == null) {
            return;
        }

        mSpeedPercent = a.getInteger(R.styleable.asia_ivity_android_marqueeview_MarqueeView_speed, DEFAULT_SPEED);
        textSize = a.getDimension(R.styleable.asia_ivity_android_marqueeview_MarqueeView_textSize, -1);
        textColor = a.getResourceId(R.styleable.asia_ivity_android_marqueeview_MarqueeView_textColor, -1);

        a.recycle();
    }

    private void init(Context context) {

        LayoutInflater.from(context).inflate(R.layout.marquee_view_internal, this, true);

        mScrollView = (ScrollView) findViewById(R.id.scrollview_internal);
        mTextField = (TextView) findViewById(R.id.tv_internal);

        if (textColor > -1)
            mTextField.setTextColor(getResources().getColor(textColor));

        if (mText != null)
            mTextField.setText(mText);

        if (textSize > -1)
            mTextField.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        // init helper
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(1);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mInterpolator = new LinearInterpolator();

        // figure out display density only once
        if(logicalScreenDensity < 0) {
            DisplayMetrics metrics = new DisplayMetrics();
            ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(metrics);
            logicalScreenDensity = metrics.density;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed) {
            prepareAnimation();
            startMarquee();
        }
    }

    /**
     * Starts the configured marquee effect.
     */

    void startMarquee() {
        if (mMarqueeNeeded) {
            startTextFieldAnimation();
        }
    }

    private void startTextFieldAnimation() {
        mAnimationStartRunnable = new Runnable() {
            public void run() {
                mTextField.startAnimation(animation);
            }
        };
        post(mAnimationStartRunnable);
    }

    /**
     * Disables the animations.
     */
    public void reset() {

        if (mAnimationStartRunnable != null)
            removeCallbacks(mAnimationStartRunnable);

        if (mTextField != null)
            mTextField.clearAnimation();

        if (animation != null)
            animation.reset();

        invalidate();
    }

    private void prepareAnimation() {
        // Measure
        mPaint.setTextSize(mTextField.getTextSize());
        mPaint.setTypeface(mTextField.getTypeface());
        mTextWidth = mPaint.measureText(mTextField.getText().toString());

        // See how much functions are needed at all
        int measuredWidth = getMeasuredWidth();

        mMarqueeNeeded = mTextWidth > measuredWidth;

        if (mMarqueeNeeded) {
            mTextField.setGravity(Gravity.LEFT);
            mTextField.setVisibility(INVISIBLE);

            mTextDifference = Math.abs((mTextWidth));

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "mTextWidth       : " + mTextWidth);
                Log.d(TAG, "measuredWidth    : " + getMeasuredWidth());
                Log.d(TAG, "mMarqueeNeeded   : " + mMarqueeNeeded);
                Log.d(TAG, "mTextDifference  : " + mTextDifference);
            }

            int duration = (int) (((measuredWidth + mTextWidth) / logicalScreenDensity) * 6);

            // multiply by speed
            float scale = (1f/100f) * mSpeedPercent;
            duration = (int) (duration / scale);

            animation = new TranslateAnimation(measuredWidth, -mTextDifference, 0, 0);
            animation.setDuration(duration);
            animation.setInterpolator(mInterpolator);
            animation.setRepeatCount(Animation.INFINITE);

            animation.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mTextField.setVisibility(VISIBLE);
                    expandTextView();
                }

                public void onAnimationEnd(Animation animation) {

                }

                public void onAnimationRepeat(Animation animation) {
                }
            });

        } else {
            mTextField.setGravity(Gravity.CENTER);
            mTextField.setVisibility(VISIBLE);
            mTextField.invalidate();
        }

    }

    private void expandTextView() {
        ViewGroup.LayoutParams lp = mTextField.getLayoutParams();
        int measuredWidth = (int) (getMeasuredWidth() + mTextWidth);
        lp.width = measuredWidth;
        mTextField.setLayoutParams(lp);
        mScrollView.getLayoutParams().width = measuredWidth;

    }

    private void cutTextView() {
        if (mTextField.getWidth() != getMeasuredWidth()) {
            ViewGroup.LayoutParams lp = mTextField.getLayoutParams();
            int newWidth = getMeasuredWidth();
            lp.width = newWidth;
            mTextField.setLayoutParams(lp);
            mScrollView.getLayoutParams().width = newWidth;
        }
    }

    public void setText(CharSequence text) {

        mText = text;

        if (mTextField != null) {

            mTextField.setText(text);

            reset();

            cutTextView();

            prepareAnimation();


            post(new Runnable() {
                @Override
                public void run() {
                    startMarquee();
                }
            });

        }
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;

        if (mTextField != null) {
            mTextField.setTextColor(textColor);
        }
    }
}
