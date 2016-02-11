package asia.ivity.android.marqueeview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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

    private Animation mMoveTextOut = null;

    private Paint mPaint;

    private boolean mMarqueeNeeded = false;

    private static final String TAG = MarqueeView.class.getSimpleName();

    private float mTextDifference;

    /**
     * Control the speed. The lower this value, the faster it will scroll.
     */
    private static final int DEFAULT_SPEED = 60;

    /**
     * Control the pause between the animations. Also, after starting this activity.
     */
    private static final int DEFAULT_ANIMATION_PAUSE = 2000;

    private int mSpeed = DEFAULT_SPEED;

    private Interpolator mInterpolator = new LinearInterpolator();

    private Runnable mAnimationStartRunnable;

    private float mTextWidth;
    private int mOriginalGravity;
    private CharSequence mText;

    /**
     * Sets the animation speed.
     * The lower the value, the faster the animation will be displayed.
     *
     * @param speed Milliseconds per PX.
     */
    public void setSpeed(int speed) {
        this.mSpeed = speed;
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

        init(context);
        extractAttributes(attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public MarqueeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
        extractAttributes(attrs);
    }

    private void extractAttributes(AttributeSet attrs) {
        if (getContext() == null) {
            return;
        }

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.asia_ivity_android_marqueeview_MarqueeView);

        if (a == null) {
            return;
        }

        mSpeed = a.getInteger(R.styleable.asia_ivity_android_marqueeview_MarqueeView_speed, DEFAULT_SPEED);

        a.recycle();
    }

    private void init(Context context) {
        // init helper
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(1);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mInterpolator = new LinearInterpolator();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (getChildCount() == 0 || getChildCount() > 1) {
            throw new RuntimeException("MarqueeView must have exactly one child element.");
        }

        if (changed && mScrollView == null) {
            View v = getChildAt(0);
            // Fixes #1: Exception when using android:layout_width="fill_parent". There seems to be an additional ScrollView parent.
            if (v instanceof ScrollView && ((ScrollView) v).getChildCount() == 1) {
                v = ((ScrollView) v).getChildAt(0);
            }

            if (!(v instanceof TextView)) {
                throw new RuntimeException("The child view of this MarqueeView must be a TextView instance.");
            }

            initView(getContext());

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
                mTextField.startAnimation(mMoveTextOut);
            }
        };
        post(mAnimationStartRunnable);
    }

    /**
     * Disables the animations.
     */
    public void reset() {

        if (mAnimationStartRunnable != null) {
            removeCallbacks(mAnimationStartRunnable);
        }

        mTextField.clearAnimation();

        mMoveTextOut.reset();

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
        } else {
            mTextField.setGravity(mOriginalGravity);
            mTextField.setVisibility(VISIBLE);
        }

        mTextDifference = Math.abs((mTextWidth));

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "mTextWidth       : " + mTextWidth);
            Log.d(TAG, "measuredWidth    : " + getMeasuredWidth());
            Log.d(TAG, "mMarqueeNeeded   : " + mMarqueeNeeded);
            Log.d(TAG, "mTextDifference  : " + mTextDifference);
        }

        final int duration = (int) (mTextDifference * mSpeed);

        mMoveTextOut = new TranslateAnimation(measuredWidth, -mTextDifference, 0, 0);
        mMoveTextOut.setDuration(duration);
        mMoveTextOut.setInterpolator(mInterpolator);
        mMoveTextOut.setRepeatCount(Animation.INFINITE);


        mMoveTextOut.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
                mTextField.setVisibility(VISIBLE);
                expandTextView();
            }

            public void onAnimationEnd(Animation animation) {

            }

            public void onAnimationRepeat(Animation animation) {
            }
        });

    }

    private void initView(Context context) {

        LayoutParams sv1lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        sv1lp.gravity = Gravity.CENTER_HORIZONTAL;
        mScrollView = new ScrollView(context);

        mTextField = (TextView) getChildAt(0);
        mTextField.setText(mText);
        mOriginalGravity = mTextField.getGravity();
        removeView(mTextField);

        mScrollView.addView(mTextField, new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                reset();
                prepareAnimation();

                cutTextView();

                post(new Runnable() {
                    @Override
                    public void run() {
                        startMarquee();
                    }
                });
            }
        });

        addView(mScrollView, sv1lp);
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
            lp.width = getMeasuredWidth();
            mTextField.setLayoutParams(lp);
        }
    }

    public void setText(CharSequence text) {

        mText = text;

        if (mTextField != null) {
            mTextField.setText(text);
        }
    }
}
