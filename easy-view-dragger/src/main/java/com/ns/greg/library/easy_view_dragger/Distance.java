package com.ns.greg.library.easy_view_dragger;

/**
 * Created by Gregory on 2017/3/21.
 */

class Distance {

  private int startPoint;
  private int dragMin;
  private int dragMax;
  private int min;
  private int max;

  Distance(int startPoint, int dragMin, int dragMax) {
    this.startPoint = startPoint;
    this.dragMin = dragMin;
    this.dragMax = dragMax;
    this.min = startPoint - dragMin;
    this.max = startPoint + dragMax;
  }

  int getStartPoint() {
    return startPoint;
  }

  int getDragMin() {
    return dragMin;
  }

  int getDragMax() {
    return dragMax;
  }

  int getMin() {
    return min;
  }

  int getMax() {
    return max;
  }

  @Override public String toString() {
    return "Min : " + min + ", Max : " + max;
  }
}
