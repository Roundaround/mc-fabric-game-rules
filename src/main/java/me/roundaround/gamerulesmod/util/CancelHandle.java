package me.roundaround.gamerulesmod.util;

import java.util.concurrent.Future;

@FunctionalInterface
public interface CancelHandle {
  boolean cancel();

  static CancelHandle of(Future<?> future) {
    return () -> future.cancel(true);
  }

  static CancelHandle of(Future<?> future, Callback callback) {
    return () -> future.cancel(true);
  }
}
