package com.bluelinelabs.conductor;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public class ControllerLifecycleOwner implements LifecycleOwner {

    private final LifecycleRegistry lifecycleRegistry;

    public <T extends Controller & LifecycleOwner> ControllerLifecycleOwner(@NonNull T lifecycleController) {
        lifecycleRegistry = new LifecycleRegistry(lifecycleController); // --> State.INITIALIZED

        lifecycleController.addLifecycleListener(new Controller.LifecycleListener() {
            @Override
            public void postContextAvailable(@NonNull Controller controller, @NonNull Context context) {
                lifecycleRegistry.handleLifecycleEvent(Event.ON_CREATE); // --> State.CREATED;
            }

            @Override
            public void postCreateView(@NonNull Controller controller, @NonNull View view) {
                lifecycleRegistry.handleLifecycleEvent(Event.ON_START); // --> State.STARTED;
            }

            @Override
            public void postAttach(@NonNull Controller controller, @NonNull View view) {
                lifecycleRegistry.handleLifecycleEvent(Event.ON_RESUME); // --> State.RESUMED;
            }

            @Override
            public void preDetach(@NonNull Controller controller, @NonNull View view) {
                lifecycleRegistry.handleLifecycleEvent(Event.ON_PAUSE); // --> State.STARTED;
            }

            @Override
            public void preDestroyView(@NonNull Controller controller, @NonNull View view) {
                lifecycleRegistry.handleLifecycleEvent(Event.ON_STOP); // --> State.CREATED;
            }

            @Override
            public void preContextUnavailable(@NonNull Controller controller, @NonNull Context context) {
                // do nothing
            }

            @Override
            public void preDestroy(@NonNull Controller controller) {
                lifecycleRegistry.handleLifecycleEvent(Event.ON_DESTROY); // --> State.DESTROYED;
            }

        });
    }

    @Override @NonNull
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

}
