package com.ns.greg.library;

/**
 * Created by Gregory on 2017/3/21.
 */

class Distance {

  private int min;

  private int max;

  Distance(int min, int max) {
    this.min = min;
    this.max = max;
  }

  int getMin() {
    return min;
  }

  void setMin(int min) {
    this.min = min;
  }

  int getMax() {
    return max;
  }

  void setMax(int max) {
    this.max = max;
  }

  @Override public String toString() {
    return "Min : " + min + ", Max : " + max;
  }
}
