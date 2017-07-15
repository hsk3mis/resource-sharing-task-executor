package com.gryglicki.concurrent;

/**
 * Interface of the Task that can be handled by {@link ResourceSharingTaskExecutor}.
 * This task interface separates opening/closing of resources from actually executing action on it.
 *
 * @param <RESOURCE_DISCRIMINATOR> resource unique identifier type
 * @param <RESOURCE> resource type
 * @author Michal Gryglicki
 * Created on 25/04/2017.
 */
public interface TaskOnResource<RESOURCE_DISCRIMINATOR, RESOURCE> {

    /**
     * It must handle hashCode properly because it'll be used as a key in the Map.
     * In example of File resource it can be a path to the file.
     * @return Unique identifier of the resource
     */
    RESOURCE_DISCRIMINATOR getResourceDiscriminator();

    /**
     * This method will be used to open/initialize/create resource in case
     * Resource with the same discriminator value is not already opened by another task
     * @return opened resource
     */
    RESOURCE openResource();

    /**
     * This method will be used to close resource in case
     * some other task is not already waiting for the Resource with the same dicriminator value.
     * @param resource resource to be closed
     */
    void closeResource(RESOURCE resource);

    /**
     * This method is used to define an action to be executed on the defined Resource.
     * com.gryglicki.concurrent.ResourceSharingTaskExecutor will handle opening and closing of this resource
     * @param resource opened resource that you can execute action on
     */
    void executeOn(RESOURCE resource);

}
