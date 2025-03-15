package me.roundaround.gamerulesmod.common.future;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class CancellableFuture<T> extends CompletableFuture<T> {
  private final Supplier<Boolean> onCancel;

  public CancellableFuture(Supplier<Boolean> onCancel) {
    this.onCancel = onCancel;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return super.cancel(mayInterruptIfRunning) && this.onCancel.get();
  }
}
