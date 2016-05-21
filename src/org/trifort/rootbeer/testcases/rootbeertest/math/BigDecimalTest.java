/*
 * Copyright 2012 Phil Pratt-Szeliga and other contributors
 * http://chirrup.org/
 *
 * See the file LICENSE for copying permission.
 */

package org.trifort.rootbeer.testcases.rootbeertest.math;

import java.util.ArrayList;
import java.util.List;

import org.trifort.rootbeer.runtime.Kernel;
import org.trifort.rootbeer.test.TestSerialization;

public class BigDecimalTest implements TestSerialization {

  public List<Kernel> create() {
    List<Kernel> ret = new ArrayList<Kernel>();
    for(int i = 0; i < 5; ++i){
      ret.add(new BigDecimalRunOnGpu(i));
    }
    return ret;
  }

  public boolean compare(Kernel original, Kernel from_heap) {
    BigDecimalRunOnGpu lhs = (BigDecimalRunOnGpu) original;
    BigDecimalRunOnGpu rhs = (BigDecimalRunOnGpu) from_heap;
    return lhs.compare(rhs);
  }
}
