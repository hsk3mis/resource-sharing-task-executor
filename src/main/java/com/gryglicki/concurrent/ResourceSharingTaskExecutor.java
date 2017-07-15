package com.gryglicki.concurrent;

/**
 * Task executor that allows to share corresponding Resources between concurrent executed tasks.
 * Each task is declaratively defined to know how to open resource, close resource and execute some task on this resource.
 * Task executor just tries to reuse opened resources if there're another tasks waiting to execute on this particular resource.
 * Resources are distinguished by the discriminator value unique for each instance of the resource.
 *
 * @author Michal Gryglicki
 * Created on 15/07/2017.
 */
public interface ResourceSharingTaskExecutor<RESOURCE_DISC, RESOURCE> {
    void execute(TaskOnResource<RESOURCE_DISC, RESOURCE> task);
}
