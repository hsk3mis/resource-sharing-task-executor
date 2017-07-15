package com.gryglicki.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
/**
 * Unit Tests for {@link ResourceSharingTaskExecutor}
 *
 * @author Michal Gryglicki
 * Created on 03/05/2017.
 */
public class ResourceSharingTaskExecutorSimpleTest
{
    private static String SOME_DISCRIMINATOR = "constantResourceDiscriminator";
    private static String SOME_CONTENT = "X";

    private ResourceSharingTaskExecutor<String, StringBuilder> resourceSharingTaskExecutor;
    private AtomicReference<String> result;
    private AtomicBoolean eventHappened;

    @BeforeEach
    public void setUp() {
        resourceSharingTaskExecutor = new SynchronizedResourceSharingTaskExecutor<>();
        result = new AtomicReference<>(null);
        eventHappened = new AtomicBoolean(false);
    }

    @Test
    public void shouldOpenResourceExecuteTaskAndCloseResource() throws Exception {
        //Given
        TaskOnResource<String, StringBuilder> task = appendToStringBuilderTaskBuilder(SOME_CONTENT, result).build();
        //When
        resourceSharingTaskExecutor.execute(task);
        //Then
        assertEquals(SOME_CONTENT, result.get());
    }

    @Test
    public void shouldThrowNullPointerExceptionOnNullTask() throws Exception {
        //When / Then - Exception
        assertThrows(NullPointerException.class, () -> {
            resourceSharingTaskExecutor.execute(null);
        });
    }

    @Test
    public void shouldPropagateExceptionOnOpeningResourceException() throws Exception {
        //Given
        TaskOnResource task = appendToStringBuilderTaskBuilder(SOME_CONTENT, result)
                        .withOpenResource(() -> { throw new RuntimeException(); })
                        .withExecuteOn(sb -> eventHappened.set(true))
                        .withCloseResource(sb -> eventHappened.set(true))
                        .build();
        //When / Then - Exception
        assertThrows(RuntimeException.class, () -> {
            resourceSharingTaskExecutor.execute(task);
        });
        assertFalse(eventHappened.get());
    }

    @Test
    public void shouldCloseResourceAndPropagateExceptionOnExecutionTaskOnResourceException() throws Exception {
        //Given
        TaskOnResource task = appendToStringBuilderTaskBuilder(SOME_CONTENT, result)
                        .withExecuteOn(sb -> { throw new RuntimeException(); })
                        .withCloseResource(sb -> eventHappened.set(true))
                        .build();
        //When / Then - Exception
        assertThrows(RuntimeException.class, () -> {
            resourceSharingTaskExecutor.execute(task);
        });
        assertTrue(eventHappened.get());
    }

    @Test
    public void shouldPropagateExceptionOnClosingResourceException() throws Exception {
        //Given
        TaskOnResource task = appendToStringBuilderTaskBuilder(SOME_CONTENT, result)
                        .withExecuteOn(sb -> eventHappened.set(true))
                        .withCloseResource(sb -> { throw new RuntimeException(); })
                        .build();
        //When / Then - Exception
        assertThrows(RuntimeException.class, () -> {
            resourceSharingTaskExecutor.execute(task);
        });
        assertTrue(eventHappened.get());
    }

    @Test
    public void shouldPropagateExceptionWithSuppressedOnExecutionTaskAndClosingResourceException() throws Exception {
        //Given
        String executeOnExceptionMessage = "executeOnExceptionMessage";
        String closeResourceExceptionMessage = "closeResourceExceptionMessage";
        TaskOnResource task = appendToStringBuilderTaskBuilder(SOME_CONTENT, result)
                        .withExecuteOn(sb -> { throw new RuntimeException(executeOnExceptionMessage); })
                        .withCloseResource(sb -> { throw new RuntimeException(closeResourceExceptionMessage); })
                        .build();
        //When / Then - Exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            resourceSharingTaskExecutor.execute(task);
        });
        assertEquals(closeResourceExceptionMessage, exception.getMessage());
        assertEquals(executeOnExceptionMessage, exception.getSuppressed()[0].getMessage());
    }

    private TaskOnResourceBuilder<String, StringBuilder> appendToStringBuilderTaskBuilder(String content, AtomicReference<String> result) {
        return TaskOnResourceBuilder.<String, StringBuilder>builderWithDiscriminator(SOME_DISCRIMINATOR)
                        .withOpenResource(StringBuilder::new)
                        .withCloseResource(sb -> result.set(sb.toString()))
                        .withExecuteOn(sb -> sb.append(content));
    }
}
