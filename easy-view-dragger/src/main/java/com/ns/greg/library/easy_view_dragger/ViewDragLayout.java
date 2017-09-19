package com.ns.greg.library.easy_view_dragger;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gregory on 2017/3/17.
 */
public class ViewDragLayout extends FrameLayout {

  private static final String TAG = "VDHLayout";

  private static final Boolean DEBUG = false;

  /*--------------------------------
   * Type definitions
   *-------------------------------*/

  public static final int HOVER_FRAME_OVERLAY = 0;

  public static final int HOVER_LINEAR_HORIZONTAL = 1;

  public static final int HOVER_LINEAR_VERTICAL = 2;

  @IntDef({ HOVER_FRAME_OVERLAY, HOVER_LINEAR_HORIZONTAL, HOVER_LINEAR_VERTICAL }) @Retention(RetentionPolicy.SOURCE)
  public @interface HoverMode {

  }

  /*--------------------------------
   * Direction definitions
   *-------------------------------*/

  public static final int LEFT = 1;
  public static final int TOP = 1 << 1;
  public static final int RIGHT = 1 << 2;
  public static final int BOTTOM = 1 << 3;
  public static final int DIRECTION_ALL = LEFT | TOP | RIGHT | BOTTOM;

  @IntDef(flag = true, value = { LEFT, TOP, RIGHT, BOTTOM, DIRECTION_ALL })
  @Retention(RetentionPolicy.SOURCE) public @interface DragFlag {

  }

  /*--------------------------------
   * Drag Threshold
   *-------------------------------*/

  private static final int VELOCITY_THRESHOLD = 50;

  /*--------------------------------
   * General declaration
   *-------------------------------*/

  private final SparseArray<View> childViews = new SparseArray<>();
  private final SparseIntArray dragFlags = new SparseIntArray();
  private final SparseArray<Distance> dragDistanceXs = new SparseArray<>();
  private final SparseArray<Distance> dragDistanceYs = new SparseArray<>();
  private final SparseIntArray edgeViews = new SparseIntArray();
  private final SparseArray<List<View>> hookList = new SparseArray<>();
  private ViewDragHelper viewDragHelper;
  private boolean vdhEnable = true;
  private @HoverMode int layoutType = HOVER_FRAME_OVERLAY;
  private boolean chainEnable = false;
  private int edgeFlag = 0;
  private boolean pullEnable = false;
  private float speedFactor = 1.0f;

  /*--------------------------------
   * Constructors
   *-------------------------------*/

  public ViewDragLayout(Context context) {
    this(context, null);
  }

  public ViewDragLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ViewDragLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  /*********************************
   * Functions
   *********************************/

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    viewDragHelper = ViewDragHelper.create(this, 1.0f, new CustomViewDragHelperCallback(this));
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // Edge tracking
    viewDragHelper.setEdgeTrackingEnabled(edgeFlag);

    // Save child views
    int childCount = getChildCount();
    childViews.clear();
    for (int i = 0; i < childCount; i++) {
      View view = getChildAt(i);
      childViews.put(view.getId(), view);
    }

