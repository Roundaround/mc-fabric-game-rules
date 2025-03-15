package me.roundaround.gamerulesmod.common.future;

import java.util.concurrent.Future;

@FunctionalInterface
public interface CancelHandle {
  void cancel();

  static CancelHandle of(Future<?> future) {
    return () -> future.cancel(true);
  }
}
