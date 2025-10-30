package io.brushforge.brushforge.data.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.brushforge.brushforge.core.common.CoroutineDispatchers;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class DispatcherModule_ProvideDispatchersFactory implements Factory<CoroutineDispatchers> {
  @Override
  public CoroutineDispatchers get() {
    return provideDispatchers();
  }

  public static DispatcherModule_ProvideDispatchersFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CoroutineDispatchers provideDispatchers() {
    return Preconditions.checkNotNullFromProvides(DispatcherModule.INSTANCE.provideDispatchers());
  }

  private static final class InstanceHolder {
    private static final DispatcherModule_ProvideDispatchersFactory INSTANCE = new DispatcherModule_ProvideDispatchersFactory();
  }
}
