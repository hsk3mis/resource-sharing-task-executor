package com.gryglicki.concurrent;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
/**
 * Task executor that allows to share corresponding Resources between concurrent executed tasks.
 * Each task is declaratively defined to know how to open resource, close resource and execute some task on this resource.
 * Task executor just tries to reuse opened resources if there're another tasks waiting to execute on this particular resource.
 * Resources are distinguished by the discriminator value unique for each instance of the resource.
 *
 * @author Michal Gryglicki
 * Created on 25/04/2017.
 */
@ThreadSafe
public class ResourceSharingTaskExecutor<RESOURCE_DISC, RESOURCE> {
    @GuardedBy("this")
    private final Map<RESOURCE_DISC, ResourceWithCounter<RESOURCE>> openResources = new HashMap<>();

    /**
     * Executes give task with this resource sharing executor.
     * Tasks can be executed concurrently.
     * If multiple tasks are accessing same Resource (identified by it's discriminator value)
     * at the same time, then the Resource will be shared between those tasks.
     * Access to the Resource will be synchronized between the tasks.
     * All tasks that operates on the Resource with the same discriminator value
     * should have the consistent Resource opening/closing implementation.
     *
     * All the synchronization happens in this single method.
     * @param task task to be executed
     */
    public void execute(TaskOnResource<RESOURCE_DISC, RESOURCE> task) {
        RESOURCE resource;
        resource = getOrCreateResource(task);
        Optional<RuntimeException> taskExecutionException = executeTaskOnResource(task, resource);
        Optional<RuntimeException> closingResourceException = tryCloseResourceAndHandleException(task);
        throwExceptionIfNeeded(taskExecutionException, closingResourceException);

        //???????????????????????????????????????????????????????
        //?? Exception => propagate to the function further down to deal with exception
    }

    private Optional<RuntimeException> executeTaskOnResource(TaskOnResource<RESOURCE_DISC, RESOURCE> task, RESOURCE resource) {
        try {
            synchronized (resource) {
                task.executeOn(resource);
            }
            return empty();
        } catch (RuntimeException ex) {
            return of(ex);
        }
    }

    private Optional<RuntimeException> tryCloseResourceAndHandleException(TaskOnResource<RESOURCE_DISC, RESOURCE> task) {
        try {
            tryCloseResource(task);
            return empty();
        } catch (RuntimeException ex) {
            return of(ex);
        }
    }

    private void throwExceptionIfNeeded(Optional<RuntimeException> taskExecutionException, Optional<RuntimeException> closingResourceException) {
        if (closingResourceException.isPresent()) {
            if (taskExecutionException.isPresent()) {
                closingResourceException.get().addSuppressed(taskExecutionException.get());
                throw closingResourceException.get();
            } else {
                throw closingResourceException.get();
            }
        } else if (taskExecutionException.isPresent()) {
            throw taskExecutionException.get();
        }
    }


    private synchronized RESOURCE getOrCreateResource(TaskOnResource<RESOURCE_DISC, RESOURCE> task) {
        RESOURCE_DISC resourceDesc = task.getResourceDiscriminator();
        if (openResources.containsKey(resourceDesc)) {
            return openResources.get(resourceDesc).incrementCounterAndGetResource();
        } else {
            RESOURCE resource = task.openResource();
            openResources.put(resourceDesc, new ResourceWithCounter<>(resource));
            return resource;
        }
    }

    private synchronized void tryCloseResource(TaskOnResource<RESOURCE_DISC, RESOURCE> task) {
        RESOURCE_DISC resourceDesc = task.getResourceDiscriminator();
        if (openResources.containsKey(resourceDesc)) {
            ResourceWithCounter<RESOURCE> resourceWithCounter = openResources.get(resourceDesc);
            if (resourceWithCounter.decrementCounterAndCompareToZero()) {
                task.closeResource(resourceWithCounter.getResource());
                openResources.remove(resourceDesc);
            }
        }
    }




    /*public void execute(com.gryglicki.concurrent.TaskOnResource<RESOURCE_DISC, RESOURCE> task) {
        RESOURCE resource;
        synchronized (this) {
            resource = getOrCreateResource(task);
        }

        Optional<RuntimeException> taskExecutionException = empty();
        Optional<RuntimeException> closingResourceException = empty();
        try {
            synchronized (resource) {
                task.executeOn(resource);
            }
        } catch (RuntimeException ex) {
            taskExecutionException = of(ex);
        } finally {
            try {
                synchronized (this) {
                    tryCloseResource(task);
                }
            } catch (RuntimeException ex) {
                closingResourceException = of(ex);
            }
            throwExceptionIfNeeded(taskExecutionException, closingResourceException);

            //???????????????????????????????????????????????????????
            //?? Exception => propagate to the function further down to deal with exception
        }
    }

    private void throwExceptionIfNeeded(Optional<RuntimeException> taskExecutionException, Optional<RuntimeException> closingResourceException) {
        if (closingResourceException.isPresent()) {
            if (taskExecutionException.isPresent()) {
                closingResourceException.get().addSuppressed(taskExecutionException.get());
                throw closingResourceException.get();
            } else {
                throw closingResourceException.get();
            }
        } else if (taskExecutionException.isPresent()) {
            throw taskExecutionException.get();
        }
    }

    private RESOURCE getOrCreateResource(com.gryglicki.concurrent.TaskOnResource<RESOURCE_DISC, RESOURCE> task) {
        RESOURCE_DISC resourceDesc = task.getResourceDiscriminator();
        if (openResources.containsKey(resourceDesc)) {
            return openResources.get(resourceDesc).incrementCounterAndGetResource();
        } else {
            RESOURCE resource = task.openResource();
            openResources.put(resourceDesc, new ResourceWithCounter<>(resource));
            return resource;
        }
    }

    private void tryCloseResource(com.gryglicki.concurrent.TaskOnResource<RESOURCE_DISC, RESOURCE> task) {
        RESOURCE_DISC resourceDesc = task.getResourceDiscriminator();
        if (openResources.containsKey(resourceDesc)) {
            ResourceWithCounter<RESOURCE> resourceWithCounter = openResources.get(resourceDesc);
            if (resourceWithCounter.decrementCounterAndCompareToZero()) {
                task.closeResource(resourceWithCounter.getResource());
                openResources.remove(resourceDesc);
            }
        }
    }*/

    private static class ResourceWithCounter<RESOURCE> {
        @GuardedBy("itself")
        private final RESOURCE resource;
        private int counter;

        public ResourceWithCounter(RESOURCE resource)
        {
            this.resource = resource;
            this.counter = 1;
        }

        public RESOURCE incrementCounterAndGetResource() {
            counter++;
            return resource;
        }

        public boolean decrementCounterAndCompareToZero()
        {
            counter--;
            return counter == 0;
        }

        public RESOURCE getResource() {
            return resource;
        }
    }
}

