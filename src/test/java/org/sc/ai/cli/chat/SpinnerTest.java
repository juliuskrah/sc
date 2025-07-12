package org.sc.ai.cli.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Spinner}.
 * 
 * @author Julius Krah
 */
class SpinnerTest {
    
    private StringWriter stringWriter;
    private PrintWriter printWriter;
    private Spinner spinner;
    
    @BeforeEach
    void setUp() {
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        spinner = new Spinner(printWriter, "Testing");
    }
    
    @Test
    void shouldCreateSpinner() {
        // Then
        assertThat(spinner).isNotNull();
        assertThat(spinner.isRunning()).isFalse();
    }
    
    @Test
    void shouldStartSpinner() {
        // When
        spinner.start();
        
        // Then
        await().atMost(Duration.ofMillis(500))
               .until(spinner::shouldStart);
        
        // Cleanup
        spinner.stop();
    }
    
    @Test
    void shouldStopSpinner() {
        // Given
        spinner.start();
        await().atMost(Duration.ofMillis(500))
               .until(spinner::shouldStart);
        
        // When
        spinner.stop();
        
        // Then
        await().atMost(Duration.ofMillis(500))
               .until(() -> !spinner.shouldStart());
    }
    
    @Test
    void shouldWriteSpinnerCharacters() {
        // When
        spinner.start();
        
        // Then - Wait longer for spinner to actually start due to delay
        await().atMost(Duration.ofMillis(300))
               .until(() -> stringWriter.toString().contains("Testing"));
        
        // Cleanup
        spinner.stop();
    }
    
    @Test
    void shouldNotStartTwice() {
        // Given
        spinner.start();
        await().atMost(Duration.ofMillis(500))
               .until(spinner::shouldStart);
        
        // When - Try to start again
        spinner.start();
        
        // Then - Still only one instance should start
        assertThat(spinner.shouldStart()).isTrue();
        
        // Cleanup
        spinner.stop();
    }
    
    @Test
    void shouldHandleNullMessage() {
        // Given
        var spinnerWithNullMessage = new Spinner(printWriter, null);
        
        // When
        spinnerWithNullMessage.start();
        
        // Then
        await().atMost(Duration.ofMillis(500))
               .until(spinnerWithNullMessage::shouldStart);
        
        // Cleanup
        spinnerWithNullMessage.stop();
    }
}
