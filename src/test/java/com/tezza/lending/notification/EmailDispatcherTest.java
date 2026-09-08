package com.tezza.lending.notification;

import com.tezza.lending.notification.internal.dispatcher.EmailDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailDispatcherTest {

    @Mock JavaMailSender mailSender;
    @InjectMocks EmailDispatcher emailDispatcher;

    @Test
    void send_callsMailSenderWithCorrectFields() {
        emailDispatcher.send("alice@example.com", "Test Subject", "Test body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getTo()).containsExactly("alice@example.com");
        assertThat(sent.getSubject()).isEqualTo("Test Subject");
        assertThat(sent.getText()).isEqualTo("Test body");
        assertThat(sent.getFrom()).isEqualTo("noreply@tezza.co.ke");
    }

    @Test
    void send_nullSubject_usesDefaultSubject() {
        emailDispatcher.send("alice@example.com", null, "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("Tezza Lending Notification");
    }
}
