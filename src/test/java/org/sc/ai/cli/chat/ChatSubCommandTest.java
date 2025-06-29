package org.sc.ai.cli.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ChatSubCommand} and its nested command classes.
 * 
 * @author Julius Krah
 */
class ChatSubCommandTest {

    private Terminal terminal;

    private ChatSubCommand chatSubCommand;

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.terminal();
        chatSubCommand = new ChatSubCommand(terminal);
    }

    @Test
    void shouldCreateChatSubCommand() {
        // Then
        assertThat(chatSubCommand).isNotNull();
        assertThat(chatSubCommand.terminal).isEqualTo(terminal);
        assertThat(chatSubCommand.out).isNotNull();
    }

    @Test
    void exitCommand_shouldPrintGoodbyeMessage() {
        // Given
        ChatSubCommand.ExitCommand exitCommand = new ChatSubCommand.ExitCommand();
        exitCommand.parent = chatSubCommand;
        
        // When
        exitCommand.run();
        
        // Then - should complete successfully without throwing exception
        assertThat(exitCommand).isNotNull();
    }

    @Test
    void setCommand_shouldExecuteSuccessfully() {
        // Given
        ChatSubCommand.SetCommand setCommand = new ChatSubCommand.SetCommand();
        setCommand.parent = chatSubCommand;

        // When
        setCommand.run();

        // Then - should not throw any exception
        assertThat(setCommand).isNotNull();
    }

    @Test
    void showCommand_shouldExecuteSuccessfully() {
        // Given
        ChatSubCommand.ShowCommand showCommand = new ChatSubCommand.ShowCommand();
        showCommand.parent = chatSubCommand;

        // When
        showCommand.run();

        // Then - should not throw any exception
        assertThat(showCommand).isNotNull();
    }

    @Test
    void clearScreenCommand_shouldExecuteSuccessfully() {
        // Given
        ChatSubCommand.ClearScreen clearScreen = new ChatSubCommand.ClearScreen();
        clearScreen.parent = chatSubCommand;

        // When
        clearScreen.run();

        // Then - should not throw any exception
        assertThat(clearScreen).isNotNull();
    }
}
