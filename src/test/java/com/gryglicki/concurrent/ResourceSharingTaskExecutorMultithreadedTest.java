package com.gryglicki.concurrent;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newFixedThreadPool;
/**
 * Tests for {@link SynchronizedResourceSharingTaskExecutor}
 *
 * @author Michal Gryglicki
 * Created on 25/04/2017.
 */
public class ResourceSharingTaskExecutorMultithreadedTest
{

    /**
     * TODO: Add multithreading and delegation to ExecutorService underneath ResourceSharingTaskExecutor
     * TODO: Return from execute() CompletableFuture ????
     * TODO: Tests - without Threads => only simple functionality of opening /closing / writing
     * TODO: Tests - with Threads, with temporary files, ...
     * TODO: Configure testing with https://travis-ci.org/
     * TODO: Exception handling while open / close / executeOn
     * TODO: Option (configurable) to close/reopen resource every X executeOn - to prevent not closing files at all
     */

//    @Test
//    @Tag("multithreading")
    public void testResourceSharingExecutorSeparateFromThreadExecutor() throws Exception {
        ExecutorService executor = newFixedThreadPool(10);
        SynchronizedResourceSharingTaskExecutor<String, FileWriter>
                        resourceSharingTaskExecutor = new SynchronizedResourceSharingTaskExecutor<>();

        TaskOnResource task = fileAppenderTask("C:/dev/test/executors/src/test/resources/file.txt", "test value");

        for (int i=0; i<100; i++) {
            executor.execute(() -> {
                resourceSharingTaskExecutor.execute(task);
            });
        }

        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

//    @Test
//    @Tag("multithreading")
    public void testResourceSharingExecutorSeparateFromThreadExecutorStringBuilder() throws Exception {
        ExecutorService executor = newFixedThreadPool(10);
        SynchronizedResourceSharingTaskExecutor<String, FileWriter>
                        resourceSharingTaskExecutor = new SynchronizedResourceSharingTaskExecutor<>();

        List<String> resultList = Collections.synchronizedList(new LinkedList<>());
        TaskOnResource task = stringBuilderTask("resourceDescription", "X", resultList);

        for (int i=0; i<100; i++) {
            executor.execute(() -> {
                resourceSharingTaskExecutor.execute(task);
            });
        }
        executor.awaitTermination(20, TimeUnit.SECONDS);

        System.out.println(resultList.size());
        String result = resultList.stream().reduce(String::concat).get();
        System.out.println(result.length());

    }

    private TaskOnResource stringBuilderTask(String path, String content, List<String> resultList) {
        return new TaskOnResource<String, StringBuilder>() {
            public String getResourceDiscriminator() {
                return path;
            }

            public StringBuilder openResource() {
                return new StringBuilder();
            }

            public void closeResource(StringBuilder sb) {
                resultList.add(sb.toString());
            }

            public void executeOn(StringBuilder sb) {
                try { Thread.sleep(1); } catch (Exception e) { e.printStackTrace(); }
                sb.append(content);
            }
        };
    }

    private TaskOnResource fileAppenderTask(String path, String content) {
        return new TaskOnResource<String, FileWriter>() {
            public String getResourceDiscriminator() {
                return path;
            }

            public FileWriter openResource() {
                try {
                    return new FileWriter(getResourceDiscriminator(), true);
                } catch (IOException e) { e.printStackTrace(); }
                return null;
            }

            public void closeResource(FileWriter writer) {
                try {
                    writer.close();
                } catch (IOException e) { e.printStackTrace(); }
            }

            public void executeOn(FileWriter writer) {
                try {
                    writer.append(content);
                }
                catch (Exception e) { e.printStackTrace(); }
            }
        };
    }
}
