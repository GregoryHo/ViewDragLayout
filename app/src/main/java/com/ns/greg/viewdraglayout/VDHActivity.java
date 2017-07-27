package com.ns.greg.viewdraglayout;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;

import android.view.View;
import com.ns.greg.library.ViewDragLayout;

/**
 * Created by Gregory on 2017/3/17.
 */
public class VDHActivity extends AppCompatActivity {

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.demo);

    /*--------------------------------
     * Horizontal
     *-------------------------------*/

    final ViewDragLayout horizontalHover = (ViewDragLayout) findViewById(R.id.horizontal_vdh);

    final int dragX = convertDpToPixel(100f, getApplicationContext());

    findViewById(R.id.horizontal_bottom_view).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        horizontalHover.dragSpecificView(R.id.horizontal_hover_view, dragX, 0);
      }
    });

    new ViewDragLayout.Builder(horizontalHover).setLayoutType(ViewDragLayout.HOVER_HORIZONTAL)
        .setSpecificDragDirectionFlag(R.id.horizontal_hover_view,
            ViewDragLayout.LEFT | ViewDragLayout.RIGHT)
        .setSpecificDragX(R.id.horizontal_hover_view, dragX, 0)
        .setSpecificDragX(R.id.horizontal_bottom_view, dragX, 0)
        .chainWith(R.id.horizontal_hover_view, R.id.horizontal_bottom_view)
        .create();

    /*--------------------------------
     * Vertical
     *-------------------------------*/

    final ViewDragLayout verticalHover = (ViewDragLayout) findViewById(R.id.vertical_vdh);

    final int dragY = convertDpToPixel(75f, getApplicationContext());

    findViewById(R.id.vertical_bottom_view).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        verticalHover.dragSpecificView(R.id.vertical_hover_view, 0, dragY);
      }
    });

    new ViewDragLayout.Builder(verticalHover).setLayoutType(ViewDragLayout.HOVER_VERTICAL)
        .setSpecificDragDirectionFlag(R.id.vertical_hover_view,
            ViewDragLayout.TOP | ViewDragLayout.BOTTOM)
        .setSpecificDragEdgeFlag(R.id.vertical_hover_view, ViewDragHelper.EDGE_TOP)
        .setDragY(dragY, 0)
        .chainAll(true)
        .create();
  }

  /**
   * Returns the screen density.
   *
   * 120dpi = 0.75
   * 160dpi = 1 (default)
   * 240dpi = 1.5
   * 320dpi = 2
   * 400dpi = 2.5
   */
  public static float getDensity(Context context) {
    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    return metrics.density;
  }

  /**
   * Coverts dp to px.
   */
  public static int convertDpToPixel(float dp, Context context) {
    return (int) (dp * getDensity(context) + 0.5f);
  }
}
