package com.gryglicki.concurrent;

import java.util.function.Consumer;
import java.util.function.Supplier;
/**
 * Builder for cleaner creation of {@link TaskOnResource} objects.
 *
 * @author Michal Gryglicki
 * Created on 04/05/2017.
 */

public class TaskOnResourceBuilder<RESOURCE_DISCRIMINATOR, RESOURCE>
{
    private final RESOURCE_DISCRIMINATOR resourceDiscriminator;
    private Supplier<RESOURCE> openResourceSupplier;
    private Consumer<RESOURCE> closeResourceConsumer;
    private Consumer<RESOURCE> executeOnConsumer;

    private TaskOnResourceBuilder(RESOURCE_DISCRIMINATOR resourceDiscriminator) {
        this.resourceDiscriminator = resourceDiscriminator;
    }

    /**
     * Creates builder with defined Resource discriminator value.
     * @param resourceDiscriminator Resource discriminator
     * @param <RESOURCE_DISCRIMINATOR> type of discriminator
     * @param <RESOURCE> type of resource
     * @return builder
     */
    public static <RESOURCE_DISCRIMINATOR, RESOURCE> TaskOnResourceBuilder<RESOURCE_DISCRIMINATOR, RESOURCE> builderWithDiscriminator(RESOURCE_DISCRIMINATOR resourceDiscriminator) {
        return new TaskOnResourceBuilder(resourceDiscriminator);
    }

    /**
     * Defines function {@link TaskOnResource#openResource}
     */
    public TaskOnResourceBuilder<RESOURCE_DISCRIMINATOR, RESOURCE> withOpenResource(Supplier<RESOURCE> openResourceSupplier) {
        this.openResourceSupplier = openResourceSupplier;
        return this;
    }

    /**
     * Defines function {@link TaskOnResource#closeResource}
     */
    public TaskOnResourceBuilder<RESOURCE_DISCRIMINATOR, RESOURCE> withCloseResource(Consumer<RESOURCE> closeResourceConsumer) {
        this.closeResourceConsumer = closeResourceConsumer;
        return this;
    }

    /**
     * Defines function {@link TaskOnResource#executeOn}
     */
    public TaskOnResourceBuilder<RESOURCE_DISCRIMINATOR, RESOURCE> withExecuteOn(Consumer<RESOURCE> executeOnConsumer) {
        this.executeOnConsumer = executeOnConsumer;
        return this;
    }

    /**
     * @return built {@link TaskOnResource} object
     */
    public TaskOnResource<RESOURCE_DISCRIMINATOR, RESOURCE> build() {
        return new TaskOnResource<RESOURCE_DISCRIMINATOR, RESOURCE>() {
            @Override
            public RESOURCE_DISCRIMINATOR getResourceDiscriminator() {
                return resourceDiscriminator;
            }

            @Override
            public RESOURCE openResource() {
                return openResourceSupplier.get();
            }

            @Override
            public void closeResource(RESOURCE resource) {
                closeResourceConsumer.accept(resource);
            }

            @Override
            public void executeOn(RESOURCE resource) {
                executeOnConsumer.accept(resource);
            }
        };
    }
}
