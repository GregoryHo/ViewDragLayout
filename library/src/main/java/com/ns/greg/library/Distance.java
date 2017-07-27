package com.ns.greg.library;

/**
 * Created by Gregory on 2017/3/21.
 */

class Distance {

  private int dragMin;

  private int dragMax;

  private int min;

  private int max;

  Distance(int startPoint, int dragMin, int dragMax) {
    this.dragMin = dragMin;
    this.dragMax = dragMax;
    this.min = startPoint - dragMin;
    this.max = startPoint + dragMax;
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
