package xyz.danoz.recyclerviewfastscroller;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SectionIndexer;

import xyz.danoz.recyclerviewfastscroller.calculation.progress.ScrollProgressCalculator;
import xyz.danoz.recyclerviewfastscroller.calculation.progress.TouchableScrollProgressCalculator;
import xyz.danoz.recyclerviewfastscroller.sectionindicator.SectionIndicator;

/**
 * Defines a basic widget that will allow for fast scrolling a RecyclerView using the basic paradigm of
 * a handle and a bar.
 *
 * TODO: More specifics and better support for effectively extending this base class
 */
public abstract class AbsRecyclerViewFastScroller extends FrameLayout implements RecyclerViewScroller {

    private static final int[] STYLEABLE = R.styleable.AbsRecyclerViewFastScroller;
    /** The long bar along which a handle travels */
    protected final ImageView mBar;
    /** The handle that signifies the user's progress in the list */
    protected final ImageView mHandle;

    /* TODO:
     *      Consider making RecyclerView final and should be passed in using a custom attribute
     *      This could allow for some type checking on the section indicator wrt the adapter of the RecyclerView
    */
    private RecyclerView mRecyclerView;
    private SectionIndicator mSectionIndicator;

    /** If I had my druthers, AbsRecyclerViewFastScroller would implement this as an interface, but Android has made
     * {@link OnScrollListener} an abstract class instead of an interface. Hmmm */
    protected OnScrollListener mOnScrollListener;

    public AbsRecyclerViewFastScroller(Context context) {
        this(context, null, 0);
    }

    public AbsRecyclerViewFastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsRecyclerViewFastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray attributes = getContext().getTheme().obtainStyledAttributes(attrs, STYLEABLE, 0, 0);

        try {
            int layoutResource = attributes.getResourceId(R.styleable.AbsRecyclerViewFastScroller_rfs_fast_scroller_layout,
                    getLayoutResourceId());
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(layoutResource, this, true);

            mBar = findViewById(R.id.scroll_bar);
            mHandle = findViewById(R.id.scroll_handle);

            Drawable barCustomDrawable = attributes.getDrawable(R.styleable.AbsRecyclerViewFastScroller_rfs_barDrawable);
            Drawable handleCustomDrawable = attributes.getDrawable(R.styleable.AbsRecyclerViewFastScroller_rfs_handleDrawable);

            int barWidth = attributes.getDimensionPixelSize(R.styleable.AbsRecyclerViewFastScroller_rfs_barWidth, 10);
            int handleWidth = attributes.getDimensionPixelSize(R.styleable.AbsRecyclerViewFastScroller_rfs_handleWidth, 50);
            int handleHeight = attributes.getDimensionPixelSize(R.styleable.AbsRecyclerViewFastScroller_rfs_handleHeight, 70);

            float handleScale = attributes.getFloat(R.styleable.AbsRecyclerViewFastScroller_rfs_handleScale, 1);

            applyBarDrawable(mBar, barCustomDrawable, barWidth);
            applyHandleDrawable(mHandle, handleCustomDrawable, handleScale, handleWidth, handleHeight);

        } finally {
            attributes.recycle();
        }

