package me.roundaround.gamerulesmod.util;

import java.util.concurrent.Future;

@FunctionalInterface
public interface CancelHandle {
  void cancel();

  static CancelHandle of(Future<?> future) {
    return () -> future.cancel(true);
  }
}