    measureChildren(widthMeasureSpec, heightMeasureSpec);
    setMeasuredDimensionType(layoutType, widthMeasureSpec, heightMeasureSpec, childCount);
  }

  /**
   * Measure view group with current layout type
   *
   * @param layoutType layout type {@link #HOVER_FRAME_OVERLAY}, {@link #HOVER_LINEAR_HORIZONTAL}, {@link
   * #HOVER_LINEAR_VERTICAL}
   * @param widthMeasureSpec measure spec
   * @param heightMeasureSpec measure spec
   * @param childCount group child count
   */
  private void setMeasuredDimensionType(@HoverMode int layoutType, int widthMeasureSpec,
      int heightMeasureSpec, int childCount) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);

    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    int offsetLeftAndRight = getPaddingLeft() + getPaddingRight();
    int offsetTopAndBottom = getPaddingTop() + getPaddingBottom();
    if (DEBUG) {
      Log.d(TAG, "measure - "
          + layoutType
          + " offsetLeftAndRight = ["
          + offsetLeftAndRight
          + ", offsetTopAndBottom = ["
          + offsetTopAndBottom
          + "]");
    }

    int width = 0;
    int height = 0;

    if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
      width = widthSize;
      height = heightSize;
    } else {
      for (int index = 0; index < childCount; index++) {
        View child = childViews.valueAt(index);

        int measureWidth = child.getMeasuredWidth();
        int measureHeight = child.getMeasuredHeight();

        switch (layoutType) {
          case HOVER_FRAME_OVERLAY:
            width = width < measureWidth ? measureWidth : width;
            height = height < measureHeight ? measureHeight : height;
            break;

          case HOVER_LINEAR_HORIZONTAL:
            width += measureWidth;
            height = height < measureHeight ? measureHeight : height;
            break;

          case HOVER_LINEAR_VERTICAL:
            width = width < measureWidth ? measureWidth : width;
            height += measureHeight;
            break;

          default:
            break;
        }
      }

      if (widthMode == MeasureSpec.EXACTLY) {
        width = widthSize;
      } else {
        width = Math.min(width + offsetLeftAndRight, widthSize);
      }

      if (heightMode == MeasureSpec.EXACTLY) {
        height = heightSize;
      } else {
        height = Math.min(height + offsetTopAndBottom, heightSize);
      }
    }

    if (DEBUG) {
      Log.d(TAG, "measure - " + layoutType + ", width = [" + width + ", height = [" + height + "]");
    }

    setMeasuredDimension(width, height);
  }

  @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (DEBUG) {
      Log.d(TAG, "onLayout - "
          + "changed = ["
          + changed
          + "], l = ["
          + l
          + "], t = ["
          + t
          + "], r = ["
          + r
          + "], b = ["
          + b
          + "]"
          + ", type = ["
          + layoutType
          + "]");
    }

    int childCount = childViews.size();
    if (childCount > 0) {
      switch (layoutType) {
        case HOVER_FRAME_OVERLAY:
          layoutOverlap(l, t, r, b, childCount);
          break;

        case HOVER_LINEAR_HORIZONTAL:
          layoutHorizontal(t, b, childCount);
          break;

        case HOVER_LINEAR_VERTICAL:
          layoutVertical(l, r, childCount);
          break;

        default:
          break;
      }
    }
  }

  /**
   * Build child layout as overlap
   *
   * @param l parent left
   * @param t parent top
   * @param r parent right
   * @param b parent bottom
   * @param childCount child count
   */
  private void layoutOverlap(int l, int t, int r, int b, int childCount) {
    for (int index = 0; index < childCount; index++) {
      View child = childViews.valueAt(index);
      int childWidth = child.getMeasuredWidth();
      int childHeight = child.getMeasuredHeight();
      int left = r - l - childWidth;
      int top = b - t - childHeight;

      if (left > 0) {
        left = (r - l) / 2 - (childWidth / 2);
      }

      if (top > 0) {
        top = (b - t) / 2 - (childHeight / 2);
      }

      child.layout(left, top, left + childWidth, top + childHeight);
    }
  }

  /**
   * Build child layout as horizontal
   *
   * @param t parent top
   * @param b parent bottom
   * @param childCount child count
   */
  private void layoutHorizontal(int t, int b, int childCount) {
    boolean isInit = childViews.valueAt(0).getLeft() == 0;
    int offset = 0;

    for (int index = 0; index < childCount; index++) {
      View child = childViews.valueAt(index);
      int childWidth = child.getMeasuredWidth();
      int childHeight = child.getMeasuredHeight();
      int top = b - t - childHeight;

      if (top > 0) {
        top = (b - t) / 2 - (childHeight / 2);
      }

      if (isInit) {
        child.layout(0, top, childWidth, top + childHeight);
        if (index > 0) {
          child.offsetLeftAndRight(offset);
        }

        offset += childWidth;
      } else {
        child.layout(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
      }
    }
  }

  /**
   * Build child layout as vertical
   *
   * @param l parent left
   * @param r parent right
   * @param childCount child count
   */
  private void layoutVertical(int l, int r, int childCount) {
    boolean isInit = childViews.valueAt(0).getTop() == 0;
    int offset = 0;

    for (int index = 0; index < childCount; index++) {
      View child = childViews.valueAt(index);
      int childWidth = child.getMeasuredWidth();
      int childHeight = child.getMeasuredHeight();
      int left = r - l - childWidth;

      if (left > 0) {
        left = (r - l) / 2 - (childWidth / 2);
      }

      if (isInit) {
        child.layout(left, 0, left + childWidth, childHeight);
        if (index > 0) {
          child.offsetTopAndBottom(offset);
        }

        offset += childHeight;
      } else {
        child.layout(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
      }
    }
  }

  @Override public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (vdhEnable) {
      final int action = MotionEventCompat.getActionMasked(ev);
      if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
        viewDragHelper.cancel();

        return false;
      }

      return viewDragHelper.shouldInterceptTouchEvent(ev);
    }

    return super.onInterceptTouchEvent(ev);
  }

  @Override public boolean onTouchEvent(MotionEvent event) {
    if (vdhEnable) {
      try {
        viewDragHelper.processTouchEvent(event);
      } catch (Exception ex) {
        ex.printStackTrace();
        return false;
      }

      return true;
    }

    return super.onTouchEvent(event);
  }

  @Override public void computeScroll() {
    if (viewDragHelper.continueSettling(true)) {
      ViewCompat.postInvalidateOnAnimation(this);
    }
  }

  public ViewDragHelper getViewDragHelper() {
    return viewDragHelper;
  }

  /**
   * Sets VDH enable status
   *
   * @param dragEnable true enable, false otherwise
   */
  public void setVDHEnable(boolean dragEnable) {
    this.vdhEnable = dragEnable;
  }

  /**
   * Drags specific view.
   *
   * @param childId specific view id
   * @param x drag distance in x-axis
   * @param y drag distance int y-axis
   */
  public void dragSpecificView(int childId, int x, int y) {
    View hoverView = childViews.get(childId);
    if (hoverView != null) {
      if (viewDragHelper.smoothSlideViewTo(hoverView, hoverView.getLeft() + x,
          hoverView.getTop() + y)) {
        ViewCompat.postInvalidateOnAnimation(this);
      }
    }
  }

  /**
   * Sets view group layout type since we can't extends other layout
   *
   * @param layoutType layout type {@link #HOVER_FRAME_OVERLAY}, {@link #HOVER_LINEAR_HORIZONTAL}, {@link
   * #HOVER_LINEAR_VERTICAL}
   */
  private void setLayoutType(@HoverMode int layoutType) {
    this.layoutType = layoutType;
  }

  /**
   * Sets all child's drag direction flag
   *
   * @param dragDirectionFlag drag flag
   */
  private void setDragDirectionFlag(@DragFlag final int dragDirectionFlag) {
    addOnLayoutChangeListener(new OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
          int oldTop, int oldRight, int oldBottom) {
        dragFlags.clear();

        int size = childViews.size();
        for (int i = 0; i < size; i++) {
          int childId = childViews.keyAt(i);
          dragFlags.put(childId, dragDirectionFlag);
        }

        removeOnLayoutChangeListener(this);
      }
    });
  }

  /**
   * Sets specific child's drag direction flag
   *
   * @param childId specific child id
   * @param dragDirectionFlag drag flag
   */
  private void setSpecificDragDirectionFlag(@IdRes final int childId,
      @DragFlag final int dragDirectionFlag) {
    addOnLayoutChangeListener(new OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
          int oldTop, int oldRight, int oldBottom) {
        View child = childViews.get(childId);
        if (child != null) {
          dragFlags.put(childId, dragDirectionFlag);
        }

        removeOnLayoutChangeListener(this);
      }
    });
  }

  /**
   * Sets all child's drag distance x
   *
   * @param leftX left distance
   * @param rightX right distance
   */
  private void setDragX(final int leftX, final int rightX) {
    addOnLayoutChangeListener(new OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
          int oldTop, int oldRight, int oldBottom) {
        dragDistanceXs.clear();
        int size = childViews.size();
        for (int i = 0; i < size; i++) {
          View child = childViews.valueAt(i);

          // Only the child has drag flag can set drag x
          int flag = dragFlags.get(child.getId());
          if (flag > 0) {
            dragDistanceXs.put(child.getId(), new Distance(child.getLeft(), leftX, rightX));
          }
        }

        removeOnLayoutChangeListener(this);
      }
    });
  }

  /**
   * Sets specific child's drag distance x
   *
   * @param childId specific child id
   * @param leftX left distance
   * @param rightX right distance
   */
  private void setSpecificDragX(@IdRes final int childId, final int leftX, final int rightX) {
    addOnLayoutChangeListener(new OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
          int oldTop, int oldRight, int oldBottom) {
        View child = childViews.get(childId);
        if (child != null) {
          dragDistanceXs.put(childId, new Distance(child.getLeft(), leftX, rightX));
        }

        removeOnLayoutChangeListener(this);
      }
    });
  }

  /**
   * Sets all child's drag distance y
   *
   * @param topY top distance
   * @param bottomY bottom distance
   */
  private void setDragY(final int topY, final int bottomY) {
    addOnLayoutChangeListener(new OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
          int oldTop, int oldRight, int oldBottom) {
        dragDistanceYs.clear();
        int size = childViews.size();
        for (int i = 0; i < size; i++) {
          View child = childViews.valueAt(i);
          dragDistanceYs.put(child.getId(), new Distance(child.getTop(), topY, bottomY));
        }

        removeOnLayoutChangeListener(this);
      }
    });
  }

  /**
   * Sets specific child's drag distance y
   *
   * @param childId specific child id
   * @param topY top distance
   * @param bottomY bottom distance
   */
  private void setSpecificDragY(@IdRes final int childId, final int topY, final int bottomY) {
    addOnLayoutChangeListener(new OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
          int oldTop, int oldRight, int oldBottom) {
        View child = childViews.get(childId);
        if (child != null) {
          dragDistanceYs.put(childId, new Distance(child.getTop(), topY, bottomY));
        }

        removeOnLayoutChangeListener(this);
      }
    });
  }

  /**
   * Sets edge tracking flag to drag specific child.
   *
   * @param dragChildId specific child id
   * @param edgeFlag tracking flag
   */
  private void setSpecificDragEdgeFlag(@IdRes final int dragChildId, final int edgeFlag) {
    this.edgeFlag = edgeFlag;
    addOnLayoutChangeListener(new OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
          int oldTop, int oldRight, int oldBottom) {
        View child = childViews.get(dragChildId);
        if (child != null) {
          edgeViews.put(dragChildId, edgeFlag);
        }

        removeOnLayoutChangeListener(this);
      }
    });
  }

  /**
   * Drag view as chain
   *
   * @param chainEnable true enable, false otherwise.
   */
  private void asChain(boolean chainEnable) {
    this.chainEnable = chainEnable;
  }

  /**
   * Generate hook list that Target view hooks chain id
   *
   * @param targetId target view that owns hook
   * @param hookId view which be hooked
   */
  private void hookWithSpecificView(final int targetId, final int[] hookId) {
    addOnLayoutChangeListener(new OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
          int oldTop, int oldRight, int oldBottom) {
        View child = childViews.get(targetId);
        if (child != null) {
          List<View> hooks = new ArrayList<>();
          for (int id : hookId) {
            View view = childViews.get(id);
            if (view != null) {
              hooks.add(view);
            }
          }

          hookList.put(targetId, hooks);
        }

        removeOnLayoutChangeListener(this);
      }
    });
  }

  /**
   * Enable the pull action
   *
   * @param pullEnable true enable, false disable
   */
  private void asPull(boolean pullEnable) {
    this.pullEnable = pullEnable;
  }

  /**
   * Sets the factor of speed
   *
   * @param speedFactor factor
   */
  private void setSpeedFactor(float speedFactor) {
    this.speedFactor = speedFactor;
  }

  /**
   * VDH callback
   */
  private static class CustomViewDragHelperCallback extends ViewDragHelper.Callback {

    private final ViewDragLayout instance;

    CustomViewDragHelperCallback(ViewDragLayout reference) {
      WeakReference<ViewDragLayout> weakReference = new WeakReference<>(reference);
      instance = weakReference.get();
    }

    @Override public void onEdgeDragStarted(int edgeFlags, int pointerId) {
      int size = instance.edgeViews.size();
      for (int i = 0; i < size; i++) {
        if (instance.edgeViews.valueAt(i) == edgeFlags) {
          int childId = instance.edgeViews.keyAt(i);

          View edgeChild = instance.childViews.get(childId);
          if (edgeChild != null) {
            instance.viewDragHelper.captureChildView(edgeChild, pointerId);
          }
        }
      }
    }

    @Override public boolean tryCaptureView(View child, int pointerId) {
      int flag = instance.dragFlags.get(child.getId());
      return flag > 0;
    }

    @Override public int clampViewPositionHorizontal(View child, int left, int dx) {
      int flag = instance.dragFlags.get(child.getId()) & (LEFT | RIGHT);
      Distance distanceX = instance.dragDistanceXs.get(child.getId());
      List<View> hooks = instance.hookList.get(child.getId());
      int adjustedDx = (int) (dx * instance.speedFactor);
      switch (flag) {
        case LEFT:
          if (dx < 0) {
            if (hooks != null) {
              for (View hooked : hooks) {
                Distance hookX = instance.dragDistanceXs.get(hooked.getId());
                if (hookX != null) {
                  if (hooked.getLeft() + adjustedDx >= hookX.getMin()) {
                    hooked.offsetLeftAndRight(adjustedDx);
                  }
                }
              }
            }

            if (distanceX != null) {
              if (left >= distanceX.getMin()) {
                return child.getLeft() + adjustedDx;
              }
            }
          }

          break;

        case RIGHT:
          if (dx > 0) {
            if (hooks != null) {
              for (View hooked : hooks) {
                Distance hookX = instance.dragDistanceXs.get(hooked.getId());
                if (hookX != null) {
                  if (hooked.getLeft() - adjustedDx <= hookX.getMax()) {
                    hooked.offsetLeftAndRight(-adjustedDx);
                  }
                }
              }
            }

            if (distanceX != null) {
              if (left <= distanceX.getMax()) {
                return child.getLeft() + adjustedDx;
              }
            }
          }

          break;

        case (LEFT | RIGHT):
          if (hooks != null) {
            for (View hooked : hooks) {
              Distance hookX = instance.dragDistanceXs.get(hooked.getId());
              if (hookX != null) {
                if (hooked.getLeft() + adjustedDx >= hookX.getMin()) {
                  hooked.offsetLeftAndRight(adjustedDx);
                } else if (hooked.getLeft() - adjustedDx <= hookX.getMax()) {
                  hooked.offsetLeftAndRight(-adjustedDx);
                }
              }
            }
          }

          if (distanceX != null) {
            if (left >= distanceX.getMin() && left <= distanceX.getMax()) {
              return child.getLeft() + adjustedDx;
            }
          }

          break;

        default:
          break;
      }

      return (int) child.getX();
    }

    @Override public int clampViewPositionVertical(View child, int top, int dy) {
      int flag = instance.dragFlags.get(child.getId()) & (TOP | BOTTOM);
      Distance distanceY = instance.dragDistanceYs.get(child.getId());
      int adjustedDy = (int) (dy * instance.speedFactor);
      switch (flag) {
        case TOP:
          if (dy < 0) {
            if (distanceY != null) {
              if (top >= distanceY.getMin()) {
                return child.getTop() + adjustedDy;
              }
            }
          }

          break;

        case BOTTOM:
          if (dy > 0) {
            if (distanceY != null) {
              if (top <= distanceY.getMax()) {
                return child.getTop() + adjustedDy;
              }
            }
          }

          break;

        case (TOP | BOTTOM):
          if (distanceY != null) {
            if (top >= distanceY.getMin() && top <= distanceY.getMax()) {
              return child.getTop() + adjustedDy;
            }
          }

          break;

        default:
          break;
      }

      return (int) child.getY();
    }

    @Override public int getViewVerticalDragRange(View child) {
      return 1;
    }

    @Override
    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
      if (instance.chainEnable) {
        switch (instance.layoutType) {
          case HOVER_FRAME_OVERLAY:
            onHorizontalViewPositionChanged(changedView, dx);
            onVerticalViewPositionChanged(changedView, dy);
            break;

          case HOVER_LINEAR_HORIZONTAL:
            onHorizontalViewPositionChanged(changedView, dx);
            break;

          case HOVER_LINEAR_VERTICAL:
            onVerticalViewPositionChanged(changedView, dy);
            break;

          default:
            break;
        }
      }
    }

    private void onHorizontalViewPositionChanged(View changedView, int dx) {
      int size = instance.childViews.size();
      for (int i = 0; i < size; i++) {
        View view = instance.childViews.valueAt(i);
        if (!view.equals(changedView)) {
          view.offsetLeftAndRight(dx);
        }
      }

      ViewCompat.postInvalidateOnAnimation(instance);
    }

    private void onVerticalViewPositionChanged(View changedView, int dy) {
      int size = instance.childViews.size();
      for (int i = 0; i < size; i++) {
        View view = instance.childViews.valueAt(i);
        if (!view.equals(changedView)) {
          view.offsetTopAndBottom(dy);
        }
      }

      ViewCompat.postInvalidateOnAnimation(instance);
    }

    @Override public void onViewReleased(View releasedChild, float xvel, float yvel) {
      switch (instance.layoutType) {
        case HOVER_FRAME_OVERLAY:
          if (instance.pullEnable) {
            pulledAnimation(releasedChild);
          } else {
            releasedAnimation(releasedChild, xvel, yvel);
          }
          break;

        case HOVER_LINEAR_HORIZONTAL:
          releasedHorizontalAnimation(releasedChild, xvel);
          break;

        case HOVER_LINEAR_VERTICAL:
          releasedVerticalAnimation(releasedChild, yvel);
          break;

        default:
          break;
      }
    }

    private void pulledAnimation(View releasedChild) {
      Distance x = instance.dragDistanceXs.get(releasedChild.getId());
      Distance y = instance.dragDistanceYs.get(releasedChild.getId());
      int left = x == null ? releasedChild.getLeft() : x.getMin();
      int top = y == null ? releasedChild.getTop() : y.getMin();
      if (instance.viewDragHelper.smoothSlideViewTo(releasedChild, left, top)) {
        ViewCompat.postInvalidateOnAnimation(instance);
      }
    }

    private void releasedAnimation(View releasedChild, float xvel, float yvel) {
      int left = releasedChild.getLeft();
      int top = releasedChild.getTop();

      Distance x = instance.dragDistanceXs.get(releasedChild.getId());
      if (x != null) {
        int distanceThreshold = x.getMax() + x.getMin() / 2;
        if (xvel < -VELOCITY_THRESHOLD || releasedChild.getLeft() < distanceThreshold) {
          left = x.getMin();
        } else if (xvel > VELOCITY_THRESHOLD || releasedChild.getLeft() > distanceThreshold) {
          left = x.getMax();
        }
      }

      Distance y = instance.dragDistanceYs.get(releasedChild.getId());
      if (y != null) {
        top = yvel < 0 ? y.getMin() : y.getMax();
      }

      if (instance.viewDragHelper.smoothSlideViewTo(releasedChild, left, top)) {
        ViewCompat.postInvalidateOnAnimation(instance);
      }
    }

    private void releasedHorizontalAnimation(View releasedChild, float xvel) {
      Distance x = instance.dragDistanceXs.get(releasedChild.getId());
      int left = releasedChild.getLeft();
      if (x != null) {
        int distanceThreshold = (x.getMax() + x.getMin()) / 2;
        if (xvel < -VELOCITY_THRESHOLD || releasedChild.getLeft() <= distanceThreshold) {
          left = x.getMin();
        } else if (xvel > VELOCITY_THRESHOLD || releasedChild.getLeft() > distanceThreshold) {
          left = x.getMax();
        }

        if (instance.viewDragHelper.smoothSlideViewTo(releasedChild, left,
            releasedChild.getTop())) {
          ViewCompat.postInvalidateOnAnimation(instance);
        }
      }

      List<View> hooks = instance.hookList.get(releasedChild.getId());
      if (hooks != null) {
        for (View hooked : hooks) {
          int hookedLeft = hooked.getLeft();
          Distance hookedX = instance.dragDistanceXs.get(hooked.getId());
          if (hookedX != null) {
            int distanceThreshold = (hookedX.getMax() + hookedX.getMin()) / 2;
            if (xvel < -VELOCITY_THRESHOLD || hooked.getLeft() <= distanceThreshold) {
              hookedLeft = hookedX.getMin();
            } else if (xvel > VELOCITY_THRESHOLD || hooked.getLeft() > distanceThreshold) {
              hookedLeft = hookedX.getMax();
            }

            if (instance.viewDragHelper.smoothSlideViewTo(hooked, hookedLeft, hooked.getTop())) {
              ViewCompat.postInvalidateOnAnimation(instance);
            }
          }
        }
      }
    }

    private void releasedVerticalAnimation(View releasedChild, float yvel) {
      Distance y = instance.dragDistanceYs.get(releasedChild.getId());
      if (y != null) {
        int top = yvel < 0 ? y.getMin() : y.getMax();

        if (instance.viewDragHelper.smoothSlideViewTo(releasedChild, releasedChild.getLeft(),
            top)) {
          ViewCompat.postInvalidateOnAnimation(instance);
        }
      }
    }
  }

  public static final class Builder {

    private final ViewDragLayout instance;

    public Builder(ViewDragLayout reference) {
      WeakReference<ViewDragLayout> weakReference = new WeakReference<>(reference);
      instance = weakReference.get();
    }

    /**
     * Sets view group layout type since we can't extends other layout
     *
     * @param layoutType layout type {@link #HOVER_FRAME_OVERLAY}, {@link #HOVER_LINEAR_HORIZONTAL}, {@link
     * #HOVER_LINEAR_VERTICAL}
     */
    public Builder setLayoutType(@HoverMode int layoutType) {
      instance.setLayoutType(layoutType);
      return this;
    }

    /**
     * Sets all child's drag direction flag
     *
     * @param dragDirectionFlag drag flag
     */
    public Builder setDragDirectionFlag(@DragFlag int dragDirectionFlag) {
      instance.setDragDirectionFlag(dragDirectionFlag);
      return this;
    }

    /**
     * Sets specific child's drag direction flag
     *
     * @param childId specific child id
     * @param dragDirectionFlag drag flag
     */
    public Builder setSpecificDragDirectionFlag(@IdRes int childId,
        @DragFlag int dragDirectionFlag) {
      instance.setSpecificDragDirectionFlag(childId, dragDirectionFlag);
      return this;
    }

    /**
     * Sets edge tracking flag to drag specific child.
     *
     * @param dragChildId specific child id
     * @param edgeFlag tracking flag
     */
    public Builder setSpecificDragEdgeFlag(@IdRes int dragChildId, int edgeFlag) {
      instance.setSpecificDragEdgeFlag(dragChildId, edgeFlag);
      return this;
    }

    /**
     * Sets all child's drag distance x
     *
     * @param leftX left distance
     * @param rightX left distance
     */
    public Builder setDragX(int leftX, int rightX) {
      instance.setDragX(leftX, rightX);
      return this;
    }

    /**
     * Sets specific child's drag distance x
     *
     * @param childId specific child id
     * @param leftX left distance
     * @param rightX right distance
     */
    public Builder setSpecificDragX(@IdRes int childId, int leftX, int rightX) {
      instance.setSpecificDragX(childId, leftX, rightX);
      return this;
    }

    /**
     * Sets all child's drag distance y
     *
     * @param topY top distance
     * @param bottomY bottom distance
     */
    public Builder setDragY(int topY, int bottomY) {
      instance.setDragY(topY, bottomY);
      return this;
    }

    /**
     * Sets specific child's drag distance y
     *
     * @param childId specific child id
     * @param topY top distance
     * @param bottomY bottom distance
     */
    public Builder setSpecificDragY(@IdRes int childId, int topY, int bottomY) {
      instance.setSpecificDragY(childId, topY, bottomY);
      return this;
    }

    /**
     * Drags the layout as chain
     * [NOTICED] this only work at linear mode
     */
    public Builder asChain() {
      instance.asChain(true);
      return this;
    }

    /**
     * Chained view together while drag
     *
     * @param targetId target root view
     * @param chainId the view you want to asChain together
     */
    public Builder hookWith(@IdRes int targetId, int... chainId) {
      instance.hookWithSpecificView(targetId, chainId);
      return this;
    }

    /**
     * No drags, just pull
     */
    public Builder asPull() {
      instance.asPull(true);
      return this;
    }

    public Builder speedFactor(float speedFactor) {
      instance.setSpeedFactor(speedFactor);
      return this;
    }

    /**
     * Must be called when you create a layout options
     */
    public void create() {
      instance.requestLayout();
      instance.invalidate();
    }
  }
}