        setOnTouchListener(new FastScrollerTouchListener(this));
    }

    private void applyBarDrawable(ImageView imageView, Drawable drawable, int dimension) {
        if (drawable != null) {
            imageView.setImageDrawable(drawable);
            imageView.getLayoutParams().width = dimension;
        }
    }

    private void applyHandleDrawable(ImageView imageView, Drawable drawable, float scale, int width, int height) {
        if (drawable != null) {
            imageView.setImageDrawable(drawable);
            imageView.getLayoutParams().width = width;
            imageView.getLayoutParams().height = height;
            imageView.setScaleX(scale);
            imageView.setScaleY(scale);
        }
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN)
    private void setViewBackground(View view, Drawable background) {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            view.setBackground(background);
        } else {
            //noinspection deprecation
            view.setBackgroundDrawable(background);
        }
    }

    @Override
    public void setRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mRecyclerView.setOnScrollListener(getOnScrollListener());
    }

    public void setSectionIndicator(SectionIndicator sectionIndicator) {
        mSectionIndicator = sectionIndicator;
    }

    @Nullable
    public SectionIndicator getSectionIndicator() {
        return mSectionIndicator;
    }

    @Override
    public void scrollTo(float scrollProgress, boolean fromTouch) {
        int position = getPositionFromScrollProgress(scrollProgress);
        mRecyclerView.smoothScrollToPosition(position);

        updateSectionIndicator(position, scrollProgress);
    }

    private void updateSectionIndicator(int position, float scrollProgress) {
        if (mSectionIndicator != null) {
            mSectionIndicator.setProgress(scrollProgress);
            if (mRecyclerView.getAdapter() instanceof SectionIndexer) {
                SectionIndexer indexer = ((SectionIndexer) mRecyclerView.getAdapter());
                int section = indexer.getSectionForPosition(position);
                Object[] sections = indexer.getSections();
                mSectionIndicator.setSection(sections[section]);
            }
        }
    }

    private int getPositionFromScrollProgress(float scrollProgress) {
        return (int) (mRecyclerView.getAdapter().getItemCount() * scrollProgress);
    }

    /**
     * Classes that extend AbsFastScroller must implement their own {@link OnScrollListener} to respond to scroll
     * events when the {@link #mRecyclerView} is scrolled NOT using the fast scroller.
     * @return an implementation for responding to scroll events from the {@link #mRecyclerView}
     */
    @NonNull
    public OnScrollListener getOnScrollListener() {
        if (mOnScrollListener == null) {
            mOnScrollListener = new OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    float scrollProgress = 0;
                    ScrollProgressCalculator scrollProgressCalculator = getScrollProgressCalculator();
                    if (scrollProgressCalculator != null) {
                        scrollProgress = scrollProgressCalculator.calculateScrollProgress(recyclerView);
                    }
                    moveHandleToPosition(scrollProgress);
                }
            };
        }
        return mOnScrollListener;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (getScrollProgressCalculator() == null) {
            onCreateScrollProgressCalculator();
        }

        // synchronize the handle position to the RecyclerView
        float scrollProgress = getScrollProgressCalculator().calculateScrollProgress(mRecyclerView);
        moveHandleToPosition(scrollProgress);
    }

    /**
     * Sub classes have to override this method and create the ScrollProgressCalculator instance in this method.
     */
    protected abstract void onCreateScrollProgressCalculator();

    /**
     * Takes a touch event and determines how much scroll progress this translates into
     * @param event touch event received by the layout
     * @return scroll progress, or fraction by which list is scrolled [0 to 1]
     */
    public float getScrollProgress(MotionEvent event) {
        ScrollProgressCalculator scrollProgressCalculator = getScrollProgressCalculator();
        if (scrollProgressCalculator != null) {
            return getScrollProgressCalculator().calculateScrollProgress(event);
        }
        return 0;
    }

    /**
     * Define a layout resource for your implementation of AbsFastScroller
     * Currently must contain a handle view (R.id.scroll_handle) and a bar (R.id.scroll_bar)
     * @return a resource id corresponding to the chosen layout.
     */
    protected abstract int getLayoutResourceId();

    /**
     * Define a ScrollProgressCalculator for your implementation of AbsFastScroller
     * @return a chosen implementation of {@link ScrollProgressCalculator}
     */
    @Nullable
    protected abstract TouchableScrollProgressCalculator getScrollProgressCalculator();

    /**
     * Moves the handle of the scroller by specific progress amount
     * @param scrollProgress fraction by which to move scroller [0 to 1]
     */
    public abstract void moveHandleToPosition(float scrollProgress);

}